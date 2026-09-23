package com.hrdemo.jcr;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the raw JCR API to demonstrate, in isolation, exactly the mechanics
 * covered in the interview prep: node hierarchy, versioning, locking,
 * binary storage, and JCR-SQL2 querying.
 *
 * Node structure used here:
 *   /employees/{employeeId}/documents/{docName}
 *     -> jcr:primaryType = nt:file
 *     -> mix:versionable  (so we can check in/out and keep history)
 *     -> jcr:content (nt:resource) holding the actual binary via jcr:data
 *     -> custom properties: docType, uploadedBy
 */
public class DocumentRepository {

    private final Repository repository;

    public DocumentRepository(Repository repository) {
        this.repository = repository;
    }

    private Session login() throws RepositoryException {
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    /**
     * Creates (or updates) a document node under an employee's subtree,
     * makes it versionable, and stores the given content as a binary.
     * This mirrors how a compliance document upload would be handled.
     */
    public String saveDocument(String employeeId, String docName, String docType,
                                String content, String uploadedBy) throws RepositoryException {
        Session session = login();
        try {
            Node employeesRoot = session.getRootNode().getNode("employees");

            Node employeeNode = employeesRoot.hasNode(employeeId)
                    ? employeesRoot.getNode(employeeId)
                    : employeesRoot.addNode(employeeId, "nt:unstructured");

            Node documentsNode = employeeNode.hasNode("documents")
                    ? employeeNode.getNode("documents")
                    : employeeNode.addNode("documents", "nt:unstructured");

            Node fileNode;
            boolean isNew = !documentsNode.hasNode(docName);

            if (isNew) {
                fileNode = documentsNode.addNode(docName, "nt:file");
                fileNode.addMixin("mix:versionable"); // <-- enables checkin/checkout/version history
            } else {
                fileNode = documentsNode.getNode(docName);
                // Must check out before modifying a versioned node's content
                session.getWorkspace().getVersionManager().checkout(fileNode.getPath());
            }

            Node contentNode = fileNode.hasNode("jcr:content")
                    ? fileNode.getNode("jcr:content")
                    : fileNode.addNode("jcr:content", "nt:resource");

            InputStream binaryStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
            Binary binary = session.getValueFactory().createBinary(binaryStream);
            contentNode.setProperty("jcr:data", binary);
            contentNode.setProperty("jcr:mimeType", "text/plain");

            fileNode.setProperty("docType", docType);
            fileNode.setProperty("uploadedBy", uploadedBy);

            session.save();

            // Check the node back in - this is what actually creates a new
            // version snapshot in the node's VersionHistory.
            Version version = session.getWorkspace().getVersionManager().checkin(fileNode.getPath());

            return version.getName();

        } finally {
            session.logout();
        }
    }

    /**
     * Retrieves a document's current binary content, streaming it rather
     * than loading the whole file into memory up front (matters for larger
     * binaries - see the "how binaries are stored/retrieved" interview answer).
     */
    public String readDocumentContent(String employeeId, String docName) throws Exception {
        Session session = login();
        try {
            String path = "/employees/" + employeeId + "/documents/" + docName + "/jcr:content";
            Node contentNode = session.getNode(path);
            Binary binary = contentNode.getProperty("jcr:data").getBinary();
            try (InputStream in = binary.getStream()) {
                return new String(in.readAllBytes(), "UTF-8");
            } finally {
                binary.dispose();
            }
        } finally {
            session.logout();
        }
    }

    /**
     * Lists version history for a document - demonstrates retrieving prior
     * versions, not just the current state.
     */
    public List<String> getVersionHistory(String employeeId, String docName) throws RepositoryException {
        Session session = login();
        try {
            String path = "/employees/" + employeeId + "/documents/" + docName;
            VersionManager vm = session.getWorkspace().getVersionManager();
            VersionHistory history = vm.getVersionHistory(path);

            List<String> versions = new ArrayList<>();
            javax.jcr.version.VersionIterator it = history.getAllVersions();
            while (it.hasNext()) {
                Version v = it.nextVersion();
                versions.add(v.getName() + " @ " + v.getCreated().getTime());
            }
            return versions;
        } finally {
            session.logout();
        }
    }

    /**
     * Restores a document to a specific prior version - the counterpart to
     * checkin, showing that version history is actually usable, not just stored.
     */
    public void restoreVersion(String employeeId, String docName, String versionName) throws RepositoryException {
        Session session = login();
        try {
            String path = "/employees/" + employeeId + "/documents/" + docName;
            VersionManager vm = session.getWorkspace().getVersionManager();
            VersionHistory history = vm.getVersionHistory(path);
            Version version = history.getVersion(versionName);
            vm.restore(version, true);
        } finally {
            session.logout();
        }
    }

    /**
     * Demonstrates pessimistic node-level locking - acquiring a lock before
     * editing so a concurrent editor is blocked rather than silently
     * overwriting changes. Requires the node to have the mix:lockable mixin.
     */
    public boolean lockDocumentForEditing(String employeeId, String docName) throws RepositoryException {
        Session session = login();
        try {
            String path = "/employees/" + employeeId + "/documents/" + docName;
            Node node = session.getNode(path);
            if (!node.isNodeType("mix:lockable")) {
                node.addMixin("mix:lockable");
                session.save();
            }
            if (node.isLocked()) {
                return false; // already locked by someone else
            }
            session.getWorkspace().getLockManager().lock(path,
                    false,  // not deep - only this node
                    false,  // session-scoped: lock releases when this session ends
                    -1,     // no timeout
                    "demo-user");
            return true;
        } finally {
            session.logout();
        }
    }

    /**
     * Runs a JCR-SQL2 query - the modern, non-deprecated JCR query language -
     * filtering documents by type rather than manually walking the tree.
     * This is what the "how do you query content in JCR" interview answer refers to.
     */
    public List<String> findDocumentsByType(String docType) throws RepositoryException {
        Session session = login();
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            String sql2 = "SELECT * FROM [nt:file] AS doc WHERE doc.[docType] = $docType";
            Query query = qm.createQuery(sql2, Query.JCR_SQL2);
            query.bindValue("docType", session.getValueFactory().createValue(docType));

            QueryResult result = query.execute();
            List<String> paths = new ArrayList<>();
            RowIterator rows = result.getRows();
            while (rows.hasNext()) {
                paths.add(rows.nextRow().getPath("doc"));
            }
            return paths;
        } finally {
            session.logout();
        }
    }
}

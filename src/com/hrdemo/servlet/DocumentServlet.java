package com.hrdemo.servlet;

import com.hrdemo.jcr.DocumentRepository;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Bridges HTTP requests to the JCR-backed DocumentRepository.
 *
 * This is the servlet that ties together everything from the JCR/Jackrabbit
 * interview prep in one runnable flow: save -> versions -> restore, plus
 * a JCR-SQL2 query by document type.
 *
 * Supported actions (via ?action= param, kept simple/RESTful-ish for demo purposes):
 *   POST /document?action=save&employeeId=E001&docName=cert.txt&docType=certification&content=...
 *   GET  /document?action=read&employeeId=E001&docName=cert.txt
 *   GET  /document?action=history&employeeId=E001&docName=cert.txt
 *   POST /document?action=restore&employeeId=E001&docName=cert.txt&version=1.1
 *   GET  /document?action=search&docType=certification
 */
public class DocumentServlet extends HttpServlet {

    private DocumentRepository documentRepository;

    @Override
    public void init() {
        Repository repo = (Repository) getServletContext().getAttribute("jcrRepository");
        this.documentRepository = new DocumentRepository(repo);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        String action = req.getParameter("action");

        try {
            switch (action == null ? "" : action) {
                case "read": {
                    String content = documentRepository.readDocumentContent(
                            req.getParameter("employeeId"), req.getParameter("docName"));
                    resp.getWriter().write("{\"content\":\"" + escape(content) + "\"}");
                    break;
                }
                case "history": {
                    List<String> versions = documentRepository.getVersionHistory(
                            req.getParameter("employeeId"), req.getParameter("docName"));
                    resp.getWriter().write(toJsonArray(versions));
                    break;
                }
                case "search": {
                    List<String> paths = documentRepository.findDocumentsByType(req.getParameter("docType"));
                    resp.getWriter().write(toJsonArray(paths));
                    break;
                }
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"unknown or missing action\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        String action = req.getParameter("action");

        try {
            switch (action == null ? "" : action) {
                case "save": {
                    String versionName = documentRepository.saveDocument(
                            req.getParameter("employeeId"),
                            req.getParameter("docName"),
                            req.getParameter("docType"),
                            req.getParameter("content"),
                            req.getParameter("uploadedBy"));
                    resp.getWriter().write("{\"savedVersion\":\"" + versionName + "\"}");
                    break;
                }
                case "restore": {
                    documentRepository.restoreVersion(
                            req.getParameter("employeeId"),
                            req.getParameter("docName"),
                            req.getParameter("version"));
                    resp.getWriter().write("{\"restored\":true}");
                    break;
                }
                case "lock": {
                    boolean locked = documentRepository.lockDocumentForEditing(
                            req.getParameter("employeeId"), req.getParameter("docName"));
                    resp.getWriter().write("{\"locked\":" + locked + "}");
                    break;
                }
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"unknown or missing action\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        // Prevent user content and repository messages from breaking the JSON
        // response. Production code should serialize a response DTO with
        // Jackson or JSON-B instead of building JSON manually.
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(items.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }
}

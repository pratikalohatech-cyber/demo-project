package com.hrdemo.jcr;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * Starts the Jackrabbit repository when the web app starts, and shuts it
 * down cleanly when the app is undeployed - this is the JCR equivalent of
 * opening/closing a connection pool at application lifecycle boundaries.
 *
 * TransientRepository is Jackrabbit's simplest repository implementation -
 * good for demos/dev. A real deployment would use a persistent, configured
 * repository (repository.xml) pointing at real storage, not this default.
 */
public class RepositoryStartupListener implements ServletContextListener {

    public static Repository repository;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // "repository-home" is where Jackrabbit stores its data on disk
            repository = new TransientRepository("repository-home");

            // Bootstrap the node structure we need: an "employees" root node
            // under which each employee gets a subtree - the "subtree-per-entity"
            // isolation pattern discussed in the JCR interview prep.
            Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            try {
                if (!session.getRootNode().hasNode("employees")) {
                    session.getRootNode().addNode("employees", "nt:folder");
                    session.save();
                    System.out.println("[JCR] Bootstrapped root 'employees' node");
                }
            } finally {
                session.logout();
            }

            sce.getServletContext().setAttribute("jcrRepository", repository);
            System.out.println("[JCR] Jackrabbit repository started");

        } catch (Exception e) {
            throw new RuntimeException("Failed to start JCR repository", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (repository instanceof TransientRepository) {
            ((TransientRepository) repository).shutdown();
            System.out.println("[JCR] Jackrabbit repository shut down");
        }
    }
}

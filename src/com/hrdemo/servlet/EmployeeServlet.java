package com.hrdemo.servlet;

import com.hrdemo.model.Employee;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A plain Servlet demonstrating the classic lifecycle: init() once at
 * startup, service() (routed internally to doGet/doPost) per request,
 * destroy() on shutdown.
 *
 * In-memory map stands in for what would be Hibernate/PostgreSQL in the
 * real project - kept simple here since this demo's focus is the
 * Servlet/Filter/JCR wiring, not a full persistence layer.
 */
public class EmployeeServlet extends HttpServlet {

    // NOTE: a single servlet instance handles MANY concurrent requests.
    // Mutable instance state like this map must be thread-safe -
    // that's exactly the thread-safety concern covered in the interview prep.
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // One-time setup - seed some demo data.
        employees.put("E001", new Employee("E001", "Asha Rao", "Engineering"));
        employees.put("E002", new Employee("E002", "Vikram Nair", "HR"));
        System.out.println("[EmployeeServlet] initialized with " + employees.size() + " seed employees");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET flow:
        // - user can request all employees or a single employee by id
        // - the servlet responds with JSON so the browser or curl can consume it
        resp.setContentType("application/json");
        String id = req.getParameter("id");

        if (id != null) {
            Employee e = employees.get(id);
            if (e == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"employee not found\"}");
                return;
            }
            resp.getWriter().write(e.toJson());
        } else {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Employee e : employees.values()) {
                if (!first) sb.append(",");
                sb.append(e.toJson());
                first = false;
            }
            sb.append("]");
            resp.getWriter().write(sb.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // POST flow:
        // - form parameters are sent as id/name/department
        // - the data is written into the shared in-memory map
        // - a synchronized block protects the shared state from concurrent requests
        String id = req.getParameter("id");
        String name = req.getParameter("name");
        String department = req.getParameter("department");

        if (id == null || name == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"id and name are required\"}");
            return;
        }

        synchronized (employees) {
            employees.put(id, new Employee(id, name, department));
        }

        resp.setContentType("application/json");
        resp.getWriter().write(employees.get(id).toJson());
    }

    @Override
    public void destroy() {
        System.out.println("[EmployeeServlet] destroyed");
    }
}

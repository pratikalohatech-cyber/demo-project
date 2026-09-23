package com.hrdemo.model;

/**
 * Plain data model for structured employee data.
 * In the real HR project this maps to a Hibernate entity backed by PostgreSQL -
 * left as a plain POJO here since this demo focuses on the Servlet/JCR wiring,
 * not a full Hibernate setup (that would need a running database).
 */
public class Employee {

    private String id;
    private String name;
    private String department;

    public Employee() {
    }

    public Employee(String id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String toJson() {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"department\":\"%s\"}",
                escapeJson(id), escapeJson(name), escapeJson(department));
    }

    // Keep JSON valid when user-provided values contain quotes, slashes, or
    // line breaks. A production application should use Jackson or JSON-B
    // instead of maintaining a serializer by hand.
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

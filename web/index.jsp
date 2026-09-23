<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>HR Demo - Legacy Java Stack + JCR</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <h1>HR Demo App</h1>
    <p>Demonstrates: Servlets, Filters, JSP, Ant/Tomcat deployment, and Apache Jackrabbit (JCR)
       for content storage/versioning, plus Fabric.js + GSAP for the shift-planning canvas.</p>

    <h2>Try it</h2>
    <ul>
        <li><a href="shift-planner.jsp">Shift Planner (Fabric.js + GSAP canvas demo)</a></li>
        <li><code>GET /employee</code> - list employees (plain servlet, in-memory data)</li>
        <li><code>GET /employee?id=E001</code> - get one employee</li>
        <li><code>POST /employee</code> - create/update an employee (form params: id, name, department)</li>
        <li>
            <code>/document</code> endpoints require header <code>X-Demo-Auth-Token: demo-secret-token</code>
            (see AuthFilter) -
            <code>POST ?action=save&amp;employeeId=E001&amp;docName=cert.txt&amp;docType=certification&amp;content=...&amp;uploadedBy=admin</code>,
            <code>GET ?action=read&amp;employeeId=E001&amp;docName=cert.txt</code>,
            <code>GET ?action=history&amp;employeeId=E001&amp;docName=cert.txt</code>,
            <code>POST ?action=restore&amp;employeeId=E001&amp;docName=cert.txt&amp;version=1.1</code>,
            <code>GET ?action=search&amp;docType=certification</code>
        </li>
    </ul>

    <p><em>Server time: <%= new java.util.Date() %></em>
    <br>(This scriptlet is here deliberately as an example of the old-style JSP pattern
    mentioned in the interview prep as a maintenance pain point - avoid this style in real code.)</p>
</body>
</html>

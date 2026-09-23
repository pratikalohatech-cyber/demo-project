<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head><title>Error</title></head>
<body>
    <h1>Something went wrong</h1>
    <p><%= exception != null ? exception.getMessage() : "Unknown error" %></p>
</body>
</html>

<%@ page import="java.io.*, java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
Integer userId = (Integer) request.getSession().getAttribute("userId");
    if (userId == null || userId == 0) {
        response.sendRedirect("/work-ai/login.jsp");
        return;
    }
    boolean isAdmin = (Boolean) request.getSession().getAttribute("isAdmin");
    
    String baseDir = isAdmin ?  application.getRealPath("/uploads/") : application.getRealPath("/uploads/" + userId); // Folder inside webapp
    String fileParam = request.getParameter("file");
    String error = null;
    String content = "";

    if (fileParam != null) {
        if (fileParam.contains("..")) {
            error = "Access denied.";
        } else {
            File file = new File(baseDir, fileParam);
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    content = sb.toString();
                } catch (IOException e) {
                    error = "Error reading file.";
                }
            } else {
                error = "File not found.";
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>File Viewer</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container mt-5">
    <h1 class="mb-4">File Viewer</h1>

    <form method="get" class="mb-3">
        <div class="input-group">
            <input type="text" name="file" class="form-control" placeholder="Enter filename (e.g., test.txt)" required>
            <button type="submit" class="btn btn-primary">View File</button>
        </div>
    </form>

    <% if (error != null) { %>
        <div class="alert alert-danger"><%= error %></div>
    <% } else if (fileParam != null) { %>
        <div class="card">
            <div class="card-header">
                Viewing: <strong><%= fileParam %></strong>
            </div>
            <div class="card-body">
                <textarea class="form-control" rows="15" readonly><%= content %></textarea>
            </div>
        </div>
    <% } %>
</div>
</body>
</html>

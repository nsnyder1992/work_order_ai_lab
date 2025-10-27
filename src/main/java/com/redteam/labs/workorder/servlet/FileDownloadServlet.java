package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.DatabaseUtil;

@WebServlet("/jsp/file-download")
public class FileDownloadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        String documentIdStr = req.getParameter("docId");
        if (documentIdStr == null || documentIdStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Document ID is required.");
            return;
        }

        try {
            int documentId = Integer.parseInt(documentIdStr);
            
            // Get document info from database and verify user has access
            String sql = "SELECT d.path, d.document_name, d.type, wo.user_id " +
                        "FROM documents d " +
                        "JOIN work_orders wo ON d.work_order_id = wo.id " +
                        "WHERE d.id = ?";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, documentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found.");
                        return;
                    }
                    
                    String filePath = rs.getString("path");
                    String fileName = rs.getString("document_name");
                    String docType = rs.getString("type");
                    int workOrderUserId = rs.getInt("user_id");
                    
                    // Security check: user can only download their own documents or admin can download all
                    if (!user.getRole().equals("admin") && user.getId() != workOrderUserId) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                        return;
                    }
                    
                    // Additional security check for compliance documents
                    if ("compliance".equals(docType) && !user.getRole().equals("admin")) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to compliance documents.");
                        return;
                    }
                    
                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on disk.");
                        return;
                    }
                    
                    // Set content type based on file extension
                    Path path = Paths.get(filePath);
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    
                    resp.setContentType(contentType);
                    resp.setContentLength((int) file.length());
                    resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    
                    // Stream the file
                    try (FileInputStream fis = new FileInputStream(file);
                         OutputStream out = resp.getOutputStream()) {
                        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid document ID.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }
}
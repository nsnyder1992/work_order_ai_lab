package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.DatabaseUtil;

@WebServlet("/jsp/file-viewer")
public class FileViewerServlet extends HttpServlet {
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
            String sql = "SELECT d.path, d.document_name, d.type, wo.user_id, wo.number, wo.title " +
                        "FROM documents d " +
                        "JOIN work_orders wo ON d.work_order_id = wo.id " +
                        "WHERE d.id = ?";
            
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, documentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        req.setAttribute("error", "Document not found.");
                        req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
                        return;
                    }
                    
                    String filePath = rs.getString("path");
                    String fileName = rs.getString("document_name");
                    String docType = rs.getString("type");
                    int workOrderUserId = rs.getInt("user_id");
                    String workOrderNumber = rs.getString("number");
                    String workOrderTitle = rs.getString("title");
                    
                    // Security check: user can only view their own documents or admin can view all
                    if (!user.getRole().equals("admin") && user.getId() != workOrderUserId) {
                        req.setAttribute("error", "Access denied.");
                        req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
                        return;
                    }
                    
                    // Additional security check for compliance documents
                    if ("compliance".equals(docType) && !user.getRole().equals("admin")) {
                        req.setAttribute("error", "Access denied to compliance documents.");
                        req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
                        return;
                    }
                    
                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        req.setAttribute("error", "File not found on disk.");
                        req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
                        return;
                    }
                    
                    // Set content type based on file extension
                    Path path = Paths.get(filePath);
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    
                    // Prepare attributes for JSP
                    req.setAttribute("fileName", fileName);
                    req.setAttribute("contentType", contentType);
                    req.setAttribute("fileSize", file.length());
                    req.setAttribute("docType", docType);
                    req.setAttribute("workOrderNumber", workOrderNumber);
                    req.setAttribute("workOrderTitle", workOrderTitle);
                    req.setAttribute("documentId", documentId);
                    
                    // For images and text files, we can embed them directly
                    if (contentType.startsWith("image/") || contentType.startsWith("text/") || 
                        contentType.equals("application/pdf") || contentType.equals("application/zip") || 
                        fileName.toLowerCase().endsWith(".zip")) {
                        req.setAttribute("canPreview", true);
                        if (contentType.startsWith("image/")) {
                            // Convert image to base64 for embedding
                            byte[] fileBytes = Files.readAllBytes(path);
                            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                            req.setAttribute("imageData", "data:" + contentType + ";base64," + base64Image);
                        } else if (contentType.startsWith("text/") && file.length() < 1024 * 1024) { // Max 1MB for text preview
                            String textContent = new String(Files.readAllBytes(path), "UTF-8");
                            req.setAttribute("textContent", textContent);
                        } else if (contentType.equals("application/zip") || fileName.toLowerCase().endsWith(".zip")) {
                            req.setAttribute("isZipFile", true);
                        }
                    } else {
                        req.setAttribute("canPreview", false);
                    }
                    
                    // Forward to JSP
                    req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
                }
            }
            
        } catch (NumberFormatException e) {
            req.setAttribute("error", "Invalid document ID.");
            req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "Database error: " + e.getMessage());
            req.getRequestDispatcher("file-viewer.jsp").forward(req, resp);
        }
    }
}
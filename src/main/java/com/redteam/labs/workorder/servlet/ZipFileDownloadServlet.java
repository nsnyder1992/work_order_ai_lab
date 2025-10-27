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

@WebServlet("/jsp/zip-file-download")
public class ZipFileDownloadServlet extends HttpServlet {
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
        String filePath = req.getParameter("file");
        
        if (documentIdStr == null || documentIdStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Document ID is required.");
            return;
        }
        
        if (filePath == null || filePath.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File path is required.");
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
                    
                    String originalZipPath = rs.getString("path");
                    String docType = rs.getString("type");
                    int workOrderUserId = rs.getInt("user_id");
                    
                    // Security check: user can only access their own documents or admin can access all
                    if (!user.getRole().equals("admin") && user.getId() != workOrderUserId) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                        return;
                    }
                    
                    // Additional security check for compliance documents
                    if ("compliance".equals(docType) && !user.getRole().equals("admin")) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to compliance documents.");
                        return;
                    }
                    
                    // Verify the original ZIP file exists
                    File originalZipFile = new File(originalZipPath);
                    if (!originalZipFile.exists() || !originalZipFile.isFile()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Original ZIP file not found.");
                        return;
                    }
                    
                    // Security check: prevent directory traversal
                    if (filePath.contains("..") || filePath.startsWith("/") || filePath.contains("\\")) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path.");
                        return;
                    }
                    
                    // Get the extracted file path
                    String extractDir = getExtractedZipDirectory(documentId);
                    File extractedFile = new File(extractDir, filePath);
                    
                    // Verify the extracted file exists and is within the extraction directory
                    if (!extractedFile.exists() || !extractedFile.isFile()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Extracted file not found. The ZIP may need to be re-extracted.");
                        return;
                    }
                    
                    // Security check: ensure the file is within the extraction directory
                    String canonicalExtractDir = new File(extractDir).getCanonicalPath();
                    String canonicalFilePath = extractedFile.getCanonicalPath();
                    if (!canonicalFilePath.startsWith(canonicalExtractDir)) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to file outside extraction directory.");
                        return;
                    }
                    
                    // Set content type and headers for download
                    Path path = Paths.get(extractedFile.getAbsolutePath());
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    
                    resp.setContentType(contentType);
                    resp.setContentLengthLong(extractedFile.length());
                    
                    // Set filename for download
                    String fileName = extractedFile.getName();
                    resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    
                    // Stream the file to the response
                    try (FileInputStream fis = new FileInputStream(extractedFile);
                         OutputStream os = resp.getOutputStream()) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid document ID.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
    
    private String getExtractedZipDirectory(int documentId) {
        String tempDir = System.getProperty("java.io.tmpdir");
        return Paths.get(tempDir, "zip_extracts", "doc_" + documentId).toString();
    }
}
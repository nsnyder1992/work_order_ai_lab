package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.Gson;
import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.DatabaseUtil;

@WebServlet("/jsp/zip-extractor")
public class ZipExtractorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    public static class ZipFileInfo {
        private String name;
        private long size;
        private boolean isDirectory;
        private String relativePath;
        
        public ZipFileInfo(String name, long size, boolean isDirectory, String relativePath) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
            this.relativePath = relativePath;
        }
        
        // Getters
        public String getName() { return name; }
        public long getSize() { return size; }
        public boolean isDirectory() { return isDirectory; }
        public String getRelativePath() { return relativePath; }
    }

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
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found.");
                        return;
                    }
                    
                    String filePath = rs.getString("path");
                    String fileName = rs.getString("document_name");
                    String docType = rs.getString("type");
                    int workOrderUserId = rs.getInt("user_id");
                    
                    // Security check: user can only view their own documents or admin can view all
                    if (!user.getRole().equals("admin") && user.getId() != workOrderUserId) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                        return;
                    }
                    
                    // Additional security check for compliance documents
                    if ("compliance".equals(docType) && !user.getRole().equals("admin")) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to compliance documents.");
                        return;
                    }
                    
                    File zipFile = new File(filePath);
                    if (!zipFile.exists() || !zipFile.isFile()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on disk.");
                        return;
                    }
                    
                    // Verify it's a ZIP file
                    Path path = Paths.get(filePath);
                    String contentType = Files.probeContentType(path);
                    if (!"application/zip".equals(contentType) && !fileName.toLowerCase().endsWith(".zip")) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File is not a ZIP archive.");
                        return;
                    }
                    
                    // Extract ZIP contents to a temporary directory
                    String extractDir = getExtractedZipDirectory(documentId);
                    List<ZipFileInfo> zipContents = extractZipFile(zipFile, extractDir);
                    
                    // Return JSON response with file list
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    
                    Gson gson = new Gson();
                    resp.getWriter().write(gson.toJson(zipContents));
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
        return Paths.get(tempDir, "zip_extracts", String.valueOf(documentId)).toString();
    }
    
    private List<ZipFileInfo> extractZipFile(File zipFile, String extractDir) throws IOException {
        List<ZipFileInfo> fileList = new ArrayList<>();
        
        // Create extraction directory
        File extractDirFile = new File(extractDir);
        if (!extractDirFile.exists()) {
            extractDirFile.mkdirs();
        }
        
        // Clear existing extracted files
        System.out.println("Extracting to: " + extractDirFile.getAbsolutePath());
        deleteDirectory(extractDirFile);
        extractDirFile.mkdirs();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Security check: prevent directory traversal
                if (entryName.contains("..") || entryName.startsWith("/")) {
                    continue;
                }
                
                File entryFile = new File(extractDir, entryName);
                
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    fileList.add(new ZipFileInfo(
                        getFileNameFromPath(entryName),
                        0,
                        true,
                        entryName
                    ));
                } else {
                    // Create parent directories if needed
                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    
                    fileList.add(new ZipFileInfo(
                        getFileNameFromPath(entryName),
                        entry.getSize() > 0 ? entry.getSize() : entryFile.length(),
                        false,
                        entryName
                    ));
                }
                zis.closeEntry();
            }
        }
        
        return fileList;
    }
    
    private String getFileNameFromPath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
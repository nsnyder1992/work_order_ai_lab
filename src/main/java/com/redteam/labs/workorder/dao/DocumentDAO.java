package com.redteam.labs.workorder.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.redteam.labs.workorder.model.Document;
import com.redteam.labs.workorder.util.DatabaseUtil;
import com.redteam.labs.workorder.util.FileUploadScanner;
import com.redteam.labs.workorder.util.FileValidationUtil;

public class DocumentDAO {
    
    public static void saveDocument(Integer id, String uploadPath, String filename, String doctype, InputStream is) {
        
        if (doctype == null || doctype.isEmpty()) {
            doctype = "normal"; // Default type if not specified
        }
        
        Path uploadDir = Paths.get(uploadPath);
        Path upload = Paths.get(uploadPath, filename);
        
        if (!upload.toAbsolutePath().startsWith(uploadDir.toAbsolutePath())) {
            throw new SecurityException("Invalid upload path");
        }

        if (upload.toFile().exists()) {
            upload.toFile().delete(); // Delete existing file if it exists
        }
        
        Document doc = new Document();
        doc.setWorkOrderId(id);
        doc.setDocumentName(filename);
        doc.setPath(upload.toFile().getAbsolutePath()); 
        doc.setType(doctype);
        doc.setUploadedAt(new Timestamp(System.currentTimeMillis()).toString());
        
        try {
            insertDocument(doc);
            // Create parent directories if they don't exist
            java.nio.file.Files.createDirectories(upload.getParent());
            // Save the file to the filesystem
            java.nio.file.Files.copy(is, upload);
            
            if (FileUploadScanner.isMaliciousFile(upload.toFile())) {
                throw new RuntimeException("Malicious file detected: " + filename);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save document", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save file to disk", e);
        }
        
        if (doctype.equals("compliance"))
            return;
        
        try {
            FileValidationUtil.validateFileExtension(upload.toFile().getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save document", e);
        }
    }

    public static void insertDocument(Document doc) throws SQLException {
        String sql = "INSERT INTO documents (work_order_id, document_name, path, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doc.getWorkOrderId());
            stmt.setString(2, doc.getDocumentName());
            stmt.setString(3, doc.getPath());
            stmt.setString(4, doc.getType());
            stmt.executeUpdate();
        }
    }

    public static List<Document> getDocumentsByWorkOrder(Connection conn, int workOrderId) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE work_order_id = ? AND type = 'normal'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Document doc = new Document();
                    doc.setId(rs.getInt("id"));
                    doc.setWorkOrderId(rs.getInt("work_order_id"));
                    doc.setDocumentName(rs.getString("document_name"));
                    doc.setPath(rs.getString("path"));
                    doc.setType(rs.getString("type"));
                    doc.setUploadedAt(rs.getString("uploaded_at"));
                    documents.add(doc);
                }
            }
        }
        return documents;
    }
    
    
    public static List<Document> getComplianceDocumentsByWorkOrder(Connection conn, int workOrderId) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE work_order_id = ? AND type = 'compliance'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Document doc = new Document();
                    doc.setId(rs.getInt("id"));
                    doc.setWorkOrderId(rs.getInt("work_order_id"));
                    doc.setDocumentName(rs.getString("document_name"));
                    doc.setPath(rs.getString("path"));
                    doc.setType(rs.getString("type"));
                    doc.setUploadedAt(rs.getString("uploaded_at"));
                    documents.add(doc);
                }
            }
        }
        return documents;
    }

}
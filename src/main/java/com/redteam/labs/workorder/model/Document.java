package com.redteam.labs.workorder.model;

public class Document {
    private int id;
    private int workOrderId;
    private String documentName;
    private String path;
    private String type; // "normal" or "compliance"
    private String uploadedAt;

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(int workOrderId) { this.workOrderId = workOrderId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}

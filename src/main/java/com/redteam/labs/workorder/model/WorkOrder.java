package com.redteam.labs.workorder.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkOrder {
    private int id;
    private int userId;  // owner of the work order
    private String number; // unique identifier for the work order
    private String title;
    private String description;
    private String status; // \"in progress\" or \"complete\"
    private LocalDateTime createdAt;
    private String solution; // solution provided for the work order
    private int quote; // estimated cost
    private int finalCost; // final cost after completion

    private List<Document> documents; // list of documents associated with the work order
    private List<Document> complianceDocuments; // list of compliance documents
    
    public WorkOrder() {
        // Default constructor
    }
    
    public WorkOrder(int id, int userId, String number, String title, String description, String status, LocalDateTime createdAt, String solution, int quote, int finalCost) {
        this.id = id;
        this.userId = userId;
        this.number = number;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.solution = solution;
        this.quote = quote;
        this.finalCost = finalCost;
    }

    public void addDocument(Document document) {
        if (documents == null) {
            documents = new ArrayList<>();
        }
        documents.add(document);
    }
    
    public void addComplianceDocument(Document document) {
        if (complianceDocuments == null) {
            complianceDocuments = new ArrayList<>();
        }
        complianceDocuments.add(document);
    }
    
    
    // Getters and setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
        
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public int getQuote() { return quote; }
    public void setQuote(int quote) { this.quote = quote; }

    public int getFinalCost() { return finalCost; }
    public void setFinalCost(int finalCost) { this.finalCost = finalCost; }
    
    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
    
    public List<Document> getComplianceDocuments() { return complianceDocuments; }
    public void setComplianceDocuments(List<Document> complianceDocuments) { 
        this.complianceDocuments = complianceDocuments; 
    }
}
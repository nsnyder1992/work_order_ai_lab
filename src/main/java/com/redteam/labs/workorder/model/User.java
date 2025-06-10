package com.redteam.labs.workorder.model;

public class User
{
    private int id;
    private String username;
    private String passwordHash;  // MD5 hash
    private String role; // Assuming a role field for user permissions

    public User() {
        // Default constructor
    }
    
    public User(int id, String username, String passwordHash, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role; // Default role, can be changed later
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role;}

}

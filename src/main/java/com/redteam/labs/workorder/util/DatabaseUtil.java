package com.redteam.labs.workorder.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil
{
    static final String DB_FILE = "database/workorder.db";
    static final String DB_URL = "jdbc:sqlite:" + (System.getenv("AI_APP_PROD") == "true" ? DB_FILE : "C:\\Users\\Nicholas.Snyder\\OneDrive - Fortive\\Desktop\\htb\\ai_prompt_lab\\" + DB_FILE);
    
    static Connection conn;
    
    private static boolean isInitialized = false;
    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            try {
                Class.forName("org.sqlite.JDBC");
                File dbFile = System.getenv("AI_APP_PROD") == "true" ? new File(DatabaseUtil.DB_FILE) : new File("C:\\Users\\Nicholas.Snyder\\OneDrive - Fortive\\Desktop\\htb\\ai_prompt_lab\\" + DatabaseUtil.DB_FILE);
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs(); // Create directories if they don't exist
                }
                else {
                    dbFile.delete(); // Delete the existing file if it exists
                }
                isInitialized = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found", e);
            }
        }
        if (conn != null && !conn.isClosed()) {
            return conn; // Return existing connection if it's open
        }
        conn = DriverManager.getConnection(DB_URL);
        return conn;
    }

}

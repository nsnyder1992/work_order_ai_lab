package com.redteam.labs.workorder.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseInit {

    private static final String SCHEMA_SQL =
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL UNIQUE, " +
            "password TEXT NOT NULL," + 
            "role TEXT CHECK(role IN ('admin', 'user')) NOT NULL DEFAULT 'user'" +
            "); " +
            "CREATE TABLE IF NOT EXISTS work_orders (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "number INTEGER NOT NULL, " +
            "title TEXT NOT NULL, " +
            "description TEXT NOT NULL, " +
            "status TEXT NOT NULL CHECK (status IN ('in progress', 'complete')), " + 
            "quote INTEGER,"+ 
            "final_cost INTEGER,"+ 
            "solution TEXT," +
            "created_at TEXT NOT NULL, " +
            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
            ");"
            + "CREATE TABLE documents ("
            + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "    work_order_id INTEGER NOT NULL,"
            + "    document_name TEXT NOT NULL,"
            + "    path TEXT NOT NULL,"
            + "    type TEXT CHECK (type IN ('normal', 'compliance')) NOT NULL DEFAULT 'normal',"
            + "    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "    FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE"
            + ");"
            + "\r\n-- Insert initial admin user\r\n"
            + "INSERT OR IGNORE INTO users (username, password, role)"
            + "VALUES ("
            + "    'jsmith',"
            + "    '%s',"
            + "    'admin'"
            + ");\r\n"
            + "-- Insert some fake work orders for jsmith (user_id = 1)\r\n"
            + "INSERT INTO work_orders (\r\n"
            + "    user_id, number, title, description, status,\r\n"
            + "    created_at, solution, quote, final_cost\r\n"
            + ") VALUES\r\n"
            + "(1, 'WO-1001', 'Replace coolant valve', 'Coolant valve is leaking in Pump Room 3', 'in progress',\r\n"
            + " '2025-06-03T12:54:45.391', NULL, 350.00, NULL),\r\n"
            + "\r\n"
            + "(1, 'WO-1002', 'Inspect pressure sensor', 'Pressure readings are inconsistent in Zone A', 'complete',\r\n"
            + " '2025-06-02T10:20:25.333', 'Recalibrated pressure sensor using factory settings.', 120.00, 130.00),\r\n"
            + "\r\n"
            + "(1, 'WO-1003', 'Upgrade PLC firmware', 'Request to upgrade firmware to v5.1 on Line B. Use password: %s to login', 'in progress',\r\n"
            + " '2025-06-01T20:34:36.698', NULL, 500.00, NULL);";

    public static void initialize() throws IOException { 
        String adminPasswordHash = BCrypt.hashpw("J3nnsk1p!", BCrypt.gensalt());
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(String.format(SCHEMA_SQL, adminPasswordHash, adminPasswordHash));
            System.out.println("Database schema created or already exists.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
    
    
}

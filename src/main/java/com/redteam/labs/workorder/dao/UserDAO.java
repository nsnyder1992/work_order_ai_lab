package com.redteam.labs.workorder.dao;

import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.DatabaseUtil;

import java.sql.*;

import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    private static final String DB_URL = "jdbc:sqlite:database/workorder.db";

    // Find user by username & password hash (login)
    public static User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (BCrypt.checkpw(password, storedHash)) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setUsername(rs.getString("username"));
                        user.setRole(rs.getString("role")); // Assuming User has a role field
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            stmt.setString(2, hashed);
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Additional helper methods if needed (e.g., user exists)
}
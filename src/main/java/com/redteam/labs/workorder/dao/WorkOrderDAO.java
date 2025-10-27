package com.redteam.labs.workorder.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.redteam.labs.workorder.model.Document;
import com.redteam.labs.workorder.model.WorkOrder;
import com.redteam.labs.workorder.util.DatabaseUtil;

public class WorkOrderDAO {
    
    public static WorkOrder getWorkOrderByNumber(String number) {
        WorkOrder order = null;
        String sql = "SELECT * FROM work_orders WHERE number = ?";
        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, number);
            try(ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    order = new WorkOrder(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("status"),
                            LocalDateTime.parse(rs.getString("created_at")),
                            rs.getString("solution"),
                            rs.getInt("quote"),
                            rs.getInt("final_cost")
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return order;
    }
    
    public static boolean createWorkOrder(Connection conn, WorkOrder order) {
        String sql = "INSERT INTO work_orders(user_id, number, title, description, status, created_at, solution, quote, final_cost) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, order.getUserId());
            stmt.setString(2, order.getNumber());
            stmt.setString(3, order.getTitle());
            stmt.setString(4, order.getDescription());
            stmt.setString(5, order.getStatus());
            stmt.setString(6, order.getCreatedAt().toString());
            stmt.setString(7, order.getSolution());
            stmt.setInt(8, order.getQuote());
            stmt.setInt(9, order.getFinalCost());
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Create a new work order
    public static boolean createWorkOrder(WorkOrder order) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            return createWorkOrder(conn, order);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get all work orders for a user
    public static List<WorkOrder> getWorkOrdersByUserId(int userId) {
        List<WorkOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM work_orders WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try(ResultSet rs = stmt.executeQuery())
            {
                while (rs.next()) {
                    WorkOrder o = new WorkOrder(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("status"),
                            LocalDateTime.parse(rs.getString("created_at")),
                            rs.getString("solution"),
                            rs.getInt("quote"),
                            rs.getInt("final_cost")
                    );
                    
                    List<Document> documents = DocumentDAO.getDocumentsByWorkOrder(conn, o.getId());
                    List<Document> complianceDocuments = DocumentDAO.getComplianceDocumentsByWorkOrder(conn, o.getId());  
                    o.setDocuments(documents);
                    o.setComplianceDocuments(complianceDocuments);
                    orders.add(o);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
    

    // Get all work orders (admin)
    public static List<WorkOrder> getAllWorkOrders() {
        List<WorkOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM work_orders ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            try(ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    WorkOrder o = new WorkOrder(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("status"),
                            LocalDateTime.parse(rs.getString("created_at")),
                            rs.getString("solution"),
                            rs.getInt("quote"),
                            rs.getInt("final_cost")
                    );
                    
                    // Fetch documents for this work order
                    List<Document> documents = DocumentDAO.getDocumentsByWorkOrder(conn, o.getId());
                    List<Document> complianceDocuments = DocumentDAO.getComplianceDocumentsByWorkOrder(conn, o.getId());  
                    o.setDocuments(documents);
                    o.setComplianceDocuments(complianceDocuments);
                    orders.add(o);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public static String getWorkOrdersAsPromptText(int userId) {
        String sql = "SELECT * FROM work_orders WHERE user_id = 1 ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            try(ResultSet rs = stmt.executeQuery()){
                return serializeWorkOrders(rs, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public static String getWorkOrdersByUserIdAsPromptText(int userId) {
        String sql = "SELECT * FROM work_orders WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try(ResultSet rs = stmt.executeQuery()){
                return serializeWorkOrders(rs, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private static String serializeWorkOrders(ResultSet rs, Integer userId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            sb.append("- Title: ").append(rs.getString("title"))
            .append(" | Status: ").append(rs.getString("status"))
            .append(" | Description: ").append(rs.getString("description"))
            .append(" | Solution: ").append(rs.getString("solution") == null ? "" : rs.getString("solution"))
            .append(" | Quote: ").append(rs.getString("quote") == null ? "" : String.format("%.2f", rs.getDouble("quote")))
            .append(" | Final Cost: ").append(rs.getString("final_cost") == null ? "" : String.format("%.2f", rs.getDouble("final_cost")))
            .append("\n");
        }
        
        if (sb.length() == 0 && userId != null) {
            sb.append("No work orders for user id: " + userId + ".\n");
        } else if (sb.length() == 0) {
            sb.append("No System work orders found.\n");
        }
        return sb.toString();
    }

}

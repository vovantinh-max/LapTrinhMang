package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {

    public static class LogEntry {
        private int id;
        private String username;
        private String clientIp;
        private String action;
        private String details;
        private String timestamp;

        public LogEntry(int id, String username, String clientIp, String action, String details, String timestamp) {
            this.id = id;
            this.username = username;
            this.clientIp = clientIp;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getClientIp() { return clientIp; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
        public String getTimestamp() { return timestamp; }
    }

    public static void log(String username, String clientIp, String action, String details) {
        String sql = "INSERT INTO activity_logs (username, client_ip, action, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, clientIp);
            pstmt.setString(3, action);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi ghi log: " + e.getMessage());
        }
    }

    public static List<LogEntry> getRecentLogs(int limit) {
        List<LogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new LogEntry(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("client_ip"),
                    rs.getString("action"),
                    rs.getString("details"),
                    rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi lấy logs: " + e.getMessage());
        }
        return list;
    }

    public static void clearLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[LogDAO] Lỗi xóa logs: " + e.getMessage());
        }
    }
}

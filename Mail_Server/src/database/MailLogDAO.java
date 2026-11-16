package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MailLogDAO {

    public static class MailLogEntry {
        private int id;
        private String clientIp;
        private String protocol;
        private String action;
        private String details;
        private String timestamp;

        public MailLogEntry(int id, String clientIp, String protocol, String action, String details, String timestamp) {
            this.id = id;
            this.clientIp = clientIp;
            this.protocol = protocol;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public String getClientIp() { return clientIp; }
        public String getProtocol() { return protocol; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
        public String getTimestamp() { return timestamp; }
    }

    public static void log(String clientIp, String protocol, String action, String details) {
        String sql = "INSERT INTO mail_logs (client_ip, protocol, action, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, clientIp);
            pstmt.setString(2, protocol);
            pstmt.setString(3, action);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MailLogDAO] Lỗi ghi log: " + e.getMessage());
        }
    }

    public static List<MailLogEntry> getRecentLogs(int limit) {
        List<MailLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM mail_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new MailLogEntry(
                    rs.getInt("id"),
                    rs.getString("client_ip"),
                    rs.getString("protocol"),
                    rs.getString("action"),
                    rs.getString("details"),
                    rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[MailLogDAO] Lỗi lấy logs: " + e.getMessage());
        }
        return list;
    }

    public static void clearLogs() {
        String sql = "DELETE FROM mail_logs";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[MailLogDAO] Lỗi xóa logs: " + e.getMessage());
        }
    }
}

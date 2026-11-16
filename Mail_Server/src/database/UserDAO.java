package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static class MailUser {
        private int id;
        private String username;
        private String password;
        private String email;
        private String createdAt;

        public MailUser(int id, String username, String password, String email, String createdAt) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.email = email;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getEmail() { return email; }
        public String getCreatedAt() { return createdAt; }
    }

    public static MailUser authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new MailUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getString("created_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi xác thực: " + e.getMessage());
        }
        return null;
    }

    public static List<MailUser> getAllUsers() {
        List<MailUser> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new MailUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi lấy danh sách user: " + e.getMessage());
        }
        return list;
    }

    public static boolean addUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi thêm user: " + e.getMessage());
            return false;
        }
    }

    public static int getTotalUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }
}

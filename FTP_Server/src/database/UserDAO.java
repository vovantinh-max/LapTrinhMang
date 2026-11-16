package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static class User {
        private int id;
        private String username;
        private String password;
        private String homeDir;
        private boolean canRead;
        private boolean canWrite;
        private boolean isActive;
        private String createdAt;

        public User() {}

        public User(int id, String username, String password, String homeDir, boolean canRead, boolean canWrite, boolean isActive, String createdAt) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.homeDir = homeDir;
            this.canRead = canRead;
            this.canWrite = canWrite;
            this.isActive = isActive;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getHomeDir() { return homeDir; }
        public void setHomeDir(String homeDir) { this.homeDir = homeDir; }

        public boolean isCanRead() { return canRead; }
        public void setCanRead(boolean canRead) { this.canRead = canRead; }

        public boolean isCanWrite() { return canWrite; }
        public void setCanWrite(boolean canWrite) { this.canWrite = canWrite; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND is_active = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi xác thực: " + e.getMessage());
        }
        return null;
    }

    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi tìm user: " + e.getMessage());
        }
        return null;
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi lấy danh sách user: " + e.getMessage());
        }
        return list;
    }

    public static boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password, home_dir, can_read, can_write, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getHomeDir() == null || user.getHomeDir().isBlank() ? "/" : user.getHomeDir());
            pstmt.setInt(4, user.isCanRead() ? 1 : 0);
            pstmt.setInt(5, user.isCanWrite() ? 1 : 0);
            pstmt.setInt(6, user.isActive() ? 1 : 0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi thêm user: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateUser(User user) {
        String sql = "UPDATE users SET password = ?, home_dir = ?, can_read = ?, can_write = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getPassword());
            pstmt.setString(2, user.getHomeDir());
            pstmt.setInt(3, user.isCanRead() ? 1 : 0);
            pstmt.setInt(4, user.isCanWrite() ? 1 : 0);
            pstmt.setInt(5, user.isActive() ? 1 : 0);
            pstmt.setInt(6, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi cập nhật user: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Lỗi xóa user: " + e.getMessage());
            return false;
        }
    }

    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("home_dir"),
            rs.getInt("can_read") == 1,
            rs.getInt("can_write") == 1,
            rs.getInt("is_active") == 1,
            rs.getString("created_at")
        );
    }
}

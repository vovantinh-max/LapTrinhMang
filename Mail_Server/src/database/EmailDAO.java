package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmailDAO {

    public static class Email {
        private int id;
        private String sender;
        private String recipient;
        private String subject;
        private String body;
        private String dateSent;
        private boolean isRead;
        private boolean isDeleted;
        private int size;

        public Email() {}

        public Email(int id, String sender, String recipient, String subject, String body, String dateSent, boolean isRead, boolean isDeleted, int size) {
            this.id = id;
            this.sender = sender;
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
            this.dateSent = dateSent;
            this.isRead = isRead;
            this.isDeleted = isDeleted;
            this.size = size;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getDateSent() { return dateSent; }
        public void setDateSent(String dateSent) { this.dateSent = dateSent; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
        public boolean isDeleted() { return isDeleted; }
        public void setDeleted(boolean deleted) { isDeleted = deleted; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    public static boolean saveEmail(Email email) {
        String sql = "INSERT INTO emails (sender, recipient, subject, body, size) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, email.getSender());
            pstmt.setString(2, email.getRecipient());
            pstmt.setString(3, email.getSubject() == null ? "(Không có tiêu đề)" : email.getSubject());
            pstmt.setString(4, email.getBody() == null ? "" : email.getBody());
            int calculatedSize = (email.getBody() != null ? email.getBody().length() : 0) + 128;
            pstmt.setInt(5, calculatedSize);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    email.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Lỗi lưu email: " + e.getMessage());
        }
        return false;
    }

    public static List<Email> getEmailsForRecipient(String recipient) {
        List<Email> list = new ArrayList<>();
        String sql = "SELECT * FROM emails WHERE (recipient = ? OR recipient LIKE ?) AND is_deleted = 0 ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipient);
            pstmt.setString(2, recipient.contains("@") ? recipient : recipient + "@%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Lỗi lấy email người nhận: " + e.getMessage());
        }
        return list;
    }

    public static Email getEmailById(int id) {
        String sql = "SELECT * FROM emails WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Lỗi tìm email: " + e.getMessage());
        }
        return null;
    }

    public static boolean markAsDeleted(int id) {
        String sql = "UPDATE emails SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Lỗi xóa email: " + e.getMessage());
            return false;
        }
    }

    public static List<Email> getAllEmails() {
        List<Email> list = new ArrayList<>();
        String sql = "SELECT * FROM emails WHERE is_deleted = 0 ORDER BY id DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Lỗi lấy tất cả email: " + e.getMessage());
        }
        return list;
    }

    public static int getTotalEmailCount() {
        String sql = "SELECT COUNT(*) FROM emails WHERE is_deleted = 0";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }

    private static Email mapResultSet(ResultSet rs) throws SQLException {
        return new Email(
            rs.getInt("id"),
            rs.getString("sender"),
            rs.getString("recipient"),
            rs.getString("subject"),
            rs.getString("body"),
            rs.getString("date_sent"),
            rs.getInt("is_read") == 1,
            rs.getInt("is_deleted") == 1,
            rs.getInt("size")
        );
    }
}

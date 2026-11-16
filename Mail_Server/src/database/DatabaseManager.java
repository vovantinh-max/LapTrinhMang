package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = System.getProperty("lab.db.path", "data/mail_server.db");
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy SQLite JDBC Driver: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        File dir = new File(DB_FILE).getAbsoluteFile().getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return DriverManager.getConnection(URL);
    }

    public static synchronized void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");

            // Bảng tài khoản người dùng
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createUsersTable);

            // Bảng lưu trữ email (Inboxes)
            String createEmailsTable = """
                CREATE TABLE IF NOT EXISTS emails (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender TEXT NOT NULL,
                    recipient TEXT NOT NULL,
                    subject TEXT,
                    body TEXT,
                    date_sent DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_read INTEGER DEFAULT 0,
                    is_deleted INTEGER DEFAULT 0,
                    size INTEGER DEFAULT 0
                );
            """;
            stmt.execute(createEmailsTable);

            // Bảng nhật ký hoạt động Mail Server (Audit Logs)
            String createLogsTable = """
                CREATE TABLE IF NOT EXISTS mail_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_ip TEXT,
                    protocol TEXT,
                    action TEXT,
                    details TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createLogsTable);

            // Khởi tạo các tài khoản mặc định
            String seedUsers = """
                INSERT OR IGNORE INTO users (id, username, password, email)
                VALUES 
                    (1, 'admin', 'admin123', 'admin@localhost'),
                    (2, 'student', '123456', 'student@localhost'),
                    (3, 'teacher', 'teacher123', 'teacher@localhost');
            """;
            stmt.execute(seedUsers);

            // Tạo sẵn 1 email chào mừng mẫu
            String seedWelcomeEmail = """
                INSERT OR IGNORE INTO emails (id, sender, recipient, subject, body, size)
                VALUES (1, 'system@localhost', 'student@localhost', 'Chào mừng bạn đến với VKU Mail Suite!',
                        'Xin chào Võ Văn Tình,\n\nĐây là hệ thống Mail Server và Client chuẩn SMTP & POP3 (Lab 3 Lập trình mạng VKU).\nHộp thư của bạn đã sẵn sàng hoạt động!\n\nTrân trọng,\nBan Quản Trị', 256);
            """;
            stmt.execute(seedWelcomeEmail);

            System.out.println("[Database] SQLite Mail Database khởi tạo thành công tại: " + DB_FILE);
        } catch (SQLException e) {
            System.err.println("[Database Error] Lỗi khởi tạo DB: " + e.getMessage());
        }
    }
}

package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = System.getProperty("lab.db.path", "data/ftp_server.db");
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

            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    home_dir TEXT NOT NULL,
                    can_read INTEGER DEFAULT 1,
                    can_write INTEGER DEFAULT 1,
                    is_active INTEGER DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createUsersTable);

            String createLogsTable = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT,
                    client_ip TEXT,
                    action TEXT,
                    details TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createLogsTable);

            String seedDefaultUsers = """
                INSERT OR IGNORE INTO users (id, username, password, home_dir, can_read, can_write, is_active)
                VALUES 
                    (1, 'admin', 'admin123', '/', 1, 1, 1),
                    (2, 'student', '123456', '/student', 1, 1, 1),
                    (3, 'anonymous', 'anonymous', '/public', 1, 0, 1);
            """;
            stmt.execute(seedDefaultUsers);

            System.out.println("[Database] Cơ sở dữ liệu SQLite khởi tạo thành công tại: " + DB_FILE);
        } catch (SQLException e) {
            System.err.println("[Database Error] Khởi tạo DB thất bại: " + e.getMessage());
        }
    }
}

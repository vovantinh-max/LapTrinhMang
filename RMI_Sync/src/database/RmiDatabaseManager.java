package database;

import model.StudentRecord;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RmiDatabaseManager {

    public static class SyncLogEntry {
        public int id;
        public String serverName;
        public String action;
        public String studentCode;
        public String details;
        public String timestamp;

        public SyncLogEntry(int id, String serverName, String action, String studentCode, String details, String timestamp) {
            this.id = id;
            this.serverName = serverName;
            this.action = action;
            this.studentCode = studentCode;
            this.details = details;
            this.timestamp = timestamp;
        }
    }

    private final String dbPath;
    private final String url;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy SQLite JDBC Driver: " + e.getMessage());
        }
    }

    public RmiDatabaseManager(String dbPath) {
        this.dbPath = dbPath;
        this.url = "jdbc:sqlite:" + dbPath;
        initialize();
    }

    private Connection getConnection() throws SQLException {
        File f = new File(dbPath);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) { stmt.execute("PRAGMA busy_timeout=5000"); }
        return conn;
    }

    public void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");

            String createTable = """
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_code TEXT UNIQUE NOT NULL,
                    full_name TEXT NOT NULL,
                    class_name TEXT NOT NULL,
                    major TEXT NOT NULL,
                    gpa REAL NOT NULL,
                    version INTEGER DEFAULT 1,
                    last_modified_server TEXT NOT NULL,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_deleted INTEGER DEFAULT 0
                );
            """;
            stmt.execute(createTable);

            String createLogs = """
                CREATE TABLE IF NOT EXISTS sync_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    server_name TEXT,
                    action TEXT,
                    student_code TEXT,
                    details TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
            stmt.execute(createLogs);

        } catch (SQLException e) {
            System.err.println("[RmiDatabaseManager] Lỗi khởi tạo DB " + dbPath + ": " + e.getMessage());
        }
    }

    public synchronized boolean insert(StudentRecord s) {
        String sql = "INSERT INTO students (student_code, full_name, class_name, major, gpa, version, last_modified_server, updated_at, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, s.getStudentCode());
            pstmt.setString(2, s.getFullName());
            pstmt.setString(3, s.getClassName());
            pstmt.setString(4, s.getMajor());
            pstmt.setDouble(5, s.getGpa());
            pstmt.setLong(6, s.getVersion());
            pstmt.setString(7, s.getLastModifiedServer());
            pstmt.setString(8, s.getUpdatedAt() != null ? s.getUpdatedAt() : now());

            int r = pstmt.executeUpdate();
            if (r > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) s.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DB Insert Error] " + e.getMessage());
        }
        return false;
    }

    public synchronized boolean update(StudentRecord s) {
        String sql = "UPDATE students SET full_name = ?, class_name = ?, major = ?, gpa = ?, version = ?, last_modified_server = ?, updated_at = ?, is_deleted = 0 WHERE student_code = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getFullName());
            pstmt.setString(2, s.getClassName());
            pstmt.setString(3, s.getMajor());
            pstmt.setDouble(4, s.getGpa());
            pstmt.setLong(5, s.getVersion());
            pstmt.setString(6, s.getLastModifiedServer());
            pstmt.setString(7, s.getUpdatedAt() != null ? s.getUpdatedAt() : now());
            pstmt.setString(8, s.getStudentCode());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB Update Error] " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean delete(String studentCode) {
        String sql = "UPDATE students SET is_deleted = 1, updated_at = ? WHERE student_code = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, now());
            pstmt.setString(2, studentCode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB Delete Error] " + e.getMessage());
            return false;
        }
    }

    public StudentRecord getByCode(String studentCode) {
        String sql = "SELECT * FROM students WHERE student_code = ? AND is_deleted = 0";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentCode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DB Get Error] " + e.getMessage());
        }
        return null;
    }

    public List<StudentRecord> getAll() {
        List<StudentRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE is_deleted = 0 ORDER BY id ASC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DB GetAll Error] " + e.getMessage());
        }
        return list;
    }

    public List<StudentRecord> search(String keyword) {
        List<StudentRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE is_deleted = 0 AND (student_code LIKE ? OR full_name LIKE ? OR class_name LIKE ?) ORDER BY id ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            pstmt.setString(1, kw);
            pstmt.setString(2, kw);
            pstmt.setString(3, kw);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DB Search Error] " + e.getMessage());
        }
        return list;
    }

    /** Include tombstones so an offline peer cannot resurrect a deleted record. */
    public StudentRecord getState(String code) {
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement("SELECT * FROM students WHERE student_code=?")) {
            p.setString(1, code);
            try (ResultSet rs = p.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public List<StudentRecord> getSyncState() {
        List<StudentRecord> result = new ArrayList<>();
        try (Connection c = getConnection(); Statement p = c.createStatement(); ResultSet rs = p.executeQuery("SELECT * FROM students")) {
            while (rs.next()) result.add(mapRow(rs));
            return result;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    /** Atomic, deterministic merge; IDs belong to each local database. */
    public synchronized boolean mergeState(StudentRecord s) {
        String sql = """
            INSERT INTO students(student_code,full_name,class_name,major,gpa,version,last_modified_server,updated_at,is_deleted)
            VALUES(?,?,?,?,?,?,?,?,?)
            ON CONFLICT(student_code) DO UPDATE SET
            full_name=excluded.full_name,class_name=excluded.class_name,major=excluded.major,gpa=excluded.gpa,
            version=excluded.version,last_modified_server=excluded.last_modified_server,
            updated_at=excluded.updated_at,is_deleted=excluded.is_deleted
            WHERE excluded.version > students.version OR
            (excluded.version = students.version AND (excluded.updated_at > students.updated_at OR
            (excluded.updated_at = students.updated_at AND excluded.last_modified_server > students.last_modified_server)))
            """;
        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1,s.getStudentCode()); p.setString(2,s.getFullName());
            p.setString(3,s.getClassName()); p.setString(4,s.getMajor()); p.setDouble(5,s.getGpa());
            p.setLong(6,s.getVersion()); p.setString(7,s.getLastModifiedServer());
            p.setString(8,s.getUpdatedAt()); p.setBoolean(9,s.isDeleted());
            return p.executeUpdate() > 0;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public synchronized void batchReplaceAll(List<StudentRecord> records) {
        // Historical method name retained for callers; never delete local-only records.
        for (StudentRecord record : records) mergeState(record);
    }

    public void logSync(String serverName, String action, String code, String details) {
        String sql = "INSERT INTO sync_logs (server_name, action, student_code, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, serverName);
            pstmt.setString(2, action);
            pstmt.setString(3, code);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB Log Error] " + e.getMessage());
        }
    }

    public List<SyncLogEntry> getRecentLogs(int limit) {
        List<SyncLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM sync_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new SyncLogEntry(
                    rs.getInt("id"),
                    rs.getString("server_name"),
                    rs.getString("action"),
                    rs.getString("student_code"),
                    rs.getString("details"),
                    rs.getString("timestamp")
                ));
            }
        } catch (SQLException ignored) {}
        return list;
    }

    private StudentRecord mapRow(ResultSet rs) throws SQLException {
        StudentRecord record = new StudentRecord(
            rs.getInt("id"),
            rs.getString("student_code"),
            rs.getString("full_name"),
            rs.getString("class_name"),
            rs.getString("major"),
            rs.getDouble("gpa"),
            rs.getLong("version"),
            rs.getString("last_modified_server"),
            rs.getString("updated_at")
        );
        record.setDeleted(rs.getBoolean("is_deleted"));
        return record;
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}

package test;

import client.Pop3ClientEngine;
import client.Pop3ClientEngine.Pop3MessageInfo;
import client.SmtpClientEngine;
import database.DatabaseManager;
import database.EmailDAO;
import database.EmailDAO.Email;
import database.MailLogDAO;
import database.MailLogDAO.MailLogEntry;
import database.UserDAO;
import server.MailServerManager;

import java.io.File;
import java.util.List;

public class MailIntegrationTest {

    private static final int SMTP_TEST_PORT = 25259;
    private static final int POP3_TEST_PORT = 11109;

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("   CHẠY BỘ KIỂM THỬ TỰ ĐỘNG (MAIL INTEGRATION TEST)");
        System.out.println("========================================================");

        DatabaseManager.initializeDatabase();

        MailServerManager server = new MailServerManager(SMTP_TEST_PORT, POP3_TEST_PORT);
        try {
            server.startAll();
            System.out.println("[PASS] 1. Khởi động Mail Server (SMTP & POP3) thành công!");

            // 1. Kiểm thử gửi thư qua SMTP
            testSmtpSend();

            // 2. Kiểm thử lưu trữ trong SQLite
            testSqliteStorage();

            // 3. Kiểm thử đọc thư qua POP3
            testPop3Retrieve();

            // 4. Kiểm thử Nhật ký Audit Logs trong SQLite
            testAuditLogs();

            System.out.println("\n🎉 TẤT CẢ 4 BÀI KIỂM THỬ MAIL SERVER & CLIENT ĐẠU 100%!");
        } catch (Exception e) {
            System.err.println("❌ KIỂM THỬ THẤT BẠI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            server.stopAll();
        }
    }

    private static void testSmtpSend() throws Exception {
        System.out.print("Testing SMTP Send (Localhost port " + SMTP_TEST_PORT + ")... ");
        SmtpClientEngine smtp = new SmtpClientEngine();
        smtp.sendLocalEmail(
                "127.0.0.1",
                SMTP_TEST_PORT,
                "teacher@localhost",
                "student@localhost",
                "Thông báo kiểm tra Lab 3 LTM",
                "Chào bạn Tình, bài Lab 3 Mail Server chạy rất tốt."
        );
        System.out.println("[OK]");
    }

    private static void testSqliteStorage() {
        System.out.print("Testing SQLite Storage (emails table)... ");
        List<Email> list = EmailDAO.getEmailsForRecipient("student@localhost");
        assert !list.isEmpty() : "Không tìm thấy email nào trong hộp thư của student@localhost!";
        boolean found = false;
        for (Email e : list) {
            if ("Thông báo kiểm tra Lab 3 LTM".equals(e.getSubject())) {
                found = true;
                assert e.getBody().contains("Chào bạn Tình") : "Nội dung body không khớp!";
            }
        }
        assert found : "Không tìm thấy email vừa gửi trong SQLite!";
        System.out.println("[OK]");
    }

    private static void testPop3Retrieve() throws Exception {
        System.out.print("Testing POP3 Retrieve (Localhost port " + POP3_TEST_PORT + ")... ");
        Pop3ClientEngine pop3 = new Pop3ClientEngine();
        pop3.connect("127.0.0.1", POP3_TEST_PORT, "student", "123456");
        assert pop3.isConnected() : "Không thể kết nối POP3!";

        List<Pop3MessageInfo> list = pop3.listMessages();
        assert !list.isEmpty() : "POP3 LIST trả về danh sách rỗng!";

        // Đọc email đầu tiên
        Email email = pop3.retrieveEmail(1);
        assert email != null : "POP3 RETR không đọc được email!";
        assert email.getSubject() != null && !email.getSubject().isEmpty() : "Subject của POP3 rỗng!";

        pop3.disconnect();
        System.out.println("[OK]");
    }

    private static void testAuditLogs() {
        System.out.print("Testing SQLite Audit Logs (mail_logs table)... ");
        List<MailLogEntry> logs = MailLogDAO.getRecentLogs(20);
        assert !logs.isEmpty() : "Bảng mail_logs rỗng!";
        boolean foundSmtp = false;
        boolean foundPop3 = false;
        for (MailLogEntry l : logs) {
            if ("SMTP".equals(l.getProtocol())) foundSmtp = true;
            if ("POP3".equals(l.getProtocol())) foundPop3 = true;
        }
        assert foundSmtp && foundPop3 : "Thiếu log SMTP hoặc POP3 trong SQLite!";
        System.out.println("[OK]");
    }
}

# MAIL SUITE (SERVER & CLIENT) · LAB 3 LẬP TRÌNH MẠNG (VKU)

Hệ thống thư điện tử hoàn chỉnh bao gồm **Mail Server (SMTP & POP3)** và **Mail Client (Gmail-style Desktop)** được xây dựng bằng **Java Sockets thuần**, cơ sở dữ liệu **SQLite** và giao diện đồ họa **Java Swing** đồng bộ phong cách Dark Navy.

---

## 1. Đáp ứng 100% Yêu cầu đề bài Lab 3 của Thầy

| Tiêu chí của đề bài | Hiện thực trong dự án |
|---|---|
| **Giao thức POP3 và SMTP** | • **SMTP Server (RFC 5321):** Chạy cổng **2525** trên Localhost (HELO, EHLO, MAIL FROM, RCPT TO, DATA, QUIT, RSET).<br>• **POP3 Server (RFC 1939):** Chạy cổng **1110** trên Localhost (USER, PASS, STAT, LIST, RETR, DELE, QUIT). |
| **Gửi được thư trên Localhost** | Hỗ trợ gửi thư nội bộ giữa các tài khoản qua SMTP cổng 2525 và tự động lưu vào **SQLite Database** của Server. |
| **Gửi được thư trên Gmail thật** | Tích hợp **Google SMTP (`smtp.gmail.com:587` với STARTTLS & AUTH LOGIN Base64)** sử dụng Mật khẩu ứng dụng (Google App Password 16 ký tự). Cho phép gửi email thật đến bất kỳ hộp thư Gmail nào trên thế giới! |
| **Có dữ liệu trên database của Server** | Lưu trữ toàn bộ người dùng (`users`), nội dung email (`emails`) và nhật ký kiểm toán (`mail_logs`) trong **SQLite** (`data/mail_server.db`). |
| **Giao diện tương tự Gmail** | **Mail Client GUI (`MailClientGUI`)** có bố cục giống Gmail: Nút **Soạn thư (Compose)**, Hộp thư đến (**Inbox**), Xem chi tiết thư, Quản lý tài khoản, cùng thanh nhật ký giao thức thời gian thực. |

---

## 2. Cấu trúc thư mục dự án

```text
Mail_Server/
├── .vscode/               # Cấu hình IDE VS Code (F5)
├── bin/                   # Bytecode sau khi build (.class)
├── data/
│   └── mail_server.db     # Cơ sở dữ liệu SQLite lưu trữ Users, Emails, Logs
├── lib/
│   └── sqlite-jdbc-3.47.1.0.jar  # Driver SQLite JDBC
├── src/
│   ├── client/
│   │   ├── Pop3ClientEngine.java # Client đọc/xóa thư qua POP3 RFC 1939
│   │   ├── SmtpClientEngine.java # Client gửi thư qua Localhost SMTP hoặc Gmail TLS
│   │   └── MailClientGUI.java    # Giao diện Mail Client (Gmail-style)
│   ├── database/
│   │   ├── DatabaseManager.java  # Khởi tạo DB SQLite và tạo bảng tự động
│   │   ├── EmailDAO.java         # Quản lý lưu trữ và truy vấn email
│   │   ├── MailLogDAO.java       # Quản lý nhật ký hoạt động
│   │   └── UserDAO.java          # Xác thực tài khoản người dùng
│   ├── server/
│   │   ├── MailServerManager.java# Quản lý khởi động/dừng SMTP và POP3
│   │   ├── Pop3ServerEngine.java # Máy chủ POP3 Socket đa luồng (Cổng 1110)
│   │   └── SmtpServerEngine.java # Máy chủ SMTP Socket đa luồng (Cổng 2525)
│   ├── ui/
│   │   ├── UITheme.java          # Hệ thống Design Tokens Dark Navy đồng bộ
│   │   └── MailServerGUI.java    # Giao diện quản trị Server Dashboard
│   └── Main.java                 # Điểm khởi chạy chính
├── test/
│   └── MailIntegrationTest.java  # Bộ kiểm thử tự động toàn diện
├── build.cmd / build.ps1         # Biên dịch dự án
├── run-server.cmd                # Mở Mail Server Dashboard
├── run-client.cmd                # Mở Mail Client GUI
└── test.cmd                      # Chạy kiểm thử tự động (PASS 100%)
```

---

## 3. Danh sách tài khoản mặc định trong SQLite

| Tên đăng nhập | Mật khẩu | Địa chỉ Email | Ghi chú |
|---|---|---|---|
| `student` | `123456` | `student@localhost` | Tài khoản sinh viên (Mặc định trong Client) |
| `admin` | `admin123` | `admin@localhost` | Quản trị viên hệ thống |
| `teacher` | `teacher123` | `teacher@localhost` | Tài khoản giảng viên |

---

## 4. Hướng dẫn khởi chạy và chấm bài

### Bước 1: Khởi động Mail Server
1. Nhấp đúp vào [run-server.cmd](file:///C:/Users/Lapto/Desktop/lập%20trình%20mạng/Mail_Server/run-server.cmd) (hoặc nhấn `F5` trong VS Code chọn `Mail Server (Dashboard)`).
2. Nhấn nút **"Khởi Động Server"**.
3. Đèn trạng thái chuyển sang `● ONLINE` (SMTP mở cổng 2525, POP3 mở cổng 1110) và đồng hồ Uptime bắt đầu chạy.

### Bước 2: Khởi động Mail Client và thử nghiệm
1. Bấm nút **"✉️ Mở Mail Client"** ngay trên giao diện của Mail Server (hoặc nhấp đúp [run-client.cmd](file:///C:/Users/Lapto/Desktop/lập%20trình%20mạng/Mail_Server/run-client.cmd)).
2. **Thử nghiệm gửi thư nội bộ (Localhost SMTP & POP3):**
   * Bấm nút **"➕ Soạn Thư Mới"** ở thanh bên trái.
   * Chọn chế độ `Localhost SMTP (Cổng 2525)`.
   * Gửi tới `admin@localhost` hoặc `teacher@localhost` rồi bấm **"🚀 Gửi Thư Ngay"**.
   * Trên Mail Server: Số lượng email tăng lên ngay lập tức, bảng danh sách thư tự động cập nhật và log hiển thị luồng trao đổi SMTP.
3. **Thử nghiệm đọc thư qua POP3:**
   * Bấm vào mục **"📥 Hộp Thư Đến (Inbox)"**.
   * Danh sách thư hiển thị đầy đủ tiêu đề, người gửi, ngày gửi.
   * Nhấp đúp chuột vào bất kỳ thư nào để mở cửa sổ xem chi tiết nội dung.
4. **Thử nghiệm gửi thư thật qua Gmail (Google SMTP):**
   * Vào tab **"⚙️ Cấu Hình Tài Khoản"**, nhập địa chỉ Gmail của bạn và Mật khẩu ứng dụng (App Password 16 ký tự).
   * Bấm **Soạn thư** ➔ Chọn chế độ `Google Gmail Thật (smtp.gmail.com:587)`.
   * Nhập địa chỉ Gmail người nhận (ví dụ Gmail của thầy hoặc bạn bè) và bấm **Gửi**. Thư sẽ bay thẳng đến hòm thư Gmail thực tế qua kết nối mã hóa STARTTLS!

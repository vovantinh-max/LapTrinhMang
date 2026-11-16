# FTP SUITE (SERVER & CLIENT) · LAB 2 LẬP TRÌNH MẠNG (VKU)

Hệ thống truyền nhận tệp tin FTP hoàn chỉnh bao gồm **FTP Server** và **FTP Client** giao diện đồ họa cao cấp, được thiết kế **đồng bộ 100% về bố cục giao diện và ngôn ngữ thiết kế (Design Tokens) với ứng dụng Chat Client (TCP Socket)**.

---

## 1. Điểm nổi bật & Tính đồng bộ với dự án Chat TCP Socket

1. **Ngôn ngữ thiết kế đồng bộ (Dark Modern Design System):**
   - **Bảng màu:** Dùng chung hệ màu Dark Navy (`#0A0B0F` & `#0F1117`), bề mặt nổi (`#14161E`, `#1C1F2A`, `#262937`), viền tối (`#26293A`) và màu thương hiệu Indigo / Teal (`#6366F1`, `#2DD4BF`).
   - **Typography & Components:** Font chuẩn hệ thống `Segoe UI`, các thành phần `ModernButton` bo tròn góc có hiệu ứng hover mượt mà, `ModernTextField` có viền sáng khi focus, thanh cuộn tùy biến `DarkScrollBarUI`.
   - **Thẻ thống kê (Stat Cards) & Chips:** Hiển thị thời gian chạy Uptime thời gian thực (`HH:mm:ss`), đèn báo trạng thái có hiệu ứng nhấp nháy (Pulsing Dot) `● ONLINE / ● STOPPED`.

2. **Hoạt động đồng bộ thời gian thực (Real-time Synchronization):**
   - Khi **FTP Client** hoặc **FileZilla** kết nối tới Server:
     - Số lượng khách online trên thẻ thống kê tăng ngay lập tức.
     - Danh sách Client trong bảng hiển thị ngay IP, tài khoản, thời gian kết nối và thư mục hiện hành.
   - Khi có bất kỳ hành động nào (`Đăng nhập`, `Upload`, `Download`, `Tạo thư mục`, `Xóa file`):
     - Ghi nhận ngay vào cơ sở dữ liệu **SQLite** (`data/ftp_server.db`).
     - Tự động làm mới bảng **Nhật ký hoạt động (Audit Logs)** với các huy hiệu màu sắc tương ứng.
     - Thanh tiến trình trên Client cập nhật phần trăm và tốc độ truyền file theo thời gian thực.

---

## 2. Cấu trúc thư mục

```text
FTP_Server/
├── .vscode/               # Cấu hình khởi chạy trên VS Code (F5)
├── bin/                   # Bytecode sau khi build (.class)
├── data/
│   ├── ftp_server.db      # Cơ sở dữ liệu SQLite quản lý Users và Logs
│   └── storage/           # Thư mục lưu trữ tập tin của Server
│       ├── public/        # Thư mục riêng của tài khoản anonymous
│       └── student/       # Thư mục riêng của tài khoản student
├── lib/
│   └── sqlite-jdbc-3.47.1.0.jar  # Driver SQLite JDBC
├── src/
│   ├── client/
│   │   ├── FtpClientEngine.java   # Engine FTP Client kết nối Socket chuẩn RFC 959
│   │   └── FtpClientGUI.java      # Giao diện FTP Client đồng bộ với ChatFrame
│   ├── database/
│   │   ├── DatabaseManager.java   # Kết nối SQLite và tự khởi tạo cấu trúc bảng
│   │   ├── UserDAO.java           # Quản lý tài khoản và phân quyền
│   │   └── LogDAO.java            # Quản lý nhật ký kiểm toán (Audit Logs)
│   ├── server/
│   │   ├── FtpServer.java         # Socket Server quản lý kết nối và đa luồng
│   │   ├── FtpClientHandler.java  # Xử lý các lệnh FTP theo chuẩn RFC 959
│   │   ├── FtpDataConnection.java # Điều phối kênh dữ liệu PASV / PORT
│   │   └── FtpSession.java        # Quản lý phiên và sandbox chống duyệt lậu
│   ├── ui/
│   │   ├── UITheme.java           # Hệ thống Design Tokens đồng bộ với Chat Socket
│   │   ├── FtpServerGUI.java      # Giao diện quản trị Server đồng bộ với ServerGUI
│   │   └── UserManagerDialog.java # Hộp thoại quản lý người dùng SQLite
│   └── Main.java                  # Điểm khởi chạy chính
├── test/
│   └── FtpIntegrationTest.java    # Bộ kiểm thử tự động toàn diện
├── build.cmd / build.ps1          # Script biên dịch dự án
├── run-gui.cmd                    # Mở giao diện FTP Server
├── run-client.cmd                 # Mở giao diện FTP Client
└── test.cmd                       # Chạy kiểm thử tự động (PASS 100%)
```

---

## 3. Hướng dẫn khởi chạy và chấm bài

### Bước 1: Khởi động FTP Server
- Nhấp đúp chuột vào [run-gui.cmd](file:///C:/Users/Lapto/Desktop/lập%20trình%20mạng/FTP_Server/run-gui.cmd) (hoặc chạy cấu hình `FTP Server (GUI)` trong VS Code).
- Nhấn nút **"Khởi Động Server"** ở góc dưới bên phải. Đèn trạng thái chuyển sang `● ONLINE` màu xanh lá và đồng hồ thời gian chạy bắt đầu đếm.

### Bước 2: Kết nối Client để demo gửi nhận tệp tin
Bạn có 2 lựa chọn (hoặc chạy cả hai cùng lúc):
1. **Dùng FTP Client đồng bộ đi kèm:**
   - Bấm nút **"🌐 Mở FTP Client"** ngay trên giao diện Server (hoặc nhấp đúp file [run-client.cmd](file:///C:/Users/Lapto/Desktop/lập%20trình%20mạng/FTP_Server/run-client.cmd)).
   - Bấm nút **"Kết Nối"** (với tài khoản mặc định `admin` / `admin123` hoặc `student` / `123456`).
   - Chọn tệp bên cột trái (Local) bấm **"Upload ➔"**, hoặc chọn tệp bên cột phải (Server) bấm **"⬅ Download"**.
2. **Dùng FileZilla Client bên ngoài:**
   - Mở FileZilla ➔ Host: `127.0.0.1`, User: `admin`, Pass: `admin123`, Port: `2121`.
   - Bấm Quickconnect ➔ Kéo thả tệp tin để truyền nhận.

---

## 4. Tài khoản mặc định trong SQLite

| Username | Password | Thư mục Home | Quyền hạn |
|---|---|---|---|
| `admin` | `admin123` | `/` | Toàn quyền (Đọc + Ghi) |
| `student` | `123456` | `/student` | Đọc + Ghi trong thư mục riêng |
| `anonymous` | `anonymous` | `/public` | Chỉ đọc (Read-only) |

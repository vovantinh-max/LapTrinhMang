# Bộ Đồ Án Lập Trình Mạng

Kho lưu trữ tổng hợp các dự án môn Lập Trình Mạng.

## Danh sách các dự án

### 1. [TCP_Socket](./TCP_Socket/)
- **Mô tả:** Ứng dụng chat phòng đa phương tiện (ChatRoom) hoạt động trên giao thức TCP Socket.
- **Tính năng:** Nhắn tin thời gian thực (riêng/nhóm), gửi file đính kèm, thoại, video, xác thực người dùng.
- **Khởi chạy:** Sử dụng `Chay-Server.bat` và `Chay-Client.bat` trong thư mục `TCP_Socket`.

### 2. [FTP_Server](./FTP_Server/)
- **Mô tả:** Máy chủ và máy khách truyền tập tin theo chuẩn giao thức FTP.
- **Tính năng:** Xác thực tài khoản, duyệt thư mục (LIST), tải lên (STOR), tải xuống (RETR), phân quyền người dùng và ghi nhật ký hoạt động vào SQLite.
- **Khởi chạy:** Chạy `build.cmd` sau đó dùng `run-server.cmd` và `run-client.cmd` (hoặc `run-gui.cmd`).

### 3. [RMI_Sync](./RMI_Sync/)
- **Mô tả:** Hệ thống phân tán đồng bộ dữ liệu sinh viên hai chiều sử dụng Java RMI (Remote Method Invocation).
- **Tính năng:** Hỗ trợ mô hình đa node, đồng bộ dữ liệu tự động hai chiều, cơ chế giải quyết xung đột (conflict resolution: version, timestamp, server name) và đồng bộ dấu xóa.
- **Khởi chạy:** Chạy `build.cmd` sau đó dùng `run-server.cmd` và `run-client.cmd`.

### 4. [Mail_Server](./Mail_Server/)
- **Mô tả:** Hệ thống máy chủ và ứng dụng gửi/nhận email nội bộ theo giao thức SMTP và POP3.
- **Tính năng:** Máy chủ SMTP tiếp nhận thư điện tử, POP3 cho phép client tải hộp thư đến, lưu trữ dữ liệu hòm thư qua SQLite, giao diện trực quan.
- **Khởi chạy:** Chạy `build.cmd` sau đó dùng `run-server.cmd` và `run-client.cmd`.

---

## Yêu cầu môi trường
- **Java Development Kit (JDK):** JDK 21 hoặc JDK 23 trở lên (đã cấu hình trong `PATH`).
- **Hệ điều hành:** Windows 10 / 11.

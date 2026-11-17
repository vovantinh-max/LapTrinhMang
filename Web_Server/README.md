# MiniHTTP — Đồ án Lập trình mạng

MiniHTTP là ứng dụng desktop Java để duyệt trang và kiểm tra giao thức HTTP. Cửa sổ chính gồm trình duyệt nhiều tab, khu vực xem phản hồi và các màn hình quản lý lịch sử, dấu trang, tệp tải xuống. Máy chủ mẫu chạy cùng ứng dụng, chỉ lắng nghe tại `127.0.0.1`.

## Mở ứng dụng trên Windows

1. Dùng **Java 21 trở lên**. Chạy `java -version` để kiểm tra phiên bản.
2. Mở **MiniHTTP.cmd**. Bạn cũng có thể mở PowerShell tại thư mục dự án và chạy `java -jar .\build\MiniHTTP.jar`.
3. Trong cửa sổ ứng dụng, chọn **Mở trang máy chủ** để gửi GET đầu tiên. Địa chỉ thực tế hiển thị trên trang bắt đầu. Máy chủ ưu tiên cổng 8000 và tự chọn cổng khác nếu cần.

Sau khi chỉnh sửa mã nguồn, chạy `.\Build.cmd` với **JDK 21 trở lên** để cập nhật tệp JAR. Dự án dùng thư viện chuẩn Java và không cần cài thư viện ngoài.

## Kịch bản trình diễn

| Bước | Thao tác | Kết quả dự kiến |
| --- | --- | --- |
| 1 | Chọn **Mở trang máy chủ**, sau đó bấm liên kết **Thông tin máy chủ**. | GET trả mã 200. Trang đầu hiện liên kết HTML và trang thứ hai hiện JSON. |
| 2 | Chọn **Quay lại** rồi mở **Tài liệu hướng dẫn (PDF)**. Chuyển sang HEAD và chọn **Gửi yêu cầu**. | Thẻ **Tiêu đề HTTP** tự mở, hiển thị mã 200, `content-type: application/pdf` và `content-length`. |
| 3 | Mở **Tab mới** và chọn **Thử POST**. Gửi JSON mẫu đã điền sẵn. | API `/api/echo` trả mã 200 và dữ liệu trong trường `received`. |
| 4 | Giữ địa chỉ và phương thức POST. Chọn kiểu biểu mẫu, nhập `ten=Minh` và `mon=Lập trình mạng` ở hai dòng riêng rồi gửi. | API trả các giá trị biểu mẫu dưới dạng JSON. Thử chuyển lại JSON và nhập `{bad` để thấy mã 400. |
| 5 | Ở tab PDF, chọn **Tải tệp**, chọn nơi lưu. Thử lại với **Âm thanh mẫu (WAV)** trên trang bắt đầu. | Màn hình **Tệp tải xuống** hiện tiến độ và kết quả. Chọn dòng đã tải rồi bấm **Mở tệp** hoặc **Mở thư mục**. |
| 6 | Chọn **☆** trên thanh địa chỉ để lưu dấu trang. Mở **Lịch sử** và **Dấu trang**, lọc theo `api` hoặc `POST` rồi bấm đúp một dòng lịch sử. | Lịch sử ghi phương thức, URL và kết quả. Mục được mở trong tab mới bằng GET để tránh tự gửi lại dữ liệu POST. |
| 7 | Gửi GET tới đường dẫn `/khong-ton-tai` của máy chủ mẫu. | Trình duyệt hiển thị mã 404 và thông báo JSON. |

Các phím tắt gồm **Ctrl+T** mở tab, **Ctrl+W** đóng tab, **Ctrl+L** chọn thanh địa chỉ, **Ctrl+H** mở lịch sử, **Ctrl+J** mở tệp tải xuống và **F5** tải lại.

## Cấu trúc

- `MiniHttp.java`, `BrowserTab.java`, `LibraryView.java`, `HomeView.java` và `Theme.java` xây dựng giao diện Swing. Các yêu cầu và lượt tải chạy trên luồng nền để giao diện vẫn phản hồi.
- `HttpTransport.java` dùng Java `HttpClient` để gửi GET, HEAD và POST, đọc tiêu đề và lưu tệp. Dữ liệu tải xuống được ghi vào tệp tạm rồi chuyển vào đích sau khi tải đủ.
- `DemoServer.java` dùng `HttpServer` để mở máy chủ HTTP/1.1 và phục vụ HTML, JSON, PDF và WAV. Có thể chạy riêng bằng `java -cp .\build\MiniHTTP.jar DemoServer 8000`.
- `Profile.java` lưu lịch sử, dấu trang và các lượt tải thành công ở `%LOCALAPPDATA%\MiniHTTP\java`. Bản Java dùng hồ sơ riêng và không nhập lịch sử của bản Python.

Trình duyệt hiển thị bản xem trước HTML cơ bản và phần văn bản gốc tối đa 512 KiB. CSS của trang, JavaScript và nội dung đa phương tiện nhúng chưa được hỗ trợ. PDF và WAV có thể tải về và mở bằng ứng dụng trên máy.

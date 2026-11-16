# LAB 5: RMI ĐỒNG BỘ HÓA DỮ LIỆU TRÊN 2 SERVER (SYNCHRONIZED DUAL SERVER)

> **Học phần:** Lập Trình Mạng (VKU)  
> **Giảng viên hướng dẫn:** ThS. Nguyễn Thanh Cẩm  
> **Mã dự án:** `RMI_Sync`  
> **Công nghệ:** Java RMI (Remote Method Invocation), SQLite JDBC, Swing Flat Dark UI.

---

## 1. Giới thiệu tổng quan
Hệ thống giải quyết bài toán đồng bộ hóa dữ liệu thời gian thực giữa hai máy chủ phân tán (Active-Active Replication) sử dụng công nghệ Java RMI:
- **Server 1 (`DataServer1`):** Lắng nghe tại cổng RMI `1099`, quản lý cơ sở dữ liệu `data/server1.db`.
- **Server 2 (`DataServer2`):** Lắng nghe tại cổng RMI `1100`, quản lý cơ sở dữ liệu `data/server2.db`.
- **RMI Client:** Cho phép kết nối tới Server 1 hoặc Server 2, thực hiện các thao tác CRUD (Thêm, Sửa, Xóa, Tìm kiếm sinh viên).
- **Cơ chế Failover:** Khi 1 Server gặp sự cố hoặc bảo trì, Client có thể bấm nút **"Chuyển Server" (Failover)** để tiếp tục làm việc bình thường trên Server còn lại.
- **Tự động Reconciliation:** Khi một máy chủ vừa phục hồi hoặc khởi động lại, nó tự động gửi thông điệp yêu cầu **Full Sync** để kéo toàn bộ dữ liệu mới nhất từ Server đối tác về database của mình.

---

## 2. Kiến trúc hệ thống (System Architecture)

```
       ┌────────────────────────────────────────────────────────┐
       │                RMI CLIENT (Swing GUI)                  │
       │       - Thêm, Sửa, Xóa, Tra cứu Sinh viên              │
       │       - Nút chuyển Server tức thời (Failover)          │
       └──────────────┬───────────────────────────┬─────────────┘
                      │ (RMI call)                │ (RMI call)
                      ▼                           ▼
        ┌───────────────────────────┐   ┌───────────────────────────┐
        │         SERVER 1          │   │         SERVER 2          │
        │      Port: 1099 (RMI)     │   │      Port: 1100 (RMI)     │
        │   Service: DataServer1    │   │   Service: DataServer2    │
        └─────────────┬─────────────┘   └─────────────┬─────────────┘
                      │                               │
                      │ ⚡ Two-Way RMI Replication    │
                      │ ◄───────────────────────────► │
                      │                               │
                      ▼                               ▼
        ┌───────────────────────────┐   ┌───────────────────────────┐
        │  SQLite: data/server1.db  │   │  SQLite: data/server2.db  │
        │   - students              │   │   - students              │
        │   - sync_logs             │   │   - sync_logs             │
        └───────────────────────────┘   └───────────────────────────┘
```

---

## 3. Cấu trúc thư mục

```
RMI_Sync/
├── lib/
│   └── sqlite-jdbc-3.47.1.0.jar        # Driver kết nối cơ sở dữ liệu SQLite
├── data/
│   ├── server1.db                      # Database độc lập của Server 1
│   └── server2.db                      # Database độc lập của Server 2
├── src/
│   ├── model/
│   │   └── StudentRecord.java          # Đối tượng dữ liệu Serializable
│   ├── rmi/
│   │   ├── DataService.java            # Remote Interface cho Client gọi CRUD
│   │   └── SyncService.java            # Remote Interface cho 2 Server đồng bộ
│   ├── database/
│   │   └── RmiDatabaseManager.java     # SQLite CRUD, WAL mode, nhật ký sync_logs
│   ├── server/
│   │   └── RmiServerNode.java          # Triển khai DataService & SyncService
│   ├── client/
│   │   └── RmiClientGUI.java           # Giao diện RMI Client hiện đại
│   ├── ui/
│   │   ├── UITheme.java                # Thiết kế Dark Navy đồng bộ 5 Labs
│   │   └── RmiDualServerGUI.java       # Dashboard điều khiển & giám sát 2 Server
│   ├── test/
│   │   └── RmiSyncIntegrationTest.java # Kiểm thử tự động 5 kịch bản đồng bộ
│   └── Main.java                       # Điểm khởi chạy chính
├── build.ps1 / build.cmd               # Script biên dịch
├── run-server.cmd                      # Khởi chạy Dual Server Dashboard
├── run-client.cmd                      # Khởi chạy RMI Client
└── test.cmd                            # Chạy kiểm thử tự động
```

---

## 4. Hướng dẫn chạy chương trình

### Cách 1: Chạy bằng file `.cmd`
1. **Biên dịch:** Nhấp đúp vào `build.cmd` (hoặc chạy trong terminal).
2. **Khởi chạy Dual Server Dashboard:** Nhấp đúp `run-server.cmd`. Cả hai máy chủ Server 1 (1099) và Server 2 (1100) sẽ tự động bật và kết nối đồng bộ hai chiều.
3. **Khởi chạy RMI Client:** Bấm trực tiếp nút xanh **"💻 Mở Client"** trên Dashboard hoặc nhấp đúp file `run-client.cmd`.

### Cách 2: Chạy trực tiếp từ dòng lệnh (Terminal / PowerShell)
```powershell
# 1. Biên dịch
powershell -ExecutionPolicy Bypass -File build.ps1

# 2. Chạy Dashboard Dual Server
java -cp "bin;lib/*" Main

# 3. Chạy RMI Client
java -cp "bin;lib/*" Main --client

# 4. Chạy kiểm thử tự động
java -cp "bin;lib/*" test.RmiSyncIntegrationTest
```

---

## 5. Kịch bản trình diễn và trả lời câu hỏi vấn đáp với Thầy Cô

### Kịch bản 1: Thêm/Sửa/Xóa dữ liệu và kiểm tra nhân bản tức thì
1. Mở song song cửa sổ **RMI Dual Server Dashboard** và **RMI Client**.
2. Trên Client, chọn kết nối tới **Server 1** và bấm **"Thêm mới"** sinh viên `22IT888 - Nguyễn Hoàng Nam`.
3. Quan sát màn hình Dashboard:
   - Cột **Server 1** xuất hiện bản ghi mới.
   - Cột **Server 2** ngay lập tức nhận bản ghi tương ứng qua RMI `syncReplicateCreate`.
   - Tab **Nhật ký đồng bộ (Audit Logs)** ghi nhận chi tiết: `LOCAL_ADD` trên Server 1 và `REPLICATE_IN_CREATE` trên Server 2.

### Kịch bản 2: Kiểm thử tính chịu lỗi (Failover)
1. Trên Dashboard, bấm nút đỏ **"Dừng S1 (1099)"**. Server 1 chuyển sang trạng thái `OFFLINE`.
2. Trên Client, bấm nút vàng **"🔄 Chuyển Server"**. Client tự động chuyển kết nối sang `Server 2 (Port 1100)`.
3. Thực hiện sửa đổi thông tin sinh viên trên Client. Mọi thao tác vẫn diễn ra bình thường trên Server 2!

### Kịch bản 3: Tự động phục hồi & Full Sync Reconciliation
1. Khi Server 1 đang tắt, trên Server 2 ta thực hiện thêm 2 sinh viên mới.
2. Trên Dashboard, bấm **"Bật S1 (1099)"**.
3. Heartbeat Timer phát hiện Server 1 đã online trở lại, Server 1 lập tức gọi `requestFullSyncState()` sang Server 2.
4. Toàn bộ các bản ghi mới trên Server 2 được đồng bộ sang Server 1. Cả hai Server đồng nhất 100%!

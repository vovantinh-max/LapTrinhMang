package test;

import database.DatabaseManager;
import database.LogDAO;
import database.LogDAO.LogEntry;
import database.UserDAO;
import server.FtpServer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FtpIntegrationTest {

    private static final int TEST_PORT = 21219;
    private static final File TEST_STORAGE = new File("build/test_storage");

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CHẠY BỘ KIỂM THỬ TỰ ĐỘNG (FTP INTEGRATION TEST)");
        System.out.println("==================================================");

        // Chuẩn bị thư mục test
        if (TEST_STORAGE.exists()) {
            deleteRecursive(TEST_STORAGE);
        }
        TEST_STORAGE.mkdirs();

        FtpServer server = new FtpServer(TEST_PORT, TEST_STORAGE);
        try {
            server.startServer();
            System.out.println("[PASS] 1. Khởi động FTP Server thành công.");

            // 1. Kiểm thử xác thực
            testAuthentication();

            // 2. Kiểm thử PASV và LIST
            testPassiveAndList();

            // 3. Kiểm thử STOR (Upload file)
            testUploadFile();

            // 4. Kiểm thử RETR (Download file)
            testDownloadFile();

            // 5. Kiểm thử Ghi nhận SQLite Database (Audit Logs)
            testDatabaseAuditLogs();

            // 6. Kiểm thử FtpClientEngine (Client kết nối và truyền nhận file)
            testFtpClientEngine();

            System.out.println("\n🎉 TẤT CẢ CÁC BÀI TEST ĐỀU ĐẠT CHUẨN 100%!");
        } catch (Exception e) {
            System.err.println("❌ BÀI TEST THẤT BẠI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            server.stopServer();
            deleteRecursive(TEST_STORAGE);
        }
    }

    private static void testAuthentication() throws IOException {
        System.out.print("Testing Authentication (Sai & Đúng password)... ");
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // Read welcome
            readUntil(reader, "220 ");

            // Test sai password
            sendCommand(writer, "USER admin");
            String res = reader.readLine();
            assert res.startsWith("331") : "Kỳ vọng 331, nhận: " + res;

            sendCommand(writer, "PASS wrong_pass");
            res = reader.readLine();
            assert res.startsWith("530") : "Kỳ vọng 530, nhận: " + res;

            // Test đúng password
            sendCommand(writer, "USER admin");
            reader.readLine();
            sendCommand(writer, "PASS admin123");
            res = reader.readLine();
            assert res.startsWith("230") : "Kỳ vọng 230, nhận: " + res;

            sendCommand(writer, "QUIT");
        }
        System.out.println("[OK]");
    }

    private static void testPassiveAndList() throws IOException {
        System.out.print("Testing PASV & LIST... ");
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readUntil(reader, "220 ");
            sendCommand(writer, "USER admin");
            reader.readLine();
            sendCommand(writer, "PASS admin123");
            reader.readLine();

            // Lệnh PWD
            sendCommand(writer, "PWD");
            String res = reader.readLine();
            assert res.startsWith("257") : "Kỳ vọng 257, nhận: " + res;

            // Lệnh PASV
            sendCommand(writer, "PASV");
            res = reader.readLine();
            assert res.startsWith("227") : "Kỳ vọng 227 PASV, nhận: " + res;

            // Parse data port
            int[] dataEndpoint = parsePasvResponse(res);
            String dataIp = dataEndpoint[0] + "." + dataEndpoint[1] + "." + dataEndpoint[2] + "." + dataEndpoint[3];
            int dataPort = dataEndpoint[4];

            // Kết nối socket dữ liệu
            Socket dataSocket = new Socket(dataIp, dataPort);
            sendCommand(writer, "LIST");

            String listRes = reader.readLine();
            assert listRes.startsWith("150") : "Kỳ vọng 150, nhận: " + listRes;

            // Đọc dữ liệu từ data socket
            BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8));
            while (dataReader.readLine() != null) {}
            dataSocket.close();

            String completeRes = reader.readLine();
            assert completeRes.startsWith("226") : "Kỳ vọng 226, nhận: " + completeRes;

            sendCommand(writer, "QUIT");
        }
        System.out.println("[OK]");
    }

    private static void testUploadFile() throws IOException {
        System.out.print("Testing STOR (Upload file)... ");
        String testContent = "Hello from automated test! UTF-8 tiếng Việt có dấu.";

        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readUntil(reader, "220 ");
            sendCommand(writer, "USER admin");
            reader.readLine();
            sendCommand(writer, "PASS admin123");
            reader.readLine();

            sendCommand(writer, "TYPE I");
            reader.readLine();

            sendCommand(writer, "PASV");
            String res = reader.readLine();
            int[] dataEndpoint = parsePasvResponse(res);
            String dataIp = dataEndpoint[0] + "." + dataEndpoint[1] + "." + dataEndpoint[2] + "." + dataEndpoint[3];
            int dataPort = dataEndpoint[4];

            Socket dataSocket = new Socket(dataIp, dataPort);
            sendCommand(writer, "STOR test_upload.txt");

            String storRes = reader.readLine();
            assert storRes.startsWith("150") : "Kỳ vọng 150, nhận: " + storRes;

            OutputStream dataOut = dataSocket.getOutputStream();
            dataOut.write(testContent.getBytes(StandardCharsets.UTF_8));
            dataOut.flush();
            dataSocket.close();

            String completeRes = reader.readLine();
            assert completeRes.startsWith("226") : "Kỳ vọng 226, nhận: " + completeRes;

            sendCommand(writer, "QUIT");
        }

        File uploadedFile = new File(TEST_STORAGE, "test_upload.txt");
        assert uploadedFile.exists() : "File tải lên không tồn tại trên đĩa!";
        System.out.println("[OK]");
    }

    private static void testDownloadFile() throws IOException {
        System.out.print("Testing RETR (Download file)... ");
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            readUntil(reader, "220 ");
            sendCommand(writer, "USER admin");
            reader.readLine();
            sendCommand(writer, "PASS admin123");
            reader.readLine();

            sendCommand(writer, "TYPE I");
            reader.readLine();

            sendCommand(writer, "PASV");
            String res = reader.readLine();
            int[] dataEndpoint = parsePasvResponse(res);
            String dataIp = dataEndpoint[0] + "." + dataEndpoint[1] + "." + dataEndpoint[2] + "." + dataEndpoint[3];
            int dataPort = dataEndpoint[4];

            Socket dataSocket = new Socket(dataIp, dataPort);
            sendCommand(writer, "RETR test_upload.txt");

            String retrRes = reader.readLine();
            assert retrRes.startsWith("150") : "Kỳ vọng 150, nhận: " + retrRes;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream dataIn = dataSocket.getInputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = dataIn.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            dataSocket.close();

            String downloadedText = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            assert downloadedText.contains("Hello from automated test!") : "Nội dung tải về không khớp!";

            String completeRes = reader.readLine();
            assert completeRes.startsWith("226") : "Kỳ vọng 226, nhận: " + completeRes;

            sendCommand(writer, "QUIT");
        }
        System.out.println("[OK]");
    }

    private static void testDatabaseAuditLogs() {
        System.out.print("Testing SQLite Audit Logs... ");
        List<LogEntry> logs = LogDAO.getRecentLogs(20);
        assert !logs.isEmpty() : "Logs không được ghi vào SQLite!";
        boolean foundUpload = false;
        boolean foundDownload = false;
        for (LogEntry l : logs) {
            if ("UPLOAD".equals(l.getAction())) foundUpload = true;
            if ("DOWNLOAD".equals(l.getAction())) foundDownload = true;
        }
        assert foundUpload && foundDownload : "Thiếu log UPLOAD hoặc DOWNLOAD trong SQLite!";
        System.out.println("[OK]");
    }

    private static void testFtpClientEngine() throws IOException {
        System.out.print("Testing FtpClientEngine (Client kết nối & truyền tệp)... ");
        client.FtpClientEngine clientEngine = new client.FtpClientEngine();
        clientEngine.connect("127.0.0.1", TEST_PORT, "admin", "admin123");
        assert clientEngine.isConnected() : "Client chưa kết nối!";

        // Tạo file tạm để test upload
        File tempUpload = new File(TEST_STORAGE, "client_engine_upload.txt");
        try (FileOutputStream fos = new FileOutputStream(tempUpload)) {
            fos.write("Dữ liệu truyền từ FtpClientEngine!".getBytes(StandardCharsets.UTF_8));
        }

        // Upload
        clientEngine.uploadFile(tempUpload, "remote_engine_test.txt");

        // List files
        List<client.FtpClientEngine.FtpFileItem> files = clientEngine.listFiles();
        boolean foundRemote = false;
        for (client.FtpClientEngine.FtpFileItem f : files) {
            if (f.name.equals("remote_engine_test.txt")) foundRemote = true;
        }
        assert foundRemote : "Không tìm thấy file vừa upload bằng FtpClientEngine!";

        // Download
        File tempDownload = new File(TEST_STORAGE, "client_engine_download.txt");
        clientEngine.downloadFile("remote_engine_test.txt", tempDownload);
        assert tempDownload.exists() : "Download file từ FtpClientEngine thất bại!";

        clientEngine.disconnect();
        System.out.println("[OK]");
    }

    private static void sendCommand(BufferedWriter writer, String cmd) throws IOException {
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private static void readUntil(BufferedReader reader, String prefix) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(prefix)) break;
        }
    }

    private static int[] parsePasvResponse(String response) {
        int start = response.indexOf('(');
        int end = response.indexOf(')');
        String sub = response.substring(start + 1, end);
        String[] parts = sub.split(",");
        int h1 = Integer.parseInt(parts[0].trim());
        int h2 = Integer.parseInt(parts[1].trim());
        int h3 = Integer.parseInt(parts[2].trim());
        int h4 = Integer.parseInt(parts[3].trim());
        int p1 = Integer.parseInt(parts[4].trim());
        int p2 = Integer.parseInt(parts[5].trim());
        int port = (p1 << 8) | p2;
        return new int[]{h1, h2, h3, h4, port};
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        f.delete();
    }
}

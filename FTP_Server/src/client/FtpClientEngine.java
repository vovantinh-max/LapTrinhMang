package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FtpClientEngine {

    public interface FtpEventListener {
        void onConnected(String host, int port, String user);
        void onDisconnected();
        void onLog(String line);
        void onProgress(String fileName, long transferred, long total);
    }

    public static class FtpFileItem {
        public String name;
        public boolean isDirectory;
        public long size;
        public String rawPermissions;
        public String date;

        public FtpFileItem(String name, boolean isDirectory, long size, String rawPermissions, String date) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.rawPermissions = rawPermissions;
            this.date = date;
        }
    }

    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean connected = false;
    private String currentRemoteDir = "/";
    private FtpEventListener listener;

    public void setListener(FtpEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected && controlSocket != null && !controlSocket.isClosed();
    }

    public synchronized void connect(String host, int port, String username, String password) throws IOException {
        disconnect();

        log("[Client] Đang kết nối tới " + host + ":" + port + "...");
        controlSocket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8));

        // Đọc thông điệp chào mừng 220
        readResponse();

        // USER
        String res = sendCommand("USER " + username);
        if (res.startsWith("331")) {
            // PASS
            res = sendCommand("PASS " + password);
            if (!res.startsWith("230")) {
                disconnect();
                throw new IOException("Đăng nhập thất bại: " + res);
            }
        } else if (!res.startsWith("230")) {
            disconnect();
            throw new IOException("Tên đăng nhập bị từ chối: " + res);
        }

        // Chuyển sang binary mode
        sendCommand("TYPE I");

        connected = true;
        currentRemoteDir = getPwd();

        log("[Client] Kết nối và đăng nhập thành công (" + username + ")!");
        if (listener != null) {
            listener.onConnected(host, port, username);
        }
    }

    public synchronized void disconnect() {
        if (connected) {
            try {
                sendCommand("QUIT");
            } catch (Exception ignored) {}
        }
        connected = false;
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (IOException ignored) {}

        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public synchronized String getPwd() throws IOException {
        String res = sendCommand("PWD");
        int first = res.indexOf('"');
        int last = res.lastIndexOf('"');
        if (first != -1 && last > first) {
            currentRemoteDir = res.substring(first + 1, last);
            return currentRemoteDir;
        }
        return "/";
    }

    public synchronized void changeDirectory(String dir) throws IOException {
        String res = sendCommand("CWD " + dir);
        if (!res.startsWith("250")) {
            throw new IOException("Không thể đổi thư mục: " + res);
        }
        getPwd();
    }

    public synchronized void changeToParentDir() throws IOException {
        String res = sendCommand("CDUP");
        if (!res.startsWith("250")) {
            throw new IOException("Không thể lên thư mục cha: " + res);
        }
        getPwd();
    }

    public synchronized List<FtpFileItem> listFiles() throws IOException {
        String pasvRes = sendCommand("PASV");
        int[] endpoint = parsePasv(pasvRes);
        String dataIp = endpoint[0] + "." + endpoint[1] + "." + endpoint[2] + "." + endpoint[3];
        int dataPort = endpoint[4];

        List<FtpFileItem> items = new ArrayList<>();
        try (Socket dataSocket = new Socket(dataIp, dataPort)) {
            String res = sendCommand("LIST");
            if (!res.startsWith("150")) {
                throw new IOException("Lệnh LIST thất bại: " + res);
            }

            try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = dataReader.readLine()) != null) {
                    FtpFileItem item = parseUnixListLine(line);
                    if (item != null) items.add(item);
                }
            }
            readResponse(); // Read 226 Directory send OK
        }
        return items;
    }

    public synchronized void uploadFile(File localFile, String remoteFileName) throws IOException {
        String pasvRes = sendCommand("PASV");
        int[] endpoint = parsePasv(pasvRes);
        String dataIp = endpoint[0] + "." + endpoint[1] + "." + endpoint[2] + "." + endpoint[3];
        int dataPort = endpoint[4];

        long totalSize = localFile.length();
        long transferred = 0;

        try (Socket dataSocket = new Socket(dataIp, dataPort);
             InputStream fileIn = new BufferedInputStream(new FileInputStream(localFile));
             OutputStream dataOut = new BufferedOutputStream(dataSocket.getOutputStream())) {

            String res = sendCommand("STOR " + remoteFileName);
            if (!res.startsWith("150")) {
                throw new IOException("Không thể bắt đầu upload: " + res);
            }

            byte[] buf = new byte[8192];
            int read;
            while ((read = fileIn.read(buf)) != -1) {
                dataOut.write(buf, 0, read);
                transferred += read;
                if (listener != null) {
                    listener.onProgress(remoteFileName, transferred, totalSize);
                }
            }
            dataOut.flush();
        }
        readResponse(); // Read 226 Transfer complete
    }

    public synchronized void downloadFile(String remoteFileName, File localDestination) throws IOException {
        String pasvRes = sendCommand("PASV");
        int[] endpoint = parsePasv(pasvRes);
        String dataIp = endpoint[0] + "." + endpoint[1] + "." + endpoint[2] + "." + endpoint[3];
        int dataPort = endpoint[4];

        // Lấy kích thước file nếu có
        long totalSize = 0;
        try {
            String sizeRes = sendCommand("SIZE " + remoteFileName);
            if (sizeRes.startsWith("213")) {
                totalSize = Long.parseLong(sizeRes.substring(4).trim());
            }
        } catch (Exception ignored) {}

        long transferred = 0;

        try (Socket dataSocket = new Socket(dataIp, dataPort);
             OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(localDestination));
             InputStream dataIn = new BufferedInputStream(dataSocket.getInputStream())) {

            String res = sendCommand("RETR " + remoteFileName);
            if (!res.startsWith("150")) {
                throw new IOException("Không thể tải file: " + res);
            }

            byte[] buf = new byte[8192];
            int read;
            while ((read = dataIn.read(buf)) != -1) {
                fileOut.write(buf, 0, read);
                transferred += read;
                if (listener != null) {
                    listener.onProgress(remoteFileName, transferred, totalSize > 0 ? totalSize : transferred);
                }
            }
            fileOut.flush();
        }
        readResponse(); // Read 226 Transfer complete
    }

    public synchronized void deleteFile(String remoteFileName) throws IOException {
        String res = sendCommand("DELE " + remoteFileName);
        if (!res.startsWith("250")) {
            throw new IOException("Không thể xóa file: " + res);
        }
    }

    public synchronized void makeDirectory(String dirName) throws IOException {
        String res = sendCommand("MKD " + dirName);
        if (!res.startsWith("257") && !res.startsWith("250")) {
            throw new IOException("Không thể tạo thư mục: " + res);
        }
    }

    public synchronized void removeDirectory(String dirName) throws IOException {
        String res = sendCommand("RMD " + dirName);
        if (!res.startsWith("250")) {
            throw new IOException("Không thể xóa thư mục: " + res);
        }
    }

    public String getCurrentRemoteDir() {
        return currentRemoteDir;
    }

    private String sendCommand(String cmd) throws IOException {
        log(">> " + cmd);
        writer.write(cmd + "\r\n");
        writer.flush();
        String response = readResponse();
        log("<< " + response);
        return response;
    }

    private String readResponse() throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Server đã đóng kết nối.");

        // Xử lý multiline responses (e.g. 220- ... 220 )
        if (line.length() >= 4 && line.charAt(3) == '-') {
            String code = line.substring(0, 3);
            StringBuilder sb = new StringBuilder(line);
            while ((line = reader.readLine()) != null) {
                sb.append("\n").append(line);
                if (line.startsWith(code + " ")) break;
            }
            return sb.toString();
        }
        return line;
    }

    private int[] parsePasv(String res) throws IOException {
        int start = res.indexOf('(');
        int end = res.indexOf(')');
        if (start == -1 || end == -1) {
            throw new IOException("Phản hồi PASV không hợp lệ: " + res);
        }
        String[] parts = res.substring(start + 1, end).split(",");
        int h1 = Integer.parseInt(parts[0].trim());
        int h2 = Integer.parseInt(parts[1].trim());
        int h3 = Integer.parseInt(parts[2].trim());
        int h4 = Integer.parseInt(parts[3].trim());
        int p1 = Integer.parseInt(parts[4].trim());
        int p2 = Integer.parseInt(parts[5].trim());
        int port = (p1 << 8) | p2;
        return new int[]{h1, h2, h3, h4, port};
    }

    private FtpFileItem parseUnixListLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\s+", 9);
        if (parts.length < 9) return null;

        String perms = parts[0];
        boolean isDir = perms.startsWith("d");
        long size = 0;
        try {
            size = Long.parseLong(parts[4]);
        } catch (NumberFormatException ignored) {}

        String date = parts[5] + " " + parts[6] + " " + parts[7];
        String name = parts[8];

        return new FtpFileItem(name, isDir, size, perms, date);
    }

    private void log(String line) {
        if (listener != null) {
            listener.onLog(line);
        }
    }
}

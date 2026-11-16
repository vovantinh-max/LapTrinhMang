package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FtpDataConnection {
    public enum Mode {
        PASSIVE,
        ACTIVE
    }

    private Mode mode = Mode.PASSIVE;
    private ServerSocket passiveServerSocket;
    private String activeHost;
    private int activePort;

    /**
     * Khởi tạo chế độ Passive (PASV)
     * Mở một ServerSocket trên cổng ngẫu nhiên khả dụng.
     */
    public synchronized String setupPassive(InetAddress serverLocalAddress) throws IOException {
        close();
        mode = Mode.PASSIVE;
        // Mở ServerSocket trên cổng khả dụng bất kỳ
        passiveServerSocket = new ServerSocket(0, 1, serverLocalAddress);
        passiveServerSocket.setSoTimeout(30000); // 30s timeout chờ client kết nối dữ liệu

        int port = passiveServerSocket.getLocalPort();
        byte[] addressBytes = serverLocalAddress.getAddress();
        
        // Nếu là loopback/0.0.0.0 hoặc wildcard, fallback về 127.0.0.1
        if (serverLocalAddress.isAnyLocalAddress()) {
            addressBytes = new byte[]{127, 0, 0, 1};
        }

        int h1 = addressBytes[0] & 0xFF;
        int h2 = addressBytes[1] & 0xFF;
        int h3 = addressBytes[2] & 0xFF;
        int h4 = addressBytes[3] & 0xFF;

        int p1 = (port >> 8) & 0xFF;
        int p2 = port & 0xFF;

        return String.format("%d,%d,%d,%d,%d,%d", h1, h2, h3, h4, p1, p2);
    }

    /**
     * Khởi tạo chế độ Active (PORT)
     */
    public synchronized void setupActive(String host, int port) {
        close();
        mode = Mode.ACTIVE;
        this.activeHost = host;
        this.activePort = port;
    }

    /**
     * Mở Socket truyền nhận dữ liệu với Client
     */
    public synchronized Socket openDataSocket() throws IOException {
        if (mode == Mode.PASSIVE) {
            if (passiveServerSocket == null || passiveServerSocket.isClosed()) {
                throw new IOException("Passive ServerSocket chưa được khởi tạo hoặc đã đóng.");
            }
            Socket socket = passiveServerSocket.accept();
            return socket;
        } else {
            if (activeHost == null || activePort <= 0) {
                throw new IOException("Địa chỉ Active PORT không hợp lệ.");
            }
            return new Socket(activeHost, activePort);
        }
    }

    /**
     * Đóng kênh kết nối dữ liệu
     */
    public synchronized void close() {
        if (passiveServerSocket != null && !passiveServerSocket.isClosed()) {
            try {
                passiveServerSocket.close();
            } catch (IOException ignored) {}
            passiveServerSocket = null;
        }
        activeHost = null;
        activePort = 0;
    }

    public Mode getMode() {
        return mode;
    }
}

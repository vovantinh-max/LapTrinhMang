package server;

import database.DatabaseManager;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FtpServer implements FtpClientHandler.ServerEventCallback {

    public interface ServerStatusListener {
        void onServerStarted(int port, String rootDir);
        void onServerStopped();
        void onLogMessage(String message);
        void onClientListUpdated(List<FtpClientHandler> clients);
    }

    private int port;
    private File baseStorageDir;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final List<FtpClientHandler> activeClients = Collections.synchronizedList(new ArrayList<>());
    private ServerStatusListener listener;

    public FtpServer(int port, File baseStorageDir) {
        this.port = port;
        this.baseStorageDir = baseStorageDir;
    }

    public void setListener(ServerStatusListener listener) {
        this.listener = listener;
    }

    public synchronized void startServer() throws IOException {
        if (running) return;

        // Đảm bảo thư mục lưu trữ tồn tại
        if (!baseStorageDir.exists()) {
            baseStorageDir.mkdirs();
        }

        // Khởi tạo cơ sở dữ liệu
        DatabaseManager.initializeDatabase();

        serverSocket = new ServerSocket(port);
        running = true;

        log("[FTP Server] Máy chủ đã khởi động trên cổng: " + port);
        log("[FTP Server] Thư mục lưu trữ gốc: " + baseStorageDir.getAbsolutePath());

        if (listener != null) {
            listener.onServerStarted(port, baseStorageDir.getAbsolutePath());
        }

        // Thread lắng nghe kết nối mới
        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    String clientIp = socket.getInetAddress().getHostAddress();
                    log("[Kết nối mới] Client từ: " + clientIp);

                    FtpClientHandler handler = new FtpClientHandler(socket, baseStorageDir, this);
                    activeClients.add(handler);
                    notifyClientsChanged();

                    // Chạy mỗi client trên một Thread riêng biệt
                    Thread clientThread = new Thread(handler, "FTP-Client-" + clientIp);
                    clientThread.start();
                } catch (IOException e) {
                    if (running) {
                        log("[Lỗi Server] " + e.getMessage());
                    }
                }
            }
        }, "FTP-Accept-Thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stopServer() {
        if (!running) return;
        running = false;

        // Đóng toàn bộ các kết nối client đang active
        List<FtpClientHandler> toClose;
        synchronized (activeClients) {
            toClose = new ArrayList<>(activeClients);
            activeClients.clear();
        }
        for (FtpClientHandler handler : toClose) {
            handler.close();
        }
        notifyClientsChanged();

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }

        log("[FTP Server] Máy chủ đã dừng.");
        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public File getBaseStorageDir() {
        return baseStorageDir;
    }

    public void setBaseStorageDir(File baseStorageDir) {
        this.baseStorageDir = baseStorageDir;
    }

    public List<FtpClientHandler> getActiveClients() {
        return new ArrayList<>(activeClients);
    }

    @Override
    public void onClientCommand(String clientIp, String command, String response) {
        log(String.format("[%s] >> %s", clientIp, command));
        notifyClientsChanged();
    }

    @Override
    public void onClientDisconnected(FtpClientHandler handler) {
        activeClients.remove(handler);
        log("[Ngắt kết nối] Client IP: " + handler.getClientIp() + " (" + handler.getUsername() + ")");
        notifyClientsChanged();
    }

    private void notifyClientsChanged() {
        if (listener != null) {
            listener.onClientListUpdated(getActiveClients());
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        if (listener != null) {
            listener.onLogMessage(msg);
        }
    }

    /**
     * Chạy server ở chế độ Console dòng lệnh
     */
    public static void main(String[] args) {
        int port = 2121;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        File storage = new File("data/storage");
        FtpServer server = new FtpServer(port, storage);
        try {
            server.startServer();
            System.out.println("Nhấn Enter để dừng server...");
            System.in.read();
            server.stopServer();
        } catch (IOException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
        }
    }
}

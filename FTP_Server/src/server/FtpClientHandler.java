package server;

import database.LogDAO;
import database.UserDAO;
import database.UserDAO.User;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FtpClientHandler implements Runnable {
    public interface ServerEventCallback {
        void onClientCommand(String clientIp, String command, String response);
        void onClientDisconnected(FtpClientHandler handler);
    }

    private final Socket controlSocket;
    private final File baseStorageDir;
    private final ServerEventCallback callback;
    private final FtpSession session;
    private final String clientIp;
    private final String connectTime;

    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean running = true;

    public FtpClientHandler(Socket socket, File baseStorageDir, ServerEventCallback callback) {
        this.controlSocket = socket;
        this.baseStorageDir = baseStorageDir;
        this.callback = callback;
        this.session = new FtpSession(baseStorageDir);
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.connectTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getConnectTime() {
        return connectTime;
    }

    public String getUsername() {
        return session.isAuthenticated() ? session.getUser().getUsername() : "(Chưa đăng nhập)";
    }

    public String getCurrentDir() {
        return session.getCurrentVirtualDir();
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"));

            // 220 Service ready for new user.
            sendReply("220- Chào mừng bạn đến với FTP Server (VKU Lập trình mạng)!");
            sendReply("220 Máy chủ sẵn sàng kết nối.");

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int spaceIdx = line.indexOf(' ');
                String command = (spaceIdx == -1 ? line : line.substring(0, spaceIdx)).toUpperCase(Locale.ROOT);
                String argument = spaceIdx == -1 ? "" : line.substring(spaceIdx + 1).trim();

                processCommand(command, argument, line);

                if (command.equals("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            // Client ngắt kết nối đột ngột
        } finally {
            close();
        }
    }

    private void processCommand(String command, String argument, String rawLine) throws IOException {
        switch (command) {
            case "USER":
                handleUser(argument);
                break;
            case "PASS":
                handlePass(argument);
                break;
            case "SYST":
                sendReply("215 UNIX Type: L8");
                break;
            case "FEAT":
                sendReply("211-Features:");
                sendReply(" UTF8");
                sendReply(" SIZE");
                sendReply(" PASV");
                sendReply(" REST STREAM");
                sendReply("211 End");
                break;
            case "OPTS":
                if (argument.toUpperCase().startsWith("UTF8")) {
                    sendReply("200 Always in UTF8 mode.");
                } else {
                    sendReply("502 Option not understood.");
                }
                break;
            case "PWD":
            case "XPWD":
                sendReply("257 \"" + session.getCurrentVirtualDir() + "\" is current directory.");
                break;
            case "TYPE":
                handleType(argument);
                break;
            case "PASV":
                handlePasv();
                break;
            case "PORT":
                handlePort(argument);
                break;
            case "LIST":
            case "NLST":
                handleList(argument, command.equals("NLST"));
                break;
            case "MLSD":
            case "MLST":
                // Báo không hỗ trợ để client dùng chuẩn LIST
                sendReply("502 Command not implemented. Use LIST instead.");
                break;
            case "CWD":
            case "XCWD":
                handleCwd(argument);
                break;
            case "CDUP":
            case "XCUP":
                handleCdup();
                break;
            case "SIZE":
                handleSize(argument);
                break;
            case "RETR":
                handleRetr(argument);
                break;
            case "STOR":
                handleStor(argument);
                break;
            case "DELE":
                handleDele(argument);
                break;
            case "MKD":
            case "XMKD":
                handleMkd(argument);
                break;
            case "RMD":
            case "XRMD":
                handleRmd(argument);
                break;
            case "RNFR":
                handleRnfr(argument);
                break;
            case "RNTO":
                handleRnto(argument);
                break;
            case "NOOP":
                sendReply("200 OK.");
                break;
            case "QUIT":
                sendReply("221 Goodbye.");
                if (session.isAuthenticated()) {
                    LogDAO.log(session.getUser().getUsername(), clientIp, "LOGOUT", "Ngắt kết nối bình thường");
                }
                running = false;
                break;
            case "AUTH":
            case "AUTH TLS":
            case "AUTH SSL":
                // Báo không hỗ trợ mã hóa SSL để FileZilla chuyển sang plain FTP
                sendReply("502 Command not implemented, fallback to plain FTP.");
                break;
            default:
                sendReply("502 Command not implemented.");
                break;
        }

        if (callback != null) {
            callback.onClientCommand(clientIp, rawLine, "Command executed");
        }
    }

    private void handleUser(String username) throws IOException {
        if (username.isEmpty()) {
            sendReply("501 Syntax error in parameters.");
            return;
        }
        session.setUserCandidate(username);
        sendReply("331 User name okay, need password for " + username + ".");
    }

    private void handlePass(String password) throws IOException {
        String username = session.getUserCandidate();
        if (username == null || username.isEmpty()) {
            sendReply("503 Bad sequence of commands. Send USER first.");
            return;
        }

        User user = UserDAO.authenticate(username, password);
        if (user != null) {
            session.setUser(user);
            sendReply("230 User " + username + " logged in successfully.");
            LogDAO.log(username, clientIp, "LOGIN_SUCCESS", "Đăng nhập thành công, homeDir: " + user.getHomeDir());
        } else {
            sendReply("530 Login incorrect. Sai tên đăng nhập hoặc mật khẩu.");
            LogDAO.log(username, clientIp, "LOGIN_FAILED", "Thất bại: sai thông tin tài khoản");
        }
    }

    private void handleType(String type) throws IOException {
        if (type.equalsIgnoreCase("A")) {
            session.setBinaryMode(false);
            sendReply("200 Type set to A (ASCII).");
        } else if (type.equalsIgnoreCase("I") || type.equalsIgnoreCase("L") || type.equalsIgnoreCase("L 8")) {
            session.setBinaryMode(true);
            sendReply("200 Type set to I (Binary).");
        } else {
            sendReply("504 Command not implemented for that parameter.");
        }
    }

    private void handlePasv() throws IOException {
        try {
            String pasvResponse = session.getDataConnection().setupPassive(controlSocket.getLocalAddress());
            sendReply("227 Entering Passive Mode (" + pasvResponse + ").");
        } catch (IOException e) {
            sendReply("425 Can't open passive connection: " + e.getMessage());
        }
    }

    private void handlePort(String arg) throws IOException {
        String[] parts = arg.split(",");
        if (parts.length != 6) {
            sendReply("501 Syntax error in PORT command.");
            return;
        }
        try {
            String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
            int p1 = Integer.parseInt(parts[4]);
            int p2 = Integer.parseInt(parts[5]);
            int port = (p1 << 8) | p2;
            session.getDataConnection().setupActive(host, port);
            sendReply("200 PORT command successful.");
        } catch (Exception e) {
            sendReply("501 Syntax error in PORT parameters.");
        }
    }

    private void handleList(String path, boolean nameOnly) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanRead()) {
            sendReply("550 Permission denied. Tài khoản không có quyền đọc.");
            return;
        }

        File target = session.resolveVirtualPath(path);
        if (!target.exists()) {
            sendReply("550 File or directory not found.");
            return;
        }

        sendReply("150 Here comes the directory listing.");

        try (Socket dataSocket = session.getDataConnection().openDataSocket();
             BufferedWriter dataWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(), "UTF-8"))) {

            File[] files = target.isDirectory() ? target.listFiles() : new File[]{target};
            if (files != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm", Locale.US);
                for (File f : files) {
                    if (nameOnly) {
                        dataWriter.write(f.getName() + "\r\n");
                    } else {
                        // Định dạng UNIX chuẩn cho FileZilla
                        String type = f.isDirectory() ? "d" : "-";
                        String perms = f.isDirectory() ? "rwxr-xr-x" : "rw-r--r--";
                        long size = f.isDirectory() ? 4096 : f.length();
                        String dateStr = sdf.format(new Date(f.lastModified()));
                        String line = String.format("%s%s   1 owner    group %10d %s %s\r\n",
                                type, perms, size, dateStr, f.getName());
                        dataWriter.write(line);
                    }
                }
            }
            dataWriter.flush();
            sendReply("226 Directory send OK.");
        } catch (IOException e) {
            sendReply("426 Connection closed; transfer aborted: " + e.getMessage());
        } finally {
            session.getDataConnection().close();
        }
    }

    private void handleCwd(String path) throws IOException {
        if (!checkAuth()) return;
        if (session.changeWorkingDirectory(path)) {
            sendReply("250 Directory successfully changed to \"" + session.getCurrentVirtualDir() + "\".");
        } else {
            sendReply("550 Failed to change directory. Thư mục không tồn tại.");
        }
    }

    private void handleCdup() throws IOException {
        if (!checkAuth()) return;
        if (session.changeWorkingDirectory("..")) {
            sendReply("250 Directory successfully changed to \"" + session.getCurrentVirtualDir() + "\".");
        } else {
            sendReply("550 Failed to change directory.");
        }
    }

    private void handleSize(String path) throws IOException {
        if (!checkAuth()) return;
        File file = session.resolveVirtualPath(path);
        if (file.exists() && file.isFile()) {
            sendReply("213 " + file.length());
        } else {
            sendReply("550 Could not get file size.");
        }
    }

    private void handleRetr(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanRead()) {
            sendReply("550 Permission denied. Không có quyền tải file.");
            return;
        }

        File file = session.resolveVirtualPath(path);
        if (!file.exists() || !file.isFile()) {
            sendReply("550 File not found or is a directory.");
            return;
        }

        sendReply("150 Opening BINARY mode data connection for " + file.getName() + " (" + file.length() + " bytes).");

        try (Socket dataSocket = session.getDataConnection().openDataSocket();
             InputStream fileIn = new BufferedInputStream(new FileInputStream(file));
             OutputStream dataOut = new BufferedOutputStream(dataSocket.getOutputStream())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long total = 0;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
                total += bytesRead;
            }
            dataOut.flush();
            sendReply("226 Transfer complete.");
            LogDAO.log(session.getUser().getUsername(), clientIp, "DOWNLOAD",
                    "File: " + file.getName() + ", Kích thước: " + total + " bytes");
        } catch (IOException e) {
            sendReply("426 Connection closed; transfer aborted: " + e.getMessage());
        } finally {
            session.getDataConnection().close();
        }
    }

    private void handleStor(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanWrite()) {
            sendReply("550 Permission denied. Không có quyền tải lên / ghi file.");
            return;
        }

        File file = session.resolveVirtualPath(path);

        sendReply("150 Ok to send data.");

        try (Socket dataSocket = session.getDataConnection().openDataSocket();
             InputStream dataIn = new BufferedInputStream(dataSocket.getInputStream());
             OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file))) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long total = 0;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                total += bytesRead;
            }
            fileOut.flush();
            sendReply("226 Transfer complete.");
            LogDAO.log(session.getUser().getUsername(), clientIp, "UPLOAD",
                    "File: " + file.getName() + ", Kích thước: " + total + " bytes");
        } catch (IOException e) {
            sendReply("426 Connection closed; transfer aborted: " + e.getMessage());
        } finally {
            session.getDataConnection().close();
        }
    }

    private void handleDele(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanWrite()) {
            sendReply("550 Permission denied. Không có quyền xóa.");
            return;
        }
        File file = session.resolveVirtualPath(path);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                sendReply("250 File deleted successfully.");
                LogDAO.log(session.getUser().getUsername(), clientIp, "DELETE_FILE", "Xóa: " + file.getName());
            } else {
                sendReply("450 Action not taken: Delete failed.");
            }
        } else {
            sendReply("550 File not found.");
        }
    }

    private void handleMkd(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanWrite()) {
            sendReply("550 Permission denied. Không có quyền tạo thư mục.");
            return;
        }
        File dir = session.resolveVirtualPath(path);
        if (dir.exists()) {
            sendReply("550 Directory already exists.");
        } else {
            if (dir.mkdirs()) {
                sendReply("257 \"" + session.fileToVirtualPath(dir) + "\" created successfully.");
                LogDAO.log(session.getUser().getUsername(), clientIp, "MKDIR", "Tạo thư mục: " + dir.getName());
            } else {
                sendReply("550 Failed to create directory.");
            }
        }
    }

    private void handleRmd(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanWrite()) {
            sendReply("550 Permission denied. Không có quyền xóa thư mục.");
            return;
        }
        File dir = session.resolveVirtualPath(path);
        if (dir.exists() && dir.isDirectory()) {
            if (dir.delete()) {
                sendReply("250 Directory removed.");
                LogDAO.log(session.getUser().getUsername(), clientIp, "RMDIR", "Xóa thư mục: " + dir.getName());
            } else {
                sendReply("450 Directory not empty or delete failed.");
            }
        } else {
            sendReply("550 Directory not found.");
        }
    }

    private void handleRnfr(String path) throws IOException {
        if (!checkAuth()) return;
        if (!session.getUser().isCanWrite()) {
            sendReply("550 Permission denied.");
            return;
        }
        File file = session.resolveVirtualPath(path);
        if (file.exists()) {
            session.setRenameFromFile(file);
            sendReply("350 File exists, ready for destination name (RNTO).");
        } else {
            sendReply("550 File not found.");
        }
    }

    private void handleRnto(String path) throws IOException {
        if (!checkAuth()) return;
        File from = session.getRenameFromFile();
        if (from == null) {
            sendReply("503 Bad sequence of commands. Gửi RNFR trước.");
            return;
        }
        File to = session.resolveVirtualPath(path);
        if (to.exists()) {
            sendReply("550 Target file already exists.");
            return;
        }
        if (from.renameTo(to)) {
            sendReply("250 Rename successful.");
            LogDAO.log(session.getUser().getUsername(), clientIp, "RENAME",
                    "Đổi từ " + from.getName() + " thành " + to.getName());
            session.setRenameFromFile(null);
        } else {
            sendReply("550 Rename failed.");
        }
    }

    private boolean checkAuth() throws IOException {
        if (!session.isAuthenticated()) {
            sendReply("530 Please login with USER and PASS first.");
            return false;
        }
        return true;
    }

    public void sendReply(String text) throws IOException {
        writer.write(text + "\r\n");
        writer.flush();
    }

    public void close() {
        running = false;
        session.getDataConnection().close();
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (IOException ignored) {}

        if (callback != null) {
            callback.onClientDisconnected(this);
        }
    }
}

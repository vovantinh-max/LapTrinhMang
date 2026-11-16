package client;

import database.EmailDAO.Email;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Pop3ClientEngine {

    public interface Pop3ClientLogListener {
        void onLog(String line);
    }

    public static class Pop3MessageInfo {
        public int number;
        public int size;

        public Pop3MessageInfo(int number, int size) {
            this.number = number;
            this.size = size;
        }
    }

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean connected = false;
    private Pop3ClientLogListener logListener;

    public void setLogListener(Pop3ClientLogListener listener) {
        this.logListener = listener;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void connect(String host, int port, String username, String password) throws IOException {
        disconnect();

        log("[POP3 Client] Đang kết nối tới " + host + ":" + port + "...");
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        expectOk();
        sendCmd("USER " + username);
        expectOk();
        sendCmd("PASS " + password);
        expectOk();

        connected = true;
        log("[POP3 Client] Đăng nhập POP3 thành công (" + username + ")!");
    }

    public List<Pop3MessageInfo> listMessages() throws IOException {
        if (!connected) throw new IOException("Chưa kết nối tới POP3 Server!");

        sendCmd("LIST");
        expectOk();

        List<Pop3MessageInfo> list = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int num = Integer.parseInt(parts[0]);
                    int size = Integer.parseInt(parts[1]);
                    list.add(new Pop3MessageInfo(num, size));
                } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    public Email retrieveEmail(int msgNum) throws IOException {
        if (!connected) throw new IOException("Chưa kết nối tới POP3 Server!");

        sendCmd("RETR " + msgNum);
        expectOk();

        Email email = new Email();
        email.setId(msgNum);

        String from = "";
        String to = "";
        String subject = "(Không có tiêu đề)";
        String date = "";
        StringBuilder body = new StringBuilder();
        boolean inHeader = true;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (line.startsWith("..")) line = line.substring(1); // Dot-unstuffing

            if (inHeader) {
                if (line.isEmpty()) {
                    inHeader = false;
                } else {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.startsWith("from:")) {
                        from = line.substring(5).trim();
                    } else if (lower.startsWith("to:")) {
                        to = line.substring(3).trim();
                    } else if (lower.startsWith("subject:")) {
                        subject = line.substring(8).trim();
                    } else if (lower.startsWith("date:")) {
                        date = line.substring(5).trim();
                    }
                }
            } else {
                body.append(line).append("\n");
            }
        }

        email.setSender(from);
        email.setRecipient(to);
        email.setSubject(subject);
        email.setDateSent(date);
        email.setBody(body.toString().trim());

        return email;
    }

    public void deleteEmail(int msgNum) throws IOException {
        if (!connected) throw new IOException("Chưa kết nối tới POP3 Server!");
        sendCmd("DELE " + msgNum);
        expectOk();
    }

    public void disconnect() {
        if (connected) {
            try {
                sendCmd("QUIT");
                expectOk();
            } catch (Exception ignored) {}
        }
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void sendCmd(String cmd) throws IOException {
        log(">> " + (cmd.startsWith("PASS") ? "PASS ********" : cmd));
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private String expectOk() throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Máy chủ đóng kết nối đột ngột.");
        log("<< " + line);
        if (!line.startsWith("+OK")) {
            throw new IOException("Lỗi POP3: " + line);
        }
        return line;
    }

    private void log(String s) {
        if (logListener != null) {
            logListener.onLog(s);
        }
    }
}

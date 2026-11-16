package server;

import database.EmailDAO;
import database.EmailDAO.Email;
import database.MailLogDAO;
import database.UserDAO;
import database.UserDAO.MailUser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Pop3ServerEngine implements Runnable {

    public interface Pop3EventListener {
        void onPop3Command(String clientIp, String command, String response);
    }

    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Pop3EventListener listener;

    public Pop3ServerEngine(int port) {
        this.port = port;
    }

    public void setListener(Pop3EventListener listener) {
        this.listener = listener;
    }

    public synchronized void startServer() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this, "POP3-Accept-Thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stopServer() {
        if (!running) return;
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    public boolean isRunning() { return running; }
    public void setPort(int port) {
        if (running) throw new IllegalStateException("Server is running");
        this.port = port;
    }

    public int getPort() { return port; }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("[POP3 Error] " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        MailLogDAO.log(clientIp, "POP3", "CONNECT", "Client kết nối tới POP3 Server");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            sendReply(writer, "+OK VKU-POP3 Server Ready (RFC 1939)");

            String userCandidate = "";
            MailUser authUser = null;
            List<Email> mailbox = new ArrayList<>();
            Set<Integer> deletedIndexes = new HashSet<>();

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int sp = line.indexOf(' ');
                String cmd = (sp == -1 ? line : line.substring(0, sp)).toUpperCase(Locale.ROOT);
                String arg = sp == -1 ? "" : line.substring(sp + 1).trim();

                notifyCommand(clientIp, line);

                if (cmd.equals("USER")) {
                    userCandidate = arg;
                    sendReply(writer, "+OK User name accepted");
                } else if (cmd.equals("PASS")) {
                    if (userCandidate.isEmpty()) {
                        sendReply(writer, "-ERR Send USER first");
                        continue;
                    }

                    authUser = UserDAO.authenticate(userCandidate, arg);
                    if (authUser != null) {
                        // Nạp danh sách email từ SQLite
                        mailbox = EmailDAO.getEmailsForRecipient(authUser.getEmail());
                        deletedIndexes.clear();
                        sendReply(writer, "+OK Mailbox open, " + mailbox.size() + " messages");
                        MailLogDAO.log(clientIp, "POP3", "LOGIN_SUCCESS", "Đăng nhập thành công: " + authUser.getUsername());
                    } else {
                        sendReply(writer, "-ERR Invalid username or password");
                        MailLogDAO.log(clientIp, "POP3", "LOGIN_FAILED", "Sai tài khoản: " + userCandidate);
                    }
                } else if (authUser == null) {
                    sendReply(writer, "-ERR Please authenticate with USER and PASS");
                } else if (cmd.equals("STAT")) {
                    int count = 0;
                    long totalBytes = 0;
                    for (int i = 0; i < mailbox.size(); i++) {
                        if (!deletedIndexes.contains(i)) {
                            count++;
                            totalBytes += mailbox.get(i).getSize();
                        }
                    }
                    sendReply(writer, "+OK " + count + " " + totalBytes);
                } else if (cmd.equals("LIST")) {
                    if (arg.isEmpty()) {
                        int count = 0;
                        long totalBytes = 0;
                        for (int i = 0; i < mailbox.size(); i++) {
                            if (!deletedIndexes.contains(i)) {
                                count++;
                                totalBytes += mailbox.get(i).getSize();
                            }
                        }
                        sendReply(writer, "+OK " + count + " messages (" + totalBytes + " octets)");
                        for (int i = 0; i < mailbox.size(); i++) {
                            if (!deletedIndexes.contains(i)) {
                                sendReply(writer, (i + 1) + " " + mailbox.get(i).getSize());
                            }
                        }
                        sendReply(writer, ".");
                    } else {
                        try {
                            int idx = Integer.parseInt(arg) - 1;
                            if (idx >= 0 && idx < mailbox.size() && !deletedIndexes.contains(idx)) {
                                sendReply(writer, "+OK " + (idx + 1) + " " + mailbox.get(idx).getSize());
                            } else {
                                sendReply(writer, "-ERR No such message");
                            }
                        } catch (NumberFormatException e) {
                            sendReply(writer, "-ERR Invalid message index");
                        }
                    }
                } else if (cmd.equals("RETR")) {
                    try {
                        int idx = Integer.parseInt(arg) - 1;
                        if (idx >= 0 && idx < mailbox.size() && !deletedIndexes.contains(idx)) {
                            Email email = mailbox.get(idx);
                            sendReply(writer, "+OK " + email.getSize() + " octets");

                            // Xuất định dạng RFC 822 Email Headers + Body
                            sendReply(writer, "From: " + email.getSender());
                            sendReply(writer, "To: " + email.getRecipient());
                            sendReply(writer, "Subject: " + email.getSubject());
                            sendReply(writer, "Date: " + email.getDateSent());
                            sendReply(writer, "Message-ID: <email-" + email.getId() + "@localhost>");
                            sendReply(writer, ""); // Dòng trống ngăn cách headers và body

                            String body = email.getBody();
                            if (body != null) {
                                String[] bodyLines = body.split("\r?\n");
                                for (String bl : bodyLines) {
                                    if (bl.startsWith(".")) bl = "." + bl; // Dot-stuffing
                                    sendReply(writer, bl);
                                }
                            }
                            sendReply(writer, ".");
                            MailLogDAO.log(clientIp, "POP3", "RETR_EMAIL", "Đọc email ID: " + email.getId());
                        } else {
                            sendReply(writer, "-ERR No such message");
                        }
                    } catch (NumberFormatException e) {
                        sendReply(writer, "-ERR Invalid message number");
                    }
                } else if (cmd.equals("DELE")) {
                    try {
                        int idx = Integer.parseInt(arg) - 1;
                        if (idx >= 0 && idx < mailbox.size() && !deletedIndexes.contains(idx)) {
                            deletedIndexes.add(idx);
                            sendReply(writer, "+OK Message " + (idx + 1) + " marked for deletion");
                        } else {
                            sendReply(writer, "-ERR No such message");
                        }
                    } catch (NumberFormatException e) {
                        sendReply(writer, "-ERR Invalid message number");
                    }
                } else if (cmd.equals("RSET")) {
                    deletedIndexes.clear();
                    sendReply(writer, "+OK Mailbox reset");
                } else if (cmd.equals("NOOP")) {
                    sendReply(writer, "+OK");
                } else if (cmd.equals("QUIT")) {
                    // Update State: Commit các email bị DELE vào SQLite
                    for (int idx : deletedIndexes) {
                        if (idx >= 0 && idx < mailbox.size()) {
                            EmailDAO.markAsDeleted(mailbox.get(idx).getId());
                        }
                    }
                    sendReply(writer, "+OK VKU-POP3 server signing off");
                    MailLogDAO.log(clientIp, "POP3", "DISCONNECT", "Client ngắt kết nối POP3");
                    break;
                } else {
                    sendReply(writer, "-ERR Command not implemented: " + cmd);
                }
            }

        } catch (IOException ignored) {}
    }

    private void sendReply(BufferedWriter writer, String text) throws IOException {
        writer.write(text + "\r\n");
        writer.flush();
    }

    private void notifyCommand(String ip, String cmd) {
        if (listener != null) {
            listener.onPop3Command(ip, cmd, "");
        }
    }
}

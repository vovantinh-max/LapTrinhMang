package server;

import database.EmailDAO;
import database.EmailDAO.Email;
import database.MailLogDAO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SmtpServerEngine implements Runnable {

    public interface SmtpEventListener {
        void onSmtpCommand(String clientIp, String command, String response);
        void onEmailReceived(Email email);
    }

    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private SmtpEventListener listener;

    public SmtpServerEngine(int port) {
        this.port = port;
    }

    public void setListener(SmtpEventListener listener) {
        this.listener = listener;
    }

    public synchronized void startServer() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this, "SMTP-Accept-Thread");
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
                    System.err.println("[SMTP Error] " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        MailLogDAO.log(clientIp, "SMTP", "CONNECT", "Client kết nối tới SMTP Server");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            sendReply(writer, "220 VKU-MailServer ESMTP Service Ready (RFC 5321)");

            String sender = "";
            String recipient = "";
            boolean inDataMode = false;

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String upper = line.toUpperCase(Locale.ROOT);
                notifyCommand(clientIp, line);

                if (upper.startsWith("HELO") || upper.startsWith("EHLO")) {
                    sendReply(writer, "250-VKU-MailServer Hello " + clientIp);
                    sendReply(writer, "250-SIZE 20971520");
                    sendReply(writer, "250-8BITMIME");
                    sendReply(writer, "250 OK");
                } else if (upper.startsWith("MAIL FROM:")) {
                    sender = extractEmailAddress(line.substring(10));
                    sendReply(writer, "250 2.1.0 Sender <" + sender + "> OK");
                } else if (upper.startsWith("RCPT TO:")) {
                    recipient = extractEmailAddress(line.substring(8));
                    sendReply(writer, "250 2.1.5 Recipient <" + recipient + "> OK");
                } else if (upper.equals("DATA")) {
                    sendReply(writer, "354 Start mail input; end with <CRLF>.<CRLF>");

                    // Đọc nội dung email cho đến khi gặp dòng chỉ có dấu chấm "."
                    StringBuilder rawMessage = new StringBuilder();
                    String subject = "(Không có tiêu đề)";
                    StringBuilder bodyBuilder = new StringBuilder();
                    boolean readingHeaders = true;

                    String dataLine;
                    while ((dataLine = reader.readLine()) != null) {
                        if (dataLine.equals(".")) {
                            break;
                        }
                        // Dot-unstuffing: nếu dòng bắt đầu bằng "..", đổi thành "."
                        if (dataLine.startsWith("..")) {
                            dataLine = dataLine.substring(1);
                        }
                        rawMessage.append(dataLine).append("\r\n");

                        if (readingHeaders) {
                            if (dataLine.isEmpty()) {
                                readingHeaders = false;
                            } else if (dataLine.toLowerCase(Locale.ROOT).startsWith("subject:")) {
                                subject = dataLine.substring(8).trim();
                            } else if (dataLine.toLowerCase(Locale.ROOT).startsWith("from:") && sender.isEmpty()) {
                                sender = extractEmailAddress(dataLine.substring(5).trim());
                            } else if (dataLine.toLowerCase(Locale.ROOT).startsWith("to:") && recipient.isEmpty()) {
                                recipient = extractEmailAddress(dataLine.substring(3).trim());
                            }
                        } else {
                            bodyBuilder.append(dataLine).append("\n");
                        }
                    }

                    // Lưu vào SQLite Database
                    Email email = new Email();
                    email.setSender(sender.isEmpty() ? "anonymous@localhost" : sender);
                    email.setRecipient(recipient.isEmpty() ? "student@localhost" : recipient);
                    email.setSubject(subject);
                    email.setBody(bodyBuilder.toString().trim());
                    email.setSize(rawMessage.length());

                    boolean saved = EmailDAO.saveEmail(email);
                    if (saved) {
                        sendReply(writer, "250 2.0.0 OK: Message accepted and stored in SQLite ID " + email.getId());
                        MailLogDAO.log(clientIp, "SMTP", "RECEIVE_EMAIL",
                                "Từ: " + email.getSender() + " -> Đến: " + email.getRecipient() + " | Tiêu đề: " + email.getSubject());
                        if (listener != null) {
                            listener.onEmailReceived(email);
                        }
                    } else {
                        sendReply(writer, "451 4.3.0 Local storage error, please try again");
                    }

                    // Reset trạng thái sau DATA
                    sender = "";
                    recipient = "";

                } else if (upper.equals("RSET")) {
                    sender = "";
                    recipient = "";
                    sendReply(writer, "250 OK Reset");
                } else if (upper.equals("NOOP")) {
                    sendReply(writer, "250 OK");
                } else if (upper.equals("QUIT")) {
                    sendReply(writer, "221 2.0.0 VKU-MailServer closing connection");
                    MailLogDAO.log(clientIp, "SMTP", "DISCONNECT", "Client ngắt kết nối SMTP");
                    break;
                } else {
                    sendReply(writer, "500 5.5.1 Command unrecognized: " + line);
                }
            }

        } catch (IOException ignored) {}
    }

    private void sendReply(BufferedWriter writer, String text) throws IOException {
        writer.write(text + "\r\n");
        writer.flush();
    }

    private String extractEmailAddress(String raw) {
        String clean = raw.trim();
        int start = clean.indexOf('<');
        int end = clean.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return clean.substring(start + 1, end).trim();
        }
        return clean.replace("<", "").replace(">", "").trim();
    }

    private void notifyCommand(String ip, String cmd) {
        if (listener != null) {
            listener.onSmtpCommand(ip, cmd, "");
        }
    }
}

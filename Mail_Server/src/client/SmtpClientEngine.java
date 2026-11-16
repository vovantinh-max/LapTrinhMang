package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

public class SmtpClientEngine {

    public interface SmtpClientLogListener {
        void onLog(String line);
    }

    private SmtpClientLogListener logListener;

    public void setLogListener(SmtpClientLogListener listener) {
        this.logListener = listener;
    }

    /**
     * Gửi email qua Localhost SMTP (Cổng 2525)
     */
    public void sendLocalEmail(String host, int port, String from, String to, String subject, String body) throws IOException {
        log("[Local SMTP] Đang kết nối tới " + host + ":" + port + "...");
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            expectReply(reader, "220");
            sendCmd(writer, reader, "HELO " + host, "250");
            sendCmd(writer, reader, "MAIL FROM:<" + from + ">", "250");
            sendCmd(writer, reader, "RCPT TO:<" + to + ">", "250");
            sendCmd(writer, reader, "DATA", "354");

            // Gửi RFC 822 Headers & Body
            writer.write("From: " + from + "\r\n");
            writer.write("To: " + to + "\r\n");
            writer.write("Subject: " + subject + "\r\n");
            writer.write("Date: " + getRfcDate() + "\r\n");
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
            writer.write(body + "\r\n");
            writer.write(".\r\n");
            writer.flush();

            expectReply(reader, "250");
            sendCmd(writer, reader, "QUIT", "221");
            log("[Local SMTP] Gửi thư thành công và đã được lưu vào Server SQLite!");
        }
    }

    /**
     * Gửi email thực tế qua Google Gmail SMTP (smtp.gmail.com:587 STARTTLS hoặc 465 SSL)
     * Sử dụng Mật khẩu ứng dụng (Google App Password 16 ký tự).
     */
    public void sendGmail(String gmailUser, String appPassword, String to, String subject, String body) throws IOException {
        log("[Gmail SMTP] Đang kết nối tới smtp.gmail.com:587 (STARTTLS)...");

        Socket socket = new Socket("smtp.gmail.com", 587);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        expectReply(reader, "220");
        sendCmd(writer, reader, "EHLO localhost", "250");

        // STARTTLS
        sendCmd(writer, reader, "STARTTLS", "220");
        log("[Gmail SMTP] Đang nâng cấp kết nối sang TLS bảo mật...");

        SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, "smtp.gmail.com", 587, true);
        sslSocket.startHandshake();

        reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));

        sendCmd(writer, reader, "EHLO localhost", "250");

        // AUTH LOGIN với Base64
        log("[Gmail SMTP] Đang xác thực tài khoản Gmail (AUTH LOGIN)...");
        sendCmd(writer, reader, "AUTH LOGIN", "334");

        String b64User = Base64.getEncoder().encodeToString(gmailUser.getBytes(StandardCharsets.UTF_8));
        String b64Pass = Base64.getEncoder().encodeToString(appPassword.replace(" ", "").getBytes(StandardCharsets.UTF_8));

        sendCmd(writer, reader, b64User, "334");
        sendCmd(writer, reader, b64Pass, "235");

        log("[Gmail SMTP] Xác thực thành công! Đang gửi email...");
        sendCmd(writer, reader, "MAIL FROM:<" + gmailUser + ">", "250");
        sendCmd(writer, reader, "RCPT TO:<" + to + ">", "250");
        sendCmd(writer, reader, "DATA", "354");

        writer.write("From: " + gmailUser + "\r\n");
        writer.write("To: " + to + "\r\n");
        writer.write("Subject: " + subject + "\r\n");
        writer.write("Date: " + getRfcDate() + "\r\n");
        writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
        writer.write(body + "\r\n");
        writer.write(".\r\n");
        writer.flush();

        expectReply(reader, "250");
        sendCmd(writer, reader, "QUIT", "221");
        sslSocket.close();

        log("[Gmail SMTP] ĐÃ GỬI THƯ THÀNH CÔNG TỚI GMAIL: " + to);
    }

    private void sendCmd(BufferedWriter writer, BufferedReader reader, String cmd, String expectedPrefix) throws IOException {
        log(">> " + (cmd.startsWith("AUTH") || cmd.length() > 20 ? (cmd.startsWith("AUTH") ? cmd : "[Auth Data]") : cmd));
        writer.write(cmd + "\r\n");
        writer.flush();
        expectReply(reader, expectedPrefix);
    }

    private void expectReply(BufferedReader reader, String expectedPrefix) throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Máy chủ đóng kết nối đột ngột.");
        log("<< " + line);

        // Xử lý Multiline response (e.g. 250- ... 250 )
        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) break;
            log("<< " + line);
        }

        if (!line.startsWith(expectedPrefix)) {
            throw new IOException("Lỗi phản hồi SMTP: " + line);
        }
    }

    private String getRfcDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        return sdf.format(new Date());
    }

    private void log(String s) {
        if (logListener != null) {
            logListener.onLog(s);
        }
    }
}

package server;

import database.DatabaseManager;
import database.EmailDAO;
import database.EmailDAO.Email;
import database.UserDAO;

import java.io.IOException;

public class MailServerManager {

    public interface MailServerStatusListener {
        void onServerStarted(int smtpPort, int pop3Port);
        void onServerStopped();
        void onEmailReceived(Email email);
        void onLogMessage(String message);
    }

    private final SmtpServerEngine smtpServer;
    private final Pop3ServerEngine pop3Server;
    private MailServerStatusListener listener;

    public MailServerManager(int smtpPort, int pop3Port) {
        this.smtpServer = new SmtpServerEngine(smtpPort);
        this.pop3Server = new Pop3ServerEngine(pop3Port);

        smtpServer.setListener(new SmtpServerEngine.SmtpEventListener() {
            @Override
            public void onSmtpCommand(String clientIp, String command, String response) {
                log(String.format("[SMTP %s] >> %s", clientIp, command));
            }

            @Override
            public void onEmailReceived(Email email) {
                log(String.format("[SMTP Nhận thư] Từ: %s -> Đến: %s | Tiêu đề: %s",
                        email.getSender(), email.getRecipient(), email.getSubject()));
                if (listener != null) {
                    listener.onEmailReceived(email);
                }
            }
        });

        pop3Server.setListener((clientIp, command, response) ->
                log(String.format("[POP3 %s] >> %s", clientIp, command)));
    }

    public void setListener(MailServerStatusListener listener) {
        this.listener = listener;
    }

    public synchronized void startAll() throws IOException {
        DatabaseManager.initializeDatabase();
        smtpServer.startServer();
        try { pop3Server.startServer(); }
        catch (IOException | RuntimeException e) { smtpServer.stopServer(); throw e; }

        log("[Mail Server] Đã khởi động SMTP trên cổng: " + smtpServer.getPort());
        log("[Mail Server] Đã khởi động POP3 trên cổng: " + pop3Server.getPort());

        if (listener != null) {
            listener.onServerStarted(smtpServer.getPort(), pop3Server.getPort());
        }
    }

    public synchronized void stopAll() {
        smtpServer.stopServer();
        pop3Server.stopServer();

        log("[Mail Server] Cả 2 dịch vụ SMTP và POP3 đã dừng.");
        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public synchronized void setPorts(int smtp, int pop3) {
        if (smtp == pop3) throw new IllegalArgumentException("SMTP and POP3 need different ports");
        smtpServer.setPort(smtp); pop3Server.setPort(pop3);
    }

    public boolean isRunning() {
        return smtpServer.isRunning() && pop3Server.isRunning();
    }

    public int getSmtpPort() { return smtpServer.getPort(); }
    public int getPop3Port() { return pop3Server.getPort(); }

    private void log(String msg) {
        System.out.println(msg);
        if (listener != null) {
            listener.onLogMessage(msg);
        }
    }
}

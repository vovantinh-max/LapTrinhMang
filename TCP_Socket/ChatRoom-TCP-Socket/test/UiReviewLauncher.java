package test;

import client.ChatClient;
import client.ui.ChatFrame;
import client.ui.LoginFrame;
import client.ui.UIUtils;
import javax.swing.*;

/** Shows the real chat form with local sample messages; makes no network connection. */
public final class UiReviewLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            if (args.length > 0 && args[0].equals("login")) { login.setVisible(true); return; }
            ChatClient preview = new ChatClient() {
                @Override public String getUsername() { return "kiemtra.giaodien"; }
                @Override public String getServerHost() { return "127.0.0.1"; }
                @Override public int getServerPort() { return 5000; }
            };
            ChatFrame chat = new ChatFrame(preview,login);
            chat.setTitle("TCP Chat - Kiểm tra giao diện (dữ liệu mẫu, chưa kết nối)");
            chat.addPublicMessage("Minh Anh", "Chào bạn! Mình đang kiểm tra giao diện của bài TCP Chat.");
            chat.addPublicMessage("kiemtra.giaodien", "Màu nền và icon đã được chỉnh dịu hơn để dễ đọc.");
            UIUtils.fitWindow(chat);
            chat.setVisible(true);
        });
    }
}

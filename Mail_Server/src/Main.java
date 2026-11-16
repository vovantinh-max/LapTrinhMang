import database.DatabaseManager;
import ui.MailServerGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set Look and Feel hệ thống Windows
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Khởi tạo Database SQLite
        DatabaseManager.initializeDatabase();

        // Khởi chạy GUI trên Event Dispatch Thread của Swing
        SwingUtilities.invokeLater(() -> {
            MailServerGUI gui = new MailServerGUI();
            gui.setVisible(true);
        });
    }
}

import database.DatabaseManager;
import ui.FtpServerGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set Look and Feel hệ thống Windows
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Khởi chạy GUI trên Event Dispatch Thread của Swing
        SwingUtilities.invokeLater(() -> {
            FtpServerGUI gui = new FtpServerGUI();
            gui.setVisible(true);
        });
    }
}

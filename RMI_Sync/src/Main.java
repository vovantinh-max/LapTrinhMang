import client.RmiClientGUI;
import ui.RmiDualServerGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--node".equalsIgnoreCase(args[0])) {
            if (args.length != 8) {
                System.err.println("Usage: Main --node NAME PORT ADVERTISED_IP PEER_IP PEER_PORT PEER_NAME DATABASE");
                System.exit(2);
            }
            try {
                System.setProperty("java.rmi.server.hostname", args[3]);
                server.RmiServerNode node = new server.RmiServerNode(args[1], Integer.parseInt(args[2]),
                        args[4], Integer.parseInt(args[5]), args[6], new database.RmiDatabaseManager(args[7]));
                node.startNode();
                Runtime.getRuntime().addShutdownHook(new Thread(node::stopNode));
                new java.util.concurrent.CountDownLatch(1).await();
            } catch (Exception e) { e.printStackTrace(); System.exit(1); }
            return;
        }
        // Set Look and Feel hệ thống Windows
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (args.length > 0 && "--client".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(() -> {
                RmiClientGUI client = new RmiClientGUI();
                client.setVisible(true);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                RmiDualServerGUI dashboard = new RmiDualServerGUI();
                dashboard.setVisible(true);
            });
        }
    }
}

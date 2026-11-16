package ui;

import client.RmiClientGUI;
import database.RmiDatabaseManager;
import model.StudentRecord;
import server.RmiServerNode;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RmiDualServerGUI extends JFrame {

    private RmiServerNode node1;
    private RmiServerNode node2;
    private RmiDatabaseManager db1;
    private RmiDatabaseManager db2;

    // Stat card labels
    private JLabel lblStatS1;
    private JLabel lblStatS2;
    private JLabel lblStatSync;
    private JLabel lblStatUptime;

    // Control buttons
    private UITheme.ModernButton btnToggleS1;
    private UITheme.ModernButton btnToggleS2;
    private UITheme.ModernButton btnFullSync;
    private UITheme.ModernButton btnAddSample;
    private UITheme.ModernButton btnOpenClient;

    // Table 1 (Server 1 DB)
    private JTable tableS1;
    private DefaultTableModel modelS1;
    private JLabel lblCountS1;

    // Table 2 (Server 2 DB)
    private JTable tableS2;
    private DefaultTableModel modelS2;
    private JLabel lblCountS2;

    // Table Logs
    private JTable tableLogs;
    private DefaultTableModel modelLogs;

    // Console logs
    private JTextArea txtConsole;

    // Uptime timer
    private long startTimeMs = 0;
    private Timer uptimeTimer;

    public RmiDualServerGUI() {
        super("RMI Dual Server Dashboard - Hệ thống đồng bộ 2 Server Active-Active");
        ui.UITheme.install();
        initNodes();
        initUI();
        startAutoServers();
    }

    private void initNodes() {
        db1 = new RmiDatabaseManager("data/server1.db");
        db2 = new RmiDatabaseManager("data/server2.db");

        try {
            node1 = new RmiServerNode("DataServer1", 1099, "127.0.0.1", 1100, "DataServer2", db1);
            node2 = new RmiServerNode("DataServer2", 1100, "127.0.0.1", 1099, "DataServer1", db2);

            RmiServerNode.ServerNodeListener listener = new RmiServerNode.ServerNodeListener() {
                @Override public void onServerStarted(String name, int port) {
                    SwingUtilities.invokeLater(() -> {
                        appendConsole("🟢 [" + name + "] Máy chủ RMI đã khởi động trên cổng " + port);
                        updateServerStatusLabels();
                        refreshAllViews();
                    });
                }
                @Override public void onServerStopped(String name) {
                    SwingUtilities.invokeLater(() -> {
                        appendConsole("🔴 [" + name + "] Máy chủ RMI đã dừng.");
                        updateServerStatusLabels();
                        refreshAllViews();
                    });
                }
                @Override public void onDataChanged(String action, StudentRecord student) {
                    SwingUtilities.invokeLater(() -> {
                        refreshAllViews();
                    });
                }
                @Override public void onPeerStatusChanged(boolean peerOnline, String peerName) {
                    SwingUtilities.invokeLater(() -> {
                        updateServerStatusLabels();
                    });
                }
                @Override public void onLogMessage(String message) {
                    SwingUtilities.invokeLater(() -> appendConsole(message));
                }
            };

            node1.setListener(listener);
            node2.setListener(listener);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khởi tạo RMI Nodes: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1050, 680));
        UITheme.fitWindow(this);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainTabs(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        // Uptime Timer
        startTimeMs = System.currentTimeMillis();
        uptimeTimer = new Timer(1000, e -> updateUptime());
        uptimeTimer.start();
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setBackground(UITheme.BG_PANEL);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(14, 20, 14, 20)
        ));

        // Top Row: Title & Action Controls
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        JLabel icon = new ui.UITheme.ThemedLabel("⚡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        brand.add(icon);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new ui.UITheme.ThemedLabel("RMI DUAL SERVER • SYNCHRONIZATION DASHBOARD");
        title.setFont(UITheme.FONT_DISPLAY);
        title.setForeground(UITheme.TXT_PRIMARY);
        JLabel sub = new ui.UITheme.ThemedLabel("Hệ thống đồng bộ dữ liệu hai chiều Active-Active Replication qua Java RMI & SQLite");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TXT_MUTED);
        titleBox.add(title);
        titleBox.add(sub);
        brand.add(titleBox);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setBorder(new EmptyBorder(10,0,10,0));
        controls.setOpaque(false);

        btnToggleS1 = new UITheme.ModernButton("Dừng S1 (1099)", UITheme.COLOR_RED, new Color(220, 38, 38), Color.WHITE);
        btnToggleS1.addActionListener(e -> toggleServer1());

        btnToggleS2 = new UITheme.ModernButton("Dừng S2 (1100)", UITheme.COLOR_RED, new Color(220, 38, 38), Color.WHITE);
        btnToggleS2.addActionListener(e -> toggleServer2());

        btnFullSync = new UITheme.ModernButton("⚡ Full Sync", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnFullSync.setToolTipText("Kích hoạt đối chiếu và đồng bộ toàn diện giữa 2 Server");
        btnFullSync.addActionListener(e -> triggerFullReconcile());

        btnAddSample = new UITheme.ModernButton("➕ Mẫu", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnAddSample.setToolTipText("Thêm dữ liệu mẫu vào Server 1 để kiểm tra đồng bộ");
        btnAddSample.addActionListener(e -> seedSampleData());

        btnOpenClient = new UITheme.ModernButton("💻 Mở Client", UITheme.COLOR_GREEN, new Color(22, 163, 74), Color.WHITE);
        btnOpenClient.addActionListener(e -> openRmiClient());

        controls.add(btnToggleS1);
        controls.add(btnToggleS2);
        controls.add(btnFullSync);
        controls.add(btnAddSample);
        controls.add(btnOpenClient);

        topRow.add(brand, BorderLayout.NORTH);
        topRow.add(controls, BorderLayout.SOUTH);
        header.add(topRow, BorderLayout.NORTH);

        // Bottom Row: 4 Stat Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        lblStatS1 = new ui.UITheme.ThemedLabel("ONLINE");
        lblStatS2 = new ui.UITheme.ThemedLabel("ONLINE");
        lblStatSync = new ui.UITheme.ThemedLabel("ACTIVE");
        lblStatUptime = new ui.UITheme.ThemedLabel("00:00:00");

        statsRow.add(UITheme.buildStatCard("SERVER 1 (PORT 1099)", lblStatS1, UITheme.COLOR_GREEN));
        statsRow.add(UITheme.buildStatCard("SERVER 2 (PORT 1100)", lblStatS2, UITheme.COLOR_CYAN));
        statsRow.add(UITheme.buildStatCard("TRẠNG THÁI ĐỒNG BỘ", lblStatSync, UITheme.PRIMARY));
        statsRow.add(UITheme.buildStatCard("THỜI GIAN HOẠT ĐỘNG", lblStatUptime, UITheme.COLOR_AMBER));

        header.add(statsRow, BorderLayout.SOUTH);
        return header;
    }

    private JTabbedPane buildMainTabs() {
        JTabbedPane tabs = new ui.UITheme.Tabs();
        tabs.setBackground(UITheme.BG_DARK);
        tabs.setForeground(UITheme.TXT_PRIMARY);
        tabs.setFont(UITheme.FONT_BOLD);

        tabs.addTab("📊 So Sánh Trực Quan 2 Server (Live Dual View)", buildSplitViewPanel());
        tabs.addTab("📜 Nhật Ký Hoạt Động & Đồng Bộ (Audit Logs)", buildAuditLogsPanel());
        tabs.addTab("💻 Server Console Logs", buildConsolePanel());

        return tabs;
    }

    private JPanel buildSplitViewPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Column Left: Server 1
        JPanel col1 = new JPanel(new BorderLayout(0, 8));
        col1.setOpaque(false);

        JPanel header1 = new JPanel(new BorderLayout());
        header1.setOpaque(false);
        JLabel title1 = new ui.UITheme.ThemedLabel("🖥️ SERVER 1 (DB: server1.db - Port 1099)");
        title1.setFont(UITheme.FONT_HEADER);
        title1.setForeground(UITheme.COLOR_GREEN);
        lblCountS1 = new ui.UITheme.ThemedLabel("0 bản ghi");
        lblCountS1.setFont(UITheme.FONT_SMALL);
        lblCountS1.setForeground(UITheme.TXT_MUTED);
        header1.add(title1, BorderLayout.WEST);
        header1.add(lblCountS1, BorderLayout.EAST);
        col1.add(header1, BorderLayout.NORTH);

        String[] cols = {"Mã SV", "Họ Tên", "Lớp", "GPA", "Ver", "Node Sửa", "Thời Gian"};
        modelS1 = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableS1 = new JTable(modelS1);
        UITheme.styleTable(tableS1);
        tableS1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] tableS1Widths = {90,160,65,60,50,110,165};
        for(int i=0;i<tableS1Widths.length;i++) tableS1.getColumnModel().getColumn(i).setPreferredWidth(tableS1Widths[i]);
        JScrollPane scroll1 = new JScrollPane(tableS1);
        scroll1.getViewport().setBackground(UITheme.SURFACE_1);
        scroll1.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll1.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        col1.add(scroll1, BorderLayout.CENTER);

        // Column Right: Server 2
        JPanel col2 = new JPanel(new BorderLayout(0, 8));
        col2.setOpaque(false);

        JPanel header2 = new JPanel(new BorderLayout());
        header2.setOpaque(false);
        JLabel title2 = new ui.UITheme.ThemedLabel("🖥️ SERVER 2 (DB: server2.db - Port 1100)");
        title2.setFont(UITheme.FONT_HEADER);
        title2.setForeground(UITheme.COLOR_CYAN);
        lblCountS2 = new ui.UITheme.ThemedLabel("0 bản ghi");
        lblCountS2.setFont(UITheme.FONT_SMALL);
        lblCountS2.setForeground(UITheme.TXT_MUTED);
        header2.add(title2, BorderLayout.WEST);
        header2.add(lblCountS2, BorderLayout.EAST);
        col2.add(header2, BorderLayout.NORTH);

        modelS2 = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableS2 = new JTable(modelS2);
        UITheme.styleTable(tableS2);
        tableS2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] tableS2Widths = {90,160,65,60,50,110,165};
        for(int i=0;i<tableS2Widths.length;i++) tableS2.getColumnModel().getColumn(i).setPreferredWidth(tableS2Widths[i]);
        JScrollPane scroll2 = new JScrollPane(tableS2);
        scroll2.getViewport().setBackground(UITheme.SURFACE_1);
        scroll2.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll2.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        col2.add(scroll2, BorderLayout.CENTER);

        panel.add(col1);
        panel.add(col2);
        return panel;
    }

    private JPanel buildAuditLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        top.setOpaque(false);
        UITheme.ModernButton btnRefreshLogs = new UITheme.ModernButton("🔄 Làm mới logs", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnRefreshLogs.addActionListener(e -> refreshAuditLogs());
        top.add(btnRefreshLogs);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "Server Phát Sinh", "Hành Động Đồng Bộ", "Mã Đối Tượng", "Chi Tiết Ghi Nhận", "Thời Gian"};
        modelLogs = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableLogs = new JTable(modelLogs);
        UITheme.styleTable(tableLogs);
        tableLogs.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableLogs.getColumnModel().getColumn(1).setPreferredWidth(110);
        tableLogs.getColumnModel().getColumn(2).setPreferredWidth(150);
        tableLogs.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableLogs.getColumnModel().getColumn(4).setPreferredWidth(320);
        tableLogs.getColumnModel().getColumn(5).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(tableLogs);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        txtConsole = new JTextArea();
        txtConsole.setBackground(UITheme.SURFACE_1);
        txtConsole.setForeground(new Color(220, 225, 240));
        txtConsole.setFont(UITheme.FONT_MONO);
        txtConsole.setEditable(false);
        txtConsole.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(txtConsole);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        UITheme.ModernButton btnClear = new UITheme.ModernButton("Xóa Log Màn Hình", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_MUTED);
        btnClear.addActionListener(e -> txtConsole.setText(""));
        bottom.add(btnClear);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(UITheme.SURFACE_1);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.BORDER),
                new EmptyBorder(6, 16, 6, 16)
        ));

        JLabel info = new ui.UITheme.ThemedLabel("Học phần Lập Trình Mạng • Lab 5: RMI Đồng bộ hóa dữ liệu 2 Server (Active-Active Replication)");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TXT_MUTED);

        JLabel env = new ui.UITheme.ThemedLabel("VKU • ThS. Nguyễn Thanh Cẩm • JDK 23");
        env.setFont(UITheme.FONT_SMALL);
        env.setForeground(UITheme.ACCENT_TEAL);

        bar.add(info, BorderLayout.WEST);
        bar.add(env, BorderLayout.EAST);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  CONTROL ACTIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void startAutoServers() {
        new Thread(() -> {
            try {
                node1.startNode();
            } catch (Exception e) {
                appendConsole("❌ Lỗi khởi động Server 1: " + e.getMessage());
            }

            try {
                // Nhỏ giọt 400ms để Server 1 sẵn sàng trước khi Server 2 kết nối
                Thread.sleep(400);
                node2.startNode();
            } catch (Exception e) {
                appendConsole("❌ Lỗi khởi động Server 2: " + e.getMessage());
            }

            SwingUtilities.invokeLater(() -> {
                updateServerStatusLabels();
                refreshAllViews();
            });
        }).start();
    }

    private void toggleServer1() {
        if (node1.isRunning()) {
            node1.stopNode();
            btnToggleS1.setText("Bật S1 (1099)");
            btnToggleS1.setBackground(UITheme.COLOR_GREEN);
        } else {
            new Thread(() -> {
                try {
                    node1.startNode();
                    SwingUtilities.invokeLater(() -> {
                        btnToggleS1.setText("Dừng S1 (1099)");
                        btnToggleS1.setBackground(UITheme.COLOR_RED);
                    });
                } catch (Exception e) {
                    appendConsole("❌ Không thể khởi động Server 1: " + e.getMessage());
                }
            }).start();
        }
    }

    private void toggleServer2() {
        if (node2.isRunning()) {
            node2.stopNode();
            btnToggleS2.setText("Bật S2 (1100)");
            btnToggleS2.setBackground(UITheme.COLOR_GREEN);
        } else {
            new Thread(() -> {
                try {
                    node2.startNode();
                    SwingUtilities.invokeLater(() -> {
                        btnToggleS2.setText("Dừng S2 (1100)");
                        btnToggleS2.setBackground(UITheme.COLOR_RED);
                    });
                } catch (Exception e) {
                    appendConsole("❌ Không thể khởi động Server 2: " + e.getMessage());
                }
            }).start();
        }
    }

    private void triggerFullReconcile() {
        appendConsole("⚡ Đang kích hoạt Full Sync hai chiều giữa Server 1 và Server 2...");
        if (node1.isRunning()) node1.triggerFullSyncFromPeer();
        if (node2.isRunning()) node2.triggerFullSyncFromPeer();
        JOptionPane.showMessageDialog(this, "Đã gửi lệnh đồng bộ toàn diện giữa 2 Server!", "Full Sync", JOptionPane.INFORMATION_MESSAGE);
    }

    private void seedSampleData() {
        new Thread(() -> {
            StudentRecord[] samples = {
                    new StudentRecord("22IT001", "Nguyễn Văn An", "22IT1", "Công nghệ thông tin", 3.85),
                    new StudentRecord("22IT045", "Trần Thị Mai", "22IT2", "Kỹ thuật phần mềm", 3.72),
                    new StudentRecord("22IT089", "Lê Hoàng Long", "22IT1", "An toàn thông tin", 3.60),
                    new StudentRecord("22IT123", "Phạm Quốc Bảo", "22IT3", "Khoa học máy tính", 3.91)
            };

            for (StudentRecord s : samples) {
                try {
                    if (node1.isRunning()) {
                        node1.addStudent(s);
                    } else if (node2.isRunning()) {
                        node2.addStudent(s);
                    }
                    Thread.sleep(150);
                } catch (Exception ignored) {}
            }
            SwingUtilities.invokeLater(() -> {
                appendConsole("🎉 Đã thêm dữ liệu mẫu thành công!");
                refreshAllViews();
            });
        }).start();
    }

    private void openRmiClient() {
        SwingUtilities.invokeLater(() -> {
            RmiClientGUI client = new RmiClientGUI();
            client.setVisible(true);
        });
    }

    private void refreshAllViews() {
        refreshTable(node1.getDb(), modelS1, lblCountS1);
        refreshTable(node2.getDb(), modelS2, lblCountS2);
        refreshAuditLogs();
    }

    private void refreshTable(RmiDatabaseManager db, DefaultTableModel model, JLabel lblCount) {
        new Thread(() -> {
            List<StudentRecord> list = db.getAll();
            DecimalFormat df = new DecimalFormat("0.00");
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (StudentRecord s : list) {
                    model.addRow(new Object[]{
                            s.getStudentCode(),
                            s.getFullName(),
                            s.getClassName(),
                            df.format(s.getGpa()),
                            "v" + s.getVersion(),
                            s.getLastModifiedServer(),
                            s.getUpdatedAt()
                    });
                }
                lblCount.setText(list.size() + " bản ghi");
            });
        }).start();
    }

    private void refreshAuditLogs() {
        new Thread(() -> {
            List<RmiDatabaseManager.SyncLogEntry> logs1 = db1.getRecentLogs(30);
            List<RmiDatabaseManager.SyncLogEntry> logs2 = db2.getRecentLogs(30);

            // Merge & sort by id
            java.util.List<RmiDatabaseManager.SyncLogEntry> combined = new java.util.ArrayList<>();
            combined.addAll(logs1);
            combined.addAll(logs2);
            combined.sort((a, b) -> b.timestamp.compareTo(a.timestamp));

            SwingUtilities.invokeLater(() -> {
                modelLogs.setRowCount(0);
                for (RmiDatabaseManager.SyncLogEntry l : combined) {
                    modelLogs.addRow(new Object[]{
                            l.id,
                            l.serverName,
                            l.action,
                            l.studentCode,
                            l.details,
                            l.timestamp
                    });
                }
            });
        }).start();
    }

    private void updateServerStatusLabels() {
        boolean s1Running = node1 != null && node1.isRunning();
        boolean s2Running = node2 != null && node2.isRunning();

        lblStatS1.setText(s1Running ? "ONLINE (1099)" : "OFFLINE");
        lblStatS1.setForeground(s1Running ? UITheme.COLOR_GREEN : UITheme.COLOR_RED);

        lblStatS2.setText(s2Running ? "ONLINE (1100)" : "OFFLINE");
        lblStatS2.setForeground(s2Running ? UITheme.COLOR_GREEN : UITheme.COLOR_RED);

        boolean synced = s1Running && s2Running && (node1.isPeerConnected() || node2.isPeerConnected());
        lblStatSync.setText(synced ? "2-WAY SYNC ACTIVE" : (s1Running || s2Running ? "STANDALONE" : "DISCONNECTED"));
        lblStatSync.setForeground(synced ? UITheme.COLOR_GREEN : (s1Running || s2Running ? UITheme.COLOR_AMBER : UITheme.COLOR_RED));
    }

    private void updateUptime() {
        long elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000;
        long h = elapsedSec / 3600;
        long m = (elapsedSec % 3600) / 60;
        long s = elapsedSec % 60;
        lblStatUptime.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void appendConsole(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        txtConsole.append("[" + time + "] " + msg + "\n");
        txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RmiDualServerGUI gui = new RmiDualServerGUI();
            gui.setVisible(true);
        });
    }
}

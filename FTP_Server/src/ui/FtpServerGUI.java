package ui;

import database.DatabaseManager;
import database.LogDAO;
import database.LogDAO.LogEntry;
import database.UserDAO;
import database.UserDAO.User;
import server.FtpClientHandler;
import server.FtpServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

public class FtpServerGUI extends JFrame implements FtpServer.ServerStatusListener {

    private final FtpServer server;
    private final String localIp;

    // Header & Status
    private JPanel portChip;
    private JLabel lblStatus;
    private JLabel statClientVal;
    private JLabel statFilesVal;
    private JLabel statUsersVal;
    private JLabel statUptimeVal;

    // Uptime & Status Pulse Timers
    private Timer uptimeTimer;
    private Timer statusDotTimer;
    private long startTimeMs = 0;
    private boolean statusDotVisible = true;

    // Controls
    private UITheme.ModernTextField txtPort;
    private UITheme.ModernTextField txtStorageDir;
    private UITheme.ModernButton btnStartStop;

    // Tab 1: Clients
    private DefaultTableModel clientsModel;
    private JTable clientsTable;
    private UITheme.ModernButton btnKick;

    // Tab 2: Users
    private DefaultTableModel usersModel;
    private JTable usersTable;

    // Tab 3: Logs
    private DefaultTableModel logsModel;
    private JTable logsTable;

    // Tab 4: Console
    private JTextArea consoleArea;

    public FtpServerGUI() {
        super("FTP Server · Quản Lý Máy Chủ (VKU Lập Trình Mạng)");
        ui.UITheme.install();
        setSize(1020, 680);
        setMinimumSize(new Dimension(880, 580));
        UITheme.fitWindow(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        localIp = detectLocalIpAddress();

        // Khởi tạo Database SQLite
        DatabaseManager.initializeDatabase();

        server = new FtpServer(2121, new File("data/storage"));
        server.setListener(this);

        createGUI();
        setupTimers();

        loadUsersData();
        loadLogsData();
        updateStorageStats();
    }

    private void createGUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BG_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenterTabs(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header Component ──────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel headerWrap = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, UITheme.BG_PANEL, getWidth(), 0, UITheme.BG_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        headerWrap.setOpaque(false);
        headerWrap.setBorder(new EmptyBorder(16, 24, 12, 24));

        // Top Row: Logo & Chips
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        // Logo + Titles
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 44;
                GradientPaint gp = new GradientPaint(0, 0, UITheme.PRIMARY, 0, s, UITheme.COLOR_VIOLET);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, s, s, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                LabIcons.icon("folder",24).paintIcon(this,g2,10,10);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(44, 44));
        left.add(logo);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel lblTitle = new ui.UITheme.ThemedLabel("FTP Server · RFC 959");
        lblTitle.setFont(UITheme.FONT_DISPLAY);
        lblTitle.setForeground(UITheme.TXT_PRIMARY);
        titleCol.add(lblTitle);

        JLabel lblSub = new ui.UITheme.ThemedLabel("Admin Control Panel  ·  VKU Lập Trình Mạng  ·  Database SQLite");
        lblSub.setFont(UITheme.FONT_TINY);
        lblSub.setForeground(UITheme.TXT_MUTED);
        titleCol.add(lblSub);
        left.add(titleCol);

        topRow.add(left, BorderLayout.WEST);

        // Right Chips
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        right.add(UITheme.buildChip("IP: " + localIp, UITheme.PRIMARY, UITheme.PRIMARY_LIGHT));
        portChip = UITheme.buildChip("PORT: 2121", UITheme.TXT_SECONDARY, UITheme.SURFACE_2);
        right.add(portChip);

        lblStatus = new ui.UITheme.ThemedLabel("● ĐÃ DỪNG");
        lblStatus.setFont(UITheme.FONT_BOLD);
        lblStatus.setForeground(UITheme.COLOR_RED);
        right.add(lblStatus);

        topRow.add(right, BorderLayout.EAST);
        headerWrap.add(topRow, BorderLayout.NORTH);

        // Stats Row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(12, 0, 0, 0));

        statClientVal = new ui.UITheme.ThemedLabel("0");
        statFilesVal = new ui.UITheme.ThemedLabel("0");
        statUsersVal = new ui.UITheme.ThemedLabel("0");
        statUptimeVal = new ui.UITheme.ThemedLabel("--:--");

        statsRow.add(UITheme.buildStatCard("👥  Khách Online", statClientVal, UITheme.COLOR_GREEN));
        statsRow.add(UITheme.buildStatCard("📁  Tệp Storage", statFilesVal, UITheme.COLOR_VIOLET));
        statsRow.add(UITheme.buildStatCard("🗄️  Tài Khoản DB", statUsersVal, UITheme.COLOR_AMBER));
        statsRow.add(UITheme.buildStatCard("⏱️  Thời Gian Chạy", statUptimeVal, UITheme.PRIMARY));

        headerWrap.add(statsRow, BorderLayout.SOUTH);
        return headerWrap;
    }

    // ── Main Tabs Component ───────────────────────────────────────────────────────
    private JTabbedPane buildCenterTabs() {
        JTabbedPane tabs = new ui.UITheme.Tabs();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.setBackground(UITheme.BG_DARK);
        tabs.setForeground(UITheme.TXT_PRIMARY);
        tabs.setBorder(new EmptyBorder(10, 16, 10, 16));

        tabs.addTab("💻 Khách Đang Kết Nối", buildClientsTab());
        tabs.addTab("👥 Quản Lý Người Dùng SQLite", buildUsersTab());
        tabs.addTab("📜 Lịch Sử Hoạt Động (Audit Logs)", buildLogsTab());
        tabs.addTab("📋 Server Console Real-Time", buildConsoleTab());

        return tabs;
    }

    private JPanel buildClientsTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"Địa chỉ Client IP", "Tài khoản", "Thời gian kết nối", "Thư mục hiện tại"};
        clientsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        clientsTable = new JTable(clientsModel);
        UITheme.styleTable(clientsTable);

        JScrollPane scroll = new JScrollPane(clientsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        btnKick = new UITheme.ModernButton("⚡ Ngắt Kết Nối Client", UITheme.COLOR_RED, new Color(200, 30, 30), Color.WHITE);
        btnKick.setEnabled(false);
        btnKick.addActionListener(e -> kickSelectedClient());

        clientsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = clientsTable.getSelectedRow() >= 0;
            btnKick.setEnabled(hasSel && server.isRunning());
        });

        btnBar.add(btnKick);
        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildUsersTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"ID", "Tên tài khoản", "Mật khẩu", "Thư mục Home", "Quyền đọc", "Quyền ghi", "Trạng thái", "Ngày tạo"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        UITheme.styleTable(usersTable);

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnAdd = new UITheme.ModernButton("➕ Thêm Tài Khoản", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        UITheme.ModernButton btnEdit = new UITheme.ModernButton("✏️ Chỉnh Sửa", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        UITheme.ModernButton btnDelete = new UITheme.ModernButton("🗑️ Xóa Tài Khoản", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);
        UITheme.ModernButton btnRefresh = new UITheme.ModernButton("🔄 Làm Mới DB", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_SECONDARY);

        btnAdd.addActionListener(e -> {
            UserManagerDialog dlg = new UserManagerDialog(this, null);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                loadUsersData();
                appendConsole("[Database] Đã thêm người dùng mới.");
            }
        });

        btnEdit.addActionListener(e -> {
            int sel = usersTable.getSelectedRow();
            if (sel >= 0) {
                String username = (String) usersModel.getValueAt(sel, 1);
                User u = UserDAO.getUserByUsername(username);
                if (u != null) {
                    UserManagerDialog dlg = new UserManagerDialog(this, u);
                    dlg.setVisible(true);
                    if (dlg.isSaved()) {
                        loadUsersData();
                        appendConsole("[Database] Đã cập nhật người dùng: " + username);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn tài khoản trong bảng cần sửa!");
            }
        });

        btnDelete.addActionListener(e -> {
            int sel = usersTable.getSelectedRow();
            if (sel >= 0) {
                int id = (int) usersModel.getValueAt(sel, 0);
                String username = (String) usersModel.getValueAt(sel, 1);
                int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa tài khoản '" + username + "'?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    UserDAO.deleteUser(id);
                    loadUsersData();
                    appendConsole("[Database] Đã xóa người dùng: " + username);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn tài khoản cần xóa!");
            }
        });

        btnRefresh.addActionListener(e -> loadUsersData());

        btnBar.add(btnAdd);
        btnBar.add(btnEdit);
        btnBar.add(btnDelete);
        btnBar.add(btnRefresh);

        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildLogsTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"ID", "Thời gian", "Client IP", "Tài khoản", "Hành động", "Chi tiết thao tác"};
        logsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        logsTable = new JTable(logsModel);
        UITheme.styleTable(logsTable);

        // Custom Cell Renderer for Action Badges
        logsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                String action = value == null ? "" : value.toString();
                l.setFont(UITheme.FONT_BOLD);
                if (action.contains("LOGIN_SUCCESS")) {
                    l.setForeground(UITheme.COLOR_GREEN);
                } else if (action.contains("FAILED") || action.contains("DELETE")) {
                    l.setForeground(UITheme.COLOR_RED);
                } else if (action.contains("UPLOAD")) {
                    l.setForeground(UITheme.ACCENT_TEAL);
                } else if (action.contains("DOWNLOAD")) {
                    l.setForeground(UITheme.PRIMARY);
                } else {
                    l.setForeground(UITheme.COLOR_AMBER);
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(logsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnReload = new UITheme.ModernButton("🔄 Làm Mới Nhật Ký", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        UITheme.ModernButton btnClear = new UITheme.ModernButton("🗑️ Xóa Sạch Nhật Ký", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);

        btnReload.addActionListener(e -> loadLogsData());
        btnClear.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa toàn bộ lịch sử trong SQLite?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                LogDAO.clearLogs();
                loadLogsData();
            }
        });

        btnBar.add(btnReload);
        btnBar.add(btnClear);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildConsoleTab() {
        JPanel panel = createTabContainer();

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setBackground(UITheme.BG_PANEL);
        consoleArea.setForeground(UITheme.TXT_SECONDARY);
        consoleArea.setFont(UITheme.FONT_MONO);
        consoleArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        consoleArea.setCaretColor(UITheme.PRIMARY);

        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnClear = new UITheme.ModernButton("Xóa Màn Hình Console", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_MUTED);
        btnClear.addActionListener(e -> consoleArea.setText(""));
        btnBar.add(btnClear);

        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTabContainer() {
        JPanel panel = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.SURFACE_1);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(UITheme.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 12, 8, 12));
        return panel;
    }

    // ── Footer Component ──────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.SURFACE_1);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.BORDER);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));

        // Left Config Controls
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        configPanel.setOpaque(false);

        JLabel lblPort = new ui.UITheme.ThemedLabel("Cổng:");
        lblPort.setFont(UITheme.FONT_BOLD);
        lblPort.setForeground(UITheme.TXT_SECONDARY);
        configPanel.add(lblPort);

        txtPort = new UITheme.ModernTextField("2121", 5);
        configPanel.add(txtPort);

        JLabel lblStorage = new ui.UITheme.ThemedLabel("Thư mục gốc:");
        lblStorage.setFont(UITheme.FONT_BOLD);
        lblStorage.setForeground(UITheme.TXT_SECONDARY);
        configPanel.add(lblStorage);

        txtStorageDir = new UITheme.ModernTextField("data/storage", 18);
        configPanel.add(txtStorageDir);

        UITheme.ModernButton btnBrowse = new UITheme.ModernButton("Chọn...", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnBrowse.addActionListener(e -> chooseStorageDir());
        configPanel.add(btnBrowse);

        UITheme.ModernButton btnOpenClient = new UITheme.ModernButton("🌐 Mở FTP Client", UITheme.SURFACE_2, UITheme.PRIMARY, UITheme.ACCENT_TEAL);
        btnOpenClient.setToolTipText("Mở giao diện FTP Client để demo gửi nhận tệp tin");
        btnOpenClient.addActionListener(e -> {
            client.FtpClientGUI clientGui = new client.FtpClientGUI(server.getPort());
            clientGui.setVisible(true);
        });
        configPanel.add(btnOpenClient);

        footer.add(configPanel, BorderLayout.WEST);

        // Right Start/Stop Button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        btnStartStop = new UITheme.ModernButton("Khởi Động Server", UITheme.COLOR_GREEN, UITheme.SURFACE_3, Color.WHITE);
        btnStartStop.setFont(UITheme.FONT_HEADER);
        btnStartStop.addActionListener(e -> toggleServer());
        actionPanel.add(btnStartStop);

        footer.add(actionPanel, BorderLayout.EAST);
        return footer;
    }

    // ── Logic & Event Handlers ────────────────────────────────────────────────────
    private void setupTimers() {
        // Ticking Uptime Timer
        uptimeTimer = new Timer(1000, e -> {
            if (startTimeMs > 0 && server.isRunning()) {
                long elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000;
                long h = elapsedSec / 3600;
                long m = (elapsedSec % 3600) / 60;
                long s = elapsedSec % 60;
                statUptimeVal.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        });

        // Blinking Status Dot Animation
        statusDotTimer = new Timer(700, e -> {
            if (server.isRunning()) {
                statusDotVisible = !statusDotVisible;
                lblStatus.setText("● ONLINE");
                lblStatus.setForeground(UITheme.COLOR_GREEN);
            }
        });
    }

    private void toggleServer() {
        if (!server.isRunning()) {
            try {
                int port = UITheme.parsePort(txtPort.getText());
                File storage = new File(txtStorageDir.getText().trim());
                server.setPort(port);
                server.setBaseStorageDir(storage);
                server.startServer();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Cổng phải là một số nguyên (1 - 65535)!", "Lỗi cấu hình", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Không thể khởi động server: " + e.getMessage(), "Lỗi máy chủ", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            server.stopServer();
        }
    }

    private void chooseStorageDir() {
        JFileChooser chooser = new JFileChooser(new File(txtStorageDir.getText().trim()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtStorageDir.setText(chooser.getSelectedFile().getAbsolutePath());
            updateStorageStats();
        }
    }

    private void kickSelectedClient() {
        int sel = clientsTable.getSelectedRow();
        if (sel >= 0) {
            List<FtpClientHandler> clients = server.getActiveClients();
            if (sel < clients.size()) {
                FtpClientHandler client = clients.get(sel);
                client.close();
                appendConsole("[Kick] Đã ngắt kết nối client: " + client.getClientIp());
            }
        }
    }

    public void loadUsersData() {
        usersModel.setRowCount(0);
        List<User> list = UserDAO.getAllUsers();
        for (User u : list) {
            usersModel.addRow(new Object[]{
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getHomeDir(),
                u.isCanRead() ? "Có" : "Không",
                u.isCanWrite() ? "Có" : "Không",
                u.isActive() ? "Kích hoạt" : "Khóa",
                u.getCreatedAt()
            });
        }
        statUsersVal.setText(String.valueOf(list.size()));
    }

    public void loadLogsData() {
        logsModel.setRowCount(0);
        List<LogEntry> logs = LogDAO.getRecentLogs(100);
        for (LogEntry l : logs) {
            logsModel.addRow(new Object[]{
                l.getId(),
                l.getTimestamp(),
                l.getClientIp(),
                l.getUsername(),
                l.getAction(),
                l.getDetails()
            });
        }
    }

    private void updateStorageStats() {
        File dir = new File(txtStorageDir.getText().trim());
        int count = countFilesRecursive(dir);
        statFilesVal.setText(String.valueOf(count));
    }

    private int countFilesRecursive(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.isFile()) count++;
            else if (f.isDirectory()) count += countFilesRecursive(f);
        }
        return count;
    }

    private void appendConsole(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (consoleArea != null) {
                consoleArea.append(msg + "\n");
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            }
        });
    }

    // ── ServerStatusListener Callbacks ────────────────────────────────────────────
    @Override
    public void onServerStarted(int port, String rootDir) {
        SwingUtilities.invokeLater(() -> {
            ((JLabel) portChip.getComponent(0)).setText("PORT: " + port);
            startTimeMs = System.currentTimeMillis();
            uptimeTimer.start();
            statusDotTimer.start();

            lblStatus.setText("● ONLINE");
            lblStatus.setForeground(UITheme.COLOR_GREEN);

            btnStartStop.setText("Dừng Máy Chủ");
            btnStartStop.setBackground(UITheme.COLOR_RED);

            txtPort.setEnabled(false);
            txtStorageDir.setEnabled(false);

            updateStorageStats();
            appendConsole("🖥  FTP Server đã khởi động thành công trên cổng: " + port);
            appendConsole("    Thư mục lưu trữ: " + rootDir);
        });
    }

    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            startTimeMs = 0;
            uptimeTimer.stop();
            statusDotTimer.stop();

            lblStatus.setText("● ĐÃ DỪNG");
            lblStatus.setForeground(UITheme.COLOR_RED);
            statUptimeVal.setText("--:--");

            btnStartStop.setText("Khởi Động Server");
            btnStartStop.setBackground(UITheme.COLOR_GREEN);

            txtPort.setEnabled(true);
            txtStorageDir.setEnabled(true);

            clientsModel.setRowCount(0);
            statClientVal.setText("0");
            btnKick.setEnabled(false);

            appendConsole("🛑 FTP Server đã dừng.");
        });
    }

    @Override
    public void onLogMessage(String message) {
        appendConsole(message);
        if (message.contains("UPLOAD") || message.contains("DOWNLOAD") || message.contains("DELETE") || message.contains("MKDIR")) {
            SwingUtilities.invokeLater(() -> {
                loadLogsData();
                updateStorageStats();
            });
        }
    }

    @Override
    public void onClientListUpdated(List<FtpClientHandler> clients) {
        SwingUtilities.invokeLater(() -> {
            clientsModel.setRowCount(0);
            for (FtpClientHandler c : clients) {
                clientsModel.addRow(new Object[]{
                    c.getClientIp(),
                    c.getUsername(),
                    c.getConnectTime(),
                    c.getCurrentDir()
                });
            }
            statClientVal.setText(String.valueOf(clients.size()));
        });
    }

    private String detectLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}

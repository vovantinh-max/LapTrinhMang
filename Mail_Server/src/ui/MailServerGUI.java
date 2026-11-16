package ui;

import database.DatabaseManager;
import database.EmailDAO;
import database.EmailDAO.Email;
import database.MailLogDAO;
import database.MailLogDAO.MailLogEntry;
import database.UserDAO;
import database.UserDAO.MailUser;
import server.MailServerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.List;

public class MailServerGUI extends JFrame implements MailServerManager.MailServerStatusListener {

    private final MailServerManager serverManager;

    // Header & Stats
    private JLabel lblStatus;
    private JLabel statEmailsVal;
    private JLabel statUsersVal;
    private JLabel statLogsVal;
    private JLabel statUptimeVal;

    // Timers
    private Timer uptimeTimer;
    private Timer statusDotTimer;
    private long startTimeMs = 0;
    private boolean statusDotVisible = true;

    // Footer Controls
    private UITheme.ModernTextField txtSmtpPort;
    private UITheme.ModernTextField txtPop3Port;
    private UITheme.ModernButton btnStartStop;

    // Tab 1: Emails Table
    private DefaultTableModel emailsModel;
    private JTable emailsTable;

    // Tab 2: Users Table
    private DefaultTableModel usersModel;
    private JTable usersTable;

    // Tab 3: Logs Table
    private DefaultTableModel logsModel;
    private JTable logsTable;

    // Tab 4: Console
    private JTextArea consoleArea;

    public MailServerGUI() {
        super("Mail Server · Quản Lý Máy Chủ SMTP & POP3 (VKU Lập Trình Mạng)");
        ui.UITheme.install();
        setSize(1040, 680);
        setMinimumSize(new Dimension(900, 580));
        UITheme.fitWindow(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DatabaseManager.initializeDatabase();

        serverManager = new MailServerManager(2525, 1110);
        serverManager.setListener(this);

        createGUI();
        setupTimers();

        loadEmailsData();
        loadUsersData();
        loadLogsData();
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

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 44;
                GradientPaint gp = new GradientPaint(0, 0, UITheme.COLOR_RED, 0, s, UITheme.COLOR_VIOLET);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, s, s, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                LabIcons.icon("mail",24).paintIcon(this,g2,10,10);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(44, 44));
        left.add(logo);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel lblTitle = new ui.UITheme.ThemedLabel("Mail Server · SMTP & POP3 (Lab 3)");
        lblTitle.setFont(UITheme.FONT_DISPLAY);
        lblTitle.setForeground(UITheme.TXT_PRIMARY);
        titleCol.add(lblTitle);

        JLabel lblSub = new ui.UITheme.ThemedLabel("Admin Control Panel  ·  Chuẩn RFC 5321 & RFC 1939  ·  SQLite Database");
        lblSub.setFont(UITheme.FONT_TINY);
        lblSub.setForeground(UITheme.TXT_MUTED);
        titleCol.add(lblSub);
        left.add(titleCol);

        topRow.add(left, BorderLayout.WEST);

        // Right Chips
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        right.add(UITheme.buildChip("SMTP: 2525", UITheme.PRIMARY, UITheme.PRIMARY_LIGHT));
        right.add(UITheme.buildChip("POP3: 1110", UITheme.ACCENT_TEAL, UITheme.SURFACE_2));

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

        statEmailsVal = new ui.UITheme.ThemedLabel("0");
        statUsersVal = new ui.UITheme.ThemedLabel("0");
        statLogsVal = new ui.UITheme.ThemedLabel("0");
        statUptimeVal = new ui.UITheme.ThemedLabel("--:--");

        statsRow.add(UITheme.buildStatCard("✉️  Tổng Email (DB)", statEmailsVal, UITheme.COLOR_GREEN));
        statsRow.add(UITheme.buildStatCard("👥  Người Dùng (DB)", statUsersVal, UITheme.COLOR_VIOLET));
        statsRow.add(UITheme.buildStatCard("📜  Nhật Ký (Logs)", statLogsVal, UITheme.COLOR_AMBER));
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

        tabs.addTab("📥 Quản Lý Thư Trong SQLite", buildEmailsTab());
        tabs.addTab("👥 Tài Khoản Người Dùng (SQLite)", buildUsersTab());
        tabs.addTab("📜 Nhật Ký Hoạt Động (Audit Logs)", buildLogsTab());
        tabs.addTab("📋 Server Console Real-Time", buildConsoleTab());

        return tabs;
    }

    private JPanel buildEmailsTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"ID", "Người gửi (From)", "Người nhận (To)", "Tiêu đề (Subject)", "Ngày gửi", "Dung lượng"};
        emailsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        emailsTable = new JTable(emailsModel);
        UITheme.styleTable(emailsTable);

        emailsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) viewSelectedEmailDetail();
            }
        });

        JScrollPane scroll = new JScrollPane(emailsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnView = new UITheme.ModernButton("📖 Xem Nội Dung", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnView.addActionListener(e -> viewSelectedEmailDetail());

        UITheme.ModernButton btnReload = new UITheme.ModernButton("🔄 Làm Mới", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnReload.addActionListener(e -> loadEmailsData());

        UITheme.ModernButton btnDelete = new UITheme.ModernButton("🗑️ Xóa Thư", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);
        btnDelete.addActionListener(e -> deleteSelectedEmail());

        btnBar.add(btnView);
        btnBar.add(btnReload);
        btnBar.add(btnDelete);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildUsersTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"ID", "Tên tài khoản", "Mật khẩu", "Địa chỉ Email", "Ngày tạo"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        UITheme.styleTable(usersTable);

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnAdd = new UITheme.ModernButton("➕ Thêm Tài Khoản Mới", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnAdd.addActionListener(e -> showAddUserDialog());

        UITheme.ModernButton btnReload = new UITheme.ModernButton("🔄 Làm Mới", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnReload.addActionListener(e -> loadUsersData());

        btnBar.add(btnAdd);
        btnBar.add(btnReload);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildLogsTab() {
        JPanel panel = createTabContainer();

        String[] cols = {"ID", "Thời gian", "Client IP", "Giao thức", "Hành động", "Chi tiết thao tác"};
        logsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        logsTable = new JTable(logsModel);
        UITheme.styleTable(logsTable);

        // Protocol badge renderer (SMTP Indigo, POP3 Teal)
        logsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UITheme.FONT_BOLD);
                String proto = value != null ? value.toString() : "";
                if ("SMTP".equals(proto)) l.setForeground(UITheme.PRIMARY);
                else l.setForeground(UITheme.ACCENT_TEAL);
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(logsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        UITheme.ModernButton btnReload = new UITheme.ModernButton("🔄 Làm Mới Log", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnReload.addActionListener(e -> loadLogsData());

        UITheme.ModernButton btnClear = new UITheme.ModernButton("🗑️ Xóa Nhật Ký", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);
        btnClear.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa lịch sử mail log trong SQLite?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                MailLogDAO.clearLogs();
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

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        configPanel.setOpaque(false);

        JLabel lblSmtp = new ui.UITheme.ThemedLabel("Cổng SMTP:");
        lblSmtp.setFont(UITheme.FONT_BOLD);
        lblSmtp.setForeground(UITheme.TXT_SECONDARY);
        configPanel.add(lblSmtp);

        txtSmtpPort = new UITheme.ModernTextField("2525", 5);
        configPanel.add(txtSmtpPort);

        JLabel lblPop3 = new ui.UITheme.ThemedLabel("Cổng POP3:");
        lblPop3.setFont(UITheme.FONT_BOLD);
        lblPop3.setForeground(UITheme.TXT_SECONDARY);
        configPanel.add(lblPop3);

        txtPop3Port = new UITheme.ModernTextField("1110", 5);
        configPanel.add(txtPop3Port);

        UITheme.ModernButton btnOpenClient = new UITheme.ModernButton("✉️ Mở Mail Client", UITheme.SURFACE_2, UITheme.PRIMARY, UITheme.ACCENT_TEAL);
        btnOpenClient.setToolTipText("Khởi chạy giao diện Mail Client (Gmail-like) để soạn và nhận thư");
        btnOpenClient.addActionListener(e -> {
            client.MailClientGUI clientGui = new client.MailClientGUI("127.0.0.1", serverManager.getSmtpPort(), serverManager.getPop3Port());
            clientGui.setVisible(true);
        });
        configPanel.add(btnOpenClient);

        footer.add(configPanel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        btnStartStop = new UITheme.ModernButton("Khởi Động Server", UITheme.COLOR_GREEN, UITheme.SURFACE_3, Color.WHITE);
        btnStartStop.setFont(UITheme.FONT_HEADER);
        btnStartStop.addActionListener(e -> toggleServer());
        actionPanel.add(btnStartStop);

        footer.add(actionPanel, BorderLayout.EAST);
        return footer;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────────
    private void setupTimers() {
        uptimeTimer = new Timer(1000, e -> {
            if (startTimeMs > 0 && serverManager.isRunning()) {
                long elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000;
                long h = elapsedSec / 3600;
                long m = (elapsedSec % 3600) / 60;
                long s = elapsedSec % 60;
                statUptimeVal.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        });

        statusDotTimer = new Timer(700, e -> {
            if (serverManager.isRunning()) {
                statusDotVisible = !statusDotVisible;
                lblStatus.setText("● ONLINE");
                lblStatus.setForeground(UITheme.COLOR_GREEN);
            }
        });
    }

    private void toggleServer() {
        if (!serverManager.isRunning()) {
            try {
                serverManager.setPorts(UITheme.parsePort(txtSmtpPort.getText()), UITheme.parsePort(txtPop3Port.getText()));
                serverManager.startAll();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "SMTP / POP3: nhập hai cổng khác nhau trong khoảng 1–65535.", "Cấu hình", JOptionPane.WARNING_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Không thể khởi động Mail Server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            serverManager.stopAll();
        }
    }

    public void loadEmailsData() {
        emailsModel.setRowCount(0);
        List<Email> list = EmailDAO.getAllEmails();
        for (Email em : list) {
            emailsModel.addRow(new Object[]{
                em.getId(),
                em.getSender(),
                em.getRecipient(),
                em.getSubject(),
                em.getDateSent(),
                em.getSize() + " B"
            });
        }
        statEmailsVal.setText(String.valueOf(EmailDAO.getTotalEmailCount()));
    }

    public void loadUsersData() {
        usersModel.setRowCount(0);
        List<MailUser> list = UserDAO.getAllUsers();
        for (MailUser u : list) {
            usersModel.addRow(new Object[]{
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getEmail(),
                u.getCreatedAt()
            });
        }
        statUsersVal.setText(String.valueOf(list.size()));
    }

    public void loadLogsData() {
        logsModel.setRowCount(0);
        List<MailLogEntry> logs = MailLogDAO.getRecentLogs(100);
        for (MailLogEntry l : logs) {
            logsModel.addRow(new Object[]{
                l.getId(),
                l.getTimestamp(),
                l.getClientIp(),
                l.getProtocol(),
                l.getAction(),
                l.getDetails()
            });
        }
        statLogsVal.setText(String.valueOf(logs.size()));
    }

    private void viewSelectedEmailDetail() {
        int sel = emailsTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 email trong bảng để xem!");
            return;
        }

        int id = (int) emailsModel.getValueAt(sel, 0);
        Email em = EmailDAO.getEmailById(id);
        if (em == null) return;

        JDialog dlg = new JDialog(this, "Chi Tiết Email ID #" + id + ": " + em.getSubject(), true);
        dlg.setSize(560, 420);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UITheme.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel headerInfo = new JPanel(new GridLayout(4, 1, 4, 4));
        headerInfo.setOpaque(false);
        headerInfo.add(createMetaLabel("Từ (Sender): " + em.getSender()));
        headerInfo.add(createMetaLabel("Đến (Recipient): " + em.getRecipient()));
        headerInfo.add(createMetaLabel("Tiêu đề (Subject): " + em.getSubject()));
        headerInfo.add(createMetaLabel("Thời gian: " + em.getDateSent()));
        p.add(headerInfo, BorderLayout.NORTH);

        JTextArea bodyArea = new JTextArea(em.getBody());
        bodyArea.setEditable(false);
        bodyArea.setFont(UITheme.FONT_REGULAR);
        bodyArea.setBackground(UITheme.SURFACE_2);
        bodyArea.setForeground(UITheme.TXT_PRIMARY);
        bodyArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(bodyArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        p.add(scroll, BorderLayout.CENTER);

        UITheme.ModernButton btnClose = new UITheme.ModernButton("Đóng", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnClose.addActionListener(e -> dlg.dispose());
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.setOpaque(false);
        b.add(btnClose);
        p.add(b, BorderLayout.SOUTH);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void deleteSelectedEmail() {
        int sel = emailsTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn email cần xóa!");
            return;
        }
        int id = (int) emailsModel.getValueAt(sel, 0);
        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa email ID #" + id + " khỏi cơ sở dữ liệu?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            EmailDAO.markAsDeleted(id);
            loadEmailsData();
        }
    }

    private void showAddUserDialog() {
        JTextField txtU = new JTextField();
        JPasswordField txtP = new JPasswordField();
        JTextField txtE = new JTextField();

        Object[] message = {
            "Tên đăng nhập:", txtU,
            "Mật khẩu:", txtP,
            "Địa chỉ Email:", txtE
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Thêm Người Dùng Mail Mới", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String u = txtU.getText().trim();
            String p = new String(txtP.getPassword()).trim();
            String e = txtE.getText().trim();
            if (!u.isEmpty() && !p.isEmpty() && !e.isEmpty()) {
                if (UserDAO.addUser(u, p, e)) {
                    loadUsersData();
                    JOptionPane.showMessageDialog(this, "Đã thêm người dùng thành công!");
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể thêm (tên đăng nhập hoặc email bị trùng)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private JLabel createMetaLabel(String text) {
        JLabel l = new ui.UITheme.ThemedLabel(text);
        l.setFont(UITheme.FONT_REGULAR);
        l.setForeground(UITheme.TXT_SECONDARY);
        return l;
    }

    private void appendConsole(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (consoleArea != null) {
                consoleArea.append(msg + "\n");
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            }
        });
    }

    // ── Server Callbacks ──────────────────────────────────────────────────────────
    @Override
    public void onServerStarted(int smtpPort, int pop3Port) {
        SwingUtilities.invokeLater(() -> {
            startTimeMs = System.currentTimeMillis();
            uptimeTimer.start();
            statusDotTimer.start();

            lblStatus.setText("● ONLINE");
            lblStatus.setForeground(UITheme.COLOR_GREEN);

            btnStartStop.setText("Dừng Máy Chủ");
            btnStartStop.setBackground(UITheme.COLOR_RED);

            txtSmtpPort.setEnabled(false);
            txtPop3Port.setEnabled(false);

            appendConsole("🖥  Mail Server đã khởi động thành công!");
            appendConsole("    SMTP: Cổng " + smtpPort + "  |  POP3: Cổng " + pop3Port);
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

            txtSmtpPort.setEnabled(true);
            txtPop3Port.setEnabled(true);

            appendConsole("🛑 Mail Server đã dừng.");
        });
    }

    @Override
    public void onEmailReceived(Email email) {
        SwingUtilities.invokeLater(() -> {
            loadEmailsData();
            loadLogsData();
        });
    }

    @Override
    public void onLogMessage(String message) {
        appendConsole(message);
        if (message.contains("LOGIN") || message.contains("RETR") || message.contains("Nhận thư")) {
            SwingUtilities.invokeLater(this::loadLogsData);
        }
    }
}

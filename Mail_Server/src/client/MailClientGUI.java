package client;

import ui.LabIcons;
import database.EmailDAO.Email;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MailClientGUI extends JFrame {

    private final SmtpClientEngine smtpClient = new SmtpClientEngine();
    private final Pop3ClientEngine pop3Client = new Pop3ClientEngine();

    // Accounts & Settings
    private UITheme.ModernTextField txtSettingHost, txtSettingSmtp, txtSettingPop3;
    private String localHost = "127.0.0.1";
    private int localSmtpPort = 2525;
    private int localPop3Port = 1110;
    private String currentUser = "student";
    private String currentPass = "123456";
    private String currentEmail = "student@localhost";

    // Gmail Settings
    private String gmailUser = "";
    private String gmailAppPassword = "";

    // Sidebar & Card Layout
    private CardLayout cardLayout;
    private JPanel mainCardPanel;

    // View 1: Inbox
    private DefaultTableModel inboxModel;
    private JTable inboxTable;
    private List<Email> currentLoadedEmails = new ArrayList<>();
    private JLabel lblInboxTitle;

    // View 2: Compose
    private JRadioButton rdoLocalMode;
    private JRadioButton rdoGmailMode;
    private UITheme.ModernTextField txtTo;
    private UITheme.ModernTextField txtSubject;
    private JTextArea txtBody;
    private UITheme.ModernButton btnSend;

    // View 3: Settings
    private UITheme.ModernTextField txtSettingUser;
    private JPasswordField txtSettingPass;
    private UITheme.ModernTextField txtGmailUser;
    private JPasswordField txtGmailAppPass;

    // Status bar & Log Terminal
    private JLabel lblClientStatus;
    private JTextArea clientLogArea;

    public MailClientGUI() { this("127.0.0.1", 2525, 1110); }

    public MailClientGUI(String host, int smtpPort, int pop3Port) {
        super("Mail Client · Hộp Thư Điện Tử (VKU Lập Trình Mạng)");
        ui.UITheme.install();
        localHost = host; localSmtpPort = smtpPort; localPop3Port = pop3Port;
        setSize(1080, 720);
        setMinimumSize(new Dimension(900, 600));
        UITheme.fitWindow(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        smtpClient.setLogListener(this::appendLog);
        pop3Client.setLogListener(this::appendLog);

        createGUI();
        refreshInbox();
    }

    private void createGUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BG_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);

        root.add(buildTopHeader(), BorderLayout.NORTH);

        // Center Split: Left Sidebar + Right Cards
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(10, 16, 6, 16));

        centerPanel.add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setOpaque(false);
        mainCardPanel.setBorder(new EmptyBorder(0, 12, 0, 0));

        mainCardPanel.add(buildInboxView(), "INBOX");
        mainCardPanel.add(buildComposeView(), "COMPOSE");
        mainCardPanel.add(buildSettingsView(), "SETTINGS");

        centerPanel.add(mainCardPanel, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        root.add(buildBottomLog(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Top Header ────────────────────────────────────────────────────────────────
    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0)) {
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
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 38;
                GradientPaint gp = new GradientPaint(0, 0, UITheme.COLOR_RED, 0, s, UITheme.COLOR_AMBER);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, s, s, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                LabIcons.icon("mail",24).paintIcon(this,g2,10,10);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(38, 38));
        left.add(logo);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel lblTitle = new ui.UITheme.ThemedLabel("VKU Mail Client · Gmail-Style");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TXT_PRIMARY);
        titleCol.add(lblTitle);

        JLabel lblSub = new ui.UITheme.ThemedLabel("Hỗ trợ gửi thư qua Gmail thật (smtp.gmail.com) & Localhost POP3/SMTP");
        lblSub.setFont(UITheme.FONT_TINY);
        lblSub.setForeground(UITheme.TXT_MUTED);
        titleCol.add(lblSub);
        left.add(titleCol);

        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(UITheme.buildChip("Tài khoản: " + currentEmail, UITheme.ACCENT_TEAL, UITheme.SURFACE_2));
        lblClientStatus = new ui.UITheme.ThemedLabel("● SẴN SÀNG");
        lblClientStatus.setFont(UITheme.FONT_BOLD);
        lblClientStatus.setForeground(UITheme.COLOR_GREEN);
        right.add(lblClientStatus);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Left Sidebar ──────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12)) {
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
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(16, 12, 16, 12));

        // Compose Button at Top
        UITheme.ModernButton btnCompose = new UITheme.ModernButton("➕ Soạn Thư Mới", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnCompose.setFont(UITheme.FONT_HEADER);
        btnCompose.setCornerRadius(10);
        btnCompose.addActionListener(e -> cardLayout.show(mainCardPanel, "COMPOSE"));
        sidebar.add(btnCompose, BorderLayout.NORTH);

        // Nav List in Middle
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(16, 0, 0, 0));

        UITheme.ModernButton btnNavInbox = new UITheme.ModernButton("📥  Hộp Thư Đến (Inbox)", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnNavInbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnNavInbox.addActionListener(e -> {
            cardLayout.show(mainCardPanel, "INBOX");
            refreshInbox();
        });

        UITheme.ModernButton btnNavSettings = new UITheme.ModernButton("⚙️  Cấu Hình Tài Khoản", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_SECONDARY);
        btnNavSettings.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnNavSettings.addActionListener(e -> cardLayout.show(mainCardPanel, "SETTINGS"));

        navList.add(btnNavInbox);
        navList.add(Box.createVerticalStrut(8));
        navList.add(btnNavSettings);

        sidebar.add(navList, BorderLayout.CENTER);

        return sidebar;
    }

    // ── View 1: Inbox ─────────────────────────────────────────────────────────────
    private JPanel buildInboxView() {
        JPanel panel = createCardContainer();

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        lblInboxTitle = new ui.UITheme.ThemedLabel("📥 Hộp Thư Đến (Đang tải...)");
        lblInboxTitle.setFont(UITheme.FONT_HEADER);
        lblInboxTitle.setForeground(UITheme.TXT_PRIMARY);
        topBar.add(lblInboxTitle, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBtns.setOpaque(false);

        UITheme.ModernButton btnRead = new UITheme.ModernButton("📖 Đọc Thư", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnRead.addActionListener(e -> openSelectedEmail());

        UITheme.ModernButton btnRefresh = new UITheme.ModernButton("🔄 Làm Mới", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnRefresh.addActionListener(e -> refreshInbox());

        UITheme.ModernButton btnDelete = new UITheme.ModernButton("🗑️ Xóa Thư", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);
        btnDelete.addActionListener(e -> deleteSelectedEmail());

        actionBtns.add(btnRead);
        actionBtns.add(btnRefresh);
        actionBtns.add(btnDelete);
        topBar.add(actionBtns, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Inbox Table
        String[] cols = {"STT", "Người gửi (From)", "Tiêu đề (Subject)", "Ngày gửi", "Kích thước"};
        inboxModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        inboxTable = new JTable(inboxModel);
        UITheme.styleTable(inboxTable);

        inboxTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedEmail();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(inboxTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ── View 2: Compose Email ─────────────────────────────────────────────────────
    private JPanel buildComposeView() {
        JPanel panel = createCardContainer();

        JLabel title = new ui.UITheme.ThemedLabel("✉️ Soạn Thư Mới (Compose Email)");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        int row = 0;

        // Mode: Local vs Gmail
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        JLabel lblMode = new ui.UITheme.ThemedLabel("Chế độ gửi:");
        lblMode.setFont(UITheme.FONT_BOLD);
        lblMode.setForeground(UITheme.TXT_SECONDARY);
        form.add(lblMode, gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        modePanel.setOpaque(false);
        rdoLocalMode = new JRadioButton("Localhost SMTP (Cổng " + localSmtpPort + ")", true);
        rdoGmailMode = new JRadioButton("Google Gmail Thật (smtp.gmail.com:587)", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rdoLocalMode);
        bg.add(rdoGmailMode);
        styleRadio(rdoLocalMode);
        styleRadio(rdoGmailMode);
        modePanel.add(rdoLocalMode);
        modePanel.add(rdoGmailMode);
        form.add(modePanel, gbc);
        row++;

        // To
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblTo = new ui.UITheme.ThemedLabel("Người nhận (To):");
        lblTo.setFont(UITheme.FONT_BOLD);
        lblTo.setForeground(UITheme.TXT_SECONDARY);
        form.add(lblTo, gbc);

        gbc.gridx = 1;
        txtTo = new UITheme.ModernTextField("admin@localhost", 30);
        form.add(txtTo, gbc);
        row++;

        // Subject
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblSub = new ui.UITheme.ThemedLabel("Tiêu đề (Subject):");
        lblSub.setFont(UITheme.FONT_BOLD);
        lblSub.setForeground(UITheme.TXT_SECONDARY);
        form.add(lblSub, gbc);

        gbc.gridx = 1;
        txtSubject = new UITheme.ModernTextField("Thử nghiệm gửi thư từ VKU Mail Client", 30);
        form.add(txtSubject, gbc);
        row++;

        // Body
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblBody = new ui.UITheme.ThemedLabel("Nội dung:");
        lblBody.setFont(UITheme.FONT_BOLD);
        lblBody.setForeground(UITheme.TXT_SECONDARY);
        form.add(lblBody, gbc);

        gbc.gridx = 1; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        txtBody = new JTextArea("Xin chào thầy và các bạn,\n\nĐây là email thử nghiệm gửi qua giao thức SMTP (Lab 3 Lập trình mạng VKU)!\nThư được gửi thành công và lưu vào cơ sở dữ liệu SQLite.\n\nTrân trọng,\nVõ Văn Tình (24IT274)");
        txtBody.setFont(UITheme.FONT_REGULAR);
        txtBody.setBackground(UITheme.SURFACE_2);
        txtBody.setForeground(UITheme.TXT_PRIMARY);
        txtBody.setCaretColor(UITheme.PRIMARY);
        txtBody.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane scrollBody = new JScrollPane(txtBody);
        scrollBody.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scrollBody.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        form.add(scrollBody, gbc);
        row++;

        panel.add(form, BorderLayout.CENTER);

        // Send Button
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);
        btnSend = new UITheme.ModernButton("🚀 Gửi Thư Ngay (Send)", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnSend.setFont(UITheme.FONT_HEADER);
        btnSend.addActionListener(e -> executeSendEmail());
        bottomBar.add(btnSend);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // ── View 3: Settings ──────────────────────────────────────────────────────────
    private JPanel buildSettingsView() {
        JPanel panel = createCardContainer();

        JLabel title = new ui.UITheme.ThemedLabel("⚙️ Cấu Hình Tài Khoản & Kết Nối");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        int row = 0;

        addSettingRow(form, gbc, row++, "Máy chủ (IP / Host):", txtSettingHost = new UITheme.ModernTextField(localHost, 20));
        addSettingRow(form, gbc, row++, "Cổng SMTP:", txtSettingSmtp = new UITheme.ModernTextField(String.valueOf(localSmtpPort), 20));
        addSettingRow(form, gbc, row++, "Cổng POP3:", txtSettingPop3 = new UITheme.ModernTextField(String.valueOf(localPop3Port), 20));
        // Localhost Account
        addSectionTitle(form, gbc, row++, "1. Tài Khoản Localhost (POP3 / SMTP)");
        addSettingRow(form, gbc, row++, "Tên đăng nhập POP3:", txtSettingUser = new UITheme.ModernTextField(currentUser, 20));
        addSettingRow(form, gbc, row++, "Mật khẩu POP3:", txtSettingPass = new JPasswordField(currentPass, 20));

        // Gmail Account
        addSectionTitle(form, gbc, row++, "2. Tài Khoản Google Gmail (Để gửi ra ngoài Internet)");
        addSettingRow(form, gbc, row++, "Địa chỉ Gmail của bạn:", txtGmailUser = new UITheme.ModernTextField(gmailUser, 20));
        txtGmailUser.setToolTipText("Ví dụ: your_email@gmail.com");

        txtGmailAppPass = new JPasswordField(gmailAppPassword, 20);
        txtGmailAppPass.setToolTipText("Mật khẩu ứng dụng (App Password) 16 ký tự tạo trong tài khoản Google");
        addSettingRow(form, gbc, row++, "Mật khẩu ứng dụng Gmail (App Password):", txtGmailAppPass);

        panel.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);
        UITheme.ModernButton btnSaveSettings = new UITheme.ModernButton("Lưu Cấu Hình", UITheme.COLOR_GREEN, UITheme.SURFACE_3, Color.WHITE);
        btnSaveSettings.addActionListener(e -> {
            try {
                int smtp = UITheme.parsePort(txtSettingSmtp.getText());
                int pop3 = UITheme.parsePort(txtSettingPop3.getText());
                if (txtSettingHost.getText().isBlank() || smtp == pop3) throw new IllegalArgumentException();
                localHost = txtSettingHost.getText().trim(); localSmtpPort = smtp; localPop3Port = pop3;
                rdoLocalMode.setText("Local SMTP (" + localSmtpPort + ")");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Nhập host và hai cổng khác nhau từ 1 đến 65535.", "Cấu hình", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentUser = txtSettingUser.getText().trim();
            currentPass = new String(txtSettingPass.getPassword()).trim();
            currentEmail = currentUser.contains("@") ? currentUser : currentUser + "@localhost";

            gmailUser = txtGmailUser.getText().trim();
            gmailAppPassword = new String(txtGmailAppPass.getPassword()).trim();

            JOptionPane.showMessageDialog(this, "Đã cập nhật cấu hình tài khoản thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainCardPanel, "INBOX");
            refreshInbox();
        });
        btnBar.add(btnSaveSettings);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    // ── Bottom Real-time Protocol Terminal ────────────────────────────────────────
    private JPanel buildBottomLog() {
        JPanel bar = new JPanel(new BorderLayout(8, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.SURFACE_1);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.BORDER);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel title = new ui.UITheme.ThemedLabel("📋 Nhật Ký Giao Thức SMTP / POP3 (Console Logs):");
        title.setFont(UITheme.FONT_TINY);
        title.setForeground(UITheme.TXT_MUTED);
        bar.add(title, BorderLayout.NORTH);

        clientLogArea = new JTextArea(3, 80);
        clientLogArea.setEditable(false);
        clientLogArea.setFont(UITheme.FONT_MONO);
        clientLogArea.setBackground(UITheme.BG_PANEL);
        clientLogArea.setForeground(UITheme.TXT_SECONDARY);
        clientLogArea.setCaretColor(UITheme.PRIMARY);

        JScrollPane scroll = new JScrollPane(clientLogArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        bar.add(scroll, BorderLayout.CENTER);

        return bar;
    }

    private JPanel createCardContainer() {
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
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        return panel;
    }

    // ── Logic: Refresh Inbox & Operations ─────────────────────────────────────────
    private void refreshInbox() {
        lblInboxTitle.setText("📥 Đang tải thư từ POP3 (" + currentEmail + ")...");
        lblClientStatus.setText("● ĐANG ĐỒNG BỘ...");
        lblClientStatus.setForeground(UITheme.COLOR_AMBER);

        new Thread(() -> {
            try {
                pop3Client.connect(localHost, localPop3Port, currentUser, currentPass);
                List<Pop3ClientEngine.Pop3MessageInfo> list = pop3Client.listMessages();

                List<Email> emails = new ArrayList<>();
                for (Pop3ClientEngine.Pop3MessageInfo info : list) {
                    Email e = pop3Client.retrieveEmail(info.number);
                    emails.add(e);
                }
                pop3Client.disconnect();

                SwingUtilities.invokeLater(() -> {
                    currentLoadedEmails = emails;
                    inboxModel.setRowCount(0);
                    for (int i = 0; i < emails.size(); i++) {
                        Email em = emails.get(i);
                        inboxModel.addRow(new Object[]{
                            (i + 1),
                            em.getSender(),
                            em.getSubject(),
                            em.getDateSent(),
                            (em.getSize() > 0 ? em.getSize() : em.getBody().length()) + " B"
                        });
                    }
                    lblInboxTitle.setText("📥 Hộp Thư Đến (" + emails.size() + " thư) · " + currentEmail);
                    lblClientStatus.setText("● SẴN SÀNG");
                    lblClientStatus.setForeground(UITheme.COLOR_GREEN);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblInboxTitle.setText("📥 Lỗi kết nối POP3: " + ex.getMessage());
                    lblClientStatus.setText("● LỖI POP3");
                    lblClientStatus.setForeground(UITheme.COLOR_RED);
                });
            }
        }).start();
    }

    private void openSelectedEmail() {
        int sel = inboxTable.getSelectedRow();
        if (sel < 0 || sel >= currentLoadedEmails.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 email trong bảng để đọc!");
            return;
        }

        Email em = currentLoadedEmails.get(sel);

        JDialog dlg = new JDialog(this, "Đọc Thư: " + em.getSubject(), true);
        dlg.setSize(560, 440);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UITheme.BG_DARK);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel headerInfo = new JPanel(new GridLayout(4, 1, 4, 4));
        headerInfo.setOpaque(false);

        headerInfo.add(createEmailMetaLabel("Từ (From): " + em.getSender()));
        headerInfo.add(createEmailMetaLabel("Đến (To): " + em.getRecipient()));
        headerInfo.add(createEmailMetaLabel("Tiêu đề: " + em.getSubject()));
        headerInfo.add(createEmailMetaLabel("Ngày gửi: " + em.getDateSent()));
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
        int sel = inboxTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn email cần xóa!");
            return;
        }

        int msgNum = (int) inboxModel.getValueAt(sel, 0);
        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa email này khỏi máy chủ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    pop3Client.connect(localHost, localPop3Port, currentUser, currentPass);
                    pop3Client.deleteEmail(msgNum);
                    pop3Client.disconnect();
                    refreshInbox();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Xóa thất bại: " + ex.getMessage()));
                }
            }).start();
        }
    }

    private void executeSendEmail() {
        String to = txtTo.getText().trim();
        String subject = txtSubject.getText().trim();
        String body = txtBody.getText().trim();

        if (to.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ người nhận (To)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isGmail = rdoGmailMode.isSelected();
        btnSend.setEnabled(false);
        btnSend.setText("Đang gửi...");

        new Thread(() -> {
            try {
                if (isGmail) {
                    if (gmailUser.isEmpty() || gmailAppPassword.isEmpty()) {
                        throw new IOException("Vui lòng vào tab 'Cấu Hình Tài Khoản' nhập Gmail và Mật khẩu ứng dụng trước!");
                    }
                    smtpClient.sendGmail(gmailUser, gmailAppPassword, to, subject, body);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "ĐÃ GỬI THƯ THÀNH CÔNG QUA GMAIL!\nĐã gửi tới: " + to, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        btnSend.setEnabled(true);
                        btnSend.setText("🚀 Gửi Thư Ngay (Send)");
                    });
                } else {
                    smtpClient.sendLocalEmail(localHost, localSmtpPort, currentEmail, to, subject, body);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Gửi thư thành công qua Localhost SMTP!\nThư đã lưu vào SQLite Database của Server.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        btnSend.setEnabled(true);
                        btnSend.setText("🚀 Gửi Thư Ngay (Send)");
                        cardLayout.show(mainCardPanel, "INBOX");
                        refreshInbox();
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("🚀 Gửi Thư Ngay (Send)");
                    JOptionPane.showMessageDialog(this, "Gửi thư thất bại: " + ex.getMessage(), "Lỗi Gửi Thư", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            if (clientLogArea != null) {
                clientLogArea.append(line + "\n");
                clientLogArea.setCaretPosition(clientLogArea.getDocument().getLength());
            }
        });
    }

    private void styleRadio(JRadioButton r) {
        r.setOpaque(false);
        r.setFont(UITheme.FONT_REGULAR);
        r.setForeground(UITheme.TXT_PRIMARY);
        r.setFocusPainted(false);
    }

    private JLabel createEmailMetaLabel(String text) {
        JLabel l = new ui.UITheme.ThemedLabel(text);
        l.setFont(UITheme.FONT_REGULAR);
        l.setForeground(UITheme.TXT_SECONDARY);
        return l;
    }

    private void addSectionTitle(JPanel p, GridBagConstraints gbc, int row, String title) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel l = new ui.UITheme.ThemedLabel(title);
        l.setFont(UITheme.FONT_HEADER);
        l.setForeground(UITheme.ACCENT_TEAL);
        l.setBorder(new EmptyBorder(8, 0, 4, 0));
        p.add(l, gbc);
        gbc.gridwidth = 1;
    }

    private void addSettingRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel l = new ui.UITheme.ThemedLabel(label);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TXT_SECONDARY);
        p.add(l, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        if (comp instanceof JPasswordField) {
            comp.setBackground(UITheme.SURFACE_2);
            comp.setForeground(UITheme.TXT_PRIMARY);
            comp.setFont(UITheme.FONT_REGULAR);
            ((JPasswordField) comp).setCaretColor(UITheme.PRIMARY);
            ((JPasswordField) comp).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER, 1),
                    new EmptyBorder(6, 10, 6, 10)
            ));
        }
        p.add(comp, gbc);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MailClientGUI client = new MailClientGUI();
            client.setVisible(true);
        });
    }
}

package client;

import ui.LabIcons;
import client.FtpClientEngine.FtpFileItem;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FtpClientGUI extends JFrame implements FtpClientEngine.FtpEventListener {

    private final FtpClientEngine client = new FtpClientEngine();

    // Top Connection Bar Controls
    private UITheme.ModernTextField txtHost;
    private UITheme.ModernTextField txtPort;
    private UITheme.ModernTextField txtUser;
    private JPasswordField txtPass;
    private UITheme.ModernButton btnConnect;
    private JLabel lblStatusChip;

    // Local Explorer
    private File currentLocalDir = new File(System.getProperty("user.home"));
    private JLabel lblLocalPath;
    private DefaultTableModel localModel;
    private JTable localTable;

    // Remote Explorer
    private JLabel lblRemotePath;
    private DefaultTableModel remoteModel;
    private JTable remoteTable;

    // Middle Action Buttons
    private UITheme.ModernButton btnUpload;
    private UITheme.ModernButton btnDownload;
    private UITheme.ModernButton btnMkdir;
    private UITheme.ModernButton btnDelete;
    private UITheme.ModernButton btnRefresh;

    // Bottom Progress & Terminal
    private JProgressBar progressBar;
    private JLabel lblProgressStatus;
    private JTextArea terminalArea;

    public FtpClientGUI() { this(2121); }

    public FtpClientGUI(int serverPort) {
        super("FTP Client · Khách Hàng Truyền Nhận Tệp (VKU Lập Trình Mạng)");
        ui.UITheme.install();
        setSize(1060, 680);
        setMinimumSize(new Dimension(920, 580));
        UITheme.fitWindow(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        client.setListener(this);

        createGUI();
        txtPort.setText(String.valueOf(serverPort));
        loadLocalDirectory(currentLocalDir);
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
        root.add(buildMainSplit(), BorderLayout.CENTER);
        root.add(buildBottomStatus(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header & Connection Bar ───────────────────────────────────────────────────
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

        // Top line: Logo & Title
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 36;
                GradientPaint gp = new GradientPaint(0, 0, UITheme.ACCENT_TEAL, 0, s, UITheme.PRIMARY);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, s, s, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                LabIcons.icon("folder",24).paintIcon(this,g2,10,10);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(36, 36));
        left.add(logo);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel lblTitle = new ui.UITheme.ThemedLabel("FTP Client · Desktop Transfer");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TXT_PRIMARY);
        titleCol.add(lblTitle);

        JLabel lblSub = new ui.UITheme.ThemedLabel("Đồng bộ giao thức với FTP Server · Quản lý tập tin 2 chiều");
        lblSub.setFont(UITheme.FONT_TINY);
        lblSub.setForeground(UITheme.TXT_MUTED);
        titleCol.add(lblSub);
        left.add(titleCol);

        topRow.add(left, BorderLayout.WEST);

        lblStatusChip = new ui.UITheme.ThemedLabel("● CHƯA KẾT NỐI");
        lblStatusChip.setFont(UITheme.FONT_BOLD);
        lblStatusChip.setForeground(UITheme.COLOR_RED);
        topRow.add(lblStatusChip, BorderLayout.EAST);
        header.add(topRow, BorderLayout.NORTH);

        // Bottom line: Connection Inputs
        JPanel connRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        connRow.setOpaque(false);
        connRow.setBorder(new EmptyBorder(8, 0, 0, 0));

        connRow.add(createInputLabel("Host:"));
        txtHost = new UITheme.ModernTextField("127.0.0.1", 9);
        connRow.add(txtHost);

        connRow.add(createInputLabel("Port:"));
        txtPort = new UITheme.ModernTextField("2121", 4);
        connRow.add(txtPort);

        connRow.add(createInputLabel("Tài khoản:"));
        txtUser = new UITheme.ModernTextField("admin", 8);
        connRow.add(txtUser);

        connRow.add(createInputLabel("Mật khẩu:"));
        txtPass = new JPasswordField("admin123", 8);
        txtPass.setBackground(UITheme.SURFACE_2);
        txtPass.setForeground(UITheme.TXT_PRIMARY);
        txtPass.setCaretColor(UITheme.PRIMARY);
        txtPass.setFont(UITheme.FONT_REGULAR);
        txtPass.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        connRow.add(txtPass);

        btnConnect = new UITheme.ModernButton("Kết Nối", UITheme.COLOR_GREEN, UITheme.SURFACE_3, Color.WHITE);
        btnConnect.addActionListener(e -> toggleConnection());
        connRow.add(btnConnect);

        header.add(connRow, BorderLayout.SOUTH);
        return header;
    }

    private JLabel createInputLabel(String text) {
        JLabel l = new ui.UITheme.ThemedLabel(text);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TXT_SECONDARY);
        return l;
    }

    // ── Central Split Explorer ────────────────────────────────────────────────────
    private JPanel buildMainSplit() {
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(10, 16, 6, 16));

        JPanel leftExplorer = buildLocalExplorerPanel();
        JPanel rightExplorer = buildRemoteExplorerPanel();
        JPanel middleToolbar = buildMiddleToolbar();

        JPanel dualPanel = new JPanel(new GridBagLayout());
        dualPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left 47%
        gbc.gridx = 0; gbc.weightx = 0.47;
        dualPanel.add(leftExplorer, gbc);

        // Middle buttons 6%
        gbc.gridx = 1; gbc.weightx = 0.06;
        dualPanel.add(middleToolbar, gbc);

        // Right 47%
        gbc.gridx = 2; gbc.weightx = 0.47;
        dualPanel.add(rightExplorer, gbc);

        center.add(dualPanel, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildLocalExplorerPanel() {
        JPanel panel = createExplorerContainer();

        // Header Local
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new ui.UITheme.ThemedLabel("🖥️ Thư Mục Cục Bộ (Local Machine)");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TXT_PRIMARY);
        top.add(title, BorderLayout.NORTH);

        JPanel navRow = new JPanel(new BorderLayout(6, 0));
        navRow.setOpaque(false);

        lblLocalPath = new ui.UITheme.ThemedLabel(currentLocalDir.getAbsolutePath());
        lblLocalPath.setFont(UITheme.FONT_SMALL);
        lblLocalPath.setForeground(UITheme.TXT_SECONDARY);
        navRow.add(lblLocalPath, BorderLayout.CENTER);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navBtns.setOpaque(false);

        UITheme.ModernButton btnUp = new UITheme.ModernButton("⬆ Lên", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnUp.addActionListener(e -> {
            File parent = currentLocalDir.getParentFile();
            if (parent != null && parent.exists()) {
                loadLocalDirectory(parent);
            }
        });

        UITheme.ModernButton btnBrowse = new UITheme.ModernButton("Chọn...", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(currentLocalDir);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loadLocalDirectory(chooser.getSelectedFile());
            }
        });

        navBtns.add(btnUp);
        navBtns.add(btnBrowse);
        navRow.add(navBtns, BorderLayout.EAST);
        top.add(navRow, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        // Local Table
        String[] cols = {"Tên tệp tin / Thư mục", "Kích thước", "Ngày sửa"};
        localModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        localTable = new JTable(localModel);
        UITheme.styleTable(localTable);

        localTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int sel = localTable.getSelectedRow();
                    if (sel >= 0) {
                        String name = (String) localModel.getValueAt(sel, 0);
                        if (name.startsWith("[DIR] ")) {
                            File sub = new File(currentLocalDir, name.substring(3));
                            if (sub.isDirectory()) loadLocalDirectory(sub);
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(localTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRemoteExplorerPanel() {
        JPanel panel = createExplorerContainer();

        // Header Remote
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new ui.UITheme.ThemedLabel("☁️ Thư Mục Máy Chủ (FTP Server)");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TXT_PRIMARY);
        top.add(title, BorderLayout.NORTH);

        JPanel navRow = new JPanel(new BorderLayout(6, 0));
        navRow.setOpaque(false);

        lblRemotePath = new ui.UITheme.ThemedLabel("/");
        lblRemotePath.setFont(UITheme.FONT_SMALL);
        lblRemotePath.setForeground(UITheme.ACCENT_TEAL);
        navRow.add(lblRemotePath, BorderLayout.CENTER);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navBtns.setOpaque(false);

        UITheme.ModernButton btnUp = new UITheme.ModernButton("⬆ Lên", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnUp.addActionListener(e -> {
            if (client.isConnected()) {
                new Thread(() -> {
                    try {
                        client.changeToParentDir();
                        loadRemoteDirectory();
                    } catch (IOException ex) {
                        showError("Không thể lên thư mục cha: " + ex.getMessage());
                    }
                }).start();
            }
        });

        UITheme.ModernButton btnReload = new UITheme.ModernButton("🔄 Làm Mới", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnReload.addActionListener(e -> {
            if (client.isConnected()) {
                new Thread(this::loadRemoteDirectory).start();
            }
        });

        navBtns.add(btnUp);
        navBtns.add(btnReload);
        navRow.add(navBtns, BorderLayout.EAST);
        top.add(navRow, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        // Remote Table
        String[] cols = {"Tên tệp tin / Thư mục", "Quyền", "Kích thước", "Ngày sửa"};
        remoteModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        remoteTable = new JTable(remoteModel);
        UITheme.styleTable(remoteTable);

        remoteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int sel = remoteTable.getSelectedRow();
                    if (sel >= 0) {
                        String name = (String) remoteModel.getValueAt(sel, 0);
                        if (name.startsWith("[DIR] ")) {
                            String dirName = name.substring(3);
                            new Thread(() -> {
                                try {
                                    client.changeDirectory(dirName);
                                    loadRemoteDirectory();
                                } catch (IOException ex) {
                                    showError("Lỗi chuyển thư mục: " + ex.getMessage());
                                }
                            }).start();
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(remoteTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildMiddleToolbar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 4, 0, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        btnUpload = new UITheme.ModernButton("Upload ➔", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnUpload.setEnabled(false);
        btnUpload.addActionListener(e -> executeUpload());
        panel.add(btnUpload, gbc);

        gbc.gridy++;
        btnDownload = new UITheme.ModernButton("⬅ Download", UITheme.ACCENT_TEAL, new Color(36, 184, 165), Color.BLACK);
        btnDownload.setEnabled(false);
        btnDownload.addActionListener(e -> executeDownload());
        panel.add(btnDownload, gbc);

        gbc.gridy++;
        btnMkdir = new UITheme.ModernButton("➕ Thư Mục", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnMkdir.setEnabled(false);
        btnMkdir.addActionListener(e -> executeMakeDirectory());
        panel.add(btnMkdir, gbc);

        gbc.gridy++;
        btnDelete = new UITheme.ModernButton("🗑️ Xóa", UITheme.SURFACE_2, new Color(60, 20, 20), UITheme.COLOR_RED);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> executeDelete());
        panel.add(btnDelete, gbc);

        return panel;
    }

    private JPanel createExplorerContainer() {
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
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        return panel;
    }

    // ── Bottom Progress & Status Bar ──────────────────────────────────────────────
    private JPanel buildBottomStatus() {
        JPanel footer = new JPanel(new BorderLayout(8, 4)) {
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
        footer.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Progress bar row
        JPanel progressRow = new JPanel(new BorderLayout(10, 0));
        progressRow.setOpaque(false);

        lblProgressStatus = new ui.UITheme.ThemedLabel("Trạng thái: Sẵn sàng");
        lblProgressStatus.setFont(UITheme.FONT_SMALL);
        lblProgressStatus.setForeground(UITheme.TXT_SECONDARY);
        progressRow.add(lblProgressStatus, BorderLayout.WEST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(UITheme.PRIMARY);
        progressBar.setBackground(UITheme.SURFACE_2);
        progressBar.setPreferredSize(new Dimension(300, 18));
        progressRow.add(progressBar, BorderLayout.EAST);

        footer.add(progressRow, BorderLayout.NORTH);

        // Terminal Log Area (collapsible / compact)
        terminalArea = new JTextArea(3, 80);
        terminalArea.setEditable(false);
        terminalArea.setFont(UITheme.FONT_MONO);
        terminalArea.setBackground(UITheme.BG_PANEL);
        terminalArea.setForeground(UITheme.TXT_SECONDARY);
        terminalArea.setCaretColor(UITheme.PRIMARY);

        JScrollPane scroll = new JScrollPane(terminalArea);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        footer.add(scroll, BorderLayout.SOUTH);

        return footer;
    }

    // ── Operations ────────────────────────────────────────────────────────────────
    private void toggleConnection() {
        if (!client.isConnected()) {
            String host = txtHost.getText().trim();
            int port;
            try {
                port = Integer.parseInt(txtPort.getText().trim());
            } catch (NumberFormatException e) {
                showError("Cổng không hợp lệ!");
                return;
            }
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();

            btnConnect.setEnabled(false);
            btnConnect.setText("Đang kết nối...");

            new Thread(() -> {
                try {
                    client.connect(host, port, user, pass);
                    loadRemoteDirectory();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Lỗi kết nối: " + ex.getMessage());
                        btnConnect.setEnabled(true);
                        btnConnect.setText("Kết Nối");
                    });
                }
            }).start();
        } else {
            client.disconnect();
        }
    }

    private void loadLocalDirectory(File dir) {
        currentLocalDir = dir;
        lblLocalPath.setText(dir.getAbsolutePath());
        localModel.setRowCount(0);

        File[] files = dir.listFiles();
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (File f : files) {
                String prefix = f.isDirectory() ? "[DIR] " : "[FILE] ";
                String size = f.isDirectory() ? "--" : formatBytes(f.length());
                String date = sdf.format(new Date(f.lastModified()));
                localModel.addRow(new Object[]{prefix + f.getName(), size, date});
            }
        }
    }

    private void loadRemoteDirectory() {
        try {
            String pwd = client.getPwd();
            List<FtpFileItem> items = client.listFiles();

            SwingUtilities.invokeLater(() -> {
                lblRemotePath.setText(pwd);
                remoteModel.setRowCount(0);
                for (FtpFileItem it : items) {
                    String prefix = it.isDirectory ? "[DIR] " : "[FILE] ";
                    String size = it.isDirectory ? "--" : formatBytes(it.size);
                    remoteModel.addRow(new Object[]{prefix + it.name, it.rawPermissions, size, it.date});
                }
            });
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> showError("Lỗi tải danh sách file từ Server: " + e.getMessage()));
        }
    }

    private void executeUpload() {
        int sel = localTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp tin cục bộ cần Upload!");
            return;
        }

        String fileName = ((String) localModel.getValueAt(sel, 0)).substring(3);
        File localFile = new File(currentLocalDir, fileName);
        if (localFile.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ upload từng tệp tin!");
            return;
        }

        new Thread(() -> {
            try {
                updateProgressStatus("Đang upload: " + fileName + "...", 0);
                client.uploadFile(localFile, fileName);
                updateProgressStatus("Upload hoàn tất: " + fileName, 100);
                loadRemoteDirectory();
            } catch (IOException ex) {
                showError("Upload thất bại: " + ex.getMessage());
            }
        }).start();
    }

    private void executeDownload() {
        int sel = remoteTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 tệp tin trên Server cần Download!");
            return;
        }

        String fileName = ((String) remoteModel.getValueAt(sel, 0)).substring(3);
        File destFile = new File(currentLocalDir, fileName);

        new Thread(() -> {
            try {
                updateProgressStatus("Đang download: " + fileName + "...", 0);
                client.downloadFile(fileName, destFile);
                updateProgressStatus("Download hoàn tất: " + fileName, 100);
                SwingUtilities.invokeLater(() -> loadLocalDirectory(currentLocalDir));
            } catch (IOException ex) {
                showError("Download thất bại: " + ex.getMessage());
            }
        }).start();
    }

    private void executeMakeDirectory() {
        String name = JOptionPane.showInputDialog(this, "Nhập tên thư mục mới cần tạo trên Server:", "Tạo Thư Mục", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    client.makeDirectory(name.trim());
                    loadRemoteDirectory();
                } catch (IOException ex) {
                    showError("Tạo thư mục thất bại: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void executeDelete() {
        int sel = remoteTable.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn tệp hoặc thư mục trên Server cần xóa!");
            return;
        }

        String raw = (String) remoteModel.getValueAt(sel, 0);
        boolean isDir = raw.startsWith("[DIR] ");
        String name = raw.substring(3);

        int opt = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa " + (isDir ? "thư mục" : "tệp tin") + " '" + name + "' trên Server?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    if (isDir) {
                        client.removeDirectory(name);
                    } else {
                        client.deleteFile(name);
                    }
                    loadRemoteDirectory();
                } catch (IOException ex) {
                    showError("Xóa thất bại: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void updateProgressStatus(String msg, int percent) {
        SwingUtilities.invokeLater(() -> {
            lblProgressStatus.setText("Trạng thái: " + msg);
            progressBar.setValue(percent);
        });
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi FTP", JOptionPane.ERROR_MESSAGE);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // ── Callbacks from FtpClientEngine ────────────────────────────────────────────
    @Override
    public void onConnected(String host, int port, String user) {
        SwingUtilities.invokeLater(() -> {
            btnConnect.setText("Ngắt Kết Nối");
            btnConnect.setBackground(UITheme.COLOR_RED);
            btnConnect.setEnabled(true);

            lblStatusChip.setText("● KẾT NỐI (" + user + ")");
            lblStatusChip.setForeground(UITheme.COLOR_GREEN);

            txtHost.setEnabled(false);
            txtPort.setEnabled(false);
            txtUser.setEnabled(false);
            txtPass.setEnabled(false);

            btnUpload.setEnabled(true);
            btnDownload.setEnabled(true);
            btnMkdir.setEnabled(true);
            btnDelete.setEnabled(true);
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            btnConnect.setText("Kết Nối");
            btnConnect.setBackground(UITheme.COLOR_GREEN);
            btnConnect.setEnabled(true);

            lblStatusChip.setText("● CHƯA KẾT NỐI");
            lblStatusChip.setForeground(UITheme.COLOR_RED);

            txtHost.setEnabled(true);
            txtPort.setEnabled(true);
            txtUser.setEnabled(true);
            txtPass.setEnabled(true);

            btnUpload.setEnabled(false);
            btnDownload.setEnabled(false);
            btnMkdir.setEnabled(false);
            btnDelete.setEnabled(false);

            remoteModel.setRowCount(0);
            lblRemotePath.setText("/");
            progressBar.setValue(0);
            lblProgressStatus.setText("Trạng thái: Đã ngắt kết nối.");
        });
    }

    @Override
    public void onLog(String line) {
        SwingUtilities.invokeLater(() -> {
            if (terminalArea != null) {
                terminalArea.append(line + "\n");
                terminalArea.setCaretPosition(terminalArea.getDocument().getLength());
            }
        });
    }

    @Override
    public void onProgress(String fileName, long transferred, long total) {
        SwingUtilities.invokeLater(() -> {
            int percent = total > 0 ? (int) ((transferred * 100) / total) : 0;
            progressBar.setValue(percent);
            lblProgressStatus.setText(String.format("Đang truyền %s: %s / %s (%d%%)",
                    fileName, formatBytes(transferred), formatBytes(total), percent));
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            FtpClientGUI gui = new FtpClientGUI();
            gui.setVisible(true);
        });
    }
}

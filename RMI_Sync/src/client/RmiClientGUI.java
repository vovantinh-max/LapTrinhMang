package client;

import model.StudentRecord;
import rmi.DataService;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.Naming;
import java.text.DecimalFormat;
import java.util.List;

public class RmiClientGUI extends JFrame {

    private final String[] serverOptions = {
            System.getProperty("rmi.server1", "rmi://127.0.0.1:1099/DataServer1"),
            System.getProperty("rmi.server2", "rmi://127.0.0.1:1100/DataServer2")
    };

    private JComboBox<String> serverCombo;
    private UITheme.ModernButton btnConnect;
    private UITheme.ModernButton btnFailover;
    private JLabel lblStatusChip;

    // Form inputs
    private UITheme.ModernTextField txtCode;
    private UITheme.ModernTextField txtName;
    private UITheme.ModernTextField txtClass;
    private UITheme.ModernTextField txtMajor;
    private UITheme.ModernTextField txtGpa;

    // Action buttons
    private UITheme.ModernButton btnAdd;
    private UITheme.ModernButton btnUpdate;
    private UITheme.ModernButton btnDelete;
    private UITheme.ModernButton btnClear;

    // Search bar
    private UITheme.ModernTextField txtSearch;
    private UITheme.ModernButton btnSearch;
    private UITheme.ModernButton btnRefresh;

    // Table
    private JTable studentTable;
    private DefaultTableModel tableModel;

    // Status bar
    private JLabel lblServerInfo;
    private JLabel lblTotalStudents;
    private JLabel lblActionMessage;

    // RMI Service stub
    private volatile DataService activeService;
    private String currentServerUrl = "";
    private String currentServerName = "None";

    public RmiClientGUI() {
        super("RMI Client - Quản lý sinh viên đồng bộ 2 Server");
        ui.UITheme.install();
        initUI();
        // Mặc định kết nối tới Server 1
        connectToServer(serverOptions[0]);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        UITheme.fitWindow(this);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeaderPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(14, 14));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        centerPanel.add(buildFormPanel(), BorderLayout.WEST);
        centerPanel.add(buildTablePanel(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UITheme.BG_PANEL);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // Title & Logo
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel logo = new ui.UITheme.ThemedLabel("⚡");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        left.add(logo);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new ui.UITheme.ThemedLabel("RMI CLIENT • SYNCHRONIZED DUAL SERVER");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TXT_PRIMARY);
        JLabel subtitle = new ui.UITheme.ThemedLabel("Kết nối và thao tác dữ liệu qua Java Remote Method Invocation (Active-Active)");
        subtitle.setFont(UITheme.FONT_TINY);
        subtitle.setForeground(UITheme.TXT_MUTED);
        titleBox.add(title);
        titleBox.add(subtitle);
        left.add(titleBox);

        // Server selector & connect
        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        right.setBorder(new EmptyBorder(10,0,0,0));
        right.setOpaque(false);

        serverCombo = new JComboBox<>(serverOptions);
        serverCombo.setEditable(true);
        serverCombo.setToolTipText("Nhập rmi://IP:PORT/DataServer1 hoặc DataServer2 để kết nối qua LAN");
        serverCombo.setBackground(UITheme.SURFACE_2);
        serverCombo.setForeground(UITheme.TXT_PRIMARY);
        serverCombo.setFont(UITheme.FONT_SMALL);
        serverCombo.setPreferredSize(new Dimension(320, 34));
        serverCombo.setFocusable(true);

        btnConnect = new UITheme.ModernButton("Kết Nối", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnConnect.addActionListener(e -> connectToServer(String.valueOf(serverCombo.getSelectedItem())));

        btnFailover = new UITheme.ModernButton("🔄 Chuyển Server", UITheme.COLOR_AMBER, new Color(217, 119, 6), Color.WHITE);
        btnFailover.setToolTipText("Tự động chuyển đổi kết nối sang Server dự phòng còn lại");
        btnFailover.addActionListener(e -> switchFailover());

        lblStatusChip = new ui.UITheme.ThemedLabel("○ Chưa kết nối");
        lblStatusChip.setFont(UITheme.FONT_SMALL);
        lblStatusChip.setForeground(UITheme.TXT_MUTED);

        right.add(serverCombo);
        right.add(btnConnect);
        right.add(btnFailover);
        right.add(lblStatusChip);

        header.add(left, BorderLayout.NORTH);
        header.add(right, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new BorderLayout(0, 12));
        form.setBackground(UITheme.SURFACE_1);
        form.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));
        form.setPreferredSize(new Dimension(300, 0));

        JLabel lblFormTitle = new ui.UITheme.ThemedLabel("THÔNG TIN SINH VIÊN");
        lblFormTitle.setFont(UITheme.FONT_HEADER);
        lblFormTitle.setForeground(UITheme.ACCENT_TEAL);
        form.add(lblFormTitle, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setOpaque(false);

        txtCode = new UITheme.ModernTextField(15);
        txtName = new UITheme.ModernTextField(15);
        txtClass = new UITheme.ModernTextField(15);
        txtMajor = new UITheme.ModernTextField(15);
        txtGpa = new UITheme.ModernTextField(15);

        fieldsPanel.add(createFieldGroup("Mã sinh viên (*)", txtCode));
        fieldsPanel.add(Box.createVerticalStrut(4));
        fieldsPanel.add(createFieldGroup("Họ và tên (*)", txtName));
        fieldsPanel.add(Box.createVerticalStrut(4));
        fieldsPanel.add(createFieldGroup("Lớp sinh hoạt", txtClass));
        fieldsPanel.add(Box.createVerticalStrut(4));
        fieldsPanel.add(createFieldGroup("Chuyên ngành", txtMajor));
        fieldsPanel.add(Box.createVerticalStrut(4));
        fieldsPanel.add(createFieldGroup("Điểm GPA (0.0 - 4.0)", txtGpa));

        JScrollPane fieldScroll = new JScrollPane(fieldsPanel);
        fieldScroll.setBorder(null);
        fieldScroll.getViewport().setBackground(UITheme.SURFACE_1);
        fieldScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fieldScroll.getVerticalScrollBar().setUnitIncrement(16);
        form.add(fieldScroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnBox = new JPanel(new GridLayout(2, 2, 8, 8));
        btnBox.setOpaque(false);

        btnAdd = new UITheme.ModernButton("➕ Thêm mới", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnUpdate = new UITheme.ModernButton("✏️ Cập nhật", UITheme.COLOR_CYAN, new Color(8, 145, 178), Color.WHITE);
        btnDelete = new UITheme.ModernButton("🗑️ Xóa", UITheme.COLOR_RED, new Color(220, 38, 38), Color.WHITE);
        btnClear = new UITheme.ModernButton("Làm mới", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_SECONDARY);

        btnAdd.addActionListener(e -> handleAddStudent());
        btnUpdate.addActionListener(e -> handleUpdateStudent());
        btnDelete.addActionListener(e -> handleDeleteStudent());
        btnClear.addActionListener(e -> clearForm());

        btnBox.add(btnAdd);
        btnBox.add(btnUpdate);
        btnBox.add(btnDelete);
        btnBox.add(btnClear);

        form.add(btnBox, BorderLayout.SOUTH);
        return form;
    }

    private JPanel createFieldGroup(String label, JTextField field) {
        JPanel group = new JPanel(new BorderLayout(0, 2));
        group.setOpaque(false);
        group.setMinimumSize(new Dimension(0,48));
        group.setPreferredSize(new Dimension(260,48));
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
        JLabel lbl = new ui.UITheme.ThemedLabel(label);
        lbl.setFont(UITheme.FONT_TINY);
        lbl.setForeground(UITheme.TXT_SECONDARY);
        group.add(lbl, BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(260, 32));
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Search & Filter Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);

        txtSearch = new UITheme.ModernTextField(22);
        txtSearch.setPreferredSize(new Dimension(260, 32));
        txtSearch.setToolTipText("Nhập mã sinh viên, họ tên hoặc lớp...");
        txtSearch.addActionListener(e -> handleSearch());

        btnSearch = new UITheme.ModernButton("🔍 Tìm", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnSearch.setPreferredSize(new Dimension(75, 32));
        btnSearch.addActionListener(e -> handleSearch());

        btnRefresh = new UITheme.ModernButton("🔄 Tải lại", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_PRIMARY);
        btnRefresh.setPreferredSize(new Dimension(85, 32));
        btnRefresh.addActionListener(e -> reloadAllStudents());

        searchBox.add(new ui.UITheme.ThemedLabel("Tìm kiếm:"));
        searchBox.getComponent(0).setForeground(UITheme.TXT_SECONDARY);
        searchBox.getComponent(0).setFont(UITheme.FONT_SMALL);
        searchBox.add(txtSearch);
        searchBox.add(btnSearch);
        searchBox.add(btnRefresh);

        toolbar.add(searchBox, BorderLayout.WEST);
        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Mã SV", "Họ và Tên", "Lớp", "Chuyên ngành", "GPA", "Version", "Node sửa cuối", "Cập nhật lúc"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        studentTable = new JTable(tableModel);
        UITheme.styleTable(studentTable);
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        studentTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        studentTable.getColumnModel().getColumn(5).setPreferredWidth(65);
        studentTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        studentTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        studentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = studentTable.getSelectedRow();
                if (row >= 0) {
                    txtCode.setText(tableModel.getValueAt(row, 0).toString());
                    txtName.setText(tableModel.getValueAt(row, 1).toString());
                    txtClass.setText(tableModel.getValueAt(row, 2).toString());
                    txtMajor.setText(tableModel.getValueAt(row, 3).toString());
                    txtGpa.setText(tableModel.getValueAt(row, 4).toString());
                    txtCode.setEnabled(false); // Khóa sửa mã khi đang chọn
                }
            }
        });

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.getViewport().setBackground(UITheme.SURFACE_1);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        scroll.getVerticalScrollBar().setUI(new UITheme.DarkScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new UITheme.DarkScrollBarUI());

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout(16, 0));
        status.setBackground(UITheme.SURFACE_1);
        status.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.BORDER),
                new EmptyBorder(6, 16, 6, 16)
        ));

        lblServerInfo = new ui.UITheme.ThemedLabel("Máy chủ: Chưa kết nối");
        lblServerInfo.setFont(UITheme.FONT_SMALL);
        lblServerInfo.setForeground(UITheme.TXT_SECONDARY);

        lblActionMessage = new ui.UITheme.ThemedLabel("Sẵn sàng");
        lblActionMessage.setFont(UITheme.FONT_SMALL);
        lblActionMessage.setForeground(UITheme.TXT_MUTED);

        lblTotalStudents = new ui.UITheme.ThemedLabel("Tổng sinh viên: 0");
        lblTotalStudents.setFont(UITheme.FONT_BOLD);
        lblTotalStudents.setForeground(UITheme.ACCENT_TEAL);

        JPanel left = new JPanel(new BorderLayout(8,3));
        left.setOpaque(false);
        left.add(lblServerInfo, BorderLayout.NORTH);
        left.add(lblActionMessage, BorderLayout.CENTER);

        status.add(left, BorderLayout.CENTER);
        status.add(lblTotalStudents, BorderLayout.EAST);
        return status;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  RMI LOGIC
    // ════════════════════════════════════════════════════════════════════════════

    public void connectToServer(String option) {
        final String url = option.trim();
        activeService = null;
        currentServerName = url.substring(url.lastIndexOf('/') + 1);
        currentServerUrl = url;

        new Thread(() -> {
            try {
                activeService = (DataService) Naming.lookup(url);
                String ping = activeService.ping();

                SwingUtilities.invokeLater(() -> {
                    lblStatusChip.setText("● ĐÃ KẾT NỐI: " + currentServerName);
                    lblStatusChip.setForeground(UITheme.COLOR_GREEN);
                    lblServerInfo.setText("Máy chủ RMI: " + url);
                    lblActionMessage.setText("Kết nối thành công! (" + ping + ")");
                    reloadAllStudents();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusChip.setText("○ MẤT KẾT NỐI: " + currentServerName);
                    lblStatusChip.setForeground(UITheme.COLOR_RED);
                    lblServerInfo.setText("Không thể kết nối tới " + url);
                    lblActionMessage.setText("Lỗi: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Không thể kết nối tới RMI Server tại:\n" + url + "\n\nNguyên nhân: " + ex.getMessage() +
                            "\n\nVui lòng khởi động Server từ giao diện RmiDualServerGUI trước!",
                            "Lỗi kết nối RMI", JOptionPane.WARNING_MESSAGE);
                });
            }
        }).start();
    }

    private void switchFailover() {
        int nextIndex = (serverCombo.getSelectedIndex() + 1) % serverOptions.length;
        serverCombo.setSelectedIndex(nextIndex);
        connectToServer(serverOptions[nextIndex]);
    }

    private void reloadAllStudents() {
        if (activeService == null) return;
        new Thread(() -> {
            try {
                List<StudentRecord> list = activeService.getAllStudents();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    DecimalFormat df = new DecimalFormat("0.00");
                    for (StudentRecord s : list) {
                        tableModel.addRow(new Object[]{
                                s.getStudentCode(),
                                s.getFullName(),
                                s.getClassName(),
                                s.getMajor(),
                                df.format(s.getGpa()),
                                "v" + s.getVersion(),
                                s.getLastModifiedServer(),
                                s.getUpdatedAt()
                        });
                    }
                    lblTotalStudents.setText("Tổng sinh viên: " + list.size());
                    lblActionMessage.setText("Đã tải " + list.size() + " sinh viên từ " + currentServerName);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblActionMessage.setText("Lỗi đọc dữ liệu: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            reloadAllStudents();
            return;
        }
        if (activeService == null) return;

        new Thread(() -> {
            try {
                List<StudentRecord> list = activeService.searchStudents(keyword);
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    DecimalFormat df = new DecimalFormat("0.00");
                    for (StudentRecord s : list) {
                        tableModel.addRow(new Object[]{
                                s.getStudentCode(),
                                s.getFullName(),
                                s.getClassName(),
                                s.getMajor(),
                                df.format(s.getGpa()),
                                "v" + s.getVersion(),
                                s.getLastModifiedServer(),
                                s.getUpdatedAt()
                        });
                    }
                    lblTotalStudents.setText("Kết quả tìm thấy: " + list.size());
                    lblActionMessage.setText("Tìm thấy " + list.size() + " kết quả cho '" + keyword + "'");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblActionMessage.setText("Lỗi tìm kiếm: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleAddStudent() {
        if (activeService == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối tới máy chủ RMI!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = txtCode.getText().trim();
        String name = txtName.getText().trim();
        String clazz = txtClass.getText().trim();
        String major = txtMajor.getText().trim();
        String gpaStr = txtGpa.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã SV và Họ Tên!", "Thông tin thiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double gpa = 0.0;
        try {
            if (!gpaStr.isEmpty()) gpa = Double.parseDouble(gpaStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Điểm GPA phải là số thực!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StudentRecord record = new StudentRecord(code, name, clazz, major, gpa);
        new Thread(() -> {
            try {
                StudentRecord res = activeService.addStudent(record);
                SwingUtilities.invokeLater(() -> {
                    if (res != null) {
                        lblActionMessage.setText("Đã thêm " + code + " thành công trên " + currentServerName + " (Đã phát lệnh sync)");
                        clearForm();
                        reloadAllStudents();
                    } else {
                        JOptionPane.showMessageDialog(this, "Không thể thêm! Có thể mã sinh viên đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi RMI: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void handleUpdateStudent() {
        if (activeService == null) return;

        String code = txtCode.getText().trim();
        String name = txtName.getText().trim();
        String clazz = txtClass.getText().trim();
        String major = txtMajor.getText().trim();
        String gpaStr = txtGpa.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập sinh viên cần cập nhật!", "Thông tin thiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double gpa = 0.0;
        try {
            if (!gpaStr.isEmpty()) gpa = Double.parseDouble(gpaStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Điểm GPA phải là số thực!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StudentRecord record = new StudentRecord(code, name, clazz, major, gpa);
        new Thread(() -> {
            try {
                boolean ok = activeService.updateStudent(record);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        lblActionMessage.setText("Đã cập nhật " + code + " trên " + currentServerName + " (Đã phát lệnh sync)");
                        clearForm();
                        reloadAllStudents();
                    } else {
                        JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi RMI: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void handleDeleteStudent() {
        if (activeService == null) return;
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập mã SV cần xóa!", "Thông tin thiếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa sinh viên [" + code + "] khỏi hệ thống đồng bộ?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                boolean ok = activeService.deleteStudent(code);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        lblActionMessage.setText("Đã xóa " + code + " khỏi " + currentServerName + " (Đã phát lệnh sync)");
                        clearForm();
                        reloadAllStudents();
                    } else {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy mã sinh viên để xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi RMI: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void clearForm() {
        txtCode.setEnabled(true);
        txtCode.setText("");
        txtName.setText("");
        txtClass.setText("");
        txtMajor.setText("");
        txtGpa.setText("");
        txtCode.requestFocus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RmiClientGUI gui = new RmiClientGUI();
            gui.setVisible(true);
        });
    }
}

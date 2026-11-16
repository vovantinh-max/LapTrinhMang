package ui;

import database.UserDAO;
import database.UserDAO.User;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class UserManagerDialog extends JDialog {
    private final UITheme.ModernTextField txtUsername = new UITheme.ModernTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final UITheme.ModernTextField txtHomeDir = new UITheme.ModernTextField(20);
    private final JCheckBox chkCanRead = new JCheckBox("Quyền đọc (Download / Liệt kê file)", true);
    private final JCheckBox chkCanWrite = new JCheckBox("Quyền ghi (Upload / Xóa / Tạo thư mục)", true);
    private final JCheckBox chkIsActive = new JCheckBox("Kích hoạt tài khoản này", true);

    private boolean saved = false;
    private final User editingUser;

    public UserManagerDialog(Frame parent, User userToEdit) {
        super(parent, userToEdit == null ? "Thêm Người Dùng FTP" : "Chỉnh Sửa Người Dùng FTP", true);
        this.editingUser = userToEdit;

        setSize(460, 420);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BG_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Title
        JLabel lblHeader = new ui.UITheme.ThemedLabel(userToEdit == null ? "➕ THÊM TÀI KHOẢN FTP MỚI" : "✏️ CẬP NHẬT TÀI KHOẢN FTP");
        lblHeader.setFont(UITheme.FONT_HEADER);
        lblHeader.setForeground(UITheme.TXT_PRIMARY);
        lblHeader.setBorder(new EmptyBorder(0, 0, 12, 0));
        root.add(lblHeader, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        addLabelAndField(formPanel, gbc, row++, "Tên đăng nhập:", txtUsername);

        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setForeground(UITheme.TXT_PRIMARY);
        txtPassword.setCaretColor(UITheme.PRIMARY);
        txtPassword.setBackground(UITheme.SURFACE_2);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(7, 12, 7, 12)
        ));
        addLabelAndField(formPanel, gbc, row++, "Mật khẩu:", txtPassword);

        addLabelAndField(formPanel, gbc, row++, "Thư mục gốc (Home):", txtHomeDir);

        styleCheckbox(chkCanRead);
        styleCheckbox(chkCanWrite);
        styleCheckbox(chkIsActive);

        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(chkCanRead, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(chkCanWrite, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(chkIsActive, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);

        UITheme.ModernButton btnSave = new UITheme.ModernButton("Lưu tài khoản", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        UITheme.ModernButton btnCancel = new UITheme.ModernButton("Hủy bỏ", UITheme.SURFACE_2, UITheme.SURFACE_3, UITheme.TXT_SECONDARY);

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);

        // Load data if editing
        if (editingUser != null) {
            txtUsername.setText(editingUser.getUsername());
            txtUsername.setEnabled(false);
            txtPassword.setText(editingUser.getPassword());
            txtHomeDir.setText(editingUser.getHomeDir());
            chkCanRead.setSelected(editingUser.isCanRead());
            chkCanWrite.setSelected(editingUser.isCanWrite());
            chkIsActive.setSelected(editingUser.isActive());
        } else {
            txtHomeDir.setText("/");
        }
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lbl = new ui.UITheme.ThemedLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TXT_SECONDARY);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(comp, gbc);
    }

    private void styleCheckbox(JCheckBox chk) {
        chk.setOpaque(false);
        chk.setFont(UITheme.FONT_REGULAR);
        chk.setForeground(UITheme.TXT_PRIMARY);
        chk.setFocusPainted(false);
    }

    private void onSave() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String homeDir = txtHomeDir.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (homeDir.isEmpty()) {
            homeDir = "/";
        }

        if (editingUser == null) {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setHomeDir(homeDir);
            newUser.setCanRead(chkCanRead.isSelected());
            newUser.setCanWrite(chkCanWrite.isSelected());
            newUser.setActive(chkIsActive.isSelected());

            if (UserDAO.addUser(newUser)) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thêm người dùng (có thể bị trùng tên)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            editingUser.setPassword(password);
            editingUser.setHomeDir(homeDir);
            editingUser.setCanRead(chkCanRead.isSelected());
            editingUser.setCanWrite(chkCanWrite.isSelected());
            editingUser.setActive(chkIsActive.isSelected());

            if (UserDAO.updateUser(editingUser)) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật người dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }
}

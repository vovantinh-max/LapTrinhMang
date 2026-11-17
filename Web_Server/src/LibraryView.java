import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class LibraryView extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");
    private final MiniHttp app;
    private final JLabel title = Theme.label("", 26, Font.BOLD, Theme.INK);
    private final JLabel count = Theme.label("", 13, Font.PLAIN, Theme.MUTED);
    private final JLabel empty = Theme.label("", 16, Font.PLAIN, Theme.MUTED);
    private final JTextField filter = new JTextField(18);
    private final CardLayout tableLayout = new CardLayout();
    private final JPanel tableView = Theme.panel(tableLayout, Theme.PAPER);
    private final Model model = new Model();
    final JTable table = new JTable(model);
    private final JButton mainButton = Theme.primary("Mở", null, this::openSelection);
    private final JButton secondary = Theme.button("Xóa", null, this::secondaryAction);
    private final List<Transfer> transfers = new ArrayList<>();
    private String section = MiniHttp.HISTORY;

    static final class Transfer {
        final URI uri;
        final Path file;
        boolean active = true;
        String status = "Đang kết nối…";

        Transfer(URI uri, Path file) {
            this.uri = uri;
            this.file = file;
        }
    }

    private record Row(URI uri, Path file, boolean open, String[] cells) {}

    LibraryView(MiniHttp app) {
        super(new BorderLayout(0, 13));
        this.app = app;
        setBackground(Theme.CANVAS);
        setBorder(Theme.inset(23, 26, 23));
        var heading = Theme.panel(new BorderLayout(15, 0), Theme.CANVAS);
        heading.add(title, BorderLayout.WEST);
        var right = Theme.row(8, Theme.CANVAS);
        right.add(Theme.label("Lọc", 13, Font.BOLD, Theme.MUTED));
        filter.setToolTipText("Lọc theo địa chỉ, phương thức hoặc tên tệp");
        filter.getAccessibleContext().setAccessibleName("Lọc danh sách");
        filter.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE), Theme.inset(7, 9, 7)));
        right.add(filter);
        right.add(secondary);
        right.add(mainButton);
        heading.add(right, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(8, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> updateActions());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.rowAtPoint(event.getPoint()) != -1) openSelection();
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
        table.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { openSelection(); }
        });
        var emptyCard = Theme.panel(new GridBagLayout(), Theme.PAPER);
        emptyCard.add(empty);
        tableView.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        tableView.add(Theme.scroll(table), "table");
        tableView.add(emptyCard, "empty");
        add(tableView);
        add(count, BorderLayout.SOUTH);
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { refresh(); }
            @Override public void removeUpdate(DocumentEvent event) { refresh(); }
            @Override public void changedUpdate(DocumentEvent event) { refresh(); }
        });
    }

    void showSection(String choice) {
        if (!section.equals(choice)) {
            section = choice;
            filter.setText("");
        }
        title.setText(section);
        mainButton.setText(section.equals(MiniHttp.DOWNLOADS) ? "Mở tệp" : "Mở trong tab mới");
        secondary.setText(switch (section) {
            case MiniHttp.HISTORY -> "Xóa lịch sử";
            case MiniHttp.BOOKMARKS -> "Bỏ dấu trang";
            default -> "Mở thư mục";
        });
        refresh();
    }

    void refresh() {
        Row selected = selection();
        var all = new ArrayList<Row>();
        String[] columns = switch (section) {
            case MiniHttp.HISTORY -> {
                for (var item : app.profile.history) {
                    all.add(new Row(item.uri(), null, true, new String[]{
                            TIME.format(item.time()), item.method(), item.uri().toString(), item.status()}));
                }
                yield new String[]{"Thời gian", "Phương thức", "Địa chỉ", "Kết quả"};
            }
            case MiniHttp.BOOKMARKS -> {
                for (var uri : app.profile.bookmarks) {
                    all.add(new Row(uri, null, true, new String[]{uri.getHost(), uri.toString()}));
                }
                yield new String[]{"Máy chủ", "Địa chỉ"};
            }
            default -> {
                for (var item : transfers) {
                    all.add(new Row(item.uri, item.file, false,
                            new String[]{item.file.getFileName().toString(), item.status, item.uri.toString()}));
                }
                for (var item : app.profile.downloads) {
                    all.add(new Row(item.uri(), item.file(), true, new String[]{
                            item.file().getFileName().toString(), "Đã lưu · " + MiniHttp.size(item.bytes()), item.uri().toString()}));
                }
                yield new String[]{"Tệp", "Trạng thái", "Nguồn"};
            }
        };
        String query = filter.getText().strip().toLowerCase(Locale.ROOT);
        var visible = all.stream().filter(row -> query.isEmpty() || String.join(" ", row.cells())
                .toLowerCase(Locale.ROOT).contains(query)).toList();
        model.replace(columns, visible);
        if (section.equals(MiniHttp.HISTORY)) widths(155, 105, 430, 132);
        else if (section.equals(MiniHttp.BOOKMARKS)) widths(175, 620);
        else widths(185, 245, 395);
        if (selected != null) {
            for (int i = 0; i < visible.size(); i++) {
                Row row = visible.get(i);
                if (row.uri().equals(selected.uri()) && java.util.Objects.equals(row.file(), selected.file())) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        empty.setText(query.isEmpty() ? switch (section) {
            case MiniHttp.HISTORY -> "Chưa có yêu cầu trong lịch sử.";
            case MiniHttp.BOOKMARKS -> "Chọn biểu tượng ngôi sao trên thanh địa chỉ để lưu dấu trang.";
            default -> "Các tệp bạn tải sẽ xuất hiện ở đây.";
        } : "Không có mục nào khớp với bộ lọc.");
        tableLayout.show(tableView, visible.isEmpty() ? "empty" : "table");
        count.setText(query.isEmpty() ? all.size() + " mục" : visible.size() + " / " + all.size() + " mục");
        updateActions();
    }

    private void widths(int... widths) {
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private Row selection() {
        int index = table.getSelectedRow();
        return index >= 0 && index < model.rows.size() ? model.rows.get(index) : null;
    }

    private void updateActions() {
        Row row = selection();
        mainButton.setEnabled(row != null && row.open());
        secondary.setEnabled(section.equals(MiniHttp.HISTORY) ? !app.profile.history.isEmpty()
                : row != null && (!section.equals(MiniHttp.DOWNLOADS) || row.open()));
    }

    private void openSelection() {
        Row row = selection();
        if (row == null || !row.open()) return;
        if (section.equals(MiniHttp.DOWNLOADS)) openFile(row.file());
        else app.addTab(row.uri());
    }

    private void secondaryAction() {
        Row row = selection();
        switch (section) {
            case MiniHttp.HISTORY -> {
                if (JOptionPane.showConfirmDialog(this, "Xóa toàn bộ lịch sử HTTP?", "Xóa lịch sử",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    app.profile.history.clear();
                    app.save(app.profile::saveHistory);
                    refresh();
                }
            }
            case MiniHttp.BOOKMARKS -> { if (row != null) app.toggleBookmark(row.uri()); }
            default -> { if (row != null) openFile(row.file().getParent()); }
        }
    }

    private void openFile(Path path) {
        try {
            if (!Files.exists(path)) throw new IOException("Đường dẫn không còn tồn tại.");
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Hệ thống không hỗ trợ mở đường dẫn từ ứng dụng.");
            }
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, path + "\n" + error.getMessage(), "Không thể mở", JOptionPane.WARNING_MESSAGE);
        }
    }

    boolean hasActiveDownloads() { return transfers.stream().anyMatch(item -> item.active); }

    boolean isDownloading(Path path) {
        return transfers.stream().anyMatch(item -> item.active && item.file.equals(path));
    }

    Transfer startDownload(URI uri, Path file) {
        var transfer = new Transfer(uri, file);
        transfers.addFirst(transfer);
        return transfer;
    }

    void finishDownload(Transfer transfer) { transfers.remove(transfer); }

    void failDownload(Transfer transfer, String message) {
        transfer.active = false;
        transfer.status = "Lỗi · " + message;
    }

    private static final class Model extends AbstractTableModel {
        String[] columns = new String[0];
        List<Row> rows = List.of();

        void replace(String[] next, List<Row> visible) {
            boolean same = java.util.Arrays.equals(columns, next);
            columns = next;
            rows = visible;
            if (same) fireTableDataChanged();
            else fireTableStructureChanged();
        }

        @Override public int getColumnCount() { return columns.length; }
        @Override public int getRowCount() { return rows.size(); }
        @Override public String getColumnName(int i) { return columns[i]; }
        @Override public Object getValueAt(int row, int col) { return rows.get(row).cells()[col]; }
    }
}

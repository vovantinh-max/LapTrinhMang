import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public final class MiniHttp extends JFrame {
    static final String BROWSER = "Trình duyệt";
    static final String HISTORY = "Lịch sử";
    static final String BOOKMARKS = "Dấu trang";
    static final String DOWNLOADS = "Tệp tải xuống";

    final HttpTransport transport = new HttpTransport();
    final Profile profile;
    final URI demoUri;
    private final DemoServer demoServer;
    final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final CardLayout layout = new CardLayout();
    private final JPanel views = Theme.panel(layout, Theme.CANVAS);
    private final LinkedHashMap<String, JButton> navigation = new LinkedHashMap<>();
    private final LibraryView library;
    private final JLabel notice = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private String section = BROWSER;
    private Path lastDirectory;

    MiniHttp(Profile profile, DemoServer demo) {
        super("MiniHTTP — Trình duyệt thực hành");
        this.profile = profile;
        demoServer = demo;
        demoUri = demo == null ? null : demo.home();
        library = new LibraryView(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1220, 790);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (library.hasActiveDownloads() && JOptionPane.showConfirmDialog(MiniHttp.this,
                        "Tệp đang tải. Bạn có muốn tiếp tục tải và giữ cửa sổ mở?", "Tệp đang tải",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.NO_OPTION) return;
                for (Component component : tabs.getComponents()) {
                    if (component instanceof BrowserTab tab) tab.cancel();
                }
                if (demoServer != null) demoServer.close();
                dispose();
            }
        });
        var root = Theme.panel(new BorderLayout(), Theme.CANVAS);
        root.add(sidebar(), BorderLayout.WEST);
        views.add(browser(), BROWSER);
        views.add(library, "library");
        root.add(views);
        setContentPane(root);
        shortcuts();
        addTab(null);
    }

    private JPanel sidebar() {
        var side = Theme.panel(new BorderLayout(), Theme.NAV);
        side.setPreferredSize(new Dimension(178, 0));
        side.setBorder(Theme.inset(19, 13, 14));
        var brand = Theme.panel(new BorderLayout(9, 0), Theme.NAV);
        brand.setBorder(Theme.inset(0, 2, 22));
        var mark = new Theme.Surface(new GridBagLayout(), new Color(0x2D62D8), null);
        mark.setPreferredSize(new Dimension(35, 35));
        mark.add(Theme.label("M", 19, Font.BOLD, Theme.PAPER));
        brand.add(mark, BorderLayout.WEST);
        brand.add(Theme.label("MiniHTTP", 20, Font.BOLD, Theme.PAPER));
        side.add(brand, BorderLayout.NORTH);

        var links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        nav(links, Theme.Symbol.HOME, BROWSER);
        nav(links, Theme.Symbol.CLOCK, HISTORY);
        nav(links, Theme.Symbol.STAR, BOOKMARKS);
        nav(links, Theme.Symbol.DOWNLOAD, DOWNLOADS);
        side.add(links);

        var state = new Theme.Surface(new BorderLayout(0, 4), new Color(0x203451), null);
        state.setBorder(Theme.inset(9, 10, 10));
        state.add(Theme.label("Máy chủ mẫu", 12, Font.BOLD, Theme.PAPER), BorderLayout.NORTH);
        state.add(Theme.label(demoUri == null ? "○  Ngoại tuyến" : "●  Đang chạy", 12, Font.PLAIN,
                demoUri == null ? new Color(0xFFC2C9) : new Color(0x9BEAC9)));
        if (demoUri != null) state.setToolTipText(demoUri.toString());
        side.add(state, BorderLayout.SOUTH);
        return side;
    }

    private void nav(JPanel items, Theme.Symbol icon, String label) {
        var button = new Theme.Button(label, label, Theme.NAV, Theme.NAV_TEXT,
                new Color(0x305291), () -> showSection(label));
        button.setIcon(icon);
        button.setIconTextGap(9);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        navigation.put(label, button);
        items.add(button);
        items.add(Box.createVerticalStrut(5));
    }

    private JPanel browser() {
        var browser = Theme.panel(new BorderLayout(), Theme.CANVAS);
        var heading = Theme.panel(new BorderLayout(12, 0), Theme.CANVAS);
        heading.setBorder(Theme.inset(9, 14, 9));
        heading.add(Theme.label(BROWSER, 21, Font.BOLD, Theme.INK), BorderLayout.WEST);
        heading.add(notice);
        heading.add(Theme.button("+  Tab mới", "Mở tab mới (Ctrl+T)", () -> addTab(null)), BorderLayout.EAST);
        browser.add(heading, BorderLayout.NORTH);
        browser.add(tabs);
        return browser;
    }

    void showSection(String choice) {
        section = choice;
        navigation.forEach((name, button) -> {
            boolean active = name.equals(choice);
            button.setSelected(active);
            button.setForeground(active ? Theme.PAPER : Theme.NAV_TEXT);
        });
        if (choice.equals(BROWSER)) layout.show(views, BROWSER);
        else {
            library.showSection(choice);
            layout.show(views, "library");
        }
    }

    BrowserTab addTab(URI uri) {
        showSection(BROWSER);
        var tab = new BrowserTab(this);
        tabs.addTab("Tab mới", tab);
        var label = Theme.label("Tab mới", 13, Font.BOLD, Theme.INK);
        var tabControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabControls.setOpaque(false);
        tabControls.add(label);
        var close = Theme.button("×", "Đóng tab", () -> closeTab(tab));
        close.setBorder(Theme.inset(2, 6, 2));
        close.setFocusable(false);
        tabControls.add(close);
        tabs.setTabComponentAt(tabs.indexOfComponent(tab), tabControls);
        tabs.setSelectedComponent(tab);
        if (uri != null) tab.open(uri);
        else tab.focusAddress();
        return tab;
    }

    private void closeTab(BrowserTab tab) {
        tab.cancel();
        tabs.remove(tab);
        if (tabs.getTabCount() == 0) addTab(null);
    }

    void title(BrowserTab tab, String title) {
        int index = tabs.indexOfComponent(tab);
        if (index == -1) return;
        ((JLabel) ((JPanel) tabs.getTabComponentAt(index)).getComponent(0)).setText(title);
        tabs.setTitleAt(index, title);
    }

    private void shortcuts() {
        shortcut("ctrl T", () -> addTab(null));
        shortcut("ctrl W", () -> { if (section.equals(BROWSER)) closeTab(selected()); });
        shortcut("ctrl L", () -> { showSection(BROWSER); selected().focusAddress(); });
        shortcut("ctrl H", () -> showSection(HISTORY));
        shortcut("ctrl J", () -> showSection(DOWNLOADS));
        shortcut("F5", () -> { if (section.equals(BROWSER)) selected().reload(); });
    }

    private void shortcut(String key, Runnable action) {
        var root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        root.getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private BrowserTab selected() { return (BrowserTab) tabs.getSelectedComponent(); }

    void record(String method, URI uri, String state) {
        save(() -> profile.record(method, uri, state));
        library.refresh();
    }

    void toggleBookmark(URI uri) {
        save(() -> profile.toggleBookmark(uri));
        for (int i = 0; i < tabs.getTabCount(); i++) ((BrowserTab) tabs.getComponentAt(i)).updateButtons();
        library.refresh();
    }

    void chooseDownload(URI uri) {
        String path = uri.getPath();
        String name = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
        name = name.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_");
        if (name.isBlank() || name.matches("\\.+")) name = "tai-xuong";
        if (lastDirectory == null) {
            Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
            lastDirectory = Files.isDirectory(downloads) ? downloads : Path.of(System.getProperty("user.home"));
        }
        var chooser = new JFileChooser(lastDirectory.toFile());
        chooser.setDialogTitle("Lưu tệp tải xuống");
        chooser.setSelectedFile(lastDirectory.resolve(name).toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (Files.isDirectory(file)) {
            JOptionPane.showMessageDialog(this, "Hãy chọn tên tệp thay vì thư mục.", "Tải tệp", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (Files.exists(file) && JOptionPane.showConfirmDialog(this, "Thay thế tệp " + file.getFileName() + "?",
                "Tệp đã tồn tại", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (library.isDownloading(file)) {
            JOptionPane.showMessageDialog(this, "Một lượt tải đang dùng đường dẫn này.", "Tải tệp", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lastDirectory = file.getParent();
        download(uri, file);
    }

    void download(URI uri, Path file) {
        var entry = library.startDownload(uri, file);
        showSection(DOWNLOADS);
        new SwingWorker<Long, String>() {
            @Override protected Long doInBackground() throws Exception {
                return transport.download(uri, file, (received, total) -> publish(total > 0
                        ? Math.min(100, Math.round(100.0 * received / total)) + "%  ·  " + size(received) + " / " + size(total)
                        : size(received)));
            }

            @Override protected void process(java.util.List<String> updates) {
                if (entry.active) {
                    entry.status = updates.getLast();
                    library.refresh();
                }
            }

            @Override protected void done() {
                try {
                    long bytes = get();
                    library.finishDownload(entry);
                    save(() -> profile.recordDownload(uri, file, bytes));
                    record("GET · tải tệp", uri, "Đã lưu " + size(bytes));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    library.failDownload(entry, "Đã gián đoạn");
                } catch (ExecutionException error) {
                    String detail = error.getCause().getMessage();
                    library.failDownload(entry, detail == null ? "Không thể tải tệp" : detail);
                    record("GET · tải tệp", uri, "Tải thất bại");
                }
                library.refresh();
            }
        }.execute();
    }

    interface Save { void run() throws IOException; }

    void save(Save action) {
        try {
            action.run();
        } catch (IOException error) {
            notice.setText("Không thể lưu hồ sơ");
            notice.setToolTipText(error.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể lưu hồ sơ.\n" + error.getMessage(),
                    "Dữ liệu ứng dụng", JOptionPane.WARNING_MESSAGE);
        }
    }

    static String size(long bytes) {
        if (bytes < 1_024) return bytes + " B";
        if (bytes < 1_048_576) return String.format(Locale.ROOT, "%.1f KiB", bytes / 1_024.0);
        if (bytes < 1_073_741_824) return String.format(Locale.ROOT, "%.1f MiB", bytes / 1_048_576.0);
        return String.format(Locale.ROOT, "%.1f GiB", bytes / 1_073_741_824.0);
    }

    private static DemoServer startDemo() throws IOException {
        try {
            return new DemoServer(8000);
        } catch (BindException occupied) {
            return new DemoServer(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            DemoServer demo = null;
            MiniHttp ui;
            try {
                String local = System.getenv("LOCALAPPDATA");
                Path directory = local == null ? Path.of(System.getProperty("user.home"), ".minihttp-java")
                        : Path.of(local, "MiniHTTP", "java");
                var profile = new Profile(directory);
                try {
                    demo = startDemo();
                } catch (IOException error) {
                    System.err.println("Máy chủ mẫu: " + error.getMessage());
                }
                ui = new MiniHttp(profile, demo);
            } catch (IOException | RuntimeException error) {
                if (demo != null) demo.close();
                JOptionPane.showMessageDialog(null, error.getMessage(), "Không thể mở MiniHTTP", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ui.setVisible(true);
        });
    }
}

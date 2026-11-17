import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

final class BrowserTab extends JPanel {
    static final String FORM = "application/x-www-form-urlencoded";
    private static final String EMPTY = "home";
    private static final String RESPONSE = "response";
    private final MiniHttp app;
    final JTextField address = new JTextField() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                var g = (Graphics2D) graphics.create();
                g.setColor(Theme.MUTED);
                g.setFont(getFont());
                var font = g.getFontMetrics();
                g.drawString("Nhập địa chỉ HTTP hoặc HTTPS", getInsets().left,
                        (getHeight() - font.getHeight()) / 2 + font.getAscent());
                g.dispose();
            }
        }
    };
    final JComboBox<String> method = new JComboBox<>(new String[]{"GET", "HEAD", "POST"});
    final JComboBox<String> type = new JComboBox<>(new String[]{FORM, "application/json"});
    final JTextArea payload = Theme.textArea();
    private final JButton back = Theme.iconButton(Theme.Symbol.BACK, "", "Quay lại", () -> traverse(-1));
    private final JButton forward = Theme.iconButton(Theme.Symbol.FORWARD, "", "Đi tới", () -> traverse(1));
    private final JButton reload = Theme.iconButton(Theme.Symbol.RELOAD, "", "Tải lại trang", this::reload);
    private final JButton bookmark = Theme.iconButton(Theme.Symbol.STAR, "", "Thêm hoặc bỏ dấu trang", this::bookmark);
    private final JButton download = Theme.iconButton(Theme.Symbol.DOWNLOAD, "Tải tệp", "Tải phản hồi GET về máy", this::download);
    final JButton send = Theme.primary("Gửi yêu cầu", "Gửi yêu cầu HTTP", this::submit);
    private final JPanel composer = Theme.panel(new BorderLayout(0, 7), Theme.PAPER);
    private final CardLayout viewLayout = new CardLayout();
    private final JPanel views = Theme.panel(viewLayout, Theme.CANVAS);
    final JTabbedPane inspection = new JTabbedPane();
    final JEditorPane html = new JEditorPane("text/html", "");
    final JTextArea preview = readOnlyArea();
    final JTextArea headers = readOnlyArea();
    final JTextArea source = readOnlyArea();
    private final CardLayout previewLayout = new CardLayout();
    private final JPanel previewCards = Theme.panel(previewLayout, Theme.PAPER);
    final JLabel status = Theme.label("Sẵn sàng", 13, Font.BOLD, Theme.MUTED);
    private final JLabel metadata = Theme.label("", 12, Font.PLAIN, Theme.MUTED);
    private final JProgressBar working = new JProgressBar();
    private final ArrayList<URI> trail = new ArrayList<>();
    private int position = 0;
    private URI current;
    private HttpTransport.Query lastQuery;
    private SwingWorker<?, ?> pending;

    BrowserTab(MiniHttp app) {
        super(new BorderLayout(0, 0));
        this.app = app;
        setBackground(Theme.PAPER);
        trail.add(null);
        var top = Theme.panel(new BorderLayout(0, 8), Theme.PAPER);
        top.setBorder(Theme.inset(11, 14, 10));
        top.add(toolbar(), BorderLayout.NORTH);
        top.add(composer);
        add(top, BorderLayout.NORTH);
        buildComposer();

        views.add(HomeView.create(app.demoUri, this::open, this::preparePost), EMPTY);
        buildInspector();
        var results = Theme.panel(new BorderLayout(), Theme.PAPER);
        results.add(inspection);
        views.add(results, RESPONSE);
        add(views);
        add(bottomBar(), BorderLayout.SOUTH);
        updateButtons();
    }

    private JPanel toolbar() {
        var toolbar = Theme.panel(new BorderLayout(8, 0), Theme.PAPER);
        var navigation = Theme.row(1, Theme.PAPER);
        navigation.add(back);
        navigation.add(forward);
        navigation.add(reload);
        navigation.add(Theme.iconButton(Theme.Symbol.HOME, "", "Trang bắt đầu", () -> { if (pending == null) home(true); }));
        toolbar.add(navigation, BorderLayout.WEST);

        var input = new Theme.Surface(new BorderLayout(6, 0), Theme.CANVAS, Theme.LINE);
        input.setBorder(Theme.inset(3, 5, 3));
        method.setPreferredSize(new Dimension(82, 32));
        method.setForeground(Theme.BLUE);
        method.setBackground(Theme.PAPER);
        method.addActionListener(event -> composer.setVisible("POST".equals(method.getSelectedItem())));
        input.add(method, BorderLayout.WEST);
        address.setBorder(Theme.inset(3, 5, 3));
        address.setOpaque(false);
        address.setToolTipText("Nhập địa chỉ HTTP hoặc HTTPS");
        address.getAccessibleContext().setAccessibleName("Địa chỉ HTTP");
        address.addActionListener(event -> submit());
        input.add(address);
        input.add(bookmark, BorderLayout.EAST);
        toolbar.add(input);

        var actions = Theme.row(6, Theme.PAPER);
        actions.add(download);
        actions.add(send);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private void buildComposer() {
        composer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE), Theme.inset(9, 10, 9)));
        var options = Theme.row(8, Theme.PAPER);
        options.add(Theme.label("Dữ liệu POST", 13, Font.BOLD, Theme.INK));
        options.add(type);
        var hint = Theme.label("Mỗi trường trên một dòng, dạng tên=giá trị", 12, Font.PLAIN, Theme.MUTED);
        options.add(hint);
        type.addActionListener(event -> hint.setText(FORM.equals(type.getSelectedItem())
                ? "Mỗi trường trên một dòng, dạng tên=giá trị" : "Nhập nội dung JSON nguyên văn"));
        composer.add(options, BorderLayout.NORTH);
        payload.setRows(3);
        payload.setBackground(Theme.CANVAS);
        payload.setBorder(Theme.inset(6, 8, 6));
        composer.add(Theme.scroll(payload));
        composer.setVisible(false);
    }

    private void buildInspector() {
        html.setEditable(false);
        html.setBackground(Theme.PAPER);
        html.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    open(HttpTransport.address(event.getDescription()));
                } catch (IllegalArgumentException error) {
                    message("Liên kết không hợp lệ", Theme.RED);
                }
            }
        });
        previewCards.add(Theme.scroll(html), "html");
        previewCards.add(Theme.scroll(preview), "text");
        inspection.addTab("Nội dung", previewCards);
        inspection.addTab("Tiêu đề HTTP", Theme.scroll(headers));
        inspection.addTab("Dữ liệu gốc", Theme.scroll(source));
    }

    private JPanel bottomBar() {
        var bar = Theme.panel(new BorderLayout(12, 0), Theme.PAPER);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                Theme.inset(8, 16, 8)));
        working.setIndeterminate(true);
        working.setPreferredSize(new Dimension(78, 5));
        working.setVisible(false);
        bar.add(status, BorderLayout.WEST);
        bar.add(metadata);
        bar.add(working, BorderLayout.EAST);
        return bar;
    }

    private static JTextArea readOnlyArea() {
        var area = Theme.textArea();
        area.setEditable(false);
        return area;
    }

    void focusAddress() {
        address.requestFocusInWindow();
        address.selectAll();
    }

    void open(URI uri) {
        if (pending != null) return;
        method.setSelectedItem("GET");
        address.setText(uri.toString());
        start(HttpTransport.Query.get(uri), true);
    }

    private void preparePost() {
        if (app.demoUri == null) return;
        address.setText(app.demoUri.resolve("/api/echo").toString());
        method.setSelectedItem("POST");
        type.setSelectedItem("application/json");
        payload.setText("{\n  \"mon\": \"Lập trình mạng\",\n  \"nhom\": 4\n}");
        payload.requestFocusInWindow();
    }

    private void submit() {
        if (pending != null) return;
        try {
            URI uri = HttpTransport.address(address.getText());
            String verb = (String) method.getSelectedItem();
            String contentType = (String) type.getSelectedItem();
            String body = "";
            if (verb.equals("POST")) {
                body = FORM.equals(contentType) ? HttpTransport.formEncode(payload.getText()) : payload.getText();
            }
            start(new HttpTransport.Query(uri, verb, contentType, body), true);
        } catch (IllegalArgumentException error) {
            message(error.getMessage(), Theme.RED);
        }
    }

    private void start(HttpTransport.Query query, boolean appendTrail) {
        if (appendTrail) {
            trail.subList(position + 1, trail.size()).clear();
            trail.add(query.uri());
            position = trail.size() - 1;
        }
        current = query.uri();
        lastQuery = query;
        address.setText(current.toString());
        viewLayout.show(views, RESPONSE);
        inspection.setSelectedIndex(0);
        setPreview("Đang chờ phản hồi từ máy chủ…");
        setText(headers, "");
        setText(source, "");
        message("Đang gửi " + query.method() + "…", Theme.BLUE);
        metadata.setText(current.getHost());
        working.setVisible(true);
        app.title(this, current.getHost());
        pending = new SwingWorker<HttpTransport.Result, Void>() {
            @Override protected HttpTransport.Result doInBackground() throws Exception {
                return app.transport.fetch(query);
            }

            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    showResult(query, get());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    showFailure(query, error.getCause());
                } finally {
                    pending = null;
                    working.setVisible(false);
                    updateButtons();
                }
            }
        };
        updateButtons();
        pending.execute();
    }

    private void showResult(HttpTransport.Query query, HttpTransport.Result result) {
        current = result.uri();
        trail.set(position, current);
        if (address.getText().equals(query.uri().toString())) address.setText(current.toString());
        String code = result.version() + "  " + result.status();
        message(code, result.status() >= 400 ? Theme.RED : Theme.GREEN);
        metadata.setText(result.elapsedMillis() + " ms   ·   " + result.mediaType()
                + result.headers().firstValueAsLong("Content-Length").stream()
                .mapToObj(bytes -> "   ·   " + MiniHttp.size(bytes)).findFirst().orElse(""));
        setText(headers, result.headerText());
        String body = result.body() + (result.truncated() ? "\n\n[Bản xem trước giới hạn ở 512 KiB]" : "");
        setText(source, body);
        if (query.method().equals("HEAD")) {
            setPreview("HEAD chỉ trả về tiêu đề phản hồi.\n\nChuyển sang thẻ Tiêu đề HTTP để xem chi tiết.");
            inspection.setSelectedIndex(1);
        } else if (result.mediaType().equals("text/html")) {
            try {
                html.setText(HtmlPreview.render(result.body(), result.uri()));
                html.setCaretPosition(0);
                previewLayout.show(previewCards, "html");
            } catch (java.io.IOException error) {
                setPreview(body);
            }
        } else if (HttpTransport.isText(result.mediaType())) {
            setPreview(body);
        } else {
            setPreview("Tài nguyên  ·  " + result.mediaType()
                    + "\n\nDùng nút Tải tệp để lưu phản hồi GET và mở bằng ứng dụng trên máy.");
        }
        app.title(this, tabTitle(current));
        app.record(query.method(), query.uri(), Integer.toString(result.status()));
    }

    private static String tabTitle(URI uri) {
        String path = uri.getPath();
        if (path == null || path.equals("/") || path.isEmpty()) return uri.getHost();
        String clean = path.replaceAll("/+$", "");
        String name = clean.substring(clean.lastIndexOf('/') + 1);
        return name.length() > 24 ? name.substring(0, 23) + "…" : name;
    }

    private void showFailure(HttpTransport.Query query, Throwable error) {
        String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        message("Không nhận được phản hồi", Theme.RED);
        setPreview(query.uri() + "\n\n" + detail);
        app.record(query.method(), query.uri(), "Lỗi kết nối");
    }

    private void home(boolean append) {
        if (append && current != null) {
            trail.subList(position + 1, trail.size()).clear();
            trail.add(null);
            position = trail.size() - 1;
        }
        current = null;
        lastQuery = null;
        address.setText("");
        method.setSelectedItem("GET");
        message("Sẵn sàng", Theme.MUTED);
        metadata.setText("");
        app.title(this, "Tab mới");
        viewLayout.show(views, EMPTY);
        updateButtons();
    }

    private void traverse(int offset) {
        if (pending != null) return;
        int next = position + offset;
        if (next < 0 || next >= trail.size()) return;
        position = next;
        URI target = trail.get(position);
        if (target == null) home(false);
        else {
            method.setSelectedItem("GET");
            start(HttpTransport.Query.get(target), false);
        }
    }

    void reload() {
        if (pending != null || lastQuery == null) return;
        if (lastQuery.method().equals("POST") && JOptionPane.showConfirmDialog(this,
                "Gửi lại dữ liệu POST đã nhập?", "Tải lại yêu cầu", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        start(lastQuery, false);
    }

    private void bookmark() {
        try {
            URI target = HttpTransport.address(address.getText());
            app.toggleBookmark(target);
            message(app.profile.bookmarks.contains(target) ? "Đã thêm dấu trang" : "Đã bỏ dấu trang", Theme.BLUE);
            updateButtons();
        } catch (IllegalArgumentException error) {
            message(error.getMessage(), Theme.RED);
        }
    }

    private void download() {
        try {
            app.chooseDownload(HttpTransport.address(address.getText()));
        } catch (IllegalArgumentException error) {
            message(error.getMessage(), Theme.RED);
        }
    }

    void updateButtons() {
        boolean idle = pending == null;
        back.setEnabled(idle && position > 0);
        forward.setEnabled(idle && position < trail.size() - 1);
        reload.setEnabled(idle && lastQuery != null);
        send.setEnabled(idle);
        bookmark.setIcon(current != null && app.profile.bookmarks.contains(current) ? Theme.Symbol.STAR_FILLED : Theme.Symbol.STAR);
    }

    boolean isBusy() { return pending != null; }

    void cancel() {
        if (pending != null) pending.cancel(true);
    }

    void message(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }

    private void setPreview(String text) {
        setText(preview, text);
        previewLayout.show(previewCards, "text");
    }

    private static void setText(JTextArea area, String text) {
        area.setText(text);
        area.setCaretPosition(0);
    }
}

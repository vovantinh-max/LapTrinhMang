import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.function.Consumer;

final class HomeView {
    private record Resource(String name, String extension, String description, String path, Color color) {}
    private static final Resource[] RESOURCES = {
            new Resource("Trang máy chủ", "HTML", "Mở trang mẫu và thử các liên kết.", "/", Theme.BLUE),
            new Resource("Thông tin máy chủ", "JSON", "Xem phương thức và tài nguyên qua API.", "/api/info", Theme.GREEN),
            new Resource("Tài liệu hướng dẫn", "PDF", "Kiểm tra tiêu đề HTTP và tải tài liệu.", "/files/huong-dan.pdf", Theme.RED),
            new Resource("Âm thanh mẫu", "WAV", "Tải tệp âm thanh tạo bởi máy chủ.", "/files/am-mau.wav", new Color(0x673BA6))
    };

    static JComponent create(URI demo, Consumer<URI> open, Runnable post) {
        var content = Theme.panel(new BorderLayout(0, 22), Theme.CANVAS);
        content.setBorder(Theme.inset(25, 28, 25));
        content.add(new Hero(demo, open, post), BorderLayout.NORTH);

        var resources = Theme.panel(new BorderLayout(0, 12), Theme.CANVAS);
        resources.add(Theme.label("Tài nguyên thực hành", 20, Font.BOLD, Theme.INK), BorderLayout.NORTH);
        var grid = Theme.panel(new GridLayout(2, 2, 12, 12), Theme.CANVAS);
        for (Resource resource : RESOURCES) grid.add(tile(resource, demo, open));
        resources.add(grid);
        var middle = Theme.panel(new BorderLayout(), Theme.CANVAS);
        middle.add(resources, BorderLayout.NORTH);
        content.add(middle);
        var scroll = Theme.scroll(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private static JComponent tile(Resource resource, URI demo, Consumer<URI> open) {
        var card = new Theme.Surface(new BorderLayout(10, 0), Theme.PAPER, Theme.LINE);
        card.setBorder(Theme.inset(15, 16, 15));
        var icon = new Theme.Surface(new GridBagLayout(), Theme.PALE, null);
        icon.setPreferredSize(new Dimension(49, 46));
        icon.add(Theme.label(resource.extension(), 12, Font.BOLD, resource.color()));
        card.add(icon, BorderLayout.WEST);
        var text = Theme.panel(new GridLayout(2, 1, 0, 4), Theme.PAPER);
        text.add(Theme.label(resource.name(), 15, Font.BOLD, Theme.INK));
        text.add(Theme.label(resource.description(), 12, Font.PLAIN, Theme.MUTED));
        card.add(text);
        var button = Theme.button("→", "Mở " + resource.name().toLowerCase(), () -> open.accept(demo.resolve(resource.path())));
        button.setEnabled(demo != null);
        card.add(button, BorderLayout.EAST);
        return card;
    }

    private static final class Hero extends JPanel {
        Hero(URI demo, Consumer<URI> open, Runnable post) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(Theme.inset(25, 26, 23));
            var title = new JPanel();
            title.setOpaque(false);
            title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
            title.add(Theme.label("Phòng thực hành HTTP", 34, Font.BOLD, Color.WHITE));
            title.add(Box.createVerticalStrut(7));
            title.add(Theme.label("Duyệt trang, gửi yêu cầu và đọc phản hồi HTTP.", 16, Font.PLAIN, new Color(0xDAE7FF)));
            title.add(Box.createVerticalStrut(19));
            var controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            controls.setOpaque(false);
            controls.setAlignmentX(LEFT_ALIGNMENT);
            var openButton = Theme.button("Mở trang máy chủ  →", null, () -> open.accept(demo));
            var postButton = new Theme.Button("Thử POST", null, new Color(0x234D9D), Color.WHITE,
                    new Color(0x2D5AB1), post);
            openButton.setEnabled(demo != null);
            postButton.setEnabled(demo != null);
            controls.add(openButton);
            controls.add(postButton);
            title.add(controls);
            add(title);
            var connection = Theme.label(demo == null ? "Máy chủ mẫu chưa khởi động" : demo.toString(),
                    12, Font.PLAIN, new Color(0xDAE7FF));
            connection.setBorder(Theme.inset(14, 0, 0));
            add(connection, BorderLayout.SOUTH);
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var shape = new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25);
            g.setPaint(new GradientPaint(0, 0, new Color(0x102951), getWidth(), getHeight(), new Color(0x285CCB)));
            g.fill(shape);
            g.clip(shape);
            int x = getWidth() - 125;
            int y = getHeight() / 2;
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(0x39FFFFFF, true));
            for (int radius : new int[]{33, 61, 89, 117, 146}) {
                g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            }
            g.drawLine(x - 150, y, x + 150, y);
            g.drawLine(x, y - 130, x, y + 130);
            g.setColor(new Color(0x9FC6FF));
            g.fillOval(x - 4, y - 4, 8, 8);
            g.fillOval(x + 55, y - 5, 10, 10);
            g.fillOval(x - 65, y - 67, 7, 7);
            g.dispose();
        }
    }
}

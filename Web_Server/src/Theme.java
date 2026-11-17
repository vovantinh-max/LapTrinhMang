import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

final class Theme {
    static final Color CANVAS = new Color(0xF2F5FA);
    static final Color PAPER = Color.WHITE;
    static final Color INK = new Color(0x15233D);
    static final Color MUTED = new Color(0x4C607B);
    static final Color LINE = new Color(0xDAE3EF);
    static final Color BLUE = new Color(0x2058CA);
    static final Color PALE = new Color(0xEAF1FF);
    static final Color NAV = new Color(0x12223B);
    static final Color NAV_TEXT = new Color(0xC8D7EB);
    static final Color GREEN = new Color(0x14634D);
    static final Color RED = new Color(0x9B2937);
    static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private Theme() {}

    static void install() {
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.font", FONT);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.font", FONT);
        UIManager.put("ComboBox.font", FONT);
        UIManager.put("TabbedPane.font", FONT.deriveFont(Font.BOLD, 13f));
        UIManager.put("TabbedPane.background", CANVAS);
        UIManager.put("TabbedPane.selected", PAPER);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("Table.font", FONT);
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.background", PAPER);
        UIManager.put("Table.selectionBackground", PALE);
        UIManager.put("Table.selectionForeground", BLUE);
        UIManager.put("TableHeader.font", FONT.deriveFont(Font.BOLD, 13f));
        UIManager.put("TableHeader.background", CANVAS);
        UIManager.put("TableHeader.foreground", MUTED);
    }

    static JLabel label(String text, float size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(FONT.deriveFont(style, size));
        label.setForeground(color);
        return label;
    }

    static JPanel panel(LayoutManager layout, Color color) {
        var panel = new JPanel(layout);
        panel.setBackground(color);
        return panel;
    }

    static JPanel row(int gap, Color color) {
        return panel(new FlowLayout(FlowLayout.LEFT, gap, 0), color);
    }

    static EmptyBorder inset(int top, int sides, int bottom) {
        return new EmptyBorder(top, sides, bottom, sides);
    }

    static JScrollPane scroll(JComponent component) {
        var scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(component.getBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    static JTextArea textArea() {
        var area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setBackground(PAPER);
        area.setForeground(INK);
        area.setCaretColor(BLUE);
        area.setSelectionColor(PALE);
        area.setBorder(inset(15, 20, 15));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    static JButton button(String text, String tip, Runnable action) {
        return new Button(text, tip, PAPER, INK, PALE, action);
    }

    static JButton primary(String text, String tip, Runnable action) {
        return new Button(text, tip, BLUE, PAPER, new Color(0x1748AA), action);
    }

    static final class Button extends JButton {
        private final Color normal;
        private final Color hover;

        Button(String text, String tip, Color normal, Color foreground, Color hover, Runnable action) {
            super(text);
            this.normal = normal;
            this.hover = hover;
            setForeground(foreground);
            setFont(FONT.deriveFont(Font.BOLD, 13f));
            setBorder(inset(8, 12, 8));
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(tip);
            getAccessibleContext().setAccessibleName(tip == null ? text : tip);
            addActionListener(event -> action.run());
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = (isSelected() || getModel().isRollover() || getModel().isPressed()) ? hover : normal;
            if (!isEnabled() && normal.equals(BLUE)) fill = new Color(0xDEE7F5);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (isFocusOwner()) {
                g.setColor(new Color(0x75A6FF));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 9, 9);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    enum Symbol implements Icon {
        HOME, CLOCK, STAR, STAR_FILLED, DOWNLOAD, BACK, FORWARD, RELOAD;

        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            var g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(component.isEnabled() ? component.getForeground() : new Color(0x97A7BE));
            switch (this) {
                case HOME -> {
                    lines(g, 2, 7, 8, 2, 14, 7);
                    lines(g, 4, 6, 4, 14, 7, 14, 7, 10, 9, 10, 9, 14, 12, 14, 12, 6);
                }
                case CLOCK -> {
                    g.drawOval(2, 2, 12, 12);
                    lines(g, 8, 5, 8, 8, 10, 9);
                }
                case STAR, STAR_FILLED -> {
                    var shape = new Path2D.Double();
                    for (int i = 0; i < 10; i++) {
                        double a = i * Math.PI / 5 - Math.PI / 2;
                        double radius = i % 2 == 0 ? 7 : 3.3;
                        double px = 8 + radius * Math.cos(a), py = 8 + radius * Math.sin(a);
                        if (i == 0) shape.moveTo(px, py); else shape.lineTo(px, py);
                    }
                    shape.closePath();
                    if (this == STAR_FILLED) g.fill(shape); else g.draw(shape);
                }
                case DOWNLOAD -> {
                    lines(g, 8, 2, 8, 10);
                    lines(g, 5, 7, 8, 10, 11, 7);
                    lines(g, 3, 11, 3, 14, 13, 14, 13, 11);
                }
                case BACK, FORWARD -> {
                    int point = this == BACK ? 3 : 13;
                    int inner = this == BACK ? 7 : 9;
                    lines(g, 3, 8, 13, 8);
                    lines(g, inner, 4, point, 8, inner, 12);
                }
                case RELOAD -> {
                    g.drawArc(2, 2, 12, 12, 46, 280);
                    lines(g, 13, 2, 13, 6, 9, 6);
                }
            }
            g.dispose();
        }

        private static void lines(Graphics2D g, int... coordinates) {
            var path = new Path2D.Double();
            path.moveTo(coordinates[0], coordinates[1]);
            for (int i = 2; i < coordinates.length; i += 2) path.lineTo(coordinates[i], coordinates[i + 1]);
            g.draw(path);
        }
    }

    static JButton iconButton(Symbol icon, String text, String description, Runnable action) {
        JButton button = button(text, description, action);
        button.setIcon(icon);
        button.setDisabledIcon(icon);
        if (text.isEmpty()) button.setBorder(inset(8, 8, 8));
        return button;
    }

    static class Surface extends JPanel {
        private final Color fill;
        private final Color border;

        Surface(LayoutManager layout, Color fill, Color border) {
            super(layout);
            this.fill = fill;
            this.border = border;
            setOpaque(false);
            setBackground(fill);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            var g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 20, 20);
            g.setColor(fill);
            g.fill(shape);
            if (border != null) {
                g.setColor(border);
                g.draw(shape);
            }
            g.dispose();
        }
    }
}

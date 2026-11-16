package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class UITheme {

    public static void install() {
        try { if (!(UIManager.getLookAndFeel() instanceof javax.swing.plaf.metal.MetalLookAndFeel)) UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel()); }
        catch (UnsupportedLookAndFeelException e) { throw new IllegalStateException(e); }
        for (String k : new String[]{"Panel", "OptionPane", "ScrollPane", "Viewport", "TabbedPane"})
            UIManager.put(k + ".background", SURFACE_1);
        for (String k : new String[]{"Label", "Button", "CheckBox", "RadioButton", "ComboBox", "TextField", "PasswordField", "TextArea", "TextPane", "EditorPane", "List", "Table", "TableHeader", "TabbedPane", "Menu", "MenuItem", "ToolTip"}) {
            UIManager.put(k + ".font", FONT_REGULAR);
            UIManager.put(k + ".foreground", TXT_PRIMARY);
            if (!k.equals("Label")) UIManager.put(k + ".background", SURFACE_2);
            UIManager.put(k + ".selectionBackground", new Color(55,75,100));
            UIManager.put(k + ".selectionForeground", TXT_PRIMARY);
            UIManager.put(k + ".caretForeground", PRIMARY);
        }
        UIManager.put("OptionPane.messageForeground", TXT_PRIMARY);
        UIManager.put("TabbedPane.selected", SURFACE_3);
        UIManager.put("TabbedPane.contentAreaColor", SURFACE_1);
        UIManager.put("SplitPane.background", BG_DARK);
        UIManager.put("control", SURFACE_2);
        UIManager.put("Panel.foreground", TXT_PRIMARY);
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());
        UIManager.put("RadioButton.icon", new ChoiceIcon(true));
        UIManager.put("CheckBox.icon", new ChoiceIcon(false));
        UIManager.put("TabbedPaneUI", FlatTabsUI.class.getName());
        UIManager.put("TabbedPane.focus", SURFACE_3);
        UIManager.put("TabbedPane.highlight", SURFACE_1);
        UIManager.put("TabbedPane.light", SURFACE_1);
        UIManager.put("TabbedPane.shadow", BORDER);
        UIManager.put("TabbedPane.darkShadow", BORDER);
        UIManager.put("Button.disabledText", TXT_MUTED);
        UIManager.put("TextField.inactiveForeground", TXT_MUTED);
        UIManager.put("TextField.inactiveBackground", SURFACE_1);
        UIManager.put("PasswordField.inactiveForeground", TXT_MUTED);
        UIManager.put("ComboBox.disabledForeground", TXT_MUTED);
        UIManager.put("CheckBox.disabledText", TXT_MUTED);
        UIManager.put("ProgressBar.foreground", PRIMARY_HOVER);
        UIManager.put("ProgressBar.background", SURFACE_2);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(7,10,7,10)));
        UIManager.put("PasswordField.border", UIManager.get("TextField.border"));
    }

    public static Color buttonFill(Color color) {
        if (color == null) return SURFACE_2;
        if (color.equals(COLOR_GREEN)) return new Color(51, 91, 76);
        if (color.equals(COLOR_RED)) return new Color(112, 59, 67);
        if (color.equals(COLOR_AMBER)) return new Color(100, 79, 46);
        if (color.equals(PRIMARY)) return new Color(65, 89, 123);
        if (color.equals(ACCENT_TEAL) || color.equals(COLOR_CYAN)) return new Color(53, 86, 98);
        if (color.equals(COLOR_VIOLET)) return new Color(76, 68, 105);
        return color;
    }
    public static class ThemedLabel extends JLabel {
        public ThemedLabel() { super(); }
        public ThemedLabel(String text) { super(text); }
        public ThemedLabel(String text, int alignment) { super(text, alignment); }
        public ThemedLabel(Icon icon) { super(icon); }
        public ThemedLabel(Icon icon, int alignment) { super(icon, alignment); }
        public ThemedLabel(String text, Icon icon, int alignment) { super(text, icon, alignment); }
        @Override public void setText(String text) {
            super.setText(LabIcons.clean(text));
            if (LabIcons.hasSymbol(text)) { setIcon(LabIcons.icon(LabIcons.nameFor(text),16)); setIconTextGap(7); }
        }
    }
    public static class Tabs extends JTabbedPane {
        @Override public void insertTab(String title, Icon icon, Component component, String tip, int index) {
            super.insertTab(LabIcons.clean(title), icon != null ? icon : LabIcons.icon(LabIcons.nameFor(title),16), component, tip, index);
        }
    }
    public static class FlatTabsUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent c) { return new FlatTabsUI(); }
        @Override protected void installDefaults() {
            super.installDefaults(); tabInsets = new Insets(10,14,10,14);
            selectedTabPadInsets = new Insets(0,0,0,0); tabAreaInsets = new Insets(0,0,8,0);
            contentBorderInsets = new Insets(0,0,0,0);
        }
        @Override protected void paintTabBackground(Graphics g,int p,int i,int x,int y,int w,int h,boolean selected) {
            Graphics2D p2=(Graphics2D)g.create();
            p2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p2.setColor(selected?SURFACE_3:BG_DARK); p2.fillRoundRect(x+2,y,w-4,h,8,8);
            if(selected){p2.setColor(PRIMARY);p2.fillRoundRect(x+14,y+h-3,w-28,2,2,2);} p2.dispose();
        }
        @Override protected void paintTabBorder(Graphics g,int p,int i,int x,int y,int w,int h,boolean selected) { }
        @Override protected void paintContentBorder(Graphics g,int p,int selected) { }
        @Override protected void paintFocusIndicator(Graphics g,int p,Rectangle[] r,int i,Rectangle a,Rectangle b,boolean s) { }
    }


    public static void fitWindow(JFrame frame) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        int width = Math.max(640, screen.width-insets.left-insets.right-24);
        int height = Math.max(480, screen.height-insets.top-insets.bottom-24);
        frame.setMinimumSize(new Dimension(Math.min(frame.getMinimumSize().width,width),Math.min(frame.getMinimumSize().height,height)));
        frame.setSize(Math.min(frame.getWidth(),width),Math.min(frame.getHeight(),height));
        frame.setLocationRelativeTo(null);
    }
    public static class ChoiceIcon implements Icon {
        private final boolean radio;
        public ChoiceIcon(boolean radio) { this.radio=radio; }
        public int getIconWidth(){return 18;}
        public int getIconHeight(){return 18;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.6f));g.setColor(SURFACE_2);
            if(radio)g.fillOval(2,2,13,13);else g.fillRoundRect(2,2,13,13,3,3);
            g.setColor(TXT_MUTED);
            if(radio)g.drawOval(2,2,13,13);else g.drawRoundRect(2,2,13,13,3,3);
            if(c instanceof AbstractButton && ((AbstractButton)c).isSelected()){
                g.setColor(PRIMARY);
                if(radio)g.fillOval(6,6,5,5);
                else {g.drawLine(5,8,8,11);g.drawLine(8,11,13,5);}
            }g.dispose();
        }
    }

    public static int parsePort(String text) {
        int port = Integer.parseInt(text.trim());
        if (port < 1 || port > 65535) throw new NumberFormatException("Port must be 1..65535");
        return port;
    }


    // ── Design Tokens đồng bộ với TCP Socket Chat ─────────────────────────────
    public static final Color BG_DARK        = new Color(23, 29, 39);
    public static final Color BG_PANEL       = new Color(27, 34, 45);
    public static final Color SURFACE_1      = new Color(31, 39, 51);
    public static final Color SURFACE_2      = new Color(38, 48, 63);
    public static final Color SURFACE_3      = new Color(47, 59, 76);
    public static final Color BORDER         = new Color(55, 67, 84);
    public static final Color BORDER_LIGHT   = new Color(70, 84, 104);
    public static final Color BORDER_FOCUS   = new Color(99, 102, 241, 180);

    public static final Color PRIMARY        = new Color(119, 151, 194);   // Indigo-500
    public static final Color PRIMARY_HOVER  = new Color(92, 124, 167);
    public static final Color PRIMARY_LIGHT  = new Color(42, 56, 76);

    public static final Color ACCENT_TEAL    = new Color(121, 173, 167);   // Teal-400
    public static final Color COLOR_GREEN    = new Color(125, 177, 151);    // Green-500
    public static final Color COLOR_RED      = new Color(207, 141, 147);    // Red-500
    public static final Color COLOR_AMBER    = new Color(196, 170, 121);   // Amber-500
    public static final Color COLOR_VIOLET   = new Color(160, 151, 194);   // Violet-500
    public static final Color COLOR_CYAN     = new Color(124, 168, 188);    // Cyan-500

    public static final Color TXT_PRIMARY    = new Color(218, 226, 236);
    public static final Color TXT_SECONDARY  = new Color(172, 186, 204);
    public static final Color TXT_MUTED      = new Color(145, 160, 181);

    // ── Typography ────────────────────────────────────────────────────────────
    public static final Font FONT_DISPLAY    = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADER     = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BOLD       = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TINY       = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO       = new Font("Consolas", Font.PLAIN, 12);

    // ── Modern Button Component ───────────────────────────────────────────────
    public static class ModernButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private final Color textColor;
        private boolean isHover = false;
        private boolean isPressed = false;
        private int cornerRadius = 8;

        public ModernButton(String text, Color normalBg, Color hoverBg, Color textColor) {
            super(text);
            this.normalBg = buttonFill(normalBg);
            this.hoverBg = buttonFill(normalBg).brighter();
            this.textColor = textColor;

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(textColor);
            setBackground(normalBg);
            setIcon(LabIcons.icon(LabIcons.nameFor(text),16));
            setIconTextGap(8);
            setFont(FONT_BOLD);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) { isHover = true; repaint(); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    isHover = false; isPressed = false; repaint();
                }
                @Override public void mousePressed(MouseEvent e) {
                    if (isEnabled()) { isPressed = true; repaint(); }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    isPressed = false; repaint();
                }
            });
        }

        @Override public void setText(String text) {
            super.setText(LabIcons.clean(text));
            setIcon(LabIcons.icon(LabIcons.nameFor(text),16));
        }
        @Override public void setBackground(Color color) { super.setBackground(buttonFill(color)); }
        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bg;
            if (!isEnabled()) {
                bg = SURFACE_2;
            } else if (isPressed) {
                bg = getBackground().darker();
            } else if (isHover) {
                bg = getBackground().equals(normalBg) ? hoverBg : getBackground().brighter();
            } else {
                bg = getBackground();
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

            // Soft inner border for dark buttons
            if (normalBg.equals(SURFACE_2) || normalBg.equals(SURFACE_1)) {
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            }

            g2.setFont(getFont());
            g2.setColor(isEnabled() ? getForeground() : TXT_MUTED);
            Rectangle view = new Rectangle(10,0,Math.max(0,getWidth()-20),getHeight());
            Rectangle iconRect = new Rectangle(), textRect = new Rectangle();
            String caption = SwingUtilities.layoutCompoundLabel(this,g2.getFontMetrics(),getText(),getIcon(),
                CENTER,CENTER,CENTER,RIGHT,view,iconRect,textRect,getIconTextGap());
            if (getIcon()!=null) getIcon().paintIcon(this,g2,iconRect.x,iconRect.y);
            g2.drawString(caption,textRect.x,textRect.y+g2.getFontMetrics().getAscent());

            g2.dispose();
        }
    }

    // ── Modern Dark TextField Component ───────────────────────────────────────
    public static class ModernTextField extends JTextField {
        private boolean isFocused = false;

        public ModernTextField(String text, int columns) {
            super(text, columns);
            initField();
        }

        public ModernTextField(int columns) {
            super(columns);
            initField();
        }

        private void initField() {
            setOpaque(false);
            setFont(FONT_REGULAR);
            setForeground(TXT_PRIMARY);
            setCaretColor(PRIMARY);
            setBorder(new EmptyBorder(7, 12, 7, 12));

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    isFocused = true; repaint();
                }
                @Override public void focusLost(FocusEvent e) {
                    isFocused = false; repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(SURFACE_2);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

            g2.setColor(isFocused ? PRIMARY : BORDER);
            g2.setStroke(new BasicStroke(isFocused ? 1.5f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Dark ScrollBar UI ─────────────────────────────────────────────────────
    public static class DarkScrollBarUI extends BasicScrollBarUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent c) { return new DarkScrollBarUI(); }
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = BORDER_LIGHT;
            this.trackColor = BG_PANEL;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // ── Helper UI Builders ────────────────────────────────────────────────────
    public static JPanel buildStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE_1);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                // Left accent bar
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(0, 6, 3, getHeight() - 12, 3, 3));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(8, 16, 8, 16));
        card.setPreferredSize(new Dimension(145, 46));

        JLabel lbl = new ThemedLabel(label);
        lbl.setFont(FONT_TINY);
        lbl.setForeground(TXT_MUTED);
        card.add(lbl, BorderLayout.NORTH);

        valueLabel.setFont(FONT_HEADER);
        valueLabel.setForeground(accent);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public static JPanel buildChip(String text, Color fg, Color bg) {
        JPanel chip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 80));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(5, 12, 5, 12));
        JLabel l = new ThemedLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(fg);
        chip.add(l);
        return chip;
    }

    public static void styleTable(JTable table) {
        table.setBackground(SURFACE_1);
        table.setForeground(TXT_PRIMARY);
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setGridColor(BORDER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(55,75,100));
        table.setSelectionForeground(TXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                super.getTableCellRendererComponent(t,v,s,f,row,col);
                setBackground(SURFACE_2); setForeground(TXT_SECONDARY); setFont(FONT_BOLD);
                setBorder(new EmptyBorder(8,10,8,10)); return this;
            }
        });
        header.setBackground(SURFACE_2);
        header.setForeground(TXT_SECONDARY);
        header.setFont(FONT_BOLD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(SURFACE_1);
        centerRenderer.setForeground(TXT_PRIMARY);
    }
}

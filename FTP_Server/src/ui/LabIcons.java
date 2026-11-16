package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.Normalizer;

/** Resolution-independent monochrome icons: no emoji/font dependency. */
public final class LabIcons {
    private LabIcons() { }
    public static boolean hasSymbol(String text) {
        return text != null && text.codePoints().anyMatch(c -> c >= 0x1f000 || c >= 0x2190 && c <= 0x2bff || c == 0xfe0f);
    }
    public static String clean(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(c -> !(c >= 0x1f000 || c >= 0x2190 && c <= 0x2bff || c == 0xfe0f || c == 0x200d))
                .forEach(out::appendCodePoint);
        return out.toString().strip();
    }
    public static String nameFor(String text) {
        if (text == null) return "server";
        String s = Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        if (s.equals("len") || s.contains("upload")) return "up";
        if (s.contains("chon") || s.contains("browse")) return "folder";
        if (s.contains("online") || s.contains("running") || s.contains("stopped")) return "dot";
        if (s.contains("xoa") || s.contains("delete")) return "trash";
        if (s.matches(".*\\btim\\b.*") || s.contains("inspector") || s.contains("search")) return "search";
        if (s.contains("lam moi") || s.contains("tai lai") || s.contains("sync") || s.contains("chuyen server") || s.contains("🔄")) return "refresh";
        if (s.contains("them") || s.contains("mau") || s.contains("➕")) return "plus";
        if (s.contains("sua") || s.contains("cap nhat") || s.contains("✏")) return "edit";
        if (s.contains("cau hinh") || s.contains("⚙")) return "settings";
        if (s.contains("nguoi dung") || s.contains("tai khoan") || s.contains("khach") || s.contains("👥")) return "users";
        if (s.contains("nhat ky") || s.contains("console") || s.contains("log") || s.contains("lich su")) return "list";
        if (s.contains("thu muc") || s.contains("storage") || s.contains("tep") || s.contains("📁")) return "folder";
        if (s.contains("gui") || s.contains("🚀")) return "send";
        if (s.contains("thu") || s.contains("email") || s.contains("mail") || s.contains("✉")) return "mail";
        if (s.contains("dung") || s.contains("ngat") || s.contains("stop")) return "stop";
        if (s.contains("khoi dong") || s.contains("bat ")) return "play";
        if (s.contains("web") || s.contains("🌐")) return "globe";
        if (s.contains("pdf") || s.contains("📄")) return "file";
        if (s.contains("mp4") || s.contains("video")) return "play";
        if (s.contains("mp3") || s.contains("audio")) return "audio";
        if (s.contains("thoi gian") || s.contains("uptime")) return "clock";
        if (s.contains("download") || s.contains("tai xuong")) return "download";
        if (s.contains("➔") || s.contains("toi")) return "right";
        if (s.contains("←")) return "left";
        if (s.contains("⌂") || s.contains("🏠")) return "home";
        return "server";
    }
    public static Icon icon(String name, int size) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x,y); g.scale(size / 24.0, size / 24.0);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(c.isEnabled() ? c.getForeground() : UITheme.TXT_MUTED);
                g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (name) {
                    case "folder" -> { g.draw(new Path2D.Double() {{ moveTo(3,7); lineTo(3,4); lineTo(10,4); lineTo(13,7); lineTo(21,7); lineTo(21,20); lineTo(3,20); closePath(); }}); g.drawLine(3,9,21,9); }
                    case "mail" -> { g.drawRoundRect(3,5,18,14,3,3); g.drawLine(3,6,12,13); g.drawLine(12,13,21,6); }
                    case "users" -> { g.drawOval(8,3,8,8); g.draw(new Arc2D.Double(4,12,16,15,0,180,Arc2D.OPEN)); g.draw(new Arc2D.Double(16,5,5,6,270,180,Arc2D.OPEN)); g.draw(new Arc2D.Double(16,13,7,10,0,90,Arc2D.OPEN)); }
                    case "refresh" -> { g.draw(new Arc2D.Double(4,4,16,16,30,290,Arc2D.OPEN)); g.drawLine(20,3,20,9); g.drawLine(14,9,20,9); }
                    case "plus" -> { g.drawLine(5,12,19,12); g.drawLine(12,5,12,19); }
                    case "trash" -> { g.drawLine(4,6,20,6); g.drawRoundRect(7,6,10,15,2,2); g.drawLine(9,3,15,3); g.drawLine(10,10,10,17); g.drawLine(14,10,14,17); }
                    case "search" -> { g.drawOval(3,3,13,13); g.drawLine(15,15,21,21); }
                    case "edit" -> { g.draw(new Path2D.Double() {{ moveTo(4,15); lineTo(15,4); lineTo(20,9); lineTo(9,20); lineTo(4,20); closePath(); }}); g.drawLine(13,6,18,11); }
                    case "list" -> { for (int y1=6;y1<=18;y1+=6) { g.drawLine(8,y1,21,y1); g.fillOval(3,y1-1,2,2); } }
                    case "file" -> { g.draw(new Path2D.Double() {{ moveTo(5,2); lineTo(14,2); lineTo(20,8); lineTo(20,22); lineTo(5,22); closePath(); }}); g.drawLine(14,2,14,8); g.drawLine(14,8,20,8); g.drawLine(8,13,16,13); g.drawLine(8,17,16,17); }
                    case "globe" -> { g.drawOval(3,3,18,18); g.drawOval(8,3,8,18); g.drawLine(3,12,21,12); }
                    case "play" -> { g.draw(new Path2D.Double() {{ moveTo(8,4); lineTo(20,12); lineTo(8,20); closePath(); }}); }
                    case "stop" -> g.drawRoundRect(5,5,14,14,2,2);
                    case "send" -> { g.draw(new Path2D.Double() {{ moveTo(3,3); lineTo(22,12); lineTo(3,21); lineTo(7,12); closePath(); }}); g.drawLine(7,12,16,12); }
                    case "clock" -> { g.drawOval(3,3,18,18); g.drawLine(12,6,12,12); g.drawLine(12,12,16,15); }
                    case "settings" -> { g.drawOval(5,5,14,14); g.drawOval(9,9,6,6); for(int i=0;i<8;i++){ double a=i*Math.PI/4; g.drawLine((int)(12+8*Math.cos(a)),(int)(12+8*Math.sin(a)),(int)(12+11*Math.cos(a)),(int)(12+11*Math.sin(a))); } }
                    case "home" -> { g.drawLine(3,11,12,3); g.drawLine(12,3,21,11); g.drawRect(6,10,12,11); g.drawRect(10,15,4,6); }
                    case "audio" -> { g.drawLine(10,6,10,18); g.drawLine(10,6,20,4); g.drawLine(20,4,20,16); g.drawOval(4,16,6,5); g.drawOval(14,14,6,5); }
                    case "download" -> { g.drawLine(12,3,12,16); g.drawLine(7,11,12,16); g.drawLine(12,16,17,11); g.drawLine(4,20,20,20); }
                    case "dot" -> g.fillOval(7,7,10,10);
                    case "up" -> { g.drawLine(12,4,12,20); g.drawLine(6,10,12,4); g.drawLine(12,4,18,10); }
                    case "left" -> { g.drawLine(4,12,20,12); g.drawLine(4,12,10,6); g.drawLine(4,12,10,18); }
                    case "right" -> { g.drawLine(4,12,20,12); g.drawLine(14,6,20,12); g.drawLine(14,18,20,12); }
                    default -> { g.drawRoundRect(3,3,18,8,2,2); g.drawRoundRect(3,14,18,8,2,2); g.fillOval(6,6,2,2); g.fillOval(6,17,2,2); g.drawLine(12,7,17,7); g.drawLine(12,18,17,18); }
                }
                g.dispose();
            }
        };
    }
}

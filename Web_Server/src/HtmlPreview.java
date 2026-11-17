import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Set;

final class HtmlPreview {
    private static final Set<String> ELEMENTS = Set.of(
            "h1", "h2", "h3", "h4", "p", "div", "ul", "ol", "li", "strong", "b", "em",
            "i", "u", "code", "pre", "blockquote", "a", "table", "tr", "td", "th", "br", "hr");

    static String render(String source, URI base) throws IOException {
        var html = new StringBuilder("""
                <html><head><style>
                body {color:#15233d; font-family:Segoe UI; font-size:13pt; margin:24px;}
                h1 {font-size:26pt; margin-bottom:14px;} h2 {font-size:20pt;}
                a {color:#2058ca;} p,ul,ol {margin-bottom:12px;}
                pre,code {color:#1748aa;}
                </style></head><body>
                """);
        var suppressed = new ArrayDeque<String>();
        new ParserDelegator().parse(new StringReader(source), new HTMLEditorKit.ParserCallback() {
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                String name = tag.toString();
                if (name.equals("head") || name.equals("script") || name.equals("style")) {
                    suppressed.push(name);
                } else if (suppressed.isEmpty() && ELEMENTS.contains(name)) {
                    html.append('<').append(name);
                    if (name.equals("a") && attrs.getAttribute(HTML.Attribute.HREF) instanceof String link) {
                        try {
                            URI target = base.resolve(link.replace(" ", "%20"));
                            if ("http".equalsIgnoreCase(target.getScheme()) || "https".equalsIgnoreCase(target.getScheme())) {
                                html.append(" href=\"").append(escape(target.toASCIIString())).append('"');
                            }
                        } catch (IllegalArgumentException ignored) {
                            // Render invalid links as text.
                        }
                    }
                    html.append('>');
                }
            }

            @Override public void handleEndTag(HTML.Tag tag, int position) {
                String name = tag.toString();
                if (!suppressed.isEmpty()) {
                    if (name.equals(suppressed.peek())) suppressed.pop();
                } else if (ELEMENTS.contains(name)) {
                    html.append("</").append(name).append('>');
                }
            }

            @Override public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                if (suppressed.isEmpty() && (tag == HTML.Tag.BR || tag == HTML.Tag.HR)) {
                    html.append('<').append(tag).append('>');
                }
            }

            @Override public void handleText(char[] data, int position) {
                if (suppressed.isEmpty()) html.append(escape(new String(data)));
            }
        }, true);
        return html.append("</body></html>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

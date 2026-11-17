import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class DemoServer implements AutoCloseable {
    private static final String HOME = """
            <!doctype html><html lang="vi"><head><title>Máy chủ thực hành</title></head>
            <body><h1>Máy chủ thực hành MiniHTTP</h1>
            <p>Chọn một tài nguyên để xem phản hồi từ máy chủ.</p>
            <ul><li><a href="/api/info">Thông tin máy chủ (JSON)</a></li>
            <li><a href="/files/huong-dan.pdf">Tài liệu hướng dẫn (PDF)</a></li>
            <li><a href="/files/am-mau.wav">Âm thanh mẫu (WAV)</a></li></ul>
            <p>Thử HEAD để xem thông tin tệp, hoặc gửi POST tới <code>/api/echo</code>.</p>
            </body></html>
            """;
    private static final Map<String, FileData> FILES = Map.of(
            "/files/huong-dan.pdf", new FileData("application/pdf", "huong-dan.pdf", pdf()),
            "/files/am-mau.wav", new FileData("audio/wav", "am-mau.wav", wav()));
    private final HttpServer server;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    private record FileData(String type, String name, byte[] body) {}

    DemoServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 16);
        server.createContext("/", DemoServer::handle);
        server.setExecutor(workers);
        server.start();
    }

    URI home() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var method = exchange.getRequestMethod();
            var path = exchange.getRequestURI().getPath();
            var file = FILES.get(path);
            if (path.equals("/api/echo")) {
                if (method.equals("POST")) echo(exchange);
                else rejectMethod(exchange, "POST");
            } else if (!path.equals("/") && !path.equals("/api/info") && file == null) {
                error(exchange, 404, "Không tìm thấy tài nguyên.");
            } else if (!method.equals("GET") && !method.equals("HEAD")) {
                rejectMethod(exchange, "GET, HEAD");
            } else if (file != null) {
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.name + "\"");
                send(exchange, 200, file.type, file.body);
            } else if (path.equals("/")) {
                send(exchange, 200, "text/html; charset=UTF-8", HOME.getBytes(StandardCharsets.UTF_8));
            } else {
                json(exchange, 200, """
                        {
                          "server": "MiniHTTP Java",
                          "protocol": "HTTP/1.1",
                          "methods": ["GET", "HEAD", "POST"],
                          "files": ["/files/huong-dan.pdf", "/files/am-mau.wav"],
                          "post": "/api/echo"
                        }
                        """);
            }
        }
    }

    private static void rejectMethod(HttpExchange exchange, String allow) throws IOException {
        exchange.getResponseHeaders().set("Allow", allow);
        error(exchange, 405, "Phương thức không được hỗ trợ cho địa chỉ này.");
    }

    private static void echo(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Content-Type");
        String media = header == null ? "" : header.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!media.equals("application/json") && !media.equals("application/x-www-form-urlencoded")) {
            error(exchange, 415, "Hãy gửi JSON hoặc dữ liệu biểu mẫu.");
            return;
        }
        byte[] data = exchange.getRequestBody().readNBytes(65_537);
        if (data.length > 65_536) {
            error(exchange, 413, "Dữ liệu vượt quá 64 KiB.");
            return;
        }
        String received;
        try {
            var input = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString();
            if (media.equals("application/json")) {
                if (!JsonSyntax.valid(input)) throw new IllegalArgumentException();
                received = input.strip();
            } else {
                var fields = new LinkedHashMap<String, ArrayList<String>>();
                if (!input.isEmpty()) {
                    for (String field : input.split("&")) {
                        String[] pair = field.split("=", 2);
                        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8);
                        fields.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                    }
                }
                received = fields.entrySet().stream().map(entry -> quote(entry.getKey()) + ": "
                        + entry.getValue().stream().map(DemoServer::quote).collect(Collectors.joining(", ", "[", "]")))
                        .collect(Collectors.joining(", ", "{", "}"));
            }
        } catch (IllegalArgumentException | java.nio.charset.CharacterCodingException error) {
            error(exchange, 400, "Dữ liệu gửi lên không hợp lệ.");
            return;
        }
        json(exchange, 200, "{\n  \"method\": \"POST\",\n  \"received\": " + received + "\n}\n");
    }

    private static String quote(String value) {
        var quoted = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            if (ch == '"' || ch == '\\') quoted.append('\\').append(ch);
            else if (ch < 0x20) quoted.append(String.format("\\u%04x", (int) ch));
            else quoted.append(ch);
        }
        return quoted.append('"').toString();
    }

    private static void error(HttpExchange exchange, int status, String message) throws IOException {
        json(exchange, status, "{\"error\": " + quote(message) + "}");
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "application/json; charset=UTF-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type);
        headers.set("Server", "MiniHTTP/2.0");
        if (exchange.getRequestMethod().equals("HEAD")) {
            headers.set("Content-Length", Integer.toString(body.length));
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static byte[] wav() {
        int samples = 8_000;
        var buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + samples * 2);
        buffer.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(16).putShort((short) 1).putShort((short) 1).putInt(samples).putInt(samples * 2);
        buffer.putShort((short) 2).putShort((short) 16);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples * 2);
        for (int i = 0; i < samples; i++) {
            double t = (i % 4_000) / (double) samples;
            double gain = Math.min(1, Math.min(t, 0.5 - t) * 100);
            buffer.putShort((short) (6500 * gain * Math.sin(2 * Math.PI * (i < 4000 ? 440 : 660) * t)));
        }
        return buffer.array();
    }

    private static byte[] pdf() {
        var stream = "BT /F1 24 Tf 54 770 Td (MiniHTTP - HTTP Lab) Tj /F1 12 Tf "
                + "0 -36 Td (GET retrieves resources.) Tj 0 -22 Td (HEAD retrieves headers.) Tj "
                + "0 -22 Td (POST submits data.) Tj ET\n";
        var objects = new String[]{
                "<< /Type /Catalog /Pages 2 0 R >>", "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.length() + " >>\nstream\n" + stream + "endstream"
        };
        var buffer = new ByteArrayOutputStream();
        buffer.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        var offsets = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            offsets.append(String.format(Locale.ROOT, "%010d 00000 n \n", buffer.size()));
            buffer.writeBytes(((i + 1) + " 0 obj\n" + objects[i] + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
        }
        int xref = buffer.size();
        buffer.writeBytes(("xref\n0 6\n0000000000 65535 f \n" + offsets
                + "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n")
                .getBytes(StandardCharsets.US_ASCII));
        return buffer.toByteArray();
    }

    @Override public void close() {
        server.stop(0);
        workers.shutdownNow();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try (var demo = new DemoServer(args.length == 0 ? 8000 : Integer.parseInt(args[0]))) {
            System.out.println(demo.home());
            new CountDownLatch(1).await();
        }
    }
}

final class JsonSyntax {
    private static final Pattern TOKEN = Pattern.compile(
            "\"(?:[^\"\\\\\\x00-\\x1f]|\\\\(?:[\"\\\\/bfnrt]|u[0-9a-fA-F]{4}))*+\""
                    + "|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|true|false|null");
    private final String text;
    private int i;

    private JsonSyntax(String text) { this.text = text; }

    static boolean valid(String text) {
        var syntax = new JsonSyntax(text);
        return syntax.value(0) && syntax.skip() == text.length();
    }

    private int skip() {
        while (i < text.length() && " \t\r\n".indexOf(text.charAt(i)) >= 0) i++;
        return i;
    }

    private boolean take(char token) {
        if (skip() == text.length() || text.charAt(i) != token) return false;
        i++;
        return true;
    }

    private boolean value(int depth) {
        if (depth > 80) return false;
        if (take('{')) return members('}', true, depth);
        if (take('[')) return members(']', false, depth);
        return literal(false);
    }

    private boolean members(char end, boolean object, int depth) {
        if (take(end)) return true;
        do {
            if (object && (!literal(true) || !take(':'))) return false;
            if (!value(depth + 1)) return false;
        } while (take(','));
        return take(end);
    }

    private boolean literal(boolean key) {
        if (skip() == text.length() || (key && text.charAt(i) != '"')) return false;
        var matcher = TOKEN.matcher(text).region(i, text.length());
        if (!matcher.lookingAt()) return false;
        i = matcher.end();
        return true;
    }
}

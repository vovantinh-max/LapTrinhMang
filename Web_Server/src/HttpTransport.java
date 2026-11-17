import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;

final class HttpTransport {
    private static final int PREVIEW_LIMIT = 512 * 1024;
    private static final Pattern CHARSET = Pattern.compile("(?i)charset\\s*=\\s*[\"']?([^;\\s\"']+)");
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    record Query(URI uri, String method, String contentType, String payload) {
        static Query get(URI uri) {
            return new Query(uri, "GET", "", "");
        }
    }

    record Result(URI uri, int status, String version, long elapsedMillis,
                  HttpHeaders headers, String mediaType, String body, boolean truncated) {
        String headerText() {
            StringBuilder text = new StringBuilder(version + " " + status + "\n\n");
            headers.map().forEach((name, values) -> values.forEach(
                    value -> text.append(name).append(": ").append(value).append('\n')));
            return text.toString();
        }
    }

    static URI address(String input) {
        String value = input.strip();
        if (value.isEmpty()) throw new IllegalArgumentException("Hãy nhập địa chỉ HTTP hoặc HTTPS.");
        if (value.startsWith("//")) value = "http:" + value;
        else if (!value.contains("://")) value = "http://" + value;
        URI uri = URI.create(value.replace(" ", "%20"));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getPort() > 65535) {
            throw new IllegalArgumentException("Địa chỉ HTTP hoặc HTTPS không hợp lệ.");
        }
        return uri;
    }

    static String formEncode(String fields) {
        var encoded = new ArrayList<String>();
        for (String line : fields.lines().toList()) {
            if (line.isBlank()) continue;
            int split = line.indexOf('=');
            if (split < 1 || line.substring(0, split).isBlank()) {
                throw new IllegalArgumentException("Nhập mỗi trường theo dạng tên=giá trị.");
            }
            encoded.add(URLEncoder.encode(line.substring(0, split).strip(), StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(line.substring(split + 1), StandardCharsets.UTF_8));
        }
        return String.join("&", encoded);
    }

    private static HttpRequest request(Query query) {
        var builder = HttpRequest.newBuilder(query.uri()).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "MiniHTTP/2.0 (Java)")
                .header("Accept", "*/*");
        if (query.method().equals("POST")) {
            builder.header("Content-Type", query.contentType() + "; charset=UTF-8");
            return builder.POST(HttpRequest.BodyPublishers.ofString(query.payload())).build();
        }
        return builder.method(query.method(), HttpRequest.BodyPublishers.noBody()).build();
    }

    Result fetch(Query query) throws IOException, InterruptedException {
        long started = System.nanoTime();
        var response = client.send(request(query), HttpResponse.BodyHandlers.ofInputStream());
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        String media = contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        String body = "";
        boolean truncated = false;
        try (InputStream input = response.body()) {
            if (!query.method().equals("HEAD") && isText(media)) {
                byte[] bytes = input.readNBytes(PREVIEW_LIMIT + 1);
                truncated = bytes.length > PREVIEW_LIMIT;
                body = new String(bytes, 0, Math.min(bytes.length, PREVIEW_LIMIT), charset(contentType));
            }
        }
        String version = response.version() == HttpClient.Version.HTTP_2 ? "HTTP/2" : "HTTP/1.1";
        return new Result(response.uri(), response.statusCode(), version,
                (System.nanoTime() - started) / 1_000_000, response.headers(), media, body, truncated);
    }

    static boolean isText(String media) {
        return media.startsWith("text/") || media.equals("application/json") || media.endsWith("+json")
                || media.equals("application/xml") || media.endsWith("+xml") || media.equals("application/javascript");
    }

    private static Charset charset(String contentType) {
        var match = CHARSET.matcher(contentType);
        if (match.find()) {
            try {
                return Charset.forName(match.group(1));
            } catch (IllegalArgumentException ignored) {
                // Use the preview's default encoding when the server names an unknown charset.
            }
        }
        return StandardCharsets.UTF_8;
    }

    long download(URI uri, Path destination, BiConsumer<Long, Long> progress)
            throws IOException, InterruptedException {
        var response = client.send(request(Query.get(uri)), HttpResponse.BodyHandlers.ofInputStream());
        Path partial = null;
        try (InputStream input = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.statusCode() == 206) {
                throw new IOException("Máy chủ trả về HTTP " + response.statusCode());
            }
            long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            partial = Files.createTempFile(destination.toAbsolutePath().getParent(), ".minihttp-", ".part");
            long received = 0;
            long lastUpdate = 0;
            byte[] buffer = new byte[64 * 1024];
            try (var output = Files.newOutputStream(partial)) {
                for (int count; (count = input.read(buffer)) != -1; ) {
                    output.write(buffer, 0, count);
                    received += count;
                    long now = System.nanoTime();
                    if (now - lastUpdate >= 100_000_000) {
                        progress.accept(received, total);
                        lastUpdate = now;
                    }
                }
            }
            if (total >= 0 && received != total) throw new IOException("Tệp tải về chưa đủ dữ liệu.");
            try {
                Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return received;
        } finally {
            if (partial != null) Files.deleteIfExists(partial);
        }
    }
}

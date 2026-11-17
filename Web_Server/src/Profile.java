import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

final class Profile {
    record Visit(LocalDateTime time, String method, URI uri, String status) {}
    record Download(LocalDateTime time, URI uri, Path file, long bytes) {}

    private final Path directory;
    final List<Visit> history = new ArrayList<>();
    final List<URI> bookmarks = new ArrayList<>();
    final List<Download> downloads = new ArrayList<>();

    Profile(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        for (String[] row : read("history")) {
            try {
                history.add(new Visit(LocalDateTime.parse(row[0]), row[1], URI.create(row[2]), row[3]));
            } catch (RuntimeException ignored) {
                // A damaged row should not prevent opening the rest of the profile.
            }
        }
        for (String[] row : read("bookmarks")) {
            try {
                bookmarks.add(URI.create(row[0]));
            } catch (RuntimeException ignored) {
            }
        }
        for (String[] row : read("downloads")) {
            try {
                downloads.add(new Download(LocalDateTime.parse(row[0]), URI.create(row[1]),
                        Path.of(row[2]), Long.parseLong(row[3])));
            } catch (RuntimeException ignored) {
            }
        }
    }

    void record(String method, URI uri, String status) throws IOException {
        history.add(0, new Visit(LocalDateTime.now(), method, uri, status));
        if (history.size() > 500) history.subList(500, history.size()).clear();
        saveHistory();
    }

    void saveHistory() throws IOException {
        write("history", history.stream()
                .map(item -> encode(item.time().toString(), item.method(), item.uri().toString(), item.status()))
                .toList());
    }

    void toggleBookmark(URI uri) throws IOException {
        if (!bookmarks.remove(uri)) bookmarks.add(0, uri);
        write("bookmarks", bookmarks.stream().map(item -> encode(item.toString())).toList());
    }

    void recordDownload(URI uri, Path file, long bytes) throws IOException {
        downloads.add(0, new Download(LocalDateTime.now(), uri, file, bytes));
        if (downloads.size() > 200) downloads.subList(200, downloads.size()).clear();
        write("downloads", downloads.stream()
                .map(item -> encode(item.time().toString(), item.uri().toString(),
                        item.file().toString(), Long.toString(item.bytes())))
                .toList());
    }

    private List<String[]> read(String name) throws IOException {
        Path file = directory.resolve(name + ".txt");
        List<String[]> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        for (String line : Files.readAllLines(file)) {
            try {
                result.add(Arrays.stream(line.split("\\t", -1))
                        .map(value -> new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8))
                        .toArray(String[]::new));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static String encode(String... fields) {
        return String.join("\t", Arrays.stream(fields)
                .map(value -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)))
                .toList());
    }

    private void write(String name, List<String> lines) throws IOException {
        Path target = directory.resolve(name + ".txt");
        Path temp = Files.createTempFile(directory, name, ".tmp");
        try {
            Files.write(temp, lines);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

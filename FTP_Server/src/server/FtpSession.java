package server;

import database.UserDAO.User;
import java.io.File;
import java.io.IOException;

public class FtpSession {
    private User user;
    private String userCandidate; // Tên đăng nhập nhập qua lệnh USER
    private String currentVirtualDir = "/";
    private File baseStorageDir;
    private boolean binaryMode = true;
    private final FtpDataConnection dataConnection = new FtpDataConnection();
    private File renameFromFile = null;

    public FtpSession(File baseStorageDir) {
        this.baseStorageDir = baseStorageDir;
    }

    public boolean isAuthenticated() {
        return user != null;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.currentVirtualDir = "/";
    }

    public String getUserCandidate() {
        return userCandidate;
    }

    public void setUserCandidate(String userCandidate) {
        this.userCandidate = userCandidate;
    }

    public String getCurrentVirtualDir() {
        return currentVirtualDir;
    }

    public boolean isBinaryMode() {
        return binaryMode;
    }

    public void setBinaryMode(boolean binaryMode) {
        this.binaryMode = binaryMode;
    }

    public FtpDataConnection getDataConnection() {
        return dataConnection;
    }

    public File getRenameFromFile() {
        return renameFromFile;
    }

    public void setRenameFromFile(File file) {
        this.renameFromFile = file;
    }

    /**
     * Lấy thư mục gốc thực tế dành riêng cho user này.
     * Nếu user có homeDir (vd: "/student"), root thực tế là storage/student
     */
    public File getUserRootDirectory() {
        if (user == null || user.getHomeDir() == null || user.getHomeDir().trim().equals("/") || user.getHomeDir().isBlank()) {
            return baseStorageDir;
        }
        String cleanHome = user.getHomeDir().replace('\\', '/').trim();
        while (cleanHome.startsWith("/")) {
            cleanHome = cleanHome.substring(1);
        }
        File userDir = new File(baseStorageDir, cleanHome);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        return userDir;
    }

    /**
     * Chuyển đổi một đường dẫn ảo (từ lệnh CD, PWD, LIST, v.v.) thành File thực tế trên đĩa,
     * ngăn chặn tuyệt đối lỗi Directory Traversal (tấn công ../ ra ngoài thư mục gốc).
     */
    public File resolveVirtualPath(String path) {
        File userRoot = getUserRootDirectory();
        if (path == null || path.isBlank()) {
            return resolveVirtualPath(currentVirtualDir);
        }

        String targetVirtual;
        path = path.replace('\\', '/');

        if (path.startsWith("/")) {
            // Đường dẫn tuyệt đối tính từ root của user
            targetVirtual = normalizeVirtualPath(path);
        } else {
            // Đường dẫn tương đối từ currentVirtualDir
            targetVirtual = normalizeVirtualPath(currentVirtualDir + "/" + path);
        }

        String subPath = targetVirtual.startsWith("/") ? targetVirtual.substring(1) : targetVirtual;
        File resolved = new File(userRoot, subPath);

        // Kiểm tra bảo mật chống path traversal
        try {
            String canonicalRoot = userRoot.getCanonicalPath();
            String canonicalTarget = resolved.getCanonicalPath();
            if (!resolved.getCanonicalFile().toPath().startsWith(userRoot.getCanonicalFile().toPath())) {
                // Vượt ra ngoài vùng cho phép -> trả về root
                return userRoot;
            }
        } catch (IOException e) {
            return userRoot;
        }

        return resolved;
    }

    /**
     * Thay đổi thư mục ảo hiện tại (lệnh CWD)
     */
    public boolean changeWorkingDirectory(String path) {
        File targetFile = resolveVirtualPath(path);
        if (targetFile.exists() && targetFile.isDirectory()) {
            this.currentVirtualDir = fileToVirtualPath(targetFile);
            return true;
        }
        return false;
    }

    /**
     * Đổi File trên đĩa thành đường dẫn ảo hiển thị cho FTP Client
     */
    public String fileToVirtualPath(File file) {
        try {
            String rootCanonical = getUserRootDirectory().getCanonicalPath();
            String fileCanonical = file.getCanonicalPath();
            if (fileCanonical.equals(rootCanonical)) {
                return "/";
            }
            if (file.getCanonicalFile().toPath().startsWith(getUserRootDirectory().getCanonicalFile().toPath())) {
                String rel = fileCanonical.substring(rootCanonical.length()).replace('\\', '/');
                if (!rel.startsWith("/")) rel = "/" + rel;
                return rel;
            }
        } catch (IOException ignored) {}
        return "/";
    }

    private String normalizeVirtualPath(String raw) {
        String[] parts = raw.split("/+");
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        for (String p : parts) {
            if (p.isEmpty() || p.equals(".")) {
                continue;
            }
            if (p.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.addLast(p);
            }
        }
        if (stack.isEmpty()) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (String segment : stack) {
            sb.append("/").append(segment);
        }
        return sb.toString();
    }
}

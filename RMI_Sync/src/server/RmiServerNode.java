package server;

import database.RmiDatabaseManager;
import model.StudentRecord;
import rmi.DataService;
import rmi.SyncService;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RmiServerNode extends UnicastRemoteObject implements DataService, SyncService {

    public interface ServerNodeListener {
        void onServerStarted(String name, int port);
        void onServerStopped(String name);
        void onDataChanged(String action, StudentRecord student);
        void onPeerStatusChanged(boolean peerOnline, String peerName);
        void onLogMessage(String message);
    }

    private final String serverName;
    private final int localPort;
    private final String peerHost;
    private final int peerPort;
    private final String peerServerName;
    private final RmiDatabaseManager db;
    private final ExecutorService asyncWorker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "rmi-replication"); t.setDaemon(true); return t; });

    private Registry localRegistry;
    private volatile boolean running = false;
    private volatile boolean peerConnected = false;
    private ServerNodeListener listener;
    private java.util.concurrent.ScheduledExecutorService peerCheckTimer;
    private boolean exported = true;
    private boolean ownsRegistry;

    public RmiServerNode(String serverName, int localPort, String peerHost, int peerPort, String peerServerName, RmiDatabaseManager db) throws RemoteException {
        super(localPort);
        this.serverName = serverName;
        this.localPort = localPort;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.peerServerName = peerServerName;
        this.db = db;
    }

    public void setListener(ServerNodeListener listener) {
        this.listener = listener;
    }

    public synchronized void startNode() throws Exception {
        if (running) return;

        if (!exported) { UnicastRemoteObject.exportObject(this, localPort); exported = true; }
        // Tạo hoặc lấy RMI Registry tại cổng cục bộ
        try {
            localRegistry = LocateRegistry.createRegistry(localPort);
            ownsRegistry = true;
        } catch (RemoteException e) {
            localRegistry = LocateRegistry.getRegistry(localPort);
        }

        localRegistry.rebind(serverName, this);
        running = true;

        log(String.format("[%s] Đã đăng ký RMI Service trên cổng %d: rmi://localhost:%d/%s",
                serverName, localPort, localPort, serverName));

        if (listener != null) {
            listener.onServerStarted(serverName, localPort);
        }

        // Khởi động Timer kiểm tra Peer Server & Đồng bộ
        startPeerHealthCheck();
    }

    public synchronized void stopNode() {
        if (!running) return;
        running = false;

        if (peerCheckTimer != null) {
            peerCheckTimer.shutdownNow();
        }

        try {
            if (localRegistry != null) {
                localRegistry.unbind(serverName);
            }
            UnicastRemoteObject.unexportObject(this, true);
            exported = false;
            if (ownsRegistry) { UnicastRemoteObject.unexportObject(localRegistry, true); ownsRegistry = false; }
            peerConnected = false;
        } catch (Exception ignored) {}

        log("[" + serverName + "] Máy chủ RMI đã dừng.");
        if (listener != null) {
            listener.onServerStopped(serverName);
        }
    }

    private void startPeerHealthCheck() {
        peerCheckTimer = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "rmi-health"); t.setDaemon(true); return t; });
        peerCheckTimer.scheduleWithFixedDelay(() -> {
            if (!running) return;
            asyncWorker.submit(() -> {
                boolean wasConnected = peerConnected;
                SyncService peer = getPeerSyncService();
                if (peer != null) {
                    try {
                        peer.syncHeartbeat(serverName);
                        peerConnected = true;
                        if (!wasConnected) {
                            log(String.format("⚡ [%s] ĐÃ KẾT NỐI ĐỒNG BỘ VỚI ĐỐI TÁC: %s (%s:%d)",
                                    serverName, peerServerName, peerHost, peerPort));
                            // Tự động Full Sync khi vừa kết nối lại
                            triggerFullSyncFromPeer();
                        }
                    } catch (RemoteException ex) {
                        peerConnected = false;
                    }
                } else {
                    peerConnected = false;
                }

                if (wasConnected != peerConnected && listener != null) {
                    listener.onPeerStatusChanged(peerConnected, peerServerName);
                }
            });
        }, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    private SyncService getPeerSyncService() {
        try {
            String rmiUrl = String.format("rmi://%s:%d/%s", peerHost, peerPort, peerServerName);
            return (SyncService) Naming.lookup(rmiUrl);
        } catch (Exception e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  DATA SERVICE IMPLEMENTATION (CHO CLIENT GỌI)
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public synchronized StudentRecord addStudent(StudentRecord student) throws RemoteException {
        validate(student);
        student.setLastModifiedServer(serverName);
        StudentRecord previous = db.getState(student.getStudentCode());
        if (previous != null && !previous.isDeleted()) return null;
        student.setDeleted(false);
        student.setVersion(previous == null ? 1 : previous.getVersion() + 1);
        student.setUpdatedAt(now());

        boolean ok = db.mergeState(student);
        if (ok) {
            db.logSync(serverName, "LOCAL_ADD", student.getStudentCode(), "Client thêm bản ghi mới");
            log(String.format("[%s] Client thêm: %s - %s", serverName, student.getStudentCode(), student.getFullName()));
            notifyDataChanged("ADD", student);

            // Replicate sang Peer Server qua RMI
            asyncWorker.submit(() -> {
                SyncService peer = getPeerSyncService();
                if (peer != null) {
                    try {
                        peer.syncReplicateCreate(student, serverName);
                        db.logSync(serverName, "SYNC_OUT_CREATE", student.getStudentCode(), "Đồng bộ sang " + peerServerName);
                    } catch (RemoteException e) {
                        log("[" + serverName + "] Lỗi đồng bộ sang " + peerServerName + ": " + e.getMessage());
                    }
                }
            });
            return student;
        }
        return null;
    }

    @Override
    public synchronized boolean updateStudent(StudentRecord student) throws RemoteException {
        validate(student);
        StudentRecord existing = db.getByCode(student.getStudentCode());
        if (existing == null) return false;
        student.setDeleted(false);
        long newVersion = existing != null ? existing.getVersion() + 1 : 1;
        student.setVersion(newVersion);
        student.setLastModifiedServer(serverName);
        student.setUpdatedAt(now());

        boolean ok = db.mergeState(student);
        if (ok) {
            db.logSync(serverName, "LOCAL_UPDATE", student.getStudentCode(), "Client cập nhật (v" + newVersion + ")");
            log(String.format("[%s] Client cập nhật: %s (v%d)", serverName, student.getStudentCode(), newVersion));
            notifyDataChanged("UPDATE", student);

            // Replicate sang Peer Server qua RMI
            asyncWorker.submit(() -> {
                SyncService peer = getPeerSyncService();
                if (peer != null) {
                    try {
                        peer.syncReplicateUpdate(student, serverName);
                        db.logSync(serverName, "SYNC_OUT_UPDATE", student.getStudentCode(), "Đồng bộ cập nhật sang " + peerServerName);
                    } catch (RemoteException e) {
                        log("[" + serverName + "] Lỗi đồng bộ cập nhật sang " + peerServerName + ": " + e.getMessage());
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean deleteStudent(String studentCode) throws RemoteException {
        StudentRecord tombstone = db.getByCode(studentCode);
        if (tombstone == null) return false;
        tombstone.setDeleted(true);
        tombstone.setVersion(tombstone.getVersion() + 1);
        tombstone.setLastModifiedServer(serverName);
        tombstone.setUpdatedAt(now());
        boolean ok = db.mergeState(tombstone);
        if (ok) {
            db.logSync(serverName, "LOCAL_DELETE", studentCode, "Client xóa bản ghi");
            log(String.format("[%s] Client xóa: %s", serverName, studentCode));
            notifyDataChanged("DELETE", null);

            // Replicate sang Peer Server qua RMI
            asyncWorker.submit(() -> {
                SyncService peer = getPeerSyncService();
                if (peer != null) {
                    try {
                        peer.syncReplicateUpdate(tombstone, serverName);
                        db.logSync(serverName, "SYNC_OUT_DELETE", studentCode, "Đồng bộ lệnh xóa sang " + peerServerName);
                    } catch (RemoteException e) {
                        log("[" + serverName + "] Lỗi đồng bộ xóa sang " + peerServerName + ": " + e.getMessage());
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public StudentRecord getStudent(String studentCode) throws RemoteException {
        return db.getByCode(studentCode);
    }

    @Override
    public List<StudentRecord> getAllStudents() throws RemoteException {
        return db.getAll();
    }

    @Override
    public List<StudentRecord> searchStudents(String keyword) throws RemoteException {
        return db.search(keyword);
    }

    @Override
    public String getServerName() throws RemoteException {
        return serverName;
    }

    @Override
    public String ping() throws RemoteException {
        return "PONG from " + serverName + " at " + now();
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  SYNC SERVICE IMPLEMENTATION (CHO PEER SERVER ĐỒNG BỘ)
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public boolean syncReplicateCreate(StudentRecord student, String originServer) throws RemoteException {
        log(String.format("📥 [%s] NHẬN ĐỒNG BỘ TẠO MỚI từ %s: %s - %s",
                serverName, originServer, student.getStudentCode(), student.getFullName()));

        boolean ok = db.mergeState(student);

        if (ok) {
            db.logSync(serverName, "REPLICATE_IN_CREATE", student.getStudentCode(), "Đồng bộ từ " + originServer);
            notifyDataChanged("SYNC_CREATE", student);
        }
        return ok;
    }

    @Override
    public boolean syncReplicateUpdate(StudentRecord student, String originServer) throws RemoteException {
        log(String.format("📥 [%s] NHẬN ĐỒNG BỘ CẬP NHẬT từ %s: %s (v%d)",
                serverName, originServer, student.getStudentCode(), student.getVersion()));

        boolean ok = db.mergeState(student);
        if (ok) {
            db.logSync(serverName, "REPLICATE_IN_UPDATE", student.getStudentCode(), "Đồng bộ cập nhật từ " + originServer);
            notifyDataChanged("SYNC_UPDATE", student);
        }
        return ok;
    }

    @Override
    public boolean syncReplicateDelete(String studentCode, String originServer) throws RemoteException {
        log(String.format("📥 [%s] NHẬN ĐỒNG BỘ XÓA từ %s: %s", serverName, originServer, studentCode));
        boolean ok = db.delete(studentCode);
        if (ok) {
            db.logSync(serverName, "REPLICATE_IN_DELETE", studentCode, "Đồng bộ xóa từ " + originServer);
            notifyDataChanged("SYNC_DELETE", null);
        }
        return ok;
    }

    @Override
    public List<StudentRecord> requestFullSyncState(String requestingServer) throws RemoteException {
        log(String.format("📦 [%s] Nhận yêu cầu gửi toàn bộ dữ liệu (Full Sync) cho %s", serverName, requestingServer));
        db.logSync(serverName, "EXPORT_FULL_SYNC", "ALL", "Cung cấp toàn bộ dữ liệu cho " + requestingServer);
        return db.getSyncState();
    }

    @Override
    public String syncHeartbeat(String senderServer) throws RemoteException {
        return "HEARTBEAT_ACK from " + serverName;
    }

    public void triggerFullSyncFromPeer() {
        asyncWorker.submit(() -> {
            SyncService peer = getPeerSyncService();
            if (peer != null) {
                try {
                    log("[" + serverName + "] Đang lấy toàn bộ dữ liệu từ " + peerServerName + "...");
                    List<StudentRecord> fullState = peer.requestFullSyncState(serverName);
                    if (fullState != null) {
                        db.batchReplaceAll(fullState);
                        db.logSync(serverName, "IMPORT_FULL_SYNC", "ALL", "Nhận " + fullState.size() + " bản ghi từ " + peerServerName);
                        log(String.format("🎉 [%s] ĐỒNG BỘ TOÀN DIỆN THÀNH CÔNG (%d bản ghi từ %s)!",
                                serverName, fullState.size(), peerServerName));
                        notifyDataChanged("FULL_SYNC", null);
                    }
                } catch (Exception ex) {
                    log("[" + serverName + "] Lỗi đồng bộ toàn diện: " + ex.getMessage());
                }
            }
        });
    }

    public boolean isPeerConnected() { return peerConnected; }
    public boolean isRunning() { return running; }
    public String getServerNodeName() { return serverName; }
    public int getLocalPort() { return localPort; }
    public RmiDatabaseManager getDb() { return db; }

    private void notifyDataChanged(String action, StudentRecord s) {
        if (listener != null) {
            listener.onDataChanged(action, s);
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        if (listener != null) {
            listener.onLogMessage(msg);
        }
    }

    private void validate(StudentRecord s) throws RemoteException {
        if (s == null || s.getStudentCode() == null || s.getStudentCode().isBlank() ||
                s.getFullName() == null || s.getFullName().isBlank() || s.getClassName() == null || s.getMajor() == null ||
                !Double.isFinite(s.getGpa()) || s.getGpa() < 0 || s.getGpa() > 4)
            throw new RemoteException("Nhập mã, họ tên và GPA từ 0 đến 4.");
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
}

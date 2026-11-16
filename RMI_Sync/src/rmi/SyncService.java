package rmi;

import model.StudentRecord;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface SyncService extends Remote {

    /**
     * Nhận đồng bộ bản ghi tạo mới từ Server đối tác
     */
    boolean syncReplicateCreate(StudentRecord student, String originServer) throws RemoteException;

    /**
     * Nhận đồng bộ bản ghi cập nhật từ Server đối tác
     */
    boolean syncReplicateUpdate(StudentRecord student, String originServer) throws RemoteException;

    /**
     * Nhận đồng bộ xóa bản ghi từ Server đối tác
     */
    boolean syncReplicateDelete(String studentCode, String originServer) throws RemoteException;

    /**
     * Yêu cầu toàn bộ trạng thái dữ liệu khi một Server khởi động lại (Full Reconciliation)
     */
    List<StudentRecord> requestFullSyncState(String requestingServer) throws RemoteException;

    /**
     * Kiểm tra trạng thái kết nối hai chiều (Heartbeat)
     */
    String syncHeartbeat(String senderServer) throws RemoteException;
}

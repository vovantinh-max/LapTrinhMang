package test;

import database.RmiDatabaseManager;
import model.StudentRecord;
import rmi.DataService;
import server.RmiServerNode;

import java.io.File;
import java.rmi.Naming;
import java.util.List;

public class RmiSyncIntegrationTest {

    private static final int PORT_S1 = 10990;
    private static final int PORT_S2 = 10991;
    private static final String S1_NAME = "TestServer1";
    private static final String S2_NAME = "TestServer2";

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  RMI SYNCHRONIZED DUAL SERVER - AUTOMATED INTEGRATION TEST");
        System.out.println("==========================================================");

        // Xóa DB test cũ nếu có
        new File("data/test_s1.db").delete();
        new File("data/test_s2.db").delete();

        RmiDatabaseManager db1 = new RmiDatabaseManager("data/test_s1.db");
        RmiDatabaseManager db2 = new RmiDatabaseManager("data/test_s2.db");

        RmiServerNode node1 = null;
        RmiServerNode node2 = null;

        int passed = 0;
        int total = 5;

        try {
            node1 = new RmiServerNode(S1_NAME, PORT_S1, "127.0.0.1", PORT_S2, S2_NAME, db1);
            node2 = new RmiServerNode(S2_NAME, PORT_S2, "127.0.0.1", PORT_S1, S1_NAME, db2);

            node1.startNode();
            Thread.sleep(300);
            node2.startNode();
            Thread.sleep(500);

            String urlS1 = "rmi://127.0.0.1:" + PORT_S1 + "/" + S1_NAME;
            String urlS2 = "rmi://127.0.0.1:" + PORT_S2 + "/" + S2_NAME;

            DataService clientS1 = (DataService) Naming.lookup(urlS1);
            DataService clientS2 = (DataService) Naming.lookup(urlS2);

            // TEST 1: Ping & Remote Service Lookup
            System.out.print("[TEST 1] RMI Registry Lookup & Ping Check ... ");
            String ping1 = clientS1.ping();
            String ping2 = clientS2.ping();
            if (ping1.contains(S1_NAME) && ping2.contains(S2_NAME)) {
                System.out.println("PASSED! (" + ping1 + " | " + ping2 + ")");
                passed++;
            } else {
                System.out.println("FAILED!");
            }

            // TEST 2: Add Student on Server 1 -> Verify Replicate to Server 2
            System.out.print("[TEST 2] Active Replication S1 -> S2 (Add Student) ... ");
            StudentRecord s1 = new StudentRecord("22IT999", "Đặng Quang Huy", "22IT1", "An toàn thông tin", 3.82);
            StudentRecord created = clientS1.addStudent(s1);
            Thread.sleep(600); // Đợi replication luồng nền hoàn tất

            StudentRecord replicated = clientS2.getStudent("22IT999");
            if (created != null && replicated != null && "Đặng Quang Huy".equals(replicated.getFullName()) && replicated.getVersion() == 1) {
                System.out.println("PASSED! (Replicated to S2: " + replicated.getStudentCode() + ", Ver=" + replicated.getVersion() + ")");
                passed++;
            } else {
                System.out.println("FAILED!");
            }

            // TEST 3: Update Student on Server 2 -> Verify Replicate to Server 1
            System.out.print("[TEST 3] Active Replication S2 -> S1 (Update Student) ... ");
            replicated.setGpa(3.99);
            replicated.setClassName("22IT-VIP");
            boolean updated = clientS2.updateStudent(replicated);
            Thread.sleep(600); // Đợi replication luồng nền

            StudentRecord s1Updated = clientS1.getStudent("22IT999");
            if (updated && s1Updated != null && Math.abs(s1Updated.getGpa() - 3.99) < 0.001 && s1Updated.getVersion() == 2) {
                System.out.println("PASSED! (Updated on S1: GPA=" + s1Updated.getGpa() + ", Ver=" + s1Updated.getVersion() + ")");
                passed++;
            } else {
                System.out.println("FAILED!");
            }

            // TEST 4: Delete Student on Server 1 -> Verify Replicate Delete to Server 2
            System.out.print("[TEST 4] Active Replication S1 -> S2 (Delete Student) ... ");
            boolean deleted = clientS1.deleteStudent("22IT999");
            Thread.sleep(600);

            StudentRecord s2Deleted = clientS2.getStudent("22IT999");
            if (deleted && s2Deleted == null) {
                System.out.println("PASSED! (Student deleted on S2)");
                passed++;
            } else {
                System.out.println("FAILED!");
            }

            // TEST 5: Full Sync Reconciliation Test
            System.out.print("[TEST 5] Full Sync Reconciliation State ... ");
            // Tạo 3 sinh viên mới trực tiếp trên Server 1
            clientS1.addStudent(new StudentRecord("22IT101", "Sinh Viên A", "22IT1", "CNTT", 3.5));
            clientS1.addStudent(new StudentRecord("22IT102", "Sinh Viên B", "22IT2", "KTPM", 3.6));
            clientS1.addStudent(new StudentRecord("22IT103", "Sinh Viên C", "22IT3", "KHMT", 3.7));
            Thread.sleep(800);

            // Kích hoạt Full Sync từ Server 2 kéo dữ liệu từ Server 1
            node2.triggerFullSyncFromPeer();
            Thread.sleep(1000);

            List<StudentRecord> s2List = clientS2.getAllStudents();
            if (s2List.size() >= 3) {
                System.out.println("PASSED! (Server 2 reconciled " + s2List.size() + " records)");
                passed++;
            } else {
                System.out.println("FAILED! S2 has " + s2List.size() + " records");
            }

        } catch (Exception e) {
            System.out.println("TEST EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (node1 != null) node1.stopNode();
            if (node2 != null) node2.stopNode();
        }

        System.out.println("==========================================================");
        System.out.println(String.format("  RESULT: %d/%d TESTS PASSED (%.1f%%)", passed, total, (passed * 100.0 / total)));
        System.out.println("==========================================================");

        if (passed == total) {
            System.out.println(">>> LAB 5 RMI SYNCHRONIZED DUAL SERVER HOÀN THÀNH XUẤT SẮC! <<<");
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}

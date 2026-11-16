package test;

import database.RmiDatabaseManager;
import model.StudentRecord;
import server.RmiServerNode;
import java.nio.file.*;

/** Exercises lifecycle and offline merge cases missing from the original suite. */
public class RmiRecoveryIntegrationTest {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createDirectories(Path.of("build", "recovery-" + System.nanoTime()));
        RmiDatabaseManager a = new RmiDatabaseManager(folder.resolve("a.db").toString());
        RmiDatabaseManager b = new RmiDatabaseManager(folder.resolve("b.db").toString());
        RmiServerNode node = new RmiServerNode("Recovery", 11991, "127.0.0.1", 11992, "Offline", a);
        try {
            node.startNode();
            StudentRecord first = node.addStudent(new StudentRecord("A", "Student A", "IT", "IT", 3));
            b.batchReplaceAll(a.getSyncState());
            node.deleteStudent("A");
            StudentRecord onlyB = new StudentRecord("B", "Student B", "IT", "IT", 3.5);
            b.mergeState(onlyB);
            b.batchReplaceAll(a.getSyncState());
            a.batchReplaceAll(b.getSyncState());
            if (a.getByCode("A") != null || b.getByCode("A") != null)
                throw new AssertionError("Offline delete resurrected");
            if (a.getByCode("B") == null || b.getByCode("B") == null)
                throw new AssertionError("Local-only record lost");
            b.mergeState(first);
            if (b.getByCode("A") != null) throw new AssertionError("Stale create overwrote tombstone");
            node.stopNode(); node.startNode();
            if (!node.isRunning()) throw new AssertionError("Restart failed");
            if (node.addStudent(new StudentRecord("A", "Restored", "IT", "IT", 4)) == null)
                throw new AssertionError("Cannot recreate deleted student");
            b.batchReplaceAll(a.getSyncState());
            if (!"Restored".equals(b.getByCode("A").getFullName())) throw new AssertionError("Restore replication");
            System.out.println("PASS: restart, offline delete, local-only merge, stale replication and restore");
        } finally { node.stopNode(); }
    }
}

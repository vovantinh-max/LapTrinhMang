package model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean deleted;
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    private int id;
    private String studentCode;
    private String fullName;
    private String className;
    private String major;
    private double gpa;
    private long version;
    private String lastModifiedServer;
    private String updatedAt;

    public StudentRecord() {
        this.version = 1;
        this.updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public StudentRecord(String studentCode, String fullName, String className, String major, double gpa) {
        this();
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.className = className;
        this.major = major;
        this.gpa = gpa;
        this.lastModifiedServer = "LOCAL";
    }

    public StudentRecord(int id, String studentCode, String fullName, String className, String major, double gpa, long version, String lastModifiedServer, String updatedAt) {
        this.id = id;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.className = className;
        this.major = major;
        this.gpa = gpa;
        this.version = version;
        this.lastModifiedServer = lastModifiedServer;
        this.updatedAt = updatedAt != null ? updatedAt : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public double getGpa() { return gpa; }
    public void setGpa(double gpa) { this.gpa = gpa; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getLastModifiedServer() { return lastModifiedServer; }
    public void setLastModifiedServer(String lastModifiedServer) { this.lastModifiedServer = lastModifiedServer; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (GPA: %.2f) [Server: %s, v%d]",
                studentCode, fullName, className, gpa, lastModifiedServer, version);
    }
}

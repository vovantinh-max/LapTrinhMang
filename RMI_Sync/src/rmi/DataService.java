package rmi;

import model.StudentRecord;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DataService extends Remote {

    StudentRecord addStudent(StudentRecord student) throws RemoteException;

    boolean updateStudent(StudentRecord student) throws RemoteException;

    boolean deleteStudent(String studentCode) throws RemoteException;

    StudentRecord getStudent(String studentCode) throws RemoteException;

    List<StudentRecord> getAllStudents() throws RemoteException;

    List<StudentRecord> searchStudents(String keyword) throws RemoteException;

    String getServerName() throws RemoteException;

    String ping() throws RemoteException;
}

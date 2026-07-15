package model;

import java.time.LocalDate;

public class AttendanceLeaveDay {
    private int leaveRequestId;
    private int employeeId;
    private String employeeCode;
    private String employeeFullName;
    private String departmentName;
    private LocalDate leaveDate;
    private String leaveTypeLabel;
    private String attendanceStatus;
    private String attendanceCode;
    private String reason;
    private String managerNote;

    public int getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(int leaveRequestId) { this.leaveRequestId = leaveRequestId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeFullName() { return employeeFullName; }
    public void setEmployeeFullName(String employeeFullName) { this.employeeFullName = employeeFullName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }

    public String getLeaveTypeLabel() { return leaveTypeLabel; }
    public void setLeaveTypeLabel(String leaveTypeLabel) { this.leaveTypeLabel = leaveTypeLabel; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }

    public String getAttendanceCode() { return attendanceCode; }
    public void setAttendanceCode(String attendanceCode) { this.attendanceCode = attendanceCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getManagerNote() { return managerNote; }
    public void setManagerNote(String managerNote) { this.managerNote = managerNote; }
}

package model;

public class Department {
    private int departmentId;
    private String departmentCode;
    private String departmentName;
    private Integer managerId;

    public Department() {}

    public int getDepartmentId()                         { return departmentId; }
    public void setDepartmentId(int departmentId)        { this.departmentId = departmentId; }

    public String getDepartmentCode()                    { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName()                    { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Integer getManagerId()                        { return managerId; }
    public void setManagerId(Integer managerId)          { this.managerId = managerId; }
}

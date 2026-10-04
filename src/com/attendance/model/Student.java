package com.attendance.model;

public class Student extends User {
    private String rollNo;
    private String department;
    private int semester;
    private String phone;
    private String status; // ACTIVE, PENDING, INACTIVE

    public Student() {
        setRole("STUDENT");
    }

    public Student(int id, String name, String email, String password, String rollNo, String department, int semester, String phone, String status) {
        super(id, name, email, password, "STUDENT");
        this.rollNo = rollNo;
        this.department = department;
        this.semester = semester;
        this.phone = phone;
        this.status = (status == null || status.isEmpty()) ? "ACTIVE" : status;
    }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

package com.attendance.model;

public class Teacher extends User {
    private String department;
    private String phone;

    public Teacher() {
        setRole("TEACHER");
    }

    public Teacher(int id, String name, String email, String password, String department, String phone) {
        super(id, name, email, password, "TEACHER");
        this.department = department;
        this.phone = phone;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

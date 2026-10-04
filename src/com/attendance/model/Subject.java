package com.attendance.model;

public class Subject {
    private int subjectId;
    private String subjectName;
    private String code;
    private String department;

    public Subject() {}

    public Subject(int subjectId, String subjectName, String code, String department) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.code = code;
        this.department = department;
    }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}

package com.attendance.model;

public class ClassModel {
    private int classId;
    private String className;
    private String section;
    private int semester;
    private int teacherId;
    private int subjectId;
    private String roomNo;
    
    // Joined display fields
    private String teacherName;
    private String subjectName;
    private String subjectCode;

    public ClassModel() {}

    public ClassModel(int classId, String className, String section, int semester, int teacherId, int subjectId, String roomNo) {
        this.classId = classId;
        this.className = className;
        this.section = section;
        this.semester = semester;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.roomNo = roomNo;
    }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
}

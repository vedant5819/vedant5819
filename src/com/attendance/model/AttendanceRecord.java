package com.attendance.model;

public class AttendanceRecord {
    private int attendanceId;
    private int studentId;
    private int qrId;
    private int classId;
    private int subjectId;
    private String date;
    private String timeIn;
    private String status; // PRESENT, LATE, ABSENT
    private Double latitude;
    private Double longitude;
    private String ipAddress;
    private String deviceInfo;

    // Joined fields for rich UI reporting
    private String studentName;
    private String rollNo;
    private String className;
    private String subjectName;
    private String subjectCode;

    public AttendanceRecord() {}

    public AttendanceRecord(int attendanceId, int studentId, int qrId, int classId, int subjectId, String date, String timeIn, String status) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.qrId = qrId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.date = date;
        this.timeIn = timeIn;
        this.status = status;
    }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getQrId() { return qrId; }
    public void setQrId(int qrId) { this.qrId = qrId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeIn() { return timeIn; }
    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
}

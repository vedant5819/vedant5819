package com.attendance.model;

public class QRCodeModel {
    private int qrId;
    private int classId;
    private int subjectId;
    private int generatedBy;
    private String qrData;
    private String generatedTime;
    private String expiryTime;
    private String status; // ACTIVE, EXPIRED

    public QRCodeModel() {}

    public QRCodeModel(int qrId, int classId, int subjectId, int generatedBy, String qrData, String generatedTime, String expiryTime, String status) {
        this.qrId = qrId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.generatedBy = generatedBy;
        this.qrData = qrData;
        this.generatedTime = generatedTime;
        this.expiryTime = expiryTime;
        this.status = status;
    }

    public int getQrId() { return qrId; }
    public void setQrId(int qrId) { this.qrId = qrId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(int generatedBy) { this.generatedBy = generatedBy; }

    public String getQrData() { return qrData; }
    public void setQrData(String qrData) { this.qrData = qrData; }

    public String getGeneratedTime() { return generatedTime; }
    public void setGeneratedTime(String generatedTime) { this.generatedTime = generatedTime; }

    public String getExpiryTime() { return expiryTime; }
    public void setExpiryTime(String expiryTime) { this.expiryTime = expiryTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

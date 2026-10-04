package com.attendance.model;

public class SystemLog {
    private int logId;
    private String eventType;
    private String userEmail;
    private String details;
    private String ipAddress;
    private String timestamp;

    public SystemLog() {}

    public SystemLog(int logId, String eventType, String userEmail, String details, String ipAddress, String timestamp) {
        this.logId = logId;
        this.eventType = eventType;
        this.userEmail = userEmail;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

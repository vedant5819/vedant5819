package com.attendance.dao;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.QRCodeModel;
import com.attendance.model.SystemLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AttendanceDAO {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================== QR Code Management ==================

    public QRCodeModel generateDynamicQRCode(int classId, int subjectId, int teacherId) {
        expireOldQRCodes(classId);

        int expirySeconds = getSettingInt("qr_expiry_seconds", 30);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusSeconds(expirySeconds);

        String generatedTimeStr = now.format(DT_FORMAT);
        String expiryTimeStr = expiry.format(DT_FORMAT);

        // Dynamic security payload
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String qrPayload = String.format("ATTEND::CLASS:%d::SUB:%d::TEACH:%d::TS:%d::TOK:%s",
                classId, subjectId, teacherId, System.currentTimeMillis(), token);

        String sql = "INSERT INTO qr_code (class_id, subject_id, generated_by, qr_data, generated_time, expiry_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, classId);
            ps.setInt(2, subjectId);
            ps.setInt(3, teacherId);
            ps.setString(4, qrPayload);
            ps.setString(5, generatedTimeStr);
            ps.setString(6, expiryTimeStr);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int qrId = rs.getInt(1);
                    return new QRCodeModel(qrId, classId, subjectId, teacherId, qrPayload, generatedTimeStr, expiryTimeStr, "ACTIVE");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void expireOldQRCodes(int classId) {
        String sql = "UPDATE qr_code SET status = 'EXPIRED' WHERE class_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QRCodeModel getActiveQRCode(int classId) {
        String sql = "SELECT * FROM qr_code WHERE class_id = ? AND status = 'ACTIVE' ORDER BY qr_id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String expiry = rs.getString("expiry_time");
                    try {
                        LocalDateTime expTime = LocalDateTime.parse(expiry, DT_FORMAT);
                        if (LocalDateTime.now().isAfter(expTime)) {
                            // Expired
                            expireQRCode(rs.getInt("qr_id"));
                            return null;
                        }
                    } catch (Exception ignored) {}

                    return new QRCodeModel(
                            rs.getInt("qr_id"),
                            rs.getInt("class_id"),
                            rs.getInt("subject_id"),
                            rs.getInt("generated_by"),
                            rs.getString("qr_data"),
                            rs.getString("generated_time"),
                            rs.getString("expiry_time"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void expireQRCode(int qrId) {
        String sql = "UPDATE qr_code SET status = 'EXPIRED' WHERE qr_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qrId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== Attendance Recording with Anti-Proxy ==================

    public Map<String, Object> markAttendance(int studentId, String qrPayload, Double lat, Double lon, String ip, String deviceInfo) {
        Map<String, Object> result = new HashMap<>();

        if (qrPayload == null || qrPayload.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid QR code data. Please scan a valid class QR code.");
            return result;
        }

        // 1. Locate QR record by payload
        String sqlQr = "SELECT * FROM qr_code WHERE qr_data = ? ORDER BY qr_id DESC LIMIT 1";
        QRCodeModel qr = null;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQr)) {
            ps.setString(1, qrPayload.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    qr = new QRCodeModel(
                            rs.getInt("qr_id"),
                            rs.getInt("class_id"),
                            rs.getInt("subject_id"),
                            rs.getInt("generated_by"),
                            rs.getString("qr_data"),
                            rs.getString("generated_time"),
                            rs.getString("expiry_time"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (qr == null) {
            logSystemEvent("PROXY_ATTEMPT", "Student ID: " + studentId, "Scan rejected: QR code not found or forged", ip);
            result.put("success", false);
            result.put("message", "Unrecognized or fake QR code!");
            return result;
        }

        // 2. Check Expiry
        try {
            LocalDateTime expTime = LocalDateTime.parse(qr.getExpiryTime(), DT_FORMAT);
            if (LocalDateTime.now().isAfter(expTime) || "EXPIRED".equalsIgnoreCase(qr.getStatus())) {
                logSystemEvent("EXPIRED_QR_SCAN", "Student ID: " + studentId, "Attempted scan of expired QR code ID: " + qr.getQrId(), ip);
                result.put("success", false);
                result.put("message", "QR Code has expired! Please ask your teacher for the refreshed QR code.");
                return result;
            }
        } catch (Exception e) {
            // parsing fallback
        }

        // 3. Geofence Check (if enabled)
        boolean geofenceEnabled = getSettingBoolean("geofence_enabled", false);
        if (geofenceEnabled) {
            double campusLat = getSettingDouble("campus_latitude", 12.9716);
            double campusLon = getSettingDouble("campus_longitude", 77.5946);
            double allowedRadiusMeters = getSettingDouble("geofence_radius_meters", 500);

            if (lat == null || lon == null) {
                logSystemEvent("PROXY_ATTEMPT", "Student ID: " + studentId, "Location disabled or missing while geofencing is strictly enforced", ip);
                result.put("success", false);
                result.put("message", "Location access is required by institutional anti-proxy policy. Please enable GPS.");
                return result;
            }

            double distance = calculateDistanceMeters(lat, lon, campusLat, campusLon);
            if (distance > allowedRadiusMeters) {
                logSystemEvent("PROXY_ATTEMPT", "Student ID: " + studentId,
                        String.format("Location out of bounds: %.1f meters away (max allowed: %.1f m)", distance, allowedRadiusMeters), ip);
                result.put("success", false);
                result.put("message", String.format("Anti-Proxy Alert: You appear to be %.1f meters outside the campus classroom boundary.", distance));
                return result;
            }
        }

        // 4. Duplicate Check (Cannot mark attendance twice for the same class on the same day)
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String sqlCheck = "SELECT attendance_id, time_in FROM attendance WHERE student_id = ? AND class_id = ? AND date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
            ps.setInt(1, studentId);
            ps.setInt(2, qr.getClassId());
            ps.setString(3, todayStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("success", false);
                    result.put("message", "You have already marked your attendance for this class today at " + rs.getString("time_in"));
                    return result;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 5. Insert Attendance Record
        String timeInStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String insertSql = "INSERT INTO attendance (student_id, qr_id, class_id, subject_id, date, time_in, status, latitude, longitude, ip_address, device_info) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PRESENT', ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, qr.getQrId());
            ps.setInt(3, qr.getClassId());
            ps.setInt(4, qr.getSubjectId());
            ps.setString(5, todayStr);
            ps.setString(6, timeInStr);
            if (lat != null) ps.setDouble(7, lat); else ps.setNull(7, Types.REAL);
            if (lon != null) ps.setDouble(8, lon); else ps.setNull(8, Types.REAL);
            ps.setString(9, ip);
            ps.setString(10, deviceInfo);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                int attId = 0;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) attId = rs.getInt(1);
                }

                // Fetch student and class details for response
                Map<String, Object> details = getAttendanceDetails(attId);
                logSystemEvent("ATTENDANCE_MARKED", "Student ID: " + studentId, "Marked attendance successfully for class ID: " + qr.getClassId(), ip);

                result.put("success", true);
                result.put("message", "Attendance recorded successfully!");
                result.put("attendanceId", attId);
                result.put("time", timeInStr);
                result.put("date", todayStr);
                result.put("details", details);
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Database error: " + e.getMessage());
            return result;
        }

        result.put("success", false);
        result.put("message", "Failed to record attendance.");
        return result;
    }

    public boolean manualMarkAttendance(int studentId, int classId, int subjectId, String date, String status) {
        String timeIn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String sql = "INSERT INTO attendance (student_id, class_id, subject_id, date, time_in, status, device_info) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'MANUAL_OVERRIDE')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, classId);
            ps.setInt(3, subjectId);
            ps.setString(4, date != null ? date : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            ps.setString(5, timeIn);
            ps.setString(6, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateAttendanceStatus(int attendanceId, String status) {
        String sql = "UPDATE attendance SET status = ? WHERE attendance_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAttendance(int attendanceId) {
        String sql = "DELETE FROM attendance WHERE attendance_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================== Attendance Retrieval ==================

    public List<AttendanceRecord> getAttendanceByStudent(int studentId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, s.name as student_name, s.roll_no, c.class_name, sub.subject_name, sub.code as subject_code " +
                "FROM attendance a " +
                "JOIN student s ON a.student_id = s.student_id " +
                "JOIN class c ON a.class_id = c.class_id " +
                "JOIN subject sub ON a.subject_id = sub.subject_id " +
                "WHERE a.student_id = ? " +
                "ORDER BY a.date DESC, a.time_in DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractAttendanceRecord(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<AttendanceRecord> getAttendanceByClassAndDate(int classId, String date) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, s.name as student_name, s.roll_no, c.class_name, sub.subject_name, sub.code as subject_code " +
                "FROM attendance a " +
                "JOIN student s ON a.student_id = s.student_id " +
                "JOIN class c ON a.class_id = c.class_id " +
                "JOIN subject sub ON a.subject_id = sub.subject_id " +
                "WHERE a.class_id = ? AND a.date = ? " +
                "ORDER BY a.time_in DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractAttendanceRecord(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<AttendanceRecord> getAllAttendanceRecords(Integer classId, Integer subjectId, String date) {
        List<AttendanceRecord> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT a.*, s.name as student_name, s.roll_no, c.class_name, sub.subject_name, sub.code as subject_code " +
                "FROM attendance a " +
                "JOIN student s ON a.student_id = s.student_id " +
                "JOIN class c ON a.class_id = c.class_id " +
                "JOIN subject sub ON a.subject_id = sub.subject_id WHERE 1=1 ");

        if (classId != null && classId > 0) sql.append(" AND a.class_id = ").append(classId);
        if (subjectId != null && subjectId > 0) sql.append(" AND a.subject_id = ").append(subjectId);
        if (date != null && !date.trim().isEmpty()) sql.append(" AND a.date = '").append(date.trim()).append("'");

        sql.append(" ORDER BY a.date DESC, a.time_in DESC LIMIT 200");

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                list.add(extractAttendanceRecord(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private AttendanceRecord extractAttendanceRecord(ResultSet rs) throws SQLException {
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceId(rs.getInt("attendance_id"));
        ar.setStudentId(rs.getInt("student_id"));
        ar.setQrId(rs.getInt("qr_id"));
        ar.setClassId(rs.getInt("class_id"));
        ar.setSubjectId(rs.getInt("subject_id"));
        ar.setDate(rs.getString("date"));
        ar.setTimeIn(rs.getString("time_in"));
        ar.setStatus(rs.getString("status"));
        ar.setLatitude(rs.getObject("latitude") != null ? rs.getDouble("latitude") : null);
        ar.setLongitude(rs.getObject("longitude") != null ? rs.getDouble("longitude") : null);
        ar.setIpAddress(rs.getString("ip_address"));
        ar.setDeviceInfo(rs.getString("device_info"));

        ar.setStudentName(rs.getString("student_name"));
        ar.setRollNo(rs.getString("roll_no"));
        ar.setClassName(rs.getString("class_name"));
        ar.setSubjectName(rs.getString("subject_name"));
        ar.setSubjectCode(rs.getString("subject_code"));
        return ar;
    }

    public Map<String, Object> getAttendanceDetails(int attendanceId) {
        String sql = "SELECT a.*, s.name as student_name, s.roll_no, c.class_name, sub.subject_name, sub.code as subject_code " +
                "FROM attendance a " +
                "JOIN student s ON a.student_id = s.student_id " +
                "JOIN class c ON a.class_id = c.class_id " +
                "JOIN subject sub ON a.subject_id = sub.subject_id " +
                "WHERE a.attendance_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("attendanceId", rs.getInt("attendance_id"));
                    map.put("studentName", rs.getString("student_name"));
                    map.put("rollNo", rs.getString("roll_no"));
                    map.put("className", rs.getString("class_name"));
                    map.put("subjectName", rs.getString("subject_name"));
                    map.put("subjectCode", rs.getString("subject_code"));
                    map.put("date", rs.getString("date"));
                    map.put("timeIn", rs.getString("time_in"));
                    map.put("status", rs.getString("status"));
                    return map;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    // ================== Analytics & Percentage Calculation ==================

    public Map<String, Object> getStudentAttendanceStats(int studentId) {
        Map<String, Object> stats = new HashMap<>();

        // Total present, total late, total attended
        String sqlOverall = "SELECT status, COUNT(*) as count FROM attendance WHERE student_id = ? GROUP BY status";
        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlOverall)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("status");
                    int cnt = rs.getInt("count");
                    if ("PRESENT".equalsIgnoreCase(st)) presentCount += cnt;
                    else if ("LATE".equalsIgnoreCase(st)) lateCount += cnt;
                    else if ("ABSENT".equalsIgnoreCase(st)) absentCount += cnt;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int totalAttended = presentCount + lateCount;

        // Total lectures conducted in student's semester
        int totalLecturesConducted = 0;
        String sqlLectures = "SELECT COUNT(DISTINCT date || '-' || class_id) FROM attendance";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlLectures)) {
            if (rs.next()) {
                totalLecturesConducted = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (totalLecturesConducted < totalAttended) {
            totalLecturesConducted = totalAttended;
        }
        if (totalLecturesConducted == 0) totalLecturesConducted = 1; // avoid / 0

        double overallPercentage = Math.round(((double) totalAttended / totalLecturesConducted) * 1000.0) / 10.0;

        // Subject-wise percentage
        List<Map<String, Object>> subjectStats = new ArrayList<>();
        String sqlSubjects = "SELECT sub.subject_id, sub.subject_name, sub.code, " +
                "COUNT(a.attendance_id) as attended_classes " +
                "FROM subject sub " +
                "LEFT JOIN attendance a ON sub.subject_id = a.subject_id AND a.student_id = ? " +
                "GROUP BY sub.subject_id, sub.subject_name, sub.code";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSubjects)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> subMap = new HashMap<>();
                    String name = rs.getString("subject_name");
                    String code = rs.getString("code");
                    int attended = rs.getInt("attended_classes");
                    int totalSubLectures = getSubjectTotalLectures(rs.getInt("subject_id"));
                    if (totalSubLectures < attended) totalSubLectures = attended;
                    if (totalSubLectures == 0) totalSubLectures = Math.max(attended, 4);

                    double subPct = Math.round(((double) attended / totalSubLectures) * 1000.0) / 10.0;
                    String statusBadge = subPct >= 75.0 ? "Safe" : (subPct >= 65.0 ? "Warning" : "Critical");

                    subMap.put("subjectId", rs.getInt("subject_id"));
                    subMap.put("subjectName", name);
                    subMap.put("subjectCode", code);
                    subMap.put("attended", attended);
                    subMap.put("total", totalSubLectures);
                    subMap.put("percentage", subPct);
                    subMap.put("statusBadge", statusBadge);
                    subjectStats.add(subMap);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        stats.put("presentCount", presentCount);
        stats.put("lateCount", lateCount);
        stats.put("totalAttended", totalAttended);
        stats.put("totalLecturesConducted", totalLecturesConducted);
        stats.put("overallPercentage", overallPercentage);
        stats.put("isEligible", overallPercentage >= 75.0);
        stats.put("subjectStats", subjectStats);

        return stats;
    }

    private int getSubjectTotalLectures(int subjectId) {
        String sql = "SELECT COUNT(DISTINCT date || '-' || class_id) FROM attendance WHERE subject_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> map = new HashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM student");
            map.put("totalStudents", rs1.next() ? rs1.getInt(1) : 0);

            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM teacher");
            map.put("totalTeachers", rs2.next() ? rs2.getInt(1) : 0);

            ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) FROM class");
            map.put("totalClasses", rs3.next() ? rs3.getInt(1) : 0);

            ResultSet rs4 = stmt.executeQuery("SELECT COUNT(*) FROM subject");
            map.put("totalSubjects", rs4.next() ? rs4.getInt(1) : 0);

            ResultSet rs5 = stmt.executeQuery("SELECT COUNT(*) FROM attendance WHERE date = date('now')");
            map.put("todayAttendanceCount", rs5.next() ? rs5.getInt(1) : 0);

            ResultSet rs6 = stmt.executeQuery("SELECT COUNT(*) FROM qr_code WHERE status = 'ACTIVE'");
            map.put("activeQRSessions", rs6.next() ? rs6.getInt(1) : 0);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    // ================== Anti-Proxy & Settings ==================

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void updateSetting(String key, String value) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getSettingInt(String key, int def) {
        String val = getSettingString(key);
        if (val != null) {
            try { return Integer.parseInt(val.trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    public boolean getSettingBoolean(String key, boolean def) {
        String val = getSettingString(key);
        if (val != null) return Boolean.parseBoolean(val.trim());
        return def;
    }

    public double getSettingDouble(String key, double def) {
        String val = getSettingString(key);
        if (val != null) {
            try { return Double.parseDouble(val.trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    public String getSettingString(String key) {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logSystemEvent(String eventType, String email, String details, String ip) {
        String sql = "INSERT INTO system_logs (event_type, user_email, details, ip_address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventType);
            ps.setString(2, email);
            ps.setString(3, details);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<SystemLog> getSystemLogs(int limit) {
        List<SystemLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM system_logs ORDER BY log_id DESC LIMIT " + limit;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new SystemLog(
                        rs.getInt("log_id"),
                        rs.getString("event_type"),
                        rs.getString("user_email"),
                        rs.getString("details"),
                        rs.getString("ip_address"),
                        rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    // Haversine formula for calculating distance in meters
    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

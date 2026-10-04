package com.attendance.server;

import com.attendance.dao.*;
import com.attendance.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiHandler implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
        String requestBody = readRequestBody(exchange);
        Map<String, Object> body = JsonUtils.parseObject(requestBody);

        Map<String, Object> response = new HashMap<>();
        int statusCode = 200;

        try {
            // ==================== AUTHENTICATION ====================
            if (path.equals("/api/auth/login") && "POST".equals(method)) {
                String email = (String) body.get("email");
                String password = (String) body.get("password");
                String role = (String) body.get("role");

                User user = userDAO.authenticate(email, password, role);
                if (user != null) {
                    response.put("success", true);
                    response.put("message", "Login successful");
                    response.put("user", user);

                    // Add role-specific details
                    if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                        Student s = userDAO.getStudentById(user.getId());
                        response.put("studentDetails", s);
                    } else if ("TEACHER".equalsIgnoreCase(user.getRole())) {
                        Teacher t = userDAO.getTeacherById(user.getId());
                        response.put("teacherDetails", t);
                    }
                } else {
                    response.put("success", false);
                    response.put("message", "Invalid email, password, or role selection");
                    statusCode = 401;
                }
            } else if (path.equals("/api/auth/register") && "POST".equals(method)) {
                Student s = new Student();
                s.setName((String) body.get("name"));
                s.setEmail((String) body.get("email"));
                s.setPassword((String) body.get("password"));
                s.setRollNo((String) body.get("rollNo"));
                s.setDepartment((String) body.get("department"));
                s.setSemester(getInt(body.get("semester"), 1));
                s.setPhone((String) body.get("phone"));
                s.setStatus("ACTIVE");

                boolean ok = userDAO.addStudent(s);
                response.put("success", ok);
                response.put("message", ok ? "Student registered successfully! You can now log in." : "Registration failed. Email or Roll Number may already exist.");
            } else if (path.equals("/api/auth/forgot-password") && "POST".equals(method)) {
                String email = (String) body.get("email");
                String role = (String) body.get("role");
                String newPass = (String) body.get("newPassword");

                boolean ok = userDAO.resetPassword(role, email, newPass);
                response.put("success", ok);
                response.put("message", ok ? "Password has been successfully reset! You can now log in." : "No user found with the provided email and role.");
            } else if (path.equals("/api/auth/change-password") && "POST".equals(method)) {
                int userId = getInt(body.get("userId"), 0);
                String role = (String) body.get("role");
                String oldPass = (String) body.get("oldPassword");
                String newPass = (String) body.get("newPassword");

                boolean ok = userDAO.changePassword(role, userId, oldPass, newPass);
                response.put("success", ok);
                response.put("message", ok ? "Password updated successfully!" : "Current password was incorrect.");

            // ==================== STUDENT APIS ====================
            } else if (path.equals("/api/student/profile") && "GET".equals(method)) {
                int studentId = getInt(queryParams.get("studentId"), 0);
                Student s = userDAO.getStudentById(studentId);
                if (s != null) {
                    s.setPassword(""); // hide password
                    response.put("success", true);
                    response.put("profile", s);
                } else {
                    response.put("success", false);
                    response.put("message", "Student not found");
                }
            } else if (path.equals("/api/student/profile") && "POST".equals(method)) {
                int studentId = getInt(body.get("studentId"), 0);
                String name = (String) body.get("name");
                String phone = (String) body.get("phone");
                String department = (String) body.get("department");
                int semester = getInt(body.get("semester"), 1);

                boolean ok = userDAO.updateStudentProfile(studentId, name, phone, department, semester);
                response.put("success", ok);
                response.put("message", ok ? "Profile updated successfully!" : "Failed to update profile.");
            } else if (path.equals("/api/student/delete-profile") && "POST".equals(method)) {
                int studentId = getInt(body.get("studentId"), 0);
                boolean ok = userDAO.deleteStudent(studentId);
                response.put("success", ok);
                response.put("message", ok ? "Profile deleted successfully." : "Failed to delete profile.");
            } else if (path.equals("/api/student/attendance") && "GET".equals(method)) {
                int studentId = getInt(queryParams.get("studentId"), 0);
                List<AttendanceRecord> records = attendanceDAO.getAttendanceByStudent(studentId);
                response.put("success", true);
                response.put("records", records);
            } else if (path.equals("/api/student/stats") && "GET".equals(method)) {
                int studentId = getInt(queryParams.get("studentId"), 0);
                Map<String, Object> stats = attendanceDAO.getStudentAttendanceStats(studentId);
                response.put("success", true);
                response.put("stats", stats);
            } else if (path.equals("/api/student/scan-qr") && "POST".equals(method)) {
                int studentId = getInt(body.get("studentId"), 0);
                String qrPayload = (String) body.get("qrPayload");
                Double lat = getDouble(body.get("latitude"));
                Double lon = getDouble(body.get("longitude"));
                String deviceInfo = (String) body.get("deviceInfo");
                String ip = exchange.getRemoteAddress().getAddress().getHostAddress();

                Map<String, Object> markResult = attendanceDAO.markAttendance(studentId, qrPayload, lat, lon, ip, deviceInfo);
                response.putAll(markResult);

            // ==================== TEACHER APIS ====================
            } else if (path.equals("/api/teacher/classes") && "GET".equals(method)) {
                int teacherId = getInt(queryParams.get("teacherId"), 0);
                List<ClassModel> classes = academicDAO.getClassesByTeacher(teacherId);
                response.put("success", true);
                response.put("classes", classes);
            } else if (path.equals("/api/teacher/start-session") && "POST".equals(method)) {
                int classId = getInt(body.get("classId"), 0);
                int subjectId = getInt(body.get("subjectId"), 0);
                int teacherId = getInt(body.get("teacherId"), 0);

                QRCodeModel qr = attendanceDAO.generateDynamicQRCode(classId, subjectId, teacherId);
                if (qr != null) {
                    response.put("success", true);
                    response.put("qr", qr);
                    response.put("expirySeconds", attendanceDAO.getSettingInt("qr_expiry_seconds", 30));
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to generate dynamic session QR code.");
                }
            } else if (path.equals("/api/teacher/active-qr") && "GET".equals(method)) {
                int classId = getInt(queryParams.get("classId"), 0);
                QRCodeModel qr = attendanceDAO.getActiveQRCode(classId);
                if (qr != null) {
                    response.put("success", true);
                    response.put("qr", qr);
                } else {
                    response.put("success", false);
                    response.put("message", "No active QR session found for this class.");
                }
            } else if (path.equals("/api/teacher/live-attendance") && "GET".equals(method)) {
                int classId = getInt(queryParams.get("classId"), 0);
                String date = queryParams.get("date");
                if (date == null || date.isEmpty()) {
                    date = java.time.LocalDate.now().toString();
                }
                List<AttendanceRecord> records = attendanceDAO.getAttendanceByClassAndDate(classId, date);
                response.put("success", true);
                response.put("records", records);
            } else if (path.equals("/api/teacher/manual-mark") && "POST".equals(method)) {
                int studentId = getInt(body.get("studentId"), 0);
                int classId = getInt(body.get("classId"), 0);
                int subjectId = getInt(body.get("subjectId"), 0);
                String date = (String) body.get("date");
                String status = (String) body.get("status");

                boolean ok = attendanceDAO.manualMarkAttendance(studentId, classId, subjectId, date, status != null ? status : "PRESENT");
                response.put("success", ok);
                response.put("message", ok ? "Manual attendance recorded successfully!" : "Failed to record manual attendance.");

            // ==================== ADMIN APIS ====================
            } else if (path.equals("/api/admin/stats") && "GET".equals(method)) {
                Map<String, Object> stats = attendanceDAO.getAdminDashboardStats();
                response.put("success", true);
                response.put("stats", stats);
            } else if (path.equals("/api/admin/students") && "GET".equals(method)) {
                List<Student> students = userDAO.getAllStudents();
                response.put("success", true);
                response.put("students", students);
            } else if (path.equals("/api/admin/students") && "POST".equals(method)) {
                int id = getInt(body.get("id"), 0);
                Student s = new Student();
                s.setId(id);
                s.setName((String) body.get("name"));
                s.setEmail((String) body.get("email"));
                s.setPassword((String) body.get("password"));
                s.setRollNo((String) body.get("rollNo"));
                s.setDepartment((String) body.get("department"));
                s.setSemester(getInt(body.get("semester"), 1));
                s.setPhone((String) body.get("phone"));
                s.setStatus((String) body.get("status"));

                boolean ok;
                if (id > 0) {
                    ok = userDAO.updateStudent(s);
                } else {
                    ok = userDAO.addStudent(s);
                }
                response.put("success", ok);
                response.put("message", ok ? "Student record saved successfully!" : "Failed to save student record.");
            } else if (path.equals("/api/admin/students") && "DELETE".equals(method)) {
                int id = getInt(queryParams.get("id"), 0);
                boolean ok = userDAO.deleteStudent(id);
                response.put("success", ok);
                response.put("message", ok ? "Student deleted successfully." : "Failed to delete student.");
            } else if (path.equals("/api/admin/teachers") && "GET".equals(method)) {
                List<Teacher> teachers = userDAO.getAllTeachers();
                response.put("success", true);
                response.put("teachers", teachers);
            } else if (path.equals("/api/admin/teachers") && "POST".equals(method)) {
                int id = getInt(body.get("id"), 0);
                Teacher t = new Teacher();
                t.setId(id);
                t.setName((String) body.get("name"));
                t.setEmail((String) body.get("email"));
                t.setPassword((String) body.get("password"));
                t.setDepartment((String) body.get("department"));
                t.setPhone((String) body.get("phone"));

                boolean ok;
                if (id > 0) {
                    ok = userDAO.updateTeacher(t);
                } else {
                    ok = userDAO.addTeacher(t);
                }
                response.put("success", ok);
                response.put("message", ok ? "Teacher record saved successfully!" : "Failed to save teacher record.");
            } else if (path.equals("/api/admin/teachers") && "DELETE".equals(method)) {
                int id = getInt(queryParams.get("id"), 0);
                boolean ok = userDAO.deleteTeacher(id);
                response.put("success", ok);
                response.put("message", ok ? "Teacher deleted successfully." : "Failed to delete teacher.");
            } else if (path.equals("/api/admin/subjects") && "GET".equals(method)) {
                List<Subject> subjects = academicDAO.getAllSubjects();
                response.put("success", true);
                response.put("subjects", subjects);
            } else if (path.equals("/api/admin/subjects") && "POST".equals(method)) {
                int id = getInt(body.get("id"), 0);
                Subject s = new Subject(id, (String) body.get("subjectName"), (String) body.get("code"), (String) body.get("department"));
                boolean ok = (id > 0) ? academicDAO.updateSubject(s) : academicDAO.addSubject(s);
                response.put("success", ok);
                response.put("message", ok ? "Subject saved successfully!" : "Failed to save subject.");
            } else if (path.equals("/api/admin/subjects") && "DELETE".equals(method)) {
                int id = getInt(queryParams.get("id"), 0);
                boolean ok = academicDAO.deleteSubject(id);
                response.put("success", ok);
                response.put("message", ok ? "Subject deleted successfully." : "Failed to delete subject.");
            } else if (path.equals("/api/admin/classes") && "GET".equals(method)) {
                List<ClassModel> classes = academicDAO.getAllClasses();
                response.put("success", true);
                response.put("classes", classes);
            } else if (path.equals("/api/admin/classes") && "POST".equals(method)) {
                int id = getInt(body.get("id"), 0);
                ClassModel c = new ClassModel(
                        id,
                        (String) body.get("className"),
                        (String) body.get("section"),
                        getInt(body.get("semester"), 1),
                        getInt(body.get("teacherId"), 0),
                        getInt(body.get("subjectId"), 0),
                        (String) body.get("roomNo")
                );
                boolean ok = (id > 0) ? academicDAO.updateClass(c) : academicDAO.addClass(c);
                response.put("success", ok);
                response.put("message", ok ? "Class saved successfully!" : "Failed to save class.");
            } else if (path.equals("/api/admin/classes") && "DELETE".equals(method)) {
                int id = getInt(queryParams.get("id"), 0);
                boolean ok = academicDAO.deleteClass(id);
                response.put("success", ok);
                response.put("message", ok ? "Class deleted successfully." : "Failed to delete class.");
            } else if (path.equals("/api/admin/attendance") && "GET".equals(method)) {
                Integer classId = getIntNullable(queryParams.get("classId"));
                Integer subjectId = getIntNullable(queryParams.get("subjectId"));
                String date = queryParams.get("date");
                List<AttendanceRecord> records = attendanceDAO.getAllAttendanceRecords(classId, subjectId, date);
                response.put("success", true);
                response.put("records", records);
            } else if (path.equals("/api/admin/attendance") && "PUT".equals(method)) {
                int attId = getInt(body.get("attendanceId"), 0);
                String status = (String) body.get("status");
                boolean ok = attendanceDAO.updateAttendanceStatus(attId, status);
                response.put("success", ok);
                response.put("message", ok ? "Attendance status updated." : "Failed to update attendance status.");
            } else if (path.equals("/api/admin/attendance") && "DELETE".equals(method)) {
                int id = getInt(queryParams.get("id"), 0);
                boolean ok = attendanceDAO.deleteAttendance(id);
                response.put("success", ok);
                response.put("message", ok ? "Attendance record deleted." : "Failed to delete attendance record.");
            } else if (path.equals("/api/admin/settings") && "GET".equals(method)) {
                Map<String, String> settings = attendanceDAO.getAllSettings();
                response.put("success", true);
                response.put("settings", settings);
            } else if (path.equals("/api/admin/settings") && "POST".equals(method)) {
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    attendanceDAO.updateSetting(entry.getKey(), String.valueOf(entry.getValue()));
                }
                response.put("success", true);
                response.put("message", "System anti-proxy & QR settings saved successfully.");
            } else if (path.equals("/api/admin/logs") && "GET".equals(method)) {
                List<SystemLog> logs = attendanceDAO.getSystemLogs(100);
                response.put("success", true);
                response.put("logs", logs);
            } else {
                response.put("error", "Not Found");
                response.put("path", path);
                response.put("method", method);
                statusCode = 404;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            statusCode = 500;
        }

        byte[] jsonBytes = JsonUtils.toJson(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, jsonBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonBytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                try {
                    params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
                               URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                } catch (Exception ignored) {}
            } else if (pair.length == 1) {
                params.put(pair[0], "");
            }
        }
        return params;
    }

    private int getInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private Integer getIntNullable(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            String s = String.valueOf(obj).trim();
            if (s.isEmpty()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return null;
        }
    }
}

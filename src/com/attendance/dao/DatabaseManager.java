package com.attendance.dao;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_FILE = "attendance_system.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Enable Foreign Keys in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Admin Table
            stmt.execute("CREATE TABLE IF NOT EXISTS admin (" +
                    "admin_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "role TEXT DEFAULT 'SUPER_ADMIN', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 2. Teacher Table
            stmt.execute("CREATE TABLE IF NOT EXISTS teacher (" +
                    "teacher_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "department TEXT NOT NULL, " +
                    "phone TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 3. Student Table
            stmt.execute("CREATE TABLE IF NOT EXISTS student (" +
                    "student_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "roll_no TEXT NOT NULL UNIQUE, " +
                    "department TEXT NOT NULL, " +
                    "semester INTEGER NOT NULL DEFAULT 1, " +
                    "phone TEXT, " +
                    "status TEXT DEFAULT 'ACTIVE', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 4. Subject Table
            stmt.execute("CREATE TABLE IF NOT EXISTS subject (" +
                    "subject_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "subject_name TEXT NOT NULL, " +
                    "code TEXT NOT NULL UNIQUE, " +
                    "department TEXT NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 5. Class Table
            stmt.execute("CREATE TABLE IF NOT EXISTS class (" +
                    "class_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "class_name TEXT NOT NULL, " +
                    "section TEXT NOT NULL, " +
                    "semester INTEGER NOT NULL, " +
                    "teacher_id INTEGER NOT NULL REFERENCES teacher(teacher_id) ON DELETE CASCADE, " +
                    "subject_id INTEGER NOT NULL REFERENCES subject(subject_id) ON DELETE CASCADE, " +
                    "room_no TEXT DEFAULT 'Room 101', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 6. QR Code Table
            stmt.execute("CREATE TABLE IF NOT EXISTS qr_code (" +
                    "qr_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "class_id INTEGER NOT NULL REFERENCES class(class_id) ON DELETE CASCADE, " +
                    "subject_id INTEGER NOT NULL REFERENCES subject(subject_id) ON DELETE CASCADE, " +
                    "generated_by INTEGER NOT NULL REFERENCES teacher(teacher_id) ON DELETE CASCADE, " +
                    "qr_data TEXT NOT NULL, " +
                    "generated_time DATETIME NOT NULL, " +
                    "expiry_time DATETIME NOT NULL, " +
                    "status TEXT DEFAULT 'ACTIVE');");

            // 7. Attendance Table
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance (" +
                    "attendance_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id INTEGER NOT NULL REFERENCES student(student_id) ON DELETE CASCADE, " +
                    "qr_id INTEGER REFERENCES qr_code(qr_id) ON DELETE SET NULL, " +
                    "class_id INTEGER NOT NULL REFERENCES class(class_id) ON DELETE CASCADE, " +
                    "subject_id INTEGER NOT NULL REFERENCES subject(subject_id) ON DELETE CASCADE, " +
                    "date TEXT NOT NULL, " +
                    "time_in TEXT NOT NULL, " +
                    "status TEXT DEFAULT 'PRESENT', " +
                    "latitude REAL, " +
                    "longitude REAL, " +
                    "ip_address TEXT, " +
                    "device_info TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 8. System Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS system_logs (" +
                    "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "event_type TEXT NOT NULL, " +
                    "user_email TEXT, " +
                    "details TEXT NOT NULL, " +
                    "ip_address TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 9. System Settings Table
            stmt.execute("CREATE TABLE IF NOT EXISTS system_settings (" +
                    "setting_key TEXT PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL);");

            seedInitialData(conn);
            System.out.println(">> Database initialized and verified successfully: " + DB_FILE);
        } catch (SQLException e) {
            System.err.println("Database Initialization Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Check if Admin exists
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admin")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        System.out.println(">> Seeding initial default accounts & academic records...");

        try (Statement stmt = conn.createStatement()) {
            // Admin
            stmt.execute("INSERT INTO admin (name, email, password, role) VALUES " +
                    "('System Administrator', 'admin@attendance.edu', 'admin123', 'SUPER_ADMIN')");

            // Teachers
            stmt.execute("INSERT INTO teacher (name, email, password, department, phone) VALUES " +
                    "('Dr. Alan Turing', 'turing@attendance.edu', 'teacher123', 'Computer Science', '+1 987-654-3210'), " +
                    "('Prof. Ada Lovelace', 'ada@attendance.edu', 'teacher123', 'Information Technology', '+1 987-654-3211'), " +
                    "('Prof. John von Neumann', 'john@attendance.edu', 'teacher123', 'Computer Science', '+1 987-654-3212')");

            // Students
            stmt.execute("INSERT INTO student (name, email, password, roll_no, department, semester, phone, status) VALUES " +
                    "('Alex Morgan', 'alex@attendance.edu', 'student123', 'CS-2024-001', 'Computer Science', 6, '+1 555-0101', 'ACTIVE'), " +
                    "('Bella Chen', 'bella@attendance.edu', 'student123', 'CS-2024-002', 'Computer Science', 6, '+1 555-0102', 'ACTIVE'), " +
                    "('Carlos Gomez', 'carlos@attendance.edu', 'student123', 'IT-2024-003', 'Information Technology', 6, '+1 555-0103', 'ACTIVE'), " +
                    "('Diana Prince', 'diana@attendance.edu', 'student123', 'CS-2024-004', 'Computer Science', 6, '+1 555-0104', 'ACTIVE'), " +
                    "('Ethan Hunt', 'ethan@attendance.edu', 'student123', 'CS-2024-005', 'Computer Science', 6, '+1 555-0105', 'ACTIVE')");

            // Subjects
            stmt.execute("INSERT INTO subject (subject_name, code, department) VALUES " +
                    "('Java Enterprise & Cloud Computing', 'CS601', 'Computer Science'), " +
                    "('Database Management Systems', 'CS602', 'Computer Science'), " +
                    "('Web Technologies & Frameworks', 'IT603', 'Information Technology'), " +
                    "('Computer Networks & Security', 'CS604', 'Computer Science')");

            // Classes
            stmt.execute("INSERT INTO class (class_name, section, semester, teacher_id, subject_id, room_no) VALUES " +
                    "('Java Advanced Programming', 'Sec-A', 6, 1, 1, 'Lab 302'), " +
                    "('Advanced Database Systems', 'Sec-A', 6, 2, 2, 'Hall B'), " +
                    "('Full Stack Web Development', 'Sec-B', 6, 2, 3, 'Lab 201'), " +
                    "('Network Security & Cryptography', 'Sec-A', 6, 3, 4, 'Lab 105')");

            // Settings
            stmt.execute("INSERT INTO system_settings (setting_key, setting_value) VALUES " +
                    "('qr_expiry_seconds', '30'), " +
                    "('geofence_enabled', 'false'), " +
                    "('campus_latitude', '12.9716'), " +
                    "('campus_longitude', '77.5946'), " +
                    "('geofence_radius_meters', '500'), " +
                    "('prevent_duplicate_device', 'true')");

            // Initial attendance records for previous days
            stmt.execute("INSERT INTO attendance (student_id, class_id, subject_id, date, time_in, status, latitude, longitude, ip_address, device_info) VALUES " +
                    "(1, 1, 1, date('now', '-2 days'), '09:05:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.101', 'Mobile Chrome / Android'), " +
                    "(2, 1, 1, date('now', '-2 days'), '09:07:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.102', 'Mobile Safari / iOS'), " +
                    "(3, 1, 1, date('now', '-2 days'), '09:18:00', 'LATE', 12.9716, 77.5946, '192.168.1.103', 'Desktop Chrome / Windows'), " +
                    "(4, 1, 1, date('now', '-2 days'), '09:03:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.104', 'Mobile Chrome / Android'), " +
                    "(1, 2, 2, date('now', '-1 days'), '11:02:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.101', 'Mobile Chrome / Android'), " +
                    "(2, 2, 2, date('now', '-1 days'), '11:04:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.102', 'Mobile Safari / iOS'), " +
                    "(4, 2, 2, date('now', '-1 days'), '11:25:00', 'LATE', 12.9716, 77.5946, '192.168.1.104', 'Mobile Chrome / Android')");

            stmt.execute("INSERT INTO system_logs (event_type, user_email, details, ip_address) VALUES " +
                    "('SYSTEM_INIT', 'admin@attendance.edu', 'System database initialized with seed records', '127.0.0.1')");
        }
    }
}

# 📱 QR Code Attendance System (Java Mini Project)

An automated, modern, role-based digital attendance management system designed for academic institutions, powered by **Java SE**, **HTML5/CSS3/JavaScript**, and relational database management (**SQLite / MySQL**).

This project strictly adheres to the architectural models, use cases, ER diagrams, and functional specifications defined in the academic mini-project report.

---

## 🌟 Key System Capabilities

- **🔄 Dynamic Rotating QR Codes**: Generates encrypted dynamic session tokens with live countdown timers (15s–30s) and auto-refresh. Prevents absent students from scanning static camera screenshots sent by peers.
- **📍 Anti-Proxy Geofencing Engine**: Validates device GPS coordinates against campus coordinates within a configurable radius tolerance (e.g. 500m). Rejects out-of-range scan attempts and records anti-proxy security logs.
- **⚡ Real-Time Attendance Stream**: Teachers witness incoming student attendance marks live on their screen with audio-visual notifications as scans occur.
- **🖥️ Fullscreen Projector HUD**: High-visibility classroom presentation mode for digital projectors with clean token rotation.
- **📊 Automatic Analytics & Eligibility**: Automatically calculates student overall attendance % and subject-wise breakdowns with color-coded alerts (`Safe > 75%`, `Warning 65–75%`, `Critical < 65%`).
- **👤 Full Student Self-Service**: Profile updates, password recovery/reset, digital attendance slip receipts, and full profile deletion as required by Section 3.2.2.
- **⚙️ Comprehensive Admin Governance**: Complete CRUD control over Students, Faculty, Subjects, and Classes, plus master audit logs and security rule configuration.
- **📥 One-Click CSV Export**: Download attendance reports for Excel / Google Sheets across classes and whole-institution master logs.
- **🗄️ Zero-Configuration Execution**: Embedded database engine auto-creates tables and seeds realistic data on first run. Also includes standalone `qr_attendance_system.sql` for MySQL deployment.

---

## 🏛️ System Architecture & Roles

```
               +-------------------------------------------------+
               |              Teacher (Web / Projector)          |
               |  - Login & Select Class                         |
               |  - Generate Dynamic Rotating QR Code            |
               |  - Monitor Real-time Stream & Manual Override   |
               +-----------------------+-------------------------+
                                       |
                                       v
               +-------------------------------------------------+
               |              Student (Mobile / Web)             |
               |  - Scan Classroom QR via Camera / File          |
               |  - Instant GPS Anti-Proxy Verification          |
               |  - View Attendance % & Exam Eligibility         |
               +-----------------------+-------------------------+
                                       |
                                       v
               +-------------------------------------------------+
               |            Java Backend HTTP Server             |
               |  - Authentication & Role Authorization          |
               |  - Dynamic Token Generation & Expiry Engine     |
               |  - Geolocation & Duplicate Detection Validator  |
               |  - REST API Dispatcher                          |
               +-----------------------+-------------------------+
                                       |
                                       v
               +-------------------------------------------------+
               |            Database (SQLite / MySQL)            |
               |  - Users (Admin, Teacher, Student)              |
               |  - Subjects & Classes                           |
               |  - Dynamic QR Sessions                          |
               |  - Attendance Records & Anti-Proxy Audit Logs   |
               +-------------------------------------------------+
```

---

## 📊 Relational Database Schema (ER Diagram)

The relational database strictly implements all tables specified in **Section 2.3 (ER Diagram)**:

1. **`admin`**: `admin_id`, `name`, `email`, `password`, `role`, `created_at`
2. **`teacher`**: `teacher_id`, `name`, `email`, `password`, `department`, `phone`, `created_at`
3. **`student`**: `student_id`, `name`, `email`, `password`, `roll_no`, `department`, `semester`, `phone`, `status`, `created_at`
4. **`subject`**: `subject_id`, `subject_name`, `code`, `department`, `created_at`
5. **`class`**: `class_id`, `class_name`, `section`, `semester`, `teacher_id`, `subject_id`, `room_no`, `created_at`
6. **`qr_code`**: `qr_id`, `class_id`, `subject_id`, `generated_by`, `qr_data`, `generated_time`, `expiry_time`, `status`
7. **`attendance`**: `attendance_id`, `student_id`, `qr_id`, `class_id`, `subject_id`, `date`, `time_in`, `status`, `latitude`, `longitude`, `ip_address`, `device_info`
8. **`system_logs`**: `log_id`, `event_type`, `user_email`, `details`, `ip_address`, `timestamp`
9. **`system_settings`**: `setting_key`, `setting_value`

> 💡 A full MySQL DDL/DML script is available in [qr_attendance_system.sql](file:///c:/Users/vedan/OneDrive/Desktop/java%20mini%20project/qr_attendance_system.sql).

---

## 🔑 Pre-Configured Demo Credentials

For quick evaluation and testing, the system is pre-seeded with the following accounts:

| Portal | Email | Password | Role / Account Details |
|---|---|---|---|
| **Student Portal** | `alex@attendance.edu` | `student123` | Alex Morgan (Roll: CS-2024-001, Sem 6) |
| **Faculty Portal** | `turing@attendance.edu` | `teacher123` | Dr. Alan Turing (Computer Science) |
| **Faculty Portal** | `ada@attendance.edu` | `teacher123` | Prof. Ada Lovelace (IT) |
| **Admin Portal** | `admin@attendance.edu` | `admin123` | Institutional Administrator |

---

## 🚀 How to Execute the Project

### Option A: One-Click Windows Batch Script (Recommended)
Simply double-click:
```bash
compile_and_run.bat
```

### Option B: Terminal Command Line
```powershell
# 1. Compile Java files
javac -encoding UTF-8 -cp "sqlite-jdbc.jar" -d bin src/com/attendance/*.java src/com/attendance/model/*.java src/com/attendance/dao/*.java src/com/attendance/server/*.java

# 2. Run the application
java -cp "sqlite-jdbc.jar;bin" com.attendance.Main
```

### Option C: Open in Browser
Once running, open your web browser at:
- **Local Application URL**: [http://localhost:8080](http://localhost:8080)
- **Direct Student Portal**: [http://localhost:8080/student.html](http://localhost:8080/student.html)
- **Direct Teacher Portal**: [http://localhost:8080/teacher.html](http://localhost:8080/teacher.html)
- **Direct Admin Portal**: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

---

## 🧪 Quick Test Walkthrough

1. Open [http://localhost:8080](http://localhost:8080) and click **"Launch Teacher Portal"** (logged in as Dr. Turing).
2. Click **"Start Dynamic QR Session"**. You will see the dynamic QR code with a live 30-second rotating countdown ring.
3. In another tab or browser window, open [http://localhost:8080](http://localhost:8080) and click **"Launch Student Portal"** (logged in as Alex Morgan).
4. Click **"Submit Scan"** in the Quick Desktop Test Scan box (or use your mobile/webcam).
5. Listen to the pleasant double audio chime and observe:
   - Student portal immediately generates a digital verified attendance slip and updates the overall attendance %.
   - Teacher portal real-time stream counter increments from 0 to 1 with an instant row addition showing Alex Morgan's timestamp!

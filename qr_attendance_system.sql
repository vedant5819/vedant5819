-- =====================================================================
-- Database Schema: qr_attendance_system
-- QR Code Attendance System (Java Mini Project)
-- Compatible with MySQL 5.7+ / 8.0+ / MariaDB
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `qr_attendance_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `qr_attendance_system`;

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS `system_logs`;
DROP TABLE IF EXISTS `attendance`;
DROP TABLE IF EXISTS `qr_code`;
DROP TABLE IF EXISTS `class`;
DROP TABLE IF EXISTS `subject`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `teacher`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `system_settings`;

-- ---------------------------------------------------------------------
-- Table: admin (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `admin` (
    `admin_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(50) DEFAULT 'SUPER_ADMIN',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: teacher (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `teacher` (
    `teacher_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `department` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: student (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `student` (
    `student_id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `roll_no` VARCHAR(50) NOT NULL UNIQUE,
    `department` VARCHAR(100) NOT NULL,
    `semester` INT NOT NULL DEFAULT 1,
    `phone` VARCHAR(20) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: subject (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `subject` (
    `subject_id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `department` VARCHAR(100) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: class (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `class` (
    `class_id` INT AUTO_INCREMENT PRIMARY KEY,
    `class_name` VARCHAR(100) NOT NULL,
    `section` VARCHAR(20) NOT NULL,
    `semester` INT NOT NULL,
    `teacher_id` INT NOT NULL,
    `subject_id` INT NOT NULL,
    `room_no` VARCHAR(50) DEFAULT 'Room 101',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: qr_code (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `qr_code` (
    `qr_id` INT AUTO_INCREMENT PRIMARY KEY,
    `class_id` INT NOT NULL,
    `subject_id` INT NOT NULL,
    `generated_by` INT NOT NULL,
    `qr_data` TEXT NOT NULL,
    `generated_time` DATETIME NOT NULL,
    `expiry_time` DATETIME NOT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT `fk_qr_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_qr_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_qr_teacher` FOREIGN KEY (`generated_by`) REFERENCES `teacher` (`teacher_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: attendance (As specified in ER Diagram)
-- ---------------------------------------------------------------------
CREATE TABLE `attendance` (
    `attendance_id` INT AUTO_INCREMENT PRIMARY KEY,
    `student_id` INT NOT NULL,
    `qr_id` INT DEFAULT NULL,
    `class_id` INT NOT NULL,
    `subject_id` INT NOT NULL,
    `date` DATE NOT NULL,
    `time_in` TIME NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PRESENT',
    `latitude` DOUBLE DEFAULT NULL,
    `longitude` DOUBLE DEFAULT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `device_info` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_att_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_att_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_att_subject` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: system_logs (Anti-Proxy and Audit Trail)
-- ---------------------------------------------------------------------
CREATE TABLE `system_logs` (
    `log_id` INT AUTO_INCREMENT PRIMARY KEY,
    `event_type` VARCHAR(50) NOT NULL,
    `user_email` VARCHAR(100) DEFAULT NULL,
    `details` TEXT NOT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Table: system_settings (Anti-Proxy configuration)
-- ---------------------------------------------------------------------
CREATE TABLE `system_settings` (
    `setting_key` VARCHAR(50) PRIMARY KEY,
    `setting_value` TEXT NOT NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Initial Seed Data
-- ---------------------------------------------------------------------

-- 1. Default Admin Account
INSERT INTO `admin` (`name`, `email`, `password`, `role`) VALUES
('Administrator', 'admin@attendance.edu', 'admin123', 'SUPER_ADMIN');

-- 2. Default Teachers
INSERT INTO `teacher` (`name`, `email`, `password`, `department`, `phone`) VALUES
('Dr. Alan Turing', 'turing@attendance.edu', 'teacher123', 'Computer Science', '+1 987-654-3210'),
('Prof. Ada Lovelace', 'ada@attendance.edu', 'teacher123', 'Information Technology', '+1 987-654-3211'),
('Prof. John von Neumann', 'john@attendance.edu', 'teacher123', 'Computer Science', '+1 987-654-3212');

-- 3. Default Students
INSERT INTO `student` (`name`, `email`, `password`, `roll_no`, `department`, `semester`, `phone`, `status`) VALUES
('Alex Morgan', 'alex@attendance.edu', 'student123', 'CS-2024-001', 'Computer Science', 6, '+1 555-0101', 'ACTIVE'),
('Bella Chen', 'bella@attendance.edu', 'student123', 'CS-2024-002', 'Computer Science', 6, '+1 555-0102', 'ACTIVE'),
('Carlos Gomez', 'carlos@attendance.edu', 'student123', 'IT-2024-003', 'Information Technology', 6, '+1 555-0103', 'ACTIVE'),
('Diana Prince', 'diana@attendance.edu', 'student123', 'CS-2024-004', 'Computer Science', 6, '+1 555-0104', 'ACTIVE'),
('Ethan Hunt', 'ethan@attendance.edu', 'student123', 'CS-2024-005', 'Computer Science', 6, '+1 555-0105', 'ACTIVE');

-- 4. Default Subjects
INSERT INTO `subject` (`subject_name`, `code`, `department`) VALUES
('Java Enterprise & Cloud Computing', 'CS601', 'Computer Science'),
('Database Management Systems', 'CS602', 'Computer Science'),
('Web Technologies & Frameworks', 'IT603', 'Information Technology'),
('Computer Networks & Security', 'CS604', 'Computer Science');

-- 5. Default Classes
INSERT INTO `class` (`class_name`, `section`, `semester`, `teacher_id`, `subject_id`, `room_no`) VALUES
('Java Advanced Programming', 'Sec-A', 6, 1, 1, 'Lab 302'),
('Advanced Database Systems', 'Sec-A', 6, 2, 2, 'Hall B'),
('Full Stack Web Development', 'Sec-B', 6, 2, 3, 'Lab 201'),
('Network Security & Cryptography', 'Sec-A', 6, 3, 4, 'Lab 105');

-- 6. Initial System Settings
INSERT INTO `system_settings` (`setting_key`, `setting_value`) VALUES
('qr_expiry_seconds', '30'),
('geofence_enabled', 'false'),
('campus_latitude', '12.9716'),
('campus_longitude', '77.5946'),
('geofence_radius_meters', '500'),
('prevent_duplicate_device', 'true');

-- 7. Seed Past Attendance Records for analytics visualization
INSERT INTO `attendance` (`student_id`, `class_id`, `subject_id`, `date`, `time_in`, `status`, `latitude`, `longitude`, `ip_address`, `device_info`) VALUES
(1, 1, 1, CURDATE() - INTERVAL 2 DAY, '09:05:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.101', 'Chrome Mobile / Android'),
(2, 1, 1, CURDATE() - INTERVAL 2 DAY, '09:07:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.102', 'Safari / iPhone'),
(3, 1, 1, CURDATE() - INTERVAL 2 DAY, '09:18:00', 'LATE', 12.9716, 77.5946, '192.168.1.103', 'Firefox / Windows'),
(4, 1, 1, CURDATE() - INTERVAL 2 DAY, '09:03:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.104', 'Chrome / Android'),
(1, 2, 2, CURDATE() - INTERVAL 1 DAY, '11:02:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.101', 'Chrome Mobile / Android'),
(2, 2, 2, CURDATE() - INTERVAL 1 DAY, '11:04:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.102', 'Safari / iPhone'),
(4, 2, 2, CURDATE() - INTERVAL 1 DAY, '11:20:00', 'LATE', 12.9716, 77.5946, '192.168.1.104', 'Chrome / Android'),
(1, 1, 1, CURDATE(), '09:01:00', 'PRESENT', 12.9716, 77.5946, '192.168.1.101', 'Chrome Mobile / Android');

-- 8. Seed Log
INSERT INTO `system_logs` (`event_type`, `user_email`, `details`, `ip_address`) VALUES
('SYSTEM_INIT', 'admin@attendance.edu', 'Database initialized and seeded successfully', '127.0.0.1');

package com.attendance.dao;

import com.attendance.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String email, String password, String requestedRole) {
        if (email == null || password == null) return null;
        email = email.trim();
        password = password.trim();

        if (requestedRole == null || requestedRole.equalsIgnoreCase("ADMIN")) {
            User admin = authenticateAdmin(email, password);
            if (admin != null) return admin;
        }
        if (requestedRole == null || requestedRole.equalsIgnoreCase("TEACHER")) {
            User teacher = authenticateTeacher(email, password);
            if (teacher != null) return teacher;
        }
        if (requestedRole == null || requestedRole.equalsIgnoreCase("STUDENT")) {
            User student = authenticateStudent(email, password);
            if (student != null) return student;
        }
        return null;
    }

    private Admin authenticateAdmin(String email, String password) {
        String sql = "SELECT * FROM admin WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                            rs.getInt("admin_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Teacher authenticateTeacher(String email, String password) {
        String sql = "SELECT * FROM teacher WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Teacher(
                            rs.getInt("teacher_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("department"),
                            rs.getString("phone")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Student authenticateStudent(String email, String password) {
        String sql = "SELECT * FROM student WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("roll_no"),
                            rs.getString("department"),
                            rs.getInt("semester"),
                            rs.getString("phone"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================== Teacher Operations ==================

    public List<Teacher> getAllTeachers() {
        List<Teacher> list = new ArrayList<>();
        String sql = "SELECT * FROM teacher ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Teacher(
                        rs.getInt("teacher_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        "", // hide password
                        rs.getString("department"),
                        rs.getString("phone")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Teacher getTeacherById(int id) {
        String sql = "SELECT * FROM teacher WHERE teacher_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Teacher(
                            rs.getInt("teacher_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("department"),
                            rs.getString("phone")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addTeacher(Teacher t) {
        String sql = "INSERT INTO teacher (name, email, password, department, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getEmail());
            ps.setString(3, t.getPassword());
            ps.setString(4, t.getDepartment());
            ps.setString(5, t.getPhone());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTeacher(Teacher t) {
        StringBuilder sql = new StringBuilder("UPDATE teacher SET name=?, email=?, department=?, phone=?");
        boolean hasPass = t.getPassword() != null && !t.getPassword().trim().isEmpty();
        if (hasPass) sql.append(", password=?");
        sql.append(" WHERE teacher_id=?");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getEmail());
            ps.setString(3, t.getDepartment());
            ps.setString(4, t.getPhone());
            int idx = 5;
            if (hasPass) {
                ps.setString(idx++, t.getPassword());
            }
            ps.setInt(idx, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating teacher: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTeacher(int id) {
        String sql = "DELETE FROM teacher WHERE teacher_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================== Student Operations ==================

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM student ORDER BY roll_no ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        "",
                        rs.getString("roll_no"),
                        rs.getString("department"),
                        rs.getInt("semester"),
                        rs.getString("phone"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Student getStudentById(int id) {
        String sql = "SELECT * FROM student WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("roll_no"),
                            rs.getString("department"),
                            rs.getInt("semester"),
                            rs.getString("phone"),
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addStudent(Student s) {
        String sql = "INSERT INTO student (name, email, password, roll_no, department, semester, phone, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getPassword());
            ps.setString(4, s.getRollNo());
            ps.setString(5, s.getDepartment());
            ps.setInt(6, s.getSemester());
            ps.setString(7, s.getPhone());
            ps.setString(8, (s.getStatus() != null) ? s.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudent(Student s) {
        StringBuilder sql = new StringBuilder("UPDATE student SET name=?, email=?, roll_no=?, department=?, semester=?, phone=?, status=?");
        boolean hasPass = s.getPassword() != null && !s.getPassword().trim().isEmpty();
        if (hasPass) sql.append(", password=?");
        sql.append(" WHERE student_id=?");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getRollNo());
            ps.setString(4, s.getDepartment());
            ps.setInt(5, s.getSemester());
            ps.setString(6, s.getPhone());
            ps.setString(7, s.getStatus());
            int idx = 8;
            if (hasPass) {
                ps.setString(idx++, s.getPassword());
            }
            ps.setInt(idx, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudentProfile(int studentId, String name, String phone, String department, int semester) {
        String sql = "UPDATE student SET name = ?, phone = ?, department = ?, semester = ? WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, department);
            ps.setInt(4, semester);
            ps.setInt(5, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteStudent(int id) {
        String sql = "DELETE FROM student WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================== Password Management ==================

    public boolean changePassword(String role, int id, String oldPassword, String newPassword) {
        String table = "student";
        String idCol = "student_id";
        if ("TEACHER".equalsIgnoreCase(role)) {
            table = "teacher";
            idCol = "teacher_id";
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            table = "admin";
            idCol = "admin_id";
        }

        String sqlCheck = "SELECT password FROM " + table + " WHERE " + idCol + " = ?";
        String sqlUpdate = "UPDATE " + table + " SET password = ? WHERE " + idCol + " = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    String current = rs.getString("password");
                    if (!current.equals(oldPassword)) {
                        return false; // Old password doesn't match
                    }
                } else {
                    return false;
                }
            }

            try (PreparedStatement psUp = conn.prepareStatement(sqlUpdate)) {
                psUp.setString(1, newPassword);
                psUp.setInt(2, id);
                return psUp.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean resetPassword(String role, String email, String newPassword) {
        String table = "student";
        if ("TEACHER".equalsIgnoreCase(role)) {
            table = "teacher";
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            table = "admin";
        }

        String sql = "UPDATE " + table + " SET password = ? WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

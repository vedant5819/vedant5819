package com.attendance.dao;

import com.attendance.model.ClassModel;
import com.attendance.model.Subject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AcademicDAO {

    // ================== Subject Operations ==================

    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT * FROM subject ORDER BY subject_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Subject(
                        rs.getInt("subject_id"),
                        rs.getString("subject_name"),
                        rs.getString("code"),
                        rs.getString("department")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Subject getSubjectById(int id) {
        String sql = "SELECT * FROM subject WHERE subject_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Subject(
                            rs.getInt("subject_id"),
                            rs.getString("subject_name"),
                            rs.getString("code"),
                            rs.getString("department")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addSubject(Subject s) {
        String sql = "INSERT INTO subject (subject_name, code, department) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSubjectName());
            ps.setString(2, s.getCode());
            ps.setString(3, s.getDepartment());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSubject(Subject s) {
        String sql = "UPDATE subject SET subject_name = ?, code = ?, department = ? WHERE subject_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getSubjectName());
            ps.setString(2, s.getCode());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getSubjectId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSubject(int id) {
        String sql = "DELETE FROM subject WHERE subject_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================== Class Operations ==================

    public List<ClassModel> getAllClasses() {
        List<ClassModel> list = new ArrayList<>();
        String sql = "SELECT c.*, t.name as teacher_name, s.subject_name, s.code as subject_code " +
                "FROM class c " +
                "LEFT JOIN teacher t ON c.teacher_id = t.teacher_id " +
                "LEFT JOIN subject s ON c.subject_id = s.subject_id " +
                "ORDER BY c.class_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ClassModel cm = new ClassModel(
                        rs.getInt("class_id"),
                        rs.getString("class_name"),
                        rs.getString("section"),
                        rs.getInt("semester"),
                        rs.getInt("teacher_id"),
                        rs.getInt("subject_id"),
                        rs.getString("room_no")
                );
                cm.setTeacherName(rs.getString("teacher_name"));
                cm.setSubjectName(rs.getString("subject_name"));
                cm.setSubjectCode(rs.getString("subject_code"));
                list.add(cm);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<ClassModel> getClassesByTeacher(int teacherId) {
        List<ClassModel> list = new ArrayList<>();
        String sql = "SELECT c.*, t.name as teacher_name, s.subject_name, s.code as subject_code " +
                "FROM class c " +
                "LEFT JOIN teacher t ON c.teacher_id = t.teacher_id " +
                "LEFT JOIN subject s ON c.subject_id = s.subject_id " +
                "WHERE c.teacher_id = ? " +
                "ORDER BY c.class_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassModel cm = new ClassModel(
                            rs.getInt("class_id"),
                            rs.getString("class_name"),
                            rs.getString("section"),
                            rs.getInt("semester"),
                            rs.getInt("teacher_id"),
                            rs.getInt("subject_id"),
                            rs.getString("room_no")
                    );
                    cm.setTeacherName(rs.getString("teacher_name"));
                    cm.setSubjectName(rs.getString("subject_name"));
                    cm.setSubjectCode(rs.getString("subject_code"));
                    list.add(cm);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ClassModel getClassById(int classId) {
        String sql = "SELECT c.*, t.name as teacher_name, s.subject_name, s.code as subject_code " +
                "FROM class c " +
                "LEFT JOIN teacher t ON c.teacher_id = t.teacher_id " +
                "LEFT JOIN subject s ON c.subject_id = s.subject_id " +
                "WHERE c.class_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClassModel cm = new ClassModel(
                            rs.getInt("class_id"),
                            rs.getString("class_name"),
                            rs.getString("section"),
                            rs.getInt("semester"),
                            rs.getInt("teacher_id"),
                            rs.getInt("subject_id"),
                            rs.getString("room_no")
                    );
                    cm.setTeacherName(rs.getString("teacher_name"));
                    cm.setSubjectName(rs.getString("subject_name"));
                    cm.setSubjectCode(rs.getString("subject_code"));
                    return cm;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addClass(ClassModel c) {
        String sql = "INSERT INTO class (class_name, section, semester, teacher_id, subject_id, room_no) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getClassName());
            ps.setString(2, c.getSection());
            ps.setInt(3, c.getSemester());
            ps.setInt(4, c.getTeacherId());
            ps.setInt(5, c.getSubjectId());
            ps.setString(6, (c.getRoomNo() != null && !c.getRoomNo().isEmpty()) ? c.getRoomNo() : "Room 101");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateClass(ClassModel c) {
        String sql = "UPDATE class SET class_name = ?, section = ?, semester = ?, teacher_id = ?, subject_id = ?, room_no = ? WHERE class_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getClassName());
            ps.setString(2, c.getSection());
            ps.setInt(3, c.getSemester());
            ps.setInt(4, c.getTeacherId());
            ps.setInt(5, c.getSubjectId());
            ps.setString(6, c.getRoomNo());
            ps.setInt(7, c.getClassId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteClass(int id) {
        String sql = "DELETE FROM class WHERE class_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

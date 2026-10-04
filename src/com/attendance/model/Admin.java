package com.attendance.model;

public class Admin extends User {
    public Admin() {
        setRole("ADMIN");
    }

    public Admin(int id, String name, String email, String password, String role) {
        super(id, name, email, password, (role == null || role.isEmpty()) ? "ADMIN" : role);
    }
}

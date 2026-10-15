package com.mycompany.cinechain.model;

import java.io.Serializable;

/**
 * Nguoi dung dang nhap. Phan login (FE-01) can luu doi tuong nay vao session
 * voi key User.SESSION_KEY sau khi dang nhap thanh cong.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_KEY = "user";

    private final int id;
    private final String account, fullName, email, role;
    private final Integer branchId;

    public User(int id, String account, String fullName, String email, String role, Integer branchId) {
        this.id = id;
        this.account = account;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.branchId = branchId;
    }

    public int getId() { return id; }
    public String getAccount() { return account; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Integer getBranchId() { return branchId; }

    public boolean isStaff() {
        return "STAFF".equals(role) || "MANAGER".equals(role) || "ADMIN".equals(role);
    }
}

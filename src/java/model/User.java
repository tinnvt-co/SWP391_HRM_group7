package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    public enum Gender { Male, Female, Other }

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private int roleId;
    private boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Role role;

    public User() {}

    public User(int userId, String username, String passwordHash, String fullName,
                String email, String phone, Gender gender, LocalDate dateOfBirth,
                String address, int roleId, boolean isActive,
                LocalDateTime lastLogin, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.email        = email;
        this.phone        = phone;
        this.gender       = gender;
        this.dateOfBirth  = dateOfBirth;
        this.address      = address;
        this.roleId       = roleId;
        this.isActive     = isActive;
        this.lastLogin    = lastLogin;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    public int getUserId()                              { return userId; }
    public void setUserId(int userId)                   { this.userId = userId; }

    public String getUsername()                         { return username; }
    public void setUsername(String username)            { this.username = username; }

    public String getPasswordHash()                     { return passwordHash; }
    public void setPasswordHash(String passwordHash)    { this.passwordHash = passwordHash; }

    public String getFullName()                         { return fullName; }
    public void setFullName(String fullName)            { this.fullName = fullName; }

    public String getEmail()                            { return email; }
    public void setEmail(String email)                  { this.email = email; }

    public String getPhone()                            { return phone; }
    public void setPhone(String phone)                  { this.phone = phone; }

    public Gender getGender()                           { return gender; }
    public void setGender(Gender gender)                { this.gender = gender; }

    public LocalDate getDateOfBirth()                   { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth)   { this.dateOfBirth = dateOfBirth; }

    public String getAddress()                          { return address; }
    public void setAddress(String address)              { this.address = address; }

    public int getRoleId()                              { return roleId; }
    public void setRoleId(int roleId)                   { this.roleId = roleId; }

    public boolean isActive()                           { return isActive; }
    public void setActive(boolean isActive)             { this.isActive = isActive; }

    public LocalDateTime getLastLogin()                 { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin)   { this.lastLogin = lastLogin; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    public Role getRole()                               { return role; }
    public void setRole(Role role)                      { this.role = role; }
}

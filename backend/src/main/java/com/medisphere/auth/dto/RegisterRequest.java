package com.medisphere.auth.dto;

import com.medisphere.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests.
 * Accessible only to users with the {@code ADMIN} role.
 */
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    private String linkedPatientId;

    private String linkedProviderId;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password, Role role,
                           String linkedPatientId, String linkedProviderId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.linkedPatientId = linkedPatientId;
        this.linkedProviderId = linkedProviderId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getLinkedPatientId() {
        return linkedPatientId;
    }

    public void setLinkedPatientId(String linkedPatientId) {
        this.linkedPatientId = linkedPatientId;
    }

    public String getLinkedProviderId() {
        return linkedProviderId;
    }

    public void setLinkedProviderId(String linkedProviderId) {
        this.linkedProviderId = linkedProviderId;
    }
}

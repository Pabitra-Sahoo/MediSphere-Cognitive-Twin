package com.medisphere.auth.dto;

import com.medisphere.auth.model.Role;

import java.util.List;

/**
 * Safe user data transfer object returned by authentication and user endpoints.
 *
 * <p>Never exposes passwords, hashes, or sensitive credentials.</p>
 */
public class UserDTO {

    private String id;
    private String username;
    private String email;
    private Role role;
    private String linkedPatientId;
    private String linkedProviderId;
    private List<String> scopes;

    public UserDTO() {
    }

    public UserDTO(String id, String username, String email, Role role,
                   String linkedPatientId, String linkedProviderId, List<String> scopes) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.linkedPatientId = linkedPatientId;
        this.linkedProviderId = linkedProviderId;
        this.scopes = scopes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}

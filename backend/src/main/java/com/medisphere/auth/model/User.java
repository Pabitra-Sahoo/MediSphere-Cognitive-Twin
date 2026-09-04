package com.medisphere.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * User document representing an authenticated identity in MediSphere.
 *
 * <p>Persisted in the {@code users} collection in MongoDB.
 * Passwords are stored exclusively as secure cryptographic hashes (BCrypt)
 * and must never be exposed or returned in API responses.</p>
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    /**
     * If the user is a PATIENT, links directly to the patient's record id.
     */
    private String linkedPatientId;

    /**
     * If the user is a PROVIDER, links directly to the provider identifier.
     */
    private String linkedProviderId;

    private Instant createdAt;

    private Instant updatedAt;

    private boolean active = true;

    public User() {
    }

    public User(String username, String email, String passwordHash, Role role,
                String linkedPatientId, String linkedProviderId) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.linkedPatientId = linkedPatientId;
        this.linkedProviderId = linkedProviderId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.active = true;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

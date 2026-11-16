package com.psicology.backend.domain.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public User() {}

    public User(UUID id, String name, String email, String passwordHash, Role role, boolean enabled, Instant createdAt) {
        this.id = id; this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = role; this.enabled = enabled; this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final User u = new User();
        public Builder id(UUID v){u.id=v;return this;}
        public Builder name(String v){u.name=v;return this;}
        public Builder email(String v){u.email=v;return this;}
        public Builder passwordHash(String v){u.passwordHash=v;return this;}
        public Builder role(Role v){u.role=v;return this;}
        public Builder enabled(boolean v){u.enabled=v;return this;}
        public Builder createdAt(Instant v){u.createdAt=v;return this;}
        public User build(){return u;}
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

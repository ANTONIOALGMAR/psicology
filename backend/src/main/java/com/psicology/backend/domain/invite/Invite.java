package com.psicology.backend.domain.invite;

import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
public class Invite {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String code;
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;
    private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable = false)
    private Instant createdAt;

    public Invite() {}
    public Invite(UUID id, String code, String email, Role role, User createdBy, User usedBy, Instant expiresAt, Instant usedAt, Instant createdAt) {
        this.id=id; this.code=code; this.email=email; this.role=role; this.createdBy=createdBy; this.usedBy=usedBy; this.expiresAt=expiresAt; this.usedAt=usedAt; this.createdAt=createdAt;
    }
    public static Builder builder(){ return new Builder(); }
    public static class Builder {
        private final Invite i = new Invite();
        public Builder id(UUID v){i.id=v;return this;}
        public Builder code(String v){i.code=v;return this;}
        public Builder email(String v){i.email=v;return this;}
        public Builder role(Role v){i.role=v;return this;}
        public Builder createdBy(User v){i.createdBy=v;return this;}
        public Builder usedBy(User v){i.usedBy=v;return this;}
        public Builder expiresAt(Instant v){i.expiresAt=v;return this;}
        public Builder usedAt(Instant v){i.usedAt=v;return this;}
        public Builder createdAt(Instant v){i.createdAt=v;return this;}
        public Invite build(){return i;}
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isValid() {
        return usedAt == null && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getUsedBy() { return usedBy; }
    public void setUsedBy(User usedBy) { this.usedBy = usedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

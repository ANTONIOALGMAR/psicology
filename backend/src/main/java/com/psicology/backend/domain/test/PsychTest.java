package com.psicology.backend.domain.test;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tests")
public class PsychTest {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String type; // PSYCHOLOGICAL, VOCATIONAL
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

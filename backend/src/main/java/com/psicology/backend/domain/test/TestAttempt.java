package com.psicology.backend.domain.test;

import com.psicology.backend.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private PsychTest test;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    @Column(nullable = false)
    private String status; // IN_PROGRESS, COMPLETED
    private Integer score;
    @Column(name = "result_summary", columnDefinition = "text")
    private String resultSummary;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (startedAt == null) startedAt = Instant.now(); if (status == null) status = "IN_PROGRESS"; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PsychTest getTest() { return test; }
    public void setTest(PsychTest test) { this.test = test; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

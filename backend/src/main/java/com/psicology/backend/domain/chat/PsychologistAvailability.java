package com.psicology.backend.domain.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "psychologist_availability")
public class PsychologistAvailability {
    @Id
    @Column(name = "psychologist_id")
    private UUID psychologistId;

    @Column(nullable = false)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime = LocalTime.of(9, 0);

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime = LocalTime.of(18, 0);

    @Column(name = "weekend_enabled", nullable = false)
    private boolean weekendEnabled = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public UUID getPsychologistId() { return psychologistId; }
    public void setPsychologistId(UUID psychologistId) { this.psychologistId = psychologistId; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public boolean isWeekendEnabled() { return weekendEnabled; }
    public void setWeekendEnabled(boolean weekendEnabled) { this.weekendEnabled = weekendEnabled; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

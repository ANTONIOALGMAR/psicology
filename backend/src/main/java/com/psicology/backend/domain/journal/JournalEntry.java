package com.psicology.backend.domain.journal;

import com.psicology.backend.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    private String mood;
    @Column(name = "entry_date", nullable = false)
    private Instant entryDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public JournalEntry() {}
    public JournalEntry(UUID id, User patient, String title, String content, String mood, Instant entryDate, Instant createdAt, Instant updatedAt) {
        this.id=id; this.patient=patient; this.title=title; this.content=content; this.mood=mood; this.entryDate=entryDate; this.createdAt=createdAt; this.updatedAt=updatedAt;
    }
    public static Builder builder(){ return new Builder(); }
    public static class Builder {
        private final JournalEntry e = new JournalEntry();
        public Builder id(UUID v){e.id=v;return this;}
        public Builder patient(User v){e.patient=v;return this;}
        public Builder title(String v){e.title=v;return this;}
        public Builder content(String v){e.content=v;return this;}
        public Builder mood(String v){e.mood=v;return this;}
        public Builder entryDate(Instant v){e.entryDate=v;return this;}
        public Builder createdAt(Instant v){e.createdAt=v;return this;}
        public Builder updatedAt(Instant v){e.updatedAt=v;return this;}
        public JournalEntry build(){return e;}
    }

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (entryDate == null) entryDate = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    public Instant getEntryDate() { return entryDate; }
    public void setEntryDate(Instant entryDate) { this.entryDate = entryDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

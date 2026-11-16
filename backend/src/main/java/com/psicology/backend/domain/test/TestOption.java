package com.psicology.backend.domain.test;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_options")
public class TestOption {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private TestQuestion question;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false)
    private int value;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TestQuestion getQuestion() { return question; }
    public void setQuestion(TestQuestion question) { this.question = question; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}

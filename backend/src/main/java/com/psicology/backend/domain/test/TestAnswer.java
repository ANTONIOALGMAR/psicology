package com.psicology.backend.domain.test;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "test_answers")
public class TestAnswer {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private TestQuestion question;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private TestOption option;
    @Column(name = "text_answer", columnDefinition = "text")
    private String textAnswer;
    private Integer value;

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TestAttempt getAttempt() { return attempt; }
    public void setAttempt(TestAttempt attempt) { this.attempt = attempt; }
    public TestQuestion getQuestion() { return question; }
    public void setQuestion(TestQuestion question) { this.question = question; }
    public TestOption getOption() { return option; }
    public void setOption(TestOption option) { this.option = option; }
    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}

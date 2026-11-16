package com.psicology.backend.domain.test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_questions")
public class TestQuestion {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private PsychTest test;
    @Column(nullable = false)
    private int position;
    @Column(nullable = false, columnDefinition = "text")
    private String prompt;
    @Column(nullable = false)
    private String kind = "SCALE";
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("value ASC")
    private List<TestOption> options = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PsychTest getTest() { return test; }
    public void setTest(PsychTest test) { this.test = test; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public List<TestOption> getOptions() { return options; }
    public void setOptions(List<TestOption> options) { this.options = options; }
}

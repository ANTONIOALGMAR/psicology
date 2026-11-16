package com.psicology.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, UUID> {
    List<TestQuestion> findByTestIdOrderByPositionAsc(UUID testId);
}

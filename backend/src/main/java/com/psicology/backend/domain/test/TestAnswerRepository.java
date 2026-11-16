package com.psicology.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestAnswerRepository extends JpaRepository<TestAnswer, UUID> {
    List<TestAnswer> findByAttemptId(UUID attemptId);
    Optional<TestAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
    void deleteByAttemptId(UUID attemptId);
}

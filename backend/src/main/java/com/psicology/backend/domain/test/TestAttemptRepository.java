package com.psicology.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {
    List<TestAttempt> findByPatientIdOrderByStartedAtDesc(UUID patientId);
    Optional<TestAttempt> findByIdAndPatientId(UUID id, UUID patientId);
    Optional<TestAttempt> findFirstByPatientIdAndTestIdAndStatusOrderByStartedAtDesc(UUID patientId, UUID testId, String status);
}

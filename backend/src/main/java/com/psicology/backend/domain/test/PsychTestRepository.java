package com.psicology.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PsychTestRepository extends JpaRepository<PsychTest, UUID> {
    List<PsychTest> findByActiveTrueOrderByCreatedAtDesc();
    List<PsychTest> findAllByOrderByCreatedAtDesc();
    long countByActiveTrue();
}

package com.psicology.backend.domain.test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TestOptionRepository extends JpaRepository<TestOption, UUID> {
    List<TestOption> findByQuestionIdIn(List<UUID> questionIds);
}

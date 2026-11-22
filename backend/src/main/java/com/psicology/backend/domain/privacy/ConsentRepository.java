package com.psicology.backend.domain.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {
    List<Consent> findByUserIdOrderByKindAscCreatedAtDesc(UUID userId);
    Optional<Consent> findFirstByUserIdAndKindAndVersionOrderByCreatedAtDesc(UUID userId, String kind, String version);
}

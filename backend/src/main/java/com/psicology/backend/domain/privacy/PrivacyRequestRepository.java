package com.psicology.backend.domain.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrivacyRequestRepository extends JpaRepository<PrivacyRequest, UUID> {
    List<PrivacyRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);
    List<PrivacyRequest> findByStatusOrderByRequestedAtAsc(String status);
    long countByStatus(String status);
    boolean existsByUserIdAndTypeAndStatus(UUID userId, String type, String status);
}

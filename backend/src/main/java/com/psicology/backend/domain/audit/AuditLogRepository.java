package com.psicology.backend.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    long countByCreatedAtAfter(Instant since);

    @Query("SELECT a FROM AuditLog a WHERE (:action IS NULL OR a.action = :action) AND " +
           "(:q IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) OR " +
           "LOWER(a.resourceId) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))))")
    Page<AuditLog> search(@Param("action") String action, @Param("q") String q, Pageable pageable);
}

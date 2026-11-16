package com.psicology.backend.audit;

import com.psicology.backend.domain.audit.AuditLog;
import com.psicology.backend.domain.audit.AuditLogRepository;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(AppUserDetails actor, String action, String resourceType, String resourceId, UUID targetUserId, String details, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        if (actor != null && actor.getUser() != null) {
            log.setActorId(actor.getId());
            log.setActorEmail(actor.getUser().getEmail());
            log.setActorRole(actor.getUser().getRole() != null ? actor.getUser().getRole().name() : null);
        }
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setTargetUserId(targetUserId);
        log.setDetails(details);
        log.setIpAddress(extractIp(request));
        repo.save(log);
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}

package com.psicology.backend.dto;

import com.psicology.backend.domain.privacy.Consent;
import com.psicology.backend.domain.privacy.PrivacyRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PrivacyDtos {
    public record ConsentRequest(@NotBlank String kind, @NotNull Boolean accepted) {}
    public record ConsentResponse(UUID id, String kind, String version, boolean accepted,
                                  Instant acceptedAt, Instant createdAt) {
        public static ConsentResponse of(Consent c) {
            return new ConsentResponse(c.getId(), c.getKind(), c.getVersion(), c.isAccepted(),
                    c.getAcceptedAt(), c.getCreatedAt());
        }
    }

    public record PrivacyRequestCreate(@NotBlank String type, String details) {}
    public record PrivacyRequestResponse(UUID id, UUID userId, String type, String status, String details,
                                         Instant requestedAt, Instant resolvedAt) {
        public static PrivacyRequestResponse of(PrivacyRequest r) {
            return new PrivacyRequestResponse(r.getId(), r.getUserId(), r.getType(), r.getStatus(),
                    r.getDetails(), r.getRequestedAt(), r.getResolvedAt());
        }
    }
    public record PrivacyOverview(List<ConsentResponse> consents, List<PrivacyRequestResponse> requests) {}
}

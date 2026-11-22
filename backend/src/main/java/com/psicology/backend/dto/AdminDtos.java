package com.psicology.backend.dto;

import com.psicology.backend.domain.audit.AuditLog;
import com.psicology.backend.domain.test.PsychTest;
import com.psicology.backend.domain.test.TestQuestion;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminDtos {
    public record UserResponse(UUID id, String name, String email, Role role, boolean enabled, Instant createdAt) {
        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
        }
    }
    public record CreateUserRequest(@NotBlank String name, @NotBlank String email,
                                    @Size(min = 8) String password, @NotNull Role role) {}
    public record UpdateRoleRequest(@NotNull Role role) {}
    public record UpdateEnabledRequest(@NotNull Boolean enabled) {}
    public record ResetPasswordRequest(@NotBlank @Size(min = 8) String newPassword) {}

    public record TestResponse(UUID id, String type, String title, String description, boolean active,
                               int questionCount, Instant createdAt) {
        public static TestResponse of(PsychTest t, int questionCount) {
            return new TestResponse(t.getId(), t.getType(), t.getTitle(), t.getDescription(),
                    t.isActive(), questionCount, t.getCreatedAt());
        }
    }
    public record TestUpsertRequest(@NotBlank String type, @NotBlank String title, String description, Boolean active) {}
    public record OptionRequest(@NotBlank String label, @NotNull Integer value) {}
    public record QuestionRequest(@NotBlank String prompt, String kind, Integer position,
                                  List<OptionRequest> options) {}
    public record OptionResponse(UUID id, String label, int value) {}
    public record QuestionResponse(UUID id, int position, String prompt, String kind,
                                   List<OptionResponse> options) {
        public static QuestionResponse of(TestQuestion q) {
            List<OptionResponse> options = q.getOptions().stream()
                    .map(o -> new OptionResponse(o.getId(), o.getLabel(), o.getValue())).toList();
            return new QuestionResponse(q.getId(), q.getPosition(), q.getPrompt(), q.getKind(), options);
        }
    }

    public record AuditResponse(UUID id, String actorEmail, String actorRole, String action,
                                String resourceType, String resourceId, UUID targetUserId,
                                String details, String ipAddress, Instant createdAt) {
        public static AuditResponse of(AuditLog l) {
            return new AuditResponse(l.getId(), l.getActorEmail(), l.getActorRole(), l.getAction(),
                    l.getResourceType(), l.getResourceId(), l.getTargetUserId(), l.getDetails(),
                    l.getIpAddress(), l.getCreatedAt());
        }
    }
    public record UpdatePrivacyRequestStatus(@NotBlank String status) {}
    public record PagedResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
        public static <T> PagedResponse<T> of(Page<T> page) {
            return new PagedResponse<>(page.getContent(), page.getTotalElements(),
                    page.getTotalPages(), page.getNumber(), page.getSize());
        }
    }
    public record OverviewResponse(long users, long patients, long psychologists, long admins,
                                   long activeTests, long pendingPrivacyRequests, long auditEventsLast7Days) {}
}

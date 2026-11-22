package com.psicology.backend.privacy;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.comment.JournalCommentRepository;
import com.psicology.backend.domain.journal.JournalAttachment;
import com.psicology.backend.domain.journal.JournalAttachmentRepository;
import com.psicology.backend.domain.journal.JournalEntry;
import com.psicology.backend.domain.journal.JournalEntryRepository;
import com.psicology.backend.domain.privacy.Consent;
import com.psicology.backend.domain.privacy.ConsentRepository;
import com.psicology.backend.domain.privacy.PrivacyRequest;
import com.psicology.backend.domain.privacy.PrivacyRequestRepository;
import com.psicology.backend.domain.test.TestAnswer;
import com.psicology.backend.domain.test.TestAnswerRepository;
import com.psicology.backend.domain.test.TestAttempt;
import com.psicology.backend.domain.test.TestAttemptRepository;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.PrivacyDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PrivacyService {
    public static final String CURRENT_VERSION = "2026-11";
    public static final Set<String> ALLOWED_KINDS = Set.of("TERMS", "PRIVACY", "DATA_SHARING");
    public static final Set<String> ALLOWED_REQUEST_TYPES = Set.of("DATA_EXPORT", "DATA_DELETION");

    private final ConsentRepository consentRepo;
    private final PrivacyRequestRepository privacyRequestRepo;
    private final UserRepository userRepo;
    private final JournalEntryRepository journalRepo;
    private final JournalAttachmentRepository attachmentRepo;
    private final JournalCommentRepository commentRepo;
    private final TestAttemptRepository attemptRepo;
    private final TestAnswerRepository answerRepo;
    private final AuditService auditService;

    public PrivacyService(ConsentRepository consentRepo, PrivacyRequestRepository privacyRequestRepo,
                          UserRepository userRepo, JournalEntryRepository journalRepo,
                          JournalAttachmentRepository attachmentRepo, JournalCommentRepository commentRepo,
                          TestAttemptRepository attemptRepo, TestAnswerRepository answerRepo,
                          AuditService auditService) {
        this.consentRepo = consentRepo;
        this.privacyRequestRepo = privacyRequestRepo;
        this.userRepo = userRepo;
        this.journalRepo = journalRepo;
        this.attachmentRepo = attachmentRepo;
        this.commentRepo = commentRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PrivacyOverview overview(UUID userId) {
        List<ConsentResponse> consents = consentRepo.findByUserIdOrderByKindAscCreatedAtDesc(userId).stream()
                .map(ConsentResponse::of).toList();
        List<PrivacyRequestResponse> requests = privacyRequestRepo.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .map(PrivacyRequestResponse::of).toList();
        return new PrivacyOverview(consents, requests);
    }

    @Transactional
    public ConsentResponse recordConsent(UUID userId, String kind, boolean accepted, HttpServletRequest request) {
        if (kind == null || !ALLOWED_KINDS.contains(kind)) {
            throw new IllegalArgumentException("Tipo de consentimento inválido");
        }
        Consent consent = consentRepo.findFirstByUserIdAndKindAndVersionOrderByCreatedAtDesc(userId, kind, CURRENT_VERSION)
                .orElseGet(() -> {
                    Consent c = new Consent();
                    c.setUserId(userId);
                    c.setKind(kind);
                    c.setVersion(CURRENT_VERSION);
                    return c;
                });
        consent.setAccepted(accepted);
        consent.setAcceptedAt(Instant.now());
        consentRepo.save(consent);
        String action = accepted ? "CONSENT_ACCEPTED" : "CONSENT_REVOKED";
        auditService.record(null, action, "CONSENT", kind + ":" + CURRENT_VERSION, userId,
                "Registro de consentimento pelo próprio titular", request);
        return ConsentResponse.of(consent);
    }

    /** Usado no cadastro, quando o titular ainda não está autenticado. */
    @Transactional
    public void recordRegistrationConsents(UUID userId, Boolean acceptTerms, Boolean acceptPrivacy, HttpServletRequest request) {
        if (Boolean.TRUE.equals(acceptTerms)) recordConsent(userId, "TERMS", true, request);
        if (Boolean.TRUE.equals(acceptPrivacy)) recordConsent(userId, "PRIVACY", true, request);
    }

    @Transactional
    public PrivacyRequestResponse createRequest(UUID userId, String type, String details, HttpServletRequest request) {
        if (type == null || !ALLOWED_REQUEST_TYPES.contains(type)) {
            throw new IllegalArgumentException("Tipo de solicitação inválido");
        }
        if (privacyRequestRepo.existsByUserIdAndTypeAndStatus(userId, type, "PENDING")) {
            throw new IllegalArgumentException("Você já possui uma solicitação pendente deste tipo");
        }
        PrivacyRequest pr = new PrivacyRequest();
        pr.setUserId(userId);
        pr.setType(type);
        pr.setStatus("PENDING");
        pr.setDetails(details);
        privacyRequestRepo.save(pr);
        auditService.record(null, "PRIVACY_REQUEST_CREATED", "PRIVACY_REQUEST", pr.getId().toString(), userId,
                type, request);
        return PrivacyRequestResponse.of(pr);
    }

    /** Portabilidade: exporta os dados do próprio titular em formato estruturado. */
    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now());
        data.put("user", Map.of(
                "id", user.getId(), "name", user.getName(), "email", user.getEmail(),
                "role", user.getRole().name(), "createdAt", user.getCreatedAt()));
        data.put("consents", overview(userId).consents());
        data.put("privacyRequests", overview(userId).requests());

        List<JournalEntry> entries = journalRepo.findByPatientId(userId, Sort.by("entryDate").descending());
        List<UUID> entryIds = entries.stream().map(JournalEntry::getId).toList();
        Map<UUID, List<JournalAttachment>> attachmentsByEntry = entryIds.isEmpty()
                ? Map.of()
                : attachmentRepo.findByEntryIdIn(entryIds).stream()
                    .collect(java.util.stream.Collectors.groupingBy(a -> a.getEntry().getId()));
        Map<UUID, List<String>> commentsByEntry = entryIds.isEmpty()
                ? Map.of()
                : commentRepo.findByEntryIdInOrderByCreatedAtAsc(entryIds).stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            c -> c.getEntry().getId(),
                            java.util.stream.Collectors.mapping(
                                    c -> c.getAuthor().getName() + " (" + c.getAuthor().getRole().name() + "): " + c.getContent(),
                                    java.util.stream.Collectors.toList())));

        List<Map<String, Object>> journal = new ArrayList<>();
        for (JournalEntry e : entries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("title", e.getTitle());
            item.put("content", e.getContent());
            item.put("mood", e.getMood());
            item.put("entryDate", e.getEntryDate());
            item.put("createdAt", e.getCreatedAt());
            item.put("updatedAt", e.getUpdatedAt());
            item.put("attachments", attachmentsByEntry.getOrDefault(e.getId(), List.of()).stream().map(a -> {
                Map<String, Object> att = new LinkedHashMap<>();
                att.put("id", a.getId());
                att.put("type", a.getType());
                att.put("originalName", a.getOriginalName());
                att.put("mimeType", a.getMimeType());
                att.put("createdAt", a.getCreatedAt());
                return att;
            }).toList());
            item.put("comments", commentsByEntry.getOrDefault(e.getId(), List.of()));
            journal.add(item);
        }
        data.put("journalEntries", journal);

        List<Map<String, Object>> attempts = new ArrayList<>();
        for (TestAttempt a : attemptRepo.findByPatientIdOrderByStartedAtDesc(userId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("testId", a.getTest().getId());
            item.put("testTitle", a.getTest().getTitle());
            item.put("status", a.getStatus());
            item.put("score", a.getScore());
            item.put("resultSummary", a.getResultSummary());
            item.put("startedAt", a.getStartedAt());
            item.put("completedAt", a.getCompletedAt());
            List<Map<String, Object>> answers = new ArrayList<>();
            for (TestAnswer ans : answerRepo.findByAttemptId(a.getId())) {
                Map<String, Object> answer = new LinkedHashMap<>();
                answer.put("question", ans.getQuestion().getPrompt());
                answer.put("option", ans.getOption() != null ? ans.getOption().getLabel() : null);
                answer.put("textAnswer", ans.getTextAnswer());
                answer.put("value", ans.getValue());
                answers.add(answer);
            }
            item.put("answers", answers);
            attempts.add(item);
        }
        data.put("testAttempts", attempts);
        return data;
    }
}

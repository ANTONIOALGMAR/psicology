package com.psicology.backend.admin;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.audit.AuditLogRepository;
import com.psicology.backend.domain.privacy.PrivacyRequest;
import com.psicology.backend.domain.privacy.PrivacyRequestRepository;
import com.psicology.backend.domain.test.*;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AdminDtos.*;
import com.psicology.backend.dto.PrivacyDtos.PrivacyRequestResponse;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminService {
    private static final Set<String> TEST_TYPES = Set.of("PSYCHOLOGICAL", "VOCATIONAL");
    private static final Set<String> REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final UserRepository userRepo;
    private final PsychTestRepository testRepo;
    private final TestQuestionRepository questionRepo;
    private final AuditLogRepository auditRepo;
    private final PrivacyRequestRepository privacyRequestRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(UserRepository userRepo, PsychTestRepository testRepo,
                        TestQuestionRepository questionRepo, AuditLogRepository auditRepo,
                        PrivacyRequestRepository privacyRequestRepo, PasswordEncoder passwordEncoder,
                        AuditService auditService) {
        this.userRepo = userRepo;
        this.testRepo = testRepo;
        this.questionRepo = questionRepo;
        this.auditRepo = auditRepo;
        this.privacyRequestRepo = privacyRequestRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> users(int page, int size, String q, Role role) {
        return userRepo.search(role, blankToNull(q), pageable(page, size)).map(UserResponse::of);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req, AppUserDetails actor, HttpServletRequest request) {
        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) throw new IllegalArgumentException("E-mail já cadastrado");
        if (req.password() == null || req.password().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres");
        }
        User user = User.builder()
                .name(req.name().trim()).email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role()).enabled(true).build();
        userRepo.save(user);
        auditService.record(actor, "ADMIN_USER_CREATED", "USER", user.getId().toString(), user.getId(),
                "Perfil: " + user.getRole().name(), request);
        return UserResponse.of(user);
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UpdateRoleRequest req, AppUserDetails actor, HttpServletRequest request) {
        User user = findUser(userId);
        guardAdminChange(user, req.role(), user.isEnabled(), actor);
        Role old = user.getRole();
        user.setRole(req.role());
        userRepo.save(user);
        auditService.record(actor, "ADMIN_USER_ROLE_CHANGED", "USER", userId.toString(), userId,
                old + " -> " + req.role(), request);
        return UserResponse.of(user);
    }

    @Transactional
    public UserResponse setEnabled(UUID userId, UpdateEnabledRequest req, AppUserDetails actor, HttpServletRequest request) {
        User user = findUser(userId);
        guardAdminChange(user, user.getRole(), Boolean.TRUE.equals(req.enabled()), actor);
        user.setEnabled(Boolean.TRUE.equals(req.enabled()));
        userRepo.save(user);
        auditService.record(actor, "ADMIN_USER_ENABLED_CHANGED", "USER", userId.toString(), userId,
                "Enabled=" + user.isEnabled(), request);
        return UserResponse.of(user);
    }

    @Transactional
    public void resetPassword(UUID userId, ResetPasswordRequest req, AppUserDetails actor, HttpServletRequest request) {
        User user = findUser(userId);
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
        auditService.record(actor, "ADMIN_USER_PASSWORD_RESET", "USER", userId.toString(), userId,
                "Senha redefinida pelo administrador", request);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> tests() {
        return testRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(t -> TestResponse.of(t, questionRepo.findByTestIdOrderByPositionAsc(t.getId()).size()))
                .toList();
    }

    @Transactional
    public TestResponse createTest(TestUpsertRequest req, AppUserDetails actor, HttpServletRequest request) {
        String type = validateType(req.type());
        PsychTest test = new PsychTest();
        applyTestData(test, type, req);
        testRepo.save(test);
        auditService.record(actor, "ADMIN_TEST_CREATED", "TEST", test.getId().toString(), null,
                test.getTitle(), request);
        return TestResponse.of(test, 0);
    }

    @Transactional
    public TestResponse updateTest(UUID id, TestUpsertRequest req, AppUserDetails actor, HttpServletRequest request) {
        PsychTest test = findTest(id);
        applyTestData(test, validateType(req.type()), req);
        testRepo.save(test);
        auditService.record(actor, "ADMIN_TEST_UPDATED", "TEST", id.toString(), null, test.getTitle(), request);
        return TestResponse.of(test, questionRepo.findByTestIdOrderByPositionAsc(id).size());
    }

    @Transactional
    public TestResponse setTestActive(UUID id, boolean active, AppUserDetails actor, HttpServletRequest request) {
        PsychTest test = findTest(id);
        test.setActive(active);
        testRepo.save(test);
        auditService.record(actor, "ADMIN_TEST_ACTIVE_CHANGED", "TEST", id.toString(), null,
                "Active=" + active, request);
        return TestResponse.of(test, questionRepo.findByTestIdOrderByPositionAsc(id).size());
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> questions(UUID testId) {
        findTest(testId);
        return questionRepo.findByTestIdOrderByPositionAsc(testId).stream().map(QuestionResponse::of).toList();
    }

    @Transactional
    public QuestionResponse addQuestion(UUID testId, QuestionRequest req, AppUserDetails actor, HttpServletRequest request) {
        PsychTest test = findTest(testId);
        TestQuestion q = new TestQuestion();
        q.setTest(test);
        q.setPrompt(req.prompt().trim());
        q.setKind(req.kind() == null || req.kind().isBlank() ? "SCALE" : req.kind().trim());
        int position = req.position() != null ? req.position()
                : questionRepo.findByTestIdOrderByPositionAsc(testId).size() + 1;
        q.setPosition(position);
        if (req.options() != null) {
            for (OptionRequest o : req.options()) {
                TestOption option = new TestOption();
                option.setQuestion(q);
                option.setLabel(o.label().trim());
                option.setValue(o.value());
                q.getOptions().add(option);
            }
        }
        questionRepo.save(q);
        auditService.record(actor, "ADMIN_TEST_QUESTION_ADDED", "TEST", testId.toString(), null,
                "Questão " + position, request);
        return QuestionResponse.of(q);
    }

    @Transactional
    public void deleteQuestion(UUID testId, UUID questionId, AppUserDetails actor, HttpServletRequest request) {
        TestQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Questão não encontrada"));
        if (!q.getTest().getId().equals(testId)) {
            throw new IllegalArgumentException("Questão não pertence a este teste");
        }
        questionRepo.delete(q);
        auditService.record(actor, "ADMIN_TEST_QUESTION_REMOVED", "TEST", testId.toString(), null,
                "Questão " + questionId, request);
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> audit(int page, int size, String action, String q) {
        return auditRepo.search(blankToNull(action), blankToNull(q), pageable(page, size)).map(AuditResponse::of);
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequestResponse> privacyRequests(String status) {
        String filter = blankToNull(status);
        List<PrivacyRequest> list = filter == null
                ? allRequests()
                : privacyRequestRepo.findByStatusOrderByRequestedAtAsc(filter);
        return list.stream().map(PrivacyRequestResponse::of).toList();
    }

    @Transactional
    public PrivacyRequestResponse updatePrivacyRequest(UUID id, UpdatePrivacyRequestStatus req,
                                                       AppUserDetails actor, HttpServletRequest request) {
        String status = req.status() == null ? "" : req.status().trim().toUpperCase();
        if (!REQUEST_STATUSES.contains(status)) throw new IllegalArgumentException("Status inválido");
        PrivacyRequest pr = privacyRequestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        pr.setStatus(status);
        pr.setResolvedAt("PENDING".equals(status) ? null : Instant.now());
        privacyRequestRepo.save(pr);
        auditService.record(actor, "ADMIN_PRIVACY_REQUEST_UPDATED", "PRIVACY_REQUEST", id.toString(),
                pr.getUserId(), status, request);
        return PrivacyRequestResponse.of(pr);
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        return new OverviewResponse(
                userRepo.count(),
                userRepo.countByRole(Role.PATIENT),
                userRepo.countByRole(Role.PSYCHOLOGIST),
                userRepo.countByRole(Role.ADMIN),
                testRepo.countByActiveTrue(),
                privacyRequestRepo.countByStatus("PENDING"),
                auditRepo.countByCreatedAtAfter(Instant.now().minus(7, ChronoUnit.DAYS)));
    }

    private List<PrivacyRequest> allRequests() {
        return privacyRequestRepo.findAll(org.springframework.data.domain.Sort.by("requestedAt").ascending());
    }

    private User findUser(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private PsychTest findTest(UUID id) {
        return testRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Teste não encontrado"));
    }

    /** Evita que o próprio administrador se bloqueie ou que o último admin ativo seja rebaixado. */
    private void guardAdminChange(User target, Role newRole, boolean newEnabled, AppUserDetails actor) {
        if (target.getRole() == Role.ADMIN && (newRole != Role.ADMIN || !newEnabled)
                && userRepo.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Não é possível alterar o último administrador ativo");
        }
        if (actor != null && target.getId().equals(actor.getId()) && (newRole != Role.ADMIN || !newEnabled)) {
            throw new IllegalArgumentException("Não é possível alterar o próprio perfil de administrador");
        }
    }

    private String validateType(String type) {
        if (type == null || !TEST_TYPES.contains(type.trim().toUpperCase())) {
            throw new IllegalArgumentException("Tipo de teste inválido");
        }
        return type.trim().toUpperCase();
    }

    private void applyTestData(PsychTest test, String type, TestUpsertRequest req) {
        test.setType(type);
        test.setTitle(req.title().trim());
        test.setDescription(req.description());
        test.setActive(req.active() == null || req.active());
    }

    private Pageable pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize,
                org.springframework.data.domain.Sort.by("createdAt").descending());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.psicology.backend.journal;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.dto.AppointmentDtos.TransferRequest;
import com.psicology.backend.dto.AppointmentDtos.TransferResponse;
import com.psicology.backend.transfer.PatientTransferService;
import com.psicology.backend.domain.comment.JournalComment;
import com.psicology.backend.domain.audit.AuditLogRepository;
import com.psicology.backend.domain.comment.JournalCommentRepository;
import com.psicology.backend.domain.journal.JournalAttachment;
import com.psicology.backend.domain.journal.JournalAttachmentRepository;
import com.psicology.backend.domain.journal.JournalEntry;
import com.psicology.backend.domain.journal.JournalEntryRepository;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.test.TestAnswerRepository;
import com.psicology.backend.domain.test.TestAttempt;
import com.psicology.backend.domain.test.TestAttemptRepository;
import com.psicology.backend.domain.test.TestOption;
import com.psicology.backend.domain.test.TestQuestion;
import com.psicology.backend.domain.test.TestQuestionRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.security.AppUserDetails;
import com.psicology.backend.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/psychologist")
@PreAuthorize("hasRole('PSYCHOLOGIST')")
public class PsychologistController {
    private final PatientLinkRepository linkRepo;
    private final UserRepository userRepository;
    private final JournalEntryRepository journalRepo;
    private final JournalAttachmentRepository attachmentRepo;
    private final JournalCommentRepository commentRepo;
    private final FileStorageService storage;
    private final JournalController journalController;
    private final AuditService auditService;
    private final TestAttemptRepository attemptRepo;
    private final TestAnswerRepository answerRepo;
    private final TestQuestionRepository questionRepo;
    private final PatientTransferService transferService;
    private final AuditLogRepository auditRepo;

    public PsychologistController(PatientLinkRepository linkRepo, UserRepository userRepository, JournalEntryRepository journalRepo, JournalAttachmentRepository attachmentRepo, JournalCommentRepository commentRepo, FileStorageService storage, JournalController journalController, AuditService auditService, TestAttemptRepository attemptRepo, TestAnswerRepository answerRepo, TestQuestionRepository questionRepo, PatientTransferService transferService, AuditLogRepository auditRepo) {
        this.linkRepo = linkRepo;
        this.userRepository = userRepository;
        this.journalRepo = journalRepo;
        this.attachmentRepo = attachmentRepo;
        this.commentRepo = commentRepo;
        this.storage = storage;
        this.journalController = journalController;
        this.auditService = auditService;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.questionRepo = questionRepo;
        this.transferService = transferService;
        this.auditRepo = auditRepo;
    }

    public record PatientSummary(UUID id, String name, String email, String linkedAt) {}
    public record LinkRequest(String email) {}
    public record CommentRequest(@NotBlank String content) {}

    @GetMapping("/patients")
    @Transactional(readOnly = true)
    public List<PatientSummary> patients(@AuthenticationPrincipal AppUserDetails principal) {
        return linkRepo.findByPsychologistIdAndStatus(principal.getId(), "ACTIVE").stream()
                .map(l -> new PatientSummary(l.getPatient().getId(), l.getPatient().getName(), l.getPatient().getEmail(),
                        l.getCreatedAt() != null ? l.getCreatedAt().toString() : null))
                .toList();
    }

    @PostMapping("/patients/link")
    @Transactional
    public PatientSummary linkPatient(@AuthenticationPrincipal AppUserDetails principal, @RequestBody LinkRequest req, HttpServletRequest request) {
        if (req.email() == null || req.email().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail obrigatório");
        User psychologist = userRepository.findById(principal.getId()).orElseThrow();
        User patient = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));
        if (patient.getRole() != Role.PATIENT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário não é paciente");
        if (linkRepo.existsByPsychologistIdAndPatientIdAndStatus(psychologist.getId(), patient.getId(), "ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paciente já vinculado");
        }
        PatientLink link = new PatientLink();
        link.setPsychologist(psychologist);
        link.setPatient(patient);
        link.setStatus("ACTIVE");
        linkRepo.save(link);
        auditService.record(principal, "PATIENT_LINKED", "PATIENT", patient.getId().toString(), patient.getId(), "Vínculo criado", request);
        return new PatientSummary(patient.getId(), patient.getName(), patient.getEmail(), link.getCreatedAt() != null ? link.getCreatedAt().toString() : null);
    }

    @PostMapping("/patients/{patientId}/transfer")
    @Transactional
    public TransferResponse transferPatient(@PathVariable UUID patientId,
                                            @Valid @RequestBody TransferRequest req,
                                            @AuthenticationPrincipal AppUserDetails principal,
                                            HttpServletRequest request) {
        return transferService.transfer(patientId, req, principal, request);
    }

    @GetMapping("/patients/{patientId}/transfers")
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> transferHistory(@PathVariable UUID patientId,
                                                               @AuthenticationPrincipal AppUserDetails principal) {
        // Histórico restrito: apenas psicólogos que já tiveram vínculo com o paciente.
        boolean related = linkRepo.findByPatientIdAndPsychologistId(patientId, principal.getId()).isPresent();
        if (!related) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem vínculo com este paciente");
        return auditRepo.findByTargetUserIdAndActionOrderByCreatedAtDesc(patientId, "PATIENT_TRANSFERRED").stream()
                .map(l -> java.util.Map.<String, Object>of(
                        "id", l.getId(), "details", l.getDetails() == null ? "" : l.getDetails(),
                        "createdAt", l.getCreatedAt()))
                .toList();
    }

    @GetMapping("/patients/{patientId}/journal")
    @Transactional(readOnly = true)
    public List<JournalController.JournalResponse> patientJournal(@AuthenticationPrincipal AppUserDetails principal,
                                                                  @PathVariable UUID patientId,
                                                                  @RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size,
                                                                  HttpServletRequest request) {
        ensureLinked(principal.getId(), patientId);
        auditService.record(principal, "PATIENT_JOURNAL_VIEW", "JOURNAL", patientId.toString(), patientId, "Visualização de diário do paciente", request);
        List<JournalEntry> entries;
        if (page != null || size != null) {
            int p = page != null && page >= 0 ? page : 0;
            int s = size != null && size > 0 ? Math.min(size, 200) : 50;
            Page<JournalEntry> pg = journalRepo.findByPatientId(patientId, PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "entryDate")));
            entries = pg.getContent();
        } else {
            entries = journalRepo.findByPatientId(patientId, Sort.by(Sort.Direction.DESC, "entryDate"));
        }
        return journalController.toDtos(entries);
    }

    @PostMapping("/journal/{entryId}/comments")
    public JournalController.CommentResponse addComment(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID entryId, @Valid @RequestBody CommentRequest req, HttpServletRequest request) {
        JournalEntry entry = journalRepo.findById(entryId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureLinked(principal.getId(), entry.getPatient().getId());
        User author = userRepository.findById(principal.getId()).orElseThrow();
        JournalComment c = new JournalComment();
        c.setEntry(entry);
        c.setAuthor(author);
        c.setContent(req.content());
        JournalComment saved = commentRepo.save(c);
        auditService.record(principal, "JOURNAL_COMMENT_CREATE", "JOURNAL_ENTRY", entryId.toString(), entry.getPatient().getId(), "Comentário adicionado", request);
        return new JournalController.CommentResponse(saved.getId(), author.getName(), author.getRole().name(), saved.getContent(), saved.getCreatedAt());
    }

    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<Resource> getAttachmentFile(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID attachmentId, HttpServletRequest request) throws MalformedURLException {
        JournalAttachment a = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureLinked(principal.getId(), a.getEntry().getPatient().getId());
        auditService.record(principal, "ATTACHMENT_VIEW", "JOURNAL_ATTACHMENT", attachmentId.toString(), a.getEntry().getPatient().getId(), "Acesso a anexo do paciente", request);
        Resource resource = storage.loadAsResource(a.getFilePath());
        String contentType = a.getMimeType() != null ? a.getMimeType() : "application/octet-stream";
        String filename = a.getOriginalName() != null ? a.getOriginalName() : "arquivo";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .body(resource);
    }

    public record AnswerView(UUID questionId, UUID optionId, Integer value) {}
    public record AttemptView(UUID id, UUID testId, String testTitle, String status, Integer score, Integer maxScore, String resultSummary, String psychologistFeedback, Instant startedAt, Instant completedAt, List<AnswerView> answers) {}

    @GetMapping("/patients/{patientId}/attempts")
    @Transactional(readOnly = true)
    public List<AttemptView> patientAttempts(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID patientId, HttpServletRequest request) {
        ensureLinked(principal.getId(), patientId);
        auditService.record(principal, "PATIENT_ATTEMPTS_VIEW", "TEST_ATTEMPT", patientId.toString(), patientId, "Visualização de tentativas de testes do paciente", request);
        return attemptRepo.findByPatientIdOrderByStartedAtDesc(patientId).stream().map(this::toAttemptView).toList();
    }

    private AttemptView toAttemptView(TestAttempt a) {
        List<AnswerView> av = answerRepo.findByAttemptId(a.getId()).stream()
                .map(x -> new AnswerView(x.getQuestion().getId(), x.getOption() != null ? x.getOption().getId() : null, x.getValue())).toList();
        Integer maxScore = a.getMaxScore();
        String feedback = a.getPsychologistFeedback();
        if ("COMPLETED".equals(a.getStatus()) && (feedback == null || feedback.isBlank())) {
            List<TestQuestion> questions = questionRepo.findByTestIdOrderByPositionAsc(a.getTest().getId());
            maxScore = computeMaxScore(questions);
            int score = a.getScore() != null ? a.getScore() : 0;
            feedback = buildPsychologistFeedback(a.getTest(), score, maxScore, av.size(), questions.size());
        }
        return new AttemptView(a.getId(), a.getTest().getId(), a.getTest().getTitle(), a.getStatus(), a.getScore(), maxScore, a.getResultSummary(), feedback, a.getStartedAt(), a.getCompletedAt(), av);
    }

    private int computeMaxScore(List<TestQuestion> questions) {
        int total = 0;
        for (TestQuestion q : questions) {
            int qMax = q.getOptions() == null ? 0 : q.getOptions().stream().mapToInt(TestOption::getValue).max().orElse(0);
            total += Math.max(0, qMax);
        }
        return total;
    }

    private String buildPsychologistFeedback(com.psicology.backend.domain.test.PsychTest test, int score, int maxScore, int answered, int totalQuestions) {
        String instrument = test.getTitle() != null ? test.getTitle() : "Instrumento";
        String typeLabel = "PSYCHOLOGICAL".equals(test.getType()) ? "autoavaliação psicológica" : "VOCATIONAL".equals(test.getType()) ? "mapeamento vocacional exploratório" : "instrumento de apoio";
        double pct = maxScore > 0 ? (score * 100.0 / maxScore) : 0;
        String band;
        String clinicalNote;
        if ("PSYCHOLOGICAL".equals(test.getType())) {
            if (pct >= 75) { band = "Faixa alta/positiva"; clinicalNote = "Indícios de bem-estar preservado no recorte avaliado. Reforçar fatores de proteção, rotina e estratégias já eficazes."; }
            else if (pct >= 50) { band = "Faixa moderada/atenção"; clinicalNote = "Há sinais mistos. Vale explorar áreas de estresse, sono, suporte social e funcionamento diário nas sessões."; }
            else { band = "Faixa baixa/alerta clínico"; clinicalNote = "Sinais de sofrimento ou bem-estar reduzido. Priorizar escuta qualificada, avaliação de risco e plano de cuidado individualizado."; }
        } else if ("VOCATIONAL".equals(test.getType())) {
            if (pct >= 66) { band = "Interesses amplos/definidos"; clinicalNote = "O paciente apresenta respostas consistentes com interesses mais claros. Aprofundar valores, habilidades e contexto de vida para decisão profissional."; }
            else if (pct >= 33) { band = "Interesses em exploração"; clinicalNote = "Perfil indica exploração em andamento. Usar o resultado como ponto de partida para entrevista vocacional e investigação de áreas de interesse."; }
            else { band = "Interesses iniciais/pouco definidos"; clinicalNote = "Resultado sugere baixa definição no momento. Investigar barreiras, autoconhecimento, expectativas familiares e experiências prévias."; }
        } else {
            band = "Resultado registrado"; clinicalNote = "Interpretar junto ao histórico clínico e contexto atual do paciente.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Respaldo do teste: ").append(instrument).append(" (").append(typeLabel).append(").\n");
        if (test.getDescription() != null && !test.getDescription().isBlank()) sb.append("Objetivo: ").append(test.getDescription().trim()).append("\n");
        sb.append("Pontuação: ").append(score).append(maxScore > 0 ? "/" + maxScore : "").append(" (").append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.0f", pct)).append("%)");
        sb.append(" | Respostas: ").append(answered).append("/").append(totalQuestions).append(".\n");
        sb.append("Faixa interpretativa: ").append(band).append(".\n");
        sb.append("Leitura para o psicólogo: ").append(clinicalNote).append("\n");
        sb.append("Observação ética: resultado de instrumento de apoio, não constitui diagnóstico isolado. Interpretar com entrevista clínica, história e contexto do paciente.");
        return sb.toString();
    }

    @GetMapping(value = "/patients/{patientId}/attempts/export", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportPatientAttempts(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID patientId, HttpServletRequest request) {
        ensureLinked(principal.getId(), patientId);
        auditService.record(principal, "PATIENT_ATTEMPTS_EXPORT", "TEST_ATTEMPT", patientId.toString(), patientId, "Exportação de tentativas de testes do paciente", request);
        StringBuilder sb = new StringBuilder();
        sb.append("test_title,status,started_at,completed_at,score,max_score,result_summary,psychologist_feedback\n");
        for (TestAttempt a : attemptRepo.findByPatientIdOrderByStartedAtDesc(patientId)) {
            sb.append(csv(a.getTest() != null ? a.getTest().getTitle() : "")).append(',')
              .append(csv(a.getStatus())).append(',')
              .append(csv(a.getStartedAt() != null ? a.getStartedAt().toString() : "")).append(',')
              .append(csv(a.getCompletedAt() != null ? a.getCompletedAt().toString() : "")).append(',')
              .append(csv(a.getScore() != null ? a.getScore().toString() : "")).append(',')
              .append(csv(a.getMaxScore() != null ? a.getMaxScore().toString() : "")).append(',')
              .append(csv(a.getResultSummary())).append(',')
              .append(csv(a.getPsychologistFeedback()))
              .append("\n");
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tentativas-paciente.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        if (s.contains(",") || s.contains("\"") || s.contains(" ")) return "\"" + s + "\"";
        return s;
    }

    private void ensureLinked(UUID psychologistId, UUID patientId) {
        boolean ok = linkRepo.existsByPsychologistIdAndPatientIdAndStatus(psychologistId, patientId, "ACTIVE");
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paciente não vinculado");
    }
}

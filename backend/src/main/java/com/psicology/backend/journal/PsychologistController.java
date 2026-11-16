package com.psicology.backend.journal;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.comment.JournalComment;
import com.psicology.backend.domain.comment.JournalCommentRepository;
import com.psicology.backend.domain.journal.JournalAttachment;
import com.psicology.backend.domain.journal.JournalAttachmentRepository;
import com.psicology.backend.domain.journal.JournalEntry;
import com.psicology.backend.domain.journal.JournalEntryRepository;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
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

    public PsychologistController(PatientLinkRepository linkRepo, UserRepository userRepository, JournalEntryRepository journalRepo, JournalAttachmentRepository attachmentRepo, JournalCommentRepository commentRepo, FileStorageService storage, JournalController journalController, AuditService auditService) {
        this.linkRepo = linkRepo;
        this.userRepository = userRepository;
        this.journalRepo = journalRepo;
        this.attachmentRepo = attachmentRepo;
        this.commentRepo = commentRepo;
        this.storage = storage;
        this.journalController = journalController;
        this.auditService = auditService;
    }

    public record PatientSummary(UUID id, String name, String email, String linkedAt) {}
    public record LinkRequest(String email) {}
    public record CommentRequest(@NotBlank String content) {}

    @GetMapping("/patients")
    public List<PatientSummary> patients(@AuthenticationPrincipal AppUserDetails principal) {
        return linkRepo.findByPsychologistIdAndStatus(principal.getId(), "ACTIVE").stream()
                .map(l -> new PatientSummary(l.getPatient().getId(), l.getPatient().getName(), l.getPatient().getEmail(),
                        l.getCreatedAt() != null ? l.getCreatedAt().toString() : null))
                .toList();
    }

    @PostMapping("/patients/link")
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

    @GetMapping("/patients/{patientId}/journal")
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

    private void ensureLinked(UUID psychologistId, UUID patientId) {
        boolean ok = linkRepo.existsByPsychologistIdAndPatientIdAndStatus(psychologistId, patientId, "ACTIVE");
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Paciente não vinculado");
    }
}

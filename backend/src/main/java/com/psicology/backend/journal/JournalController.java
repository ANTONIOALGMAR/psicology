package com.psicology.backend.journal;

import com.psicology.backend.domain.comment.JournalComment;
import com.psicology.backend.domain.comment.JournalCommentRepository;
import com.psicology.backend.domain.journal.JournalAttachment;
import com.psicology.backend.domain.journal.JournalAttachmentRepository;
import com.psicology.backend.domain.journal.JournalEntry;
import com.psicology.backend.domain.journal.JournalEntryRepository;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.security.AppUserDetails;
import com.psicology.backend.storage.FileStorageService;
import com.psicology.backend.audit.AuditService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journal")
public class JournalController {
    private final JournalEntryRepository repo;
    private final JournalAttachmentRepository attachmentRepo;
    private final JournalCommentRepository commentRepo;
    private final PatientLinkRepository linkRepo;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final AuditService auditService;

    public JournalController(JournalEntryRepository repo, JournalAttachmentRepository attachmentRepo, JournalCommentRepository commentRepo, PatientLinkRepository linkRepo, UserRepository userRepository, FileStorageService storage, AuditService auditService) {
        this.repo = repo;
        this.attachmentRepo = attachmentRepo;
        this.commentRepo = commentRepo;
        this.linkRepo = linkRepo;
        this.userRepository = userRepository;
        this.storage = storage;
        this.auditService = auditService;
    }

    public record JournalRequest(@NotBlank String content, String title, String mood, Instant entryDate) {}
    public record AttachmentResponse(UUID id, String type, String originalName, String mimeType, String url, Instant createdAt) {}
    public record CommentResponse(UUID id, String authorName, String authorRole, String content, Instant createdAt) {}
    public record JournalResponse(UUID id, String title, String content, String mood, Instant entryDate, Instant createdAt, List<AttachmentResponse> attachments, List<CommentResponse> comments) {}

    @GetMapping
    public List<JournalResponse> list(@AuthenticationPrincipal AppUserDetails principal,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        List<JournalEntry> entries;
        if (page != null || size != null) {
            int p = page != null && page >= 0 ? page : 0;
            int s = size != null && size > 0 ? Math.min(size, 200) : 50;
            Page<JournalEntry> pg = repo.findByPatientId(principal.getId(), PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "entryDate")));
            entries = pg.getContent();
        } else {
            entries = repo.findByPatientId(principal.getId(), Sort.by(Sort.Direction.DESC, "entryDate"));
        }
        return toDtos(entries);
    }

    @PostMapping
    public JournalResponse create(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody JournalRequest req) {
        User patient = userRepository.findById(principal.getId()).orElseThrow();
        JournalEntry e = JournalEntry.builder()
                .patient(patient).title(req.title()).content(req.content()).mood(req.mood()).entryDate(req.entryDate()).build();
        JournalEntry saved = repo.save(e);
        return toDto(saved, List.of(), List.of());
    }

    @PutMapping("/{id}")
    public JournalResponse update(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id, @Valid @RequestBody JournalRequest req) {
        JournalEntry e = ownedEntry(principal.getId(), id);
        e.setTitle(req.title()); e.setContent(req.content()); e.setMood(req.mood());
        if (req.entryDate() != null) e.setEntryDate(req.entryDate());
        JournalEntry saved = repo.save(e);
        return toDto(saved, attachmentRepo.findByEntryId(saved.getId()), commentRepo.findByEntryIdOrderByCreatedAtAsc(saved.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        JournalEntry e = ownedEntry(principal.getId(), id);
        List<JournalAttachment> atts = attachmentRepo.findByEntryId(id);
        for (JournalAttachment a : atts) {
            try { storage.delete(a.getFilePath()); } catch (IOException ignored) {}
        }
        attachmentRepo.deleteAll(atts);
        repo.delete(e);
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse uploadAttachment(@AuthenticationPrincipal AppUserDetails principal,
                                               @PathVariable UUID id,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        JournalEntry e = ownedEntry(principal.getId(), id);
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String type = contentType.startsWith("audio/") ? "AUDIO" : "FILE";
        String stored = storage.store(file, "journal/" + e.getId());
        JournalAttachment a = new JournalAttachment();
        a.setEntry(e);
        a.setType(type);
        a.setFilePath(stored);
        a.setOriginalName(file.getOriginalFilename());
        a.setMimeType(contentType);
        JournalAttachment saved = attachmentRepo.save(a);
        return toAttachmentDto(saved);
    }

    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<Resource> getAttachmentFile(@AuthenticationPrincipal AppUserDetails principal,
                                                      @PathVariable UUID attachmentId,
                                                      HttpServletRequest request) throws MalformedURLException {
        JournalAttachment a = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UUID uid = principal.getId();
        UUID patientId = a.getEntry().getPatient().getId();
        boolean psychologistAccess = false;
        if (!patientId.equals(uid)) {
            boolean linked = principal.getUser().getRole() == Role.PSYCHOLOGIST
                    && linkRepo.existsByPsychologistIdAndPatientIdAndStatus(uid, patientId, "ACTIVE");
            if (!linked) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            psychologistAccess = true;
        }
        if (psychologistAccess) {
            auditService.record(principal, "ATTACHMENT_VIEW", "JOURNAL_ATTACHMENT", attachmentId.toString(), patientId, "Acesso a anexo via diário", request);
        }
        Resource resource = storage.loadAsResource(a.getFilePath());
        String contentType = a.getMimeType() != null ? a.getMimeType() : "application/octet-stream";
        String filename = a.getOriginalName() != null ? a.getOriginalName() : "arquivo";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID attachmentId) throws IOException {
        JournalAttachment a = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!a.getEntry().getPatient().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        storage.delete(a.getFilePath());
        attachmentRepo.delete(a);
    }

    private JournalEntry ownedEntry(UUID userId, UUID id) {
        JournalEntry e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!e.getPatient().getId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return e;
    }

    public List<JournalResponse> toDtos(List<JournalEntry> entries) {
        if (entries.isEmpty()) return List.of();
        List<UUID> ids = entries.stream().map(JournalEntry::getId).toList();
        Map<UUID, List<JournalAttachment>> byEntry = attachmentRepo.findByEntryIdIn(ids).stream().collect(Collectors.groupingBy(a -> a.getEntry().getId()));
        Map<UUID, List<JournalComment>> commentsByEntry = commentRepo.findByEntryIdInOrderByCreatedAtAsc(ids).stream().collect(Collectors.groupingBy(c -> c.getEntry().getId()));
        return entries.stream().map(e -> toDto(e, byEntry.getOrDefault(e.getId(), List.of()), commentsByEntry.getOrDefault(e.getId(), List.of()))).toList();
    }

    private JournalResponse toDto(JournalEntry e, List<JournalAttachment> attachments, List<JournalComment> comments) {
        List<AttachmentResponse> atts = attachments.stream()
                .sorted(Comparator.comparing(JournalAttachment::getCreatedAt))
                .map(this::toAttachmentDto).toList();
        List<CommentResponse> comms = comments.stream()
                .sorted(Comparator.comparing(JournalComment::getCreatedAt))
                .map(c -> new CommentResponse(c.getId(), c.getAuthor().getName(), c.getAuthor().getRole().name(), c.getContent(), c.getCreatedAt()))
                .toList();
        return new JournalResponse(e.getId(), e.getTitle(), e.getContent(), e.getMood(), e.getEntryDate(), e.getCreatedAt(), atts, comms);
    }

    private AttachmentResponse toAttachmentDto(JournalAttachment a) {
        return new AttachmentResponse(a.getId(), a.getType(), a.getOriginalName(), a.getMimeType(),
                "/api/journal/attachments/" + a.getId() + "/file", a.getCreatedAt());
    }
}

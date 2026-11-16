package com.psicology.backend.journal;

import com.psicology.backend.domain.journal.JournalEntry;
import com.psicology.backend.domain.journal.JournalEntryRepository;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/journal")
public class JournalController {
    private final JournalEntryRepository repo;
    private final UserRepository userRepository;

    public JournalController(JournalEntryRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public record JournalRequest(@NotBlank String content, String title, String mood, Instant entryDate) {}
    public record JournalResponse(UUID id, String title, String content, String mood, Instant entryDate, Instant createdAt) {}

    @GetMapping
    public List<JournalResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
        return repo.findByPatientId(principal.getId(), Sort.by(Sort.Direction.DESC, "entryDate"))
                .stream().map(this::toDto).toList();
    }

    @PostMapping
    public JournalResponse create(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody JournalRequest req) {
        User patient = userRepository.findById(principal.getId()).orElseThrow();
        JournalEntry e = JournalEntry.builder()
                .patient(patient).title(req.title()).content(req.content()).mood(req.mood()).entryDate(req.entryDate()).build();
        return toDto(repo.save(e));
    }

    @PutMapping("/{id}")
    public JournalResponse update(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id, @Valid @RequestBody JournalRequest req) {
        JournalEntry e = ownedEntry(principal.getId(), id);
        e.setTitle(req.title()); e.setContent(req.content()); e.setMood(req.mood());
        if (req.entryDate() != null) e.setEntryDate(req.entryDate());
        return toDto(repo.save(e));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        repo.delete(ownedEntry(principal.getId(), id));
    }

    private JournalEntry ownedEntry(UUID userId, UUID id) {
        JournalEntry e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!e.getPatient().getId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return e;
    }

    private JournalResponse toDto(JournalEntry e) {
        return new JournalResponse(e.getId(), e.getTitle(), e.getContent(), e.getMood(), e.getEntryDate(), e.getCreatedAt());
    }
}

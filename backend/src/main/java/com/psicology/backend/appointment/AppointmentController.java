package com.psicology.backend.appointment;

import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.dto.AppointmentDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final PatientLinkRepository linkRepo;

    public AppointmentController(AppointmentService service, PatientLinkRepository linkRepo) {
        this.service = service;
        this.linkRepo = linkRepo;
    }

    @GetMapping
    public List<AppointmentResponse> list(@AuthenticationPrincipal AppUserDetails principal,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) UUID psychologistId,
                                          @RequestParam(required = false) UUID patientId) {
        return service.list(principal, from, to, status, psychologistId, patientId);
    }

    @GetMapping("/my-psychologist")
    public ResponseEntity<PsychologistDirectoryEntry> myPsychologist(@AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.of(Optional.ofNullable(service.myPsychologist(principal.getId())));
    }

    @GetMapping("/directory")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public List<PsychologistDirectoryEntry> directory(@AuthenticationPrincipal AppUserDetails principal) {
        return service.directory(principal.getId());
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST') or hasRole('ADMIN')")
    public List<AppointmentResponse> patientAppointments(@AuthenticationPrincipal AppUserDetails principal,
                                                         @PathVariable UUID patientId) {
        if (principal.getUser().getRole() == com.psicology.backend.domain.user.Role.PSYCHOLOGIST
                && !linkRepo.existsByPsychologistIdAndPatientIdAndStatus(principal.getId(), patientId, "ACTIVE")) {
            throw new IllegalArgumentException("Paciente não vinculado");
        }
        return service.mine(patientId);
    }

    @PostMapping
    public AppointmentResponse create(@AuthenticationPrincipal AppUserDetails principal,
                                      @Valid @RequestBody AppointmentRequest req,
                                      HttpServletRequest request) {
        return service.create(principal, req, request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal,
                                      @RequestBody AppointmentRequest req, HttpServletRequest request) {
        return service.update(id, req, principal, request);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal,
                       HttpServletRequest request) {
        service.cancel(id, principal, request);
    }
}

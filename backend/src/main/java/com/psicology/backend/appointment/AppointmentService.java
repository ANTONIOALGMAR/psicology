package com.psicology.backend.appointment;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.appointment.Appointment;
import com.psicology.backend.domain.appointment.AppointmentRepository;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AppointmentDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private static final Set<String> MODES = Set.of("ONLINE", "PRESENTIAL");
    private static final Set<String> STATUSES = Set.of("SCHEDULED", "CONFIRMED", "COMPLETED", "CANCELLED");

    private final AppointmentRepository repo;
    private final PatientLinkRepository linkRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;

    public AppointmentService(AppointmentRepository repo, PatientLinkRepository linkRepo,
                              UserRepository userRepo, AuditService auditService) {
        this.repo = repo;
        this.linkRepo = linkRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(AppUserDetails principal, Instant from, Instant to,
                                          String status, UUID psychologistFilter, UUID patientFilter) {
        Role role = principal.getUser().getRole();
        UUID psy = role == Role.PSYCHOLOGIST ? principal.getId() : psychologistFilter;
        UUID pat = role == Role.PATIENT ? principal.getId() : patientFilter;
        return toResponses(repo.search(psy, pat, status, from, to));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> mine(UUID patientId) {
        return toResponses(repo.search(null, patientId, null, null, null));
    }

    @Transactional
    public AppointmentResponse create(AppUserDetails principal, AppointmentRequest req, HttpServletRequest request) {
        Instant start = req.startsAt();
        if (start == null) throw new IllegalArgumentException("Data e hora de início são obrigatórias");
        Instant end = req.endsAt() != null ? req.endsAt() : start.plus(50, ChronoUnit.MINUTES);
        if (!end.isAfter(start)) throw new IllegalArgumentException("O término deve ser posterior ao início");

        Role role = principal.getUser().getRole();
        UUID patientId;
        UUID psychologistId;
        if (role == Role.PATIENT) {
            patientId = principal.getId();
            PatientLink link = activeLinkForPatient(patientId);
            psychologistId = req.psychologistId() != null ? req.psychologistId() : link.getPsychologist().getId();
            if (!psychologistId.equals(link.getPsychologist().getId())) {
                throw new IllegalArgumentException("Solicitação permitida apenas para o psicólogo vinculado");
            }
        } else if (role == Role.PSYCHOLOGIST) {
            psychologistId = principal.getId();
            patientId = requireId(req.patientId(), "Paciente obrigatório");
            ensureLinked(psychologistId, patientId);
        } else {
            patientId = requireId(req.patientId(), "Paciente obrigatório");
            psychologistId = requireId(req.psychologistId(), "Psicólogo obrigatório");
            User patient = findUser(patientId);
            User psychologist = findUser(psychologistId);
            if (patient.getRole() != Role.PATIENT || psychologist.getRole() != Role.PSYCHOLOGIST) {
                throw new IllegalArgumentException("Participantes inválidos para o agendamento");
            }
            ensureLinked(psychologistId, patientId);
        }

        String mode = normalize(req.mode(), "ONLINE", MODES, "Modalidade inválida");
        String status = normalize(req.status(), "SCHEDULED", STATUSES, "Status inválido");
        ensureNoOverlap(psychologistId, start, end, null);

        Appointment a = new Appointment();
        a.setPatientId(patientId);
        a.setPsychologistId(psychologistId);
        a.setStartsAt(start);
        a.setEndsAt(end);
        a.setMode(mode);
        a.setStatus(status);
        a.setTitle(req.title() == null || req.title().isBlank() ? "Atendimento" : req.title().trim());
        a.setNotes(req.notes());
        repo.save(a);
        auditService.record(principal, "APPOINTMENT_CREATED", "APPOINTMENT", a.getId().toString(), patientId,
                psychologistId + " | " + start, request);
        return toResponse(a);
    }

    @Transactional
    public AppointmentResponse update(UUID id, AppointmentRequest req, AppUserDetails actor, HttpServletRequest request) {
        Appointment a = find(id);
        authorizeResponsible(a, actor);
        Instant start = req.startsAt() != null ? req.startsAt() : a.getStartsAt();
        Instant end = req.endsAt() != null ? req.endsAt() : a.getEndsAt();
        if (!end.isAfter(start)) throw new IllegalArgumentException("O término deve ser posterior ao início");

        UUID patientId = a.getPatientId();
        if (req.patientId() != null && !req.patientId().equals(patientId)) {
            patientId = req.patientId();
            ensureLinked(a.getPsychologistId(), patientId);
        }
        String status = req.status() != null
                ? normalize(req.status(), null, STATUSES, "Status inválido") : a.getStatus();
        if (!"CANCELLED".equals(status)) {
            ensureNoOverlap(a.getPsychologistId(), start, end, id);
        }
        a.setPatientId(patientId);
        a.setStartsAt(start);
        a.setEndsAt(end);
        if (req.mode() != null) a.setMode(normalize(req.mode(), null, MODES, "Modalidade inválida"));
        a.setStatus(status);
        if (req.title() != null) a.setTitle(req.title().trim());
        if (req.notes() != null) a.setNotes(req.notes());
        repo.save(a);
        auditService.record(actor, "APPOINTMENT_UPDATED", "APPOINTMENT", id.toString(), patientId,
                "Status=" + status + " | " + start, request);
        return toResponse(a);
    }

    @Transactional
    public void cancel(UUID id, AppUserDetails actor, HttpServletRequest request) {
        Appointment a = find(id);
        Role role = actor.getUser().getRole();
        boolean allowed = role == Role.ADMIN
                || (role == Role.PSYCHOLOGIST && a.getPsychologistId().equals(actor.getId()))
                || (role == Role.PATIENT && a.getPatientId().equals(actor.getId()));
        if (!allowed) throw new IllegalArgumentException("Sem permissão para cancelar este agendamento");
        a.setStatus("CANCELLED");
        repo.save(a);
        auditService.record(actor, "APPOINTMENT_CANCELLED", "APPOINTMENT", id.toString(), a.getPatientId(),
                "Cancelamento", request);
    }

    /** Usado na transferência: apenas atendimentos futuros ainda não realizados mudam de profissional. */
    @Transactional
    public int reassignFuture(UUID patientId, UUID fromPsychologistId, UUID toPsychologistId) {
        List<Appointment> future = repo.findByPatientIdAndPsychologistIdAndStartsAtAfter(
                patientId, fromPsychologistId, Instant.now()).stream()
                .filter(a -> "SCHEDULED".equals(a.getStatus()) || "CONFIRMED".equals(a.getStatus()))
                .toList();
        for (Appointment a : future) a.setPsychologistId(toPsychologistId);
        repo.saveAll(future);
        return future.size();
    }

    @Transactional(readOnly = true)
    public PsychologistDirectoryEntry myPsychologist(UUID patientId) {
        PatientLink link = activeLinkForPatient(patientId);
        return link == null ? null : entry(link.getPsychologist());
    }

    @Transactional(readOnly = true)
    public List<PsychologistDirectoryEntry> directory(UUID excludeId) {
        return userRepo.findByRole(Role.PSYCHOLOGIST).stream()
                .filter(u -> !u.getId().equals(excludeId))
                .map(this::entry).toList();
    }

    private Appointment find(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
    }

    private void authorizeResponsible(Appointment a, AppUserDetails actor) {
        boolean allowed = actor.getUser().getRole() == Role.ADMIN
                || (actor.getUser().getRole() == Role.PSYCHOLOGIST && a.getPsychologistId().equals(actor.getId()));
        if (!allowed) throw new IllegalArgumentException("Somente o psicólogo responsável ou administrador pode alterar o agendamento");
    }

    private void ensureNoOverlap(UUID psychologistId, Instant start, Instant end, UUID ignoreId) {
        if (!repo.findOverlaps(psychologistId, start, end, ignoreId).isEmpty()) {
            throw new IllegalArgumentException("Horário indisponível para este psicólogo");
        }
    }

    private PatientLink activeLinkForPatient(UUID patientId) {
        return linkRepo.findByPatientIdAndStatus(patientId, "ACTIVE").stream().findFirst().orElse(null);
    }

    private void ensureLinked(UUID psychologistId, UUID patientId) {
        if (!linkRepo.existsByPsychologistIdAndPatientIdAndStatus(psychologistId, patientId, "ACTIVE")) {
            throw new IllegalArgumentException("Paciente não vinculado a este psicólogo");
        }
    }

    private String normalize(String value, String fallback, Set<String> allowed, String error) {
        if (value == null || value.isBlank()) {
            if (fallback == null) throw new IllegalArgumentException(error);
            return fallback;
        }
        String v = value.trim().toUpperCase();
        if (!allowed.contains(v)) throw new IllegalArgumentException(error);
        return v;
    }

    private UUID requireId(UUID id, String message) {
        if (id == null) throw new IllegalArgumentException(message);
        return id;
    }

    private User findUser(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private PsychologistDirectoryEntry entry(User u) {
        return new PsychologistDirectoryEntry(u.getId(), u.getName());
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.of(a, nameOf(a.getPatientId()), nameOf(a.getPsychologistId()));
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        Set<UUID> ids = appointments.stream()
                .flatMap(a -> List.of(a.getPatientId(), a.getPsychologistId()).stream())
                .collect(Collectors.toSet());
        Map<UUID, String> names = userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        Function<UUID, String> name = id -> names.getOrDefault(id, "");
        return appointments.stream()
                .map(a -> AppointmentResponse.of(a, name.apply(a.getPatientId()), name.apply(a.getPsychologistId())))
                .toList();
    }

    private String nameOf(UUID id) {
        return userRepo.findById(id).map(User::getName).orElse("");
    }
}

package com.psicology.backend.transfer;

import com.psicology.backend.appointment.AppointmentService;
import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AppointmentDtos.TransferRequest;
import com.psicology.backend.dto.AppointmentDtos.TransferResponse;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientTransferService {
    private final PatientLinkRepository linkRepo;
    private final UserRepository userRepo;
    private final AppointmentService appointmentService;
    private final AuditService auditService;

    public PatientTransferService(PatientLinkRepository linkRepo, UserRepository userRepo,
                                  AppointmentService appointmentService, AuditService auditService) {
        this.linkRepo = linkRepo;
        this.userRepo = userRepo;
        this.appointmentService = appointmentService;
        this.auditService = auditService;
    }

    @Transactional
    public TransferResponse transfer(UUID patientId, TransferRequest req,
                                     AppUserDetails actor, HttpServletRequest request) {
        User patient = userRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        if (patient.getRole() != Role.PATIENT) throw new IllegalArgumentException("Usuário não é paciente");

        PatientLink current = linkRepo.findByPatientIdAndStatus(patientId, "ACTIVE").stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Paciente não possui vínculo ativo"));

        Role actorRole = actor.getUser().getRole();
        if (actorRole != Role.ADMIN && !(actorRole == Role.PSYCHOLOGIST
                && current.getPsychologist().getId().equals(actor.getId()))) {
            throw new IllegalArgumentException("Somente o psicólogo responsável ou administrador pode transferir o cliente");
        }

        UUID targetId = req.toPsychologistId();
        if (targetId == null) throw new IllegalArgumentException("Psicólogo de destino obrigatório");
        if (targetId.equals(current.getPsychologist().getId())) {
            throw new IllegalArgumentException("O paciente já está vinculado a este psicólogo");
        }
        User target = userRepo.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Psicólogo de destino não encontrado"));
        if (target.getRole() != Role.PSYCHOLOGIST) {
            throw new IllegalArgumentException("O destino deve ser um psicólogo");
        }

        UUID fromId = current.getPsychologist().getId();
        current.setStatus("TRANSFERRED");
        linkRepo.save(current);

        PatientLink next = linkRepo.findByPatientIdAndPsychologistId(patientId, targetId).orElseGet(() -> {
            PatientLink link = new PatientLink();
            link.setPatient(patient);
            link.setPsychologist(target);
            return link;
        });
        next.setStatus("ACTIVE");
        linkRepo.save(next);

        int rescheduled = appointmentService.reassignFuture(patientId, fromId, targetId);
        auditService.record(actor, "PATIENT_TRANSFERRED", "PATIENT", patientId.toString(), patientId,
                fromId + " -> " + targetId + (req.reason() == null || req.reason().isBlank()
                        ? "" : " | Motivo: " + req.reason()), request);
        return new TransferResponse(patientId, fromId, targetId, rescheduled);
    }
}

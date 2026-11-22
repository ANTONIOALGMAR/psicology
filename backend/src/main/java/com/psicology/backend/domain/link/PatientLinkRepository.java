package com.psicology.backend.domain.link;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientLinkRepository extends JpaRepository<PatientLink, UUID> {
    List<PatientLink> findByPsychologistIdAndStatus(UUID psychologistId, String status);
    Optional<PatientLink> findByPsychologistIdAndPatientIdAndStatus(UUID psychologistId, UUID patientId, String status);
    boolean existsByPsychologistIdAndPatientIdAndStatus(UUID psychologistId, UUID patientId, String status);
    List<PatientLink> findByPatientIdAndStatus(UUID patientId, String status);
    Optional<PatientLink> findByPatientIdAndPsychologistId(UUID patientId, UUID psychologistId);
}

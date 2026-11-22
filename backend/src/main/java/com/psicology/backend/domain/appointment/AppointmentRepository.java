package com.psicology.backend.domain.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query("SELECT a FROM Appointment a WHERE (:psychologistId IS NULL OR a.psychologistId = :psychologistId) " +
           "AND (:patientId IS NULL OR a.patientId = :patientId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:from IS NULL OR a.startsAt >= :from) " +
           "AND (:to IS NULL OR a.startsAt <= :to) " +
           "ORDER BY a.startsAt ASC")
    List<Appointment> search(@Param("psychologistId") UUID psychologistId,
                             @Param("patientId") UUID patientId,
                             @Param("status") String status,
                             @Param("from") Instant from,
                             @Param("to") Instant to);

    @Query("SELECT a FROM Appointment a WHERE a.psychologistId = :psychologistId " +
           "AND a.startsAt < :end AND a.endsAt > :start " +
           "AND a.status <> 'CANCELLED' " +
           "AND (:ignoreId IS NULL OR a.id <> :ignoreId)")
    List<Appointment> findOverlaps(@Param("psychologistId") UUID psychologistId,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end,
                                   @Param("ignoreId") UUID ignoreId);

    List<Appointment> findByPatientIdAndPsychologistIdAndStartsAtAfter(UUID patientId, UUID psychologistId, Instant from);
}

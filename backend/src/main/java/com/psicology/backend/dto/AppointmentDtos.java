package com.psicology.backend.dto;

import com.psicology.backend.domain.appointment.Appointment;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class AppointmentDtos {
    public record AppointmentRequest(UUID patientId, UUID psychologistId,
                                     @NotNull Instant startsAt, Instant endsAt,
                                     String mode, String status, String title, String notes) {}
    public record AppointmentResponse(UUID id, UUID patientId, String patientName,
                                      UUID psychologistId, String psychologistName,
                                      Instant startsAt, Instant endsAt, String mode, String status,
                                      String title, String notes, Instant createdAt) {
        public static AppointmentResponse of(Appointment a, String patientName, String psychologistName) {
            return new AppointmentResponse(a.getId(), a.getPatientId(), patientName,
                    a.getPsychologistId(), psychologistName, a.getStartsAt(), a.getEndsAt(),
                    a.getMode(), a.getStatus(), a.getTitle(), a.getNotes(), a.getCreatedAt());
        }
    }
    public record TransferRequest(@NotNull UUID toPsychologistId, String reason) {}
    public record TransferResponse(UUID patientId, UUID fromPsychologistId, UUID toPsychologistId,
                                   int rescheduledAppointments) {}
    public record PsychologistDirectoryEntry(UUID id, String name) {}
}

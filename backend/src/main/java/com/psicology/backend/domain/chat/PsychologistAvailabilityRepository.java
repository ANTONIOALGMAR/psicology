package com.psicology.backend.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PsychologistAvailabilityRepository extends JpaRepository<PsychologistAvailability, UUID> {
}

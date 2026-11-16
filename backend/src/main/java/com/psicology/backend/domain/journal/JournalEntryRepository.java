package com.psicology.backend.domain.journal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    List<JournalEntry> findByPatientId(UUID patientId, Sort sort);
    Page<JournalEntry> findByPatientId(UUID patientId, Pageable pageable);
}

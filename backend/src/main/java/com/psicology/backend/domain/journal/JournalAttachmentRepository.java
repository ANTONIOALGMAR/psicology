package com.psicology.backend.domain.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JournalAttachmentRepository extends JpaRepository<JournalAttachment, UUID> {
    List<JournalAttachment> findByEntryId(UUID entryId);
    List<JournalAttachment> findByEntryIdIn(List<UUID> entryIds);
}

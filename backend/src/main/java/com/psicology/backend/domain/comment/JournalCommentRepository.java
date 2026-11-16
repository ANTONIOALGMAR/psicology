package com.psicology.backend.domain.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JournalCommentRepository extends JpaRepository<JournalComment, UUID> {
    List<JournalComment> findByEntryIdInOrderByCreatedAtAsc(List<UUID> entryIds);
    List<JournalComment> findByEntryIdOrderByCreatedAtAsc(UUID entryId);
}

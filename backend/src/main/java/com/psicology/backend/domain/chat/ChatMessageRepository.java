package com.psicology.backend.domain.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ChatMessage> findByCrisisTrueAndUserIdInAndCreatedAtAfterOrderByCreatedAtDesc(
            Collection<UUID> userIds, Instant from);
    Page<ChatMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

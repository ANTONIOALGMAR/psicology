package com.psicology.backend.dto;

import com.psicology.backend.domain.chat.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class ChatDtos {
    public record ChatStatus(boolean psychologistOnline, String psychologistName, String message,
                             boolean consentRequired) {}
    public record ChatSendRequest(@NotBlank @Size(max = 2000) String content, UUID sessionId) {}
    public record ChatMessageResponse(UUID id, String sender, String content, boolean crisis, Instant createdAt,
                                      String userEmail) {
        public static ChatMessageResponse of(ChatMessage m) {
            return of(m, null);
        }
        public static ChatMessageResponse of(ChatMessage m, String userEmail) {
            return new ChatMessageResponse(m.getId(), m.getSender(), m.getContent(), m.isCrisis(), m.getCreatedAt(), userEmail);
        }
    }
    public record ChatReplyResponse(UUID sessionId, ChatMessageResponse userMessage,
                                    ChatMessageResponse botMessage, boolean crisis) {}
    public record AvailabilityRequest(String timezone, @NotBlank String startTime, @NotBlank String endTime,
                                      Boolean weekendEnabled) {}
    public record AvailabilityResponse(UUID psychologistId, String timezone, LocalTime startTime,
                                       LocalTime endTime, boolean weekendEnabled) {}
    public record CrisisAlertResponse(UUID id, UUID patientId, String patientName, Instant createdAt) {}
}

package com.psicology.backend.chat;

import com.psicology.backend.dto.ChatDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@PreAuthorize("hasRole('PATIENT')")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/status")
    public ChatStatus status(@AuthenticationPrincipal AppUserDetails principal) {
        return chatService.status(principal.getId());
    }

    @PostMapping("/messages")
    public ChatReplyResponse send(@AuthenticationPrincipal AppUserDetails principal,
                                  @Valid @RequestBody ChatSendRequest req,
                                  HttpServletRequest request) {
        return chatService.send(principal.getId(), req, principal, request);
    }

    @GetMapping("/history")
    public List<ChatMessageResponse> history(@AuthenticationPrincipal AppUserDetails principal,
                                             @RequestParam(required = false) UUID sessionId) {
        return sessionId == null ? chatService.myHistory(principal.getId())
                : chatService.sessionHistory(principal.getId(), sessionId);
    }
}

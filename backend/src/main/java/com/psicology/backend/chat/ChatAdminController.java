package com.psicology.backend.chat;

import com.psicology.backend.dto.AdminDtos.PagedResponse;
import com.psicology.backend.dto.ChatDtos.ChatMessageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class ChatAdminController {
    private final ChatService chatService;

    public ChatAdminController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public PagedResponse<ChatMessageResponse> history(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        return chatService.adminHistory(PageRequest.of(Math.max(page, 0), safeSize));
    }
}

package com.psicology.backend.chat;

import com.psicology.backend.dto.ChatDtos.AvailabilityRequest;
import com.psicology.backend.dto.ChatDtos.AvailabilityResponse;
import com.psicology.backend.dto.ChatDtos.CrisisAlertResponse;
import com.psicology.backend.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/psychologist")
@PreAuthorize("hasRole('PSYCHOLOGIST')")
public class AvailabilityController {
    private final ChatService chatService;

    public AvailabilityController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@AuthenticationPrincipal AppUserDetails principal) {
        return chatService.availability(principal.getId());
    }

    @PutMapping("/availability")
    public AvailabilityResponse save(@AuthenticationPrincipal AppUserDetails principal,
                                     @Valid @RequestBody AvailabilityRequest req) {
        return chatService.saveAvailability(principal.getId(), req);
    }

    @GetMapping("/alerts")
    public List<CrisisAlertResponse> alerts(@AuthenticationPrincipal AppUserDetails principal) {
        return chatService.crisisAlerts(principal.getId());
    }
}

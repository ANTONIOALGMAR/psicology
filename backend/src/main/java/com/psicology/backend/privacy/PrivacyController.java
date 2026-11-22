package com.psicology.backend.privacy;

import com.psicology.backend.dto.PrivacyDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {
    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @GetMapping("/me")
    public PrivacyOverview me(@AuthenticationPrincipal AppUserDetails principal) {
        return privacyService.overview(principal.getId());
    }

    @PostMapping("/consents")
    public ConsentResponse consent(@AuthenticationPrincipal AppUserDetails principal,
                                   @Valid @RequestBody ConsentRequest req,
                                   HttpServletRequest request) {
        return privacyService.recordConsent(principal.getId(), req.kind(), Boolean.TRUE.equals(req.accepted()), request);
    }

    @PostMapping("/requests")
    public PrivacyRequestResponse request(@AuthenticationPrincipal AppUserDetails principal,
                                          @Valid @RequestBody PrivacyRequestCreate req,
                                          HttpServletRequest request) {
        return privacyService.createRequest(principal.getId(), req.type(), req.details(), request);
    }

    @GetMapping("/export")
    public Map<String, Object> export(@AuthenticationPrincipal AppUserDetails principal) {
        return privacyService.export(principal.getId());
    }
}

package com.psicology.backend.auth;

import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AuthDtos.*;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) { return authService.register(req, request); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        User u = principal.getUser();
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    @PostMapping("/invites")
    public InviteResponse invite(@Valid @RequestBody InviteRequest req, @AuthenticationPrincipal AppUserDetails principal) {
        User creator = userRepository.findById(principal.getId()).orElseThrow();
        return authService.createInvite(req, creator);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest req, @AuthenticationPrincipal AppUserDetails principal) {
        try {
            return authService.updateProfile(principal.getId(), req);
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req, @AuthenticationPrincipal AppUserDetails principal) {
        try {
            authService.changePassword(principal.getId(), req);
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

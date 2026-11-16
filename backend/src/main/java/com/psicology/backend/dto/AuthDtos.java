package com.psicology.backend.dto;

import com.psicology.backend.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(@NotBlank String name, @Email @NotBlank String email,
                                  @Size(min = 6) String password, Role role, String inviteCode) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserResponse user) {}
    public record UserResponse(java.util.UUID id, String name, String email, Role role) {}
    public record InviteRequest(@Email String email, Role role) {}
    public record InviteResponse(String code, Role role, String email) {}
}

package com.psicology.backend.dto;

import com.psicology.backend.domain.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 128)
                                  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$",
                                          message = "deve conter maiúscula, minúscula, número e caractere especial")
                                  String password, Role role, String inviteCode, Boolean acceptTerms, Boolean acceptPrivacy) {}
    public record LoginRequest(@Email @NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {}
    public record AuthResponse(String token, UserResponse user) {}
    public record UserResponse(java.util.UUID id, String name, String email, Role role) {}
    public record InviteRequest(@Email String email, Role role) {}
    public record InviteResponse(String code, Role role, String email) {}
    public record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                        @NotBlank @Size(min = 8, max = 128)
                                        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$",
                                                message = "deve conter maiúscula, minúscula, número e caractere especial")
                                        String newPassword) {}
    public record UpdateProfileRequest(@NotBlank @Size(max = 120) String name) {}
}

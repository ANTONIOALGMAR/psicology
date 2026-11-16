package com.psicology.backend.auth;

import com.psicology.backend.domain.invite.Invite;
import com.psicology.backend.domain.invite.InviteRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AuthDtos.*;
import com.psicology.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, InviteRepository inviteRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) throw new IllegalArgumentException("E-mail já cadastrado");
        Role role = req.role() != null ? req.role() : Role.PATIENT;
        Invite invite = null;
        if (req.inviteCode() != null && !req.inviteCode().isBlank()) {
            invite = inviteRepository.findByCode(req.inviteCode())
                    .orElseThrow(() -> new IllegalArgumentException("Convite inválido"));
            if (!invite.isValid()) throw new IllegalArgumentException("Convite expirado ou já utilizado");
            role = invite.getRole();
        }
        if (invite == null && (role == Role.ADMIN)) role = Role.PATIENT;
        User user = User.builder()
                .name(req.name()).email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role).enabled(true).build();
        userRepository.save(user);
        if (invite != null) {
            invite.setUsedBy(user);
            invite.setUsedAt(Instant.now());
            inviteRepository.save(invite);
        }
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().toLowerCase(), req.password()));
        User user = userRepository.findByEmail(req.email().toLowerCase()).orElseThrow();
        return authResponse(user);
    }

    @Transactional
    public InviteResponse createInvite(InviteRequest req, User creator) {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Invite invite = Invite.builder()
                .code(code)
                .email(req.email())
                .role(req.role() != null ? req.role() : Role.PATIENT)
                .createdBy(creator)
                .expiresAt(Instant.now().plusSeconds(7L * 24 * 3600))
                .build();
        inviteRepository.save(invite);
        return new InviteResponse(invite.getCode(), invite.getRole(), invite.getEmail());
    }

    private AuthResponse authResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of("role", user.getRole().name(), "uid", user.getId().toString()));
        return new AuthResponse(token, new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }
}

package com.psicology.backend.auth;

import com.psicology.backend.domain.invite.Invite;
import com.psicology.backend.domain.invite.InviteRepository;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.user.Role;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AuthDtos.*;
import com.psicology.backend.privacy.PrivacyService;
import com.psicology.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final PatientLinkRepository patientLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PrivacyService privacyService;

    public AuthService(UserRepository userRepository, InviteRepository inviteRepository, PatientLinkRepository patientLinkRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, PrivacyService privacyService) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.patientLinkRepository = patientLinkRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.privacyService = privacyService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest request) {
        if (userRepository.existsByEmail(req.email())) throw new IllegalArgumentException("E-mail já cadastrado");
        Role role = req.role() != null ? req.role() : Role.PATIENT;
        Invite invite = null;
        if (req.inviteCode() != null && !req.inviteCode().isBlank()) {
            invite = inviteRepository.findByCode(req.inviteCode())
                    .orElseThrow(() -> new IllegalArgumentException("Convite inválido"));
            if (!invite.isValid()) throw new IllegalArgumentException("Convite expirado ou já utilizado");
            role = invite.getRole();
        }
        if (invite == null) role = Role.PATIENT;
        User user = User.builder()
                .name(req.name()).email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role).enabled(true).build();
        userRepository.save(user);
        privacyService.recordRegistrationConsents(user.getId(), req.acceptTerms(), req.acceptPrivacy(), request);
        if (invite != null) {
            invite.setUsedBy(user);
            invite.setUsedAt(Instant.now());
            inviteRepository.save(invite);
            User creator = invite.getCreatedBy();
            if (creator != null && creator.getRole() == Role.PSYCHOLOGIST && user.getRole() == Role.PATIENT) {
                boolean exists = patientLinkRepository.existsByPsychologistIdAndPatientIdAndStatus(creator.getId(), user.getId(), "ACTIVE");
                if (!exists) {
                    PatientLink link = new PatientLink();
                    link.setPsychologist(creator);
                    link.setPatient(user);
                    link.setStatus("ACTIVE");
                    patientLinkRepository.save(link);
                }
            }
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
        if (creator.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Apenas administradores podem criar convites");
        }
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

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        String name = req.name() != null ? req.name().trim() : "";
        if (name.isBlank()) throw new IllegalArgumentException("Nome inválido");
        user.setName(name);
        userRepository.save(user);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        if (req.newPassword().equals(req.currentPassword())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    private AuthResponse authResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of("role", user.getRole().name(), "uid", user.getId().toString()));
        return new AuthResponse(token, new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }
}

package com.psicology.backend.admin;

import com.psicology.backend.domain.user.Role;
import com.psicology.backend.dto.AdminDtos.*;
import com.psicology.backend.dto.PrivacyDtos.PrivacyRequestResponse;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public OverviewResponse overview() { return adminService.overview(); }

    @GetMapping("/users")
    public PagedResponse<UserResponse> users(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Role role) {
        return PagedResponse.of(adminService.users(page, size, q, role));
    }

    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest req,
                                   @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.createUser(req, actor, request);
    }

    @PutMapping("/users/{id}/role")
    public UserResponse updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest req,
                                   @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.updateRole(id, req, actor, request);
    }

    @PutMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@PathVariable UUID id, @Valid @RequestBody UpdateEnabledRequest req,
                                   @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.setEnabled(id, req, actor, request);
    }

    @PostMapping("/users/{id}/reset-password")
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest req,
                              @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        adminService.resetPassword(id, req, actor, request);
    }

    @GetMapping("/tests")
    public List<TestResponse> tests() { return adminService.tests(); }

    @PostMapping("/tests")
    public TestResponse createTest(@Valid @RequestBody TestUpsertRequest req,
                                   @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.createTest(req, actor, request);
    }

    @PutMapping("/tests/{id}")
    public TestResponse updateTest(@PathVariable UUID id, @Valid @RequestBody TestUpsertRequest req,
                                   @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.updateTest(id, req, actor, request);
    }

    @PutMapping("/tests/{id}/active")
    public TestResponse setTestActive(@PathVariable UUID id, @RequestParam boolean active,
                                      @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.setTestActive(id, active, actor, request);
    }

    @GetMapping("/tests/{id}/questions")
    public List<QuestionResponse> questions(@PathVariable UUID id) { return adminService.questions(id); }

    @PostMapping("/tests/{id}/questions")
    public QuestionResponse addQuestion(@PathVariable UUID id, @Valid @RequestBody QuestionRequest req,
                                        @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        return adminService.addQuestion(id, req, actor, request);
    }

    @DeleteMapping("/tests/{id}/questions/{questionId}")
    public void deleteQuestion(@PathVariable UUID id, @PathVariable UUID questionId,
                               @AuthenticationPrincipal AppUserDetails actor, HttpServletRequest request) {
        adminService.deleteQuestion(id, questionId, actor, request);
    }

    @GetMapping("/audit")
    public PagedResponse<AuditResponse> audit(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String action,
                                              @RequestParam(required = false) String q) {
        return PagedResponse.of(adminService.audit(page, size, action, q));
    }

    @GetMapping("/privacy-requests")
    public List<PrivacyRequestResponse> privacyRequests(@RequestParam(required = false) String status) {
        return adminService.privacyRequests(status);
    }

    @PutMapping("/privacy-requests/{id}/status")
    public PrivacyRequestResponse updatePrivacyRequest(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdatePrivacyRequestStatus req,
                                                       @AuthenticationPrincipal AppUserDetails actor,
                                                       HttpServletRequest request) {
        return adminService.updatePrivacyRequest(id, req, actor, request);
    }
}

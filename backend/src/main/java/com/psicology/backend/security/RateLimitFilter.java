package com.psicology.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for sensitive endpoints.
 * Limits auth attempts per client IP to reduce brute-force risk.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int authLimit;
    private final int generalLimit;
    private final long windowSeconds;
    private final boolean enabled;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.auth-limit:20}") int authLimit,
            @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.general-limit:120}") int generalLimit,
            @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.authLimit = authLimit;
        this.generalLimit = generalLimit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (isLimitedPath(path)) {
            String ip = clientIp(request);
            int limit = path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register") ? authLimit : generalLimit;
            String key = ip + "|" + bucket(path);
            if (!allow(key, limit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.getWriter().write("{\"message\":\"Muitas requisições. Tente novamente em instantes.\"}");
                return;
            }
        }
        if (counters.size() > 10_000) cleanup();
        filterChain.doFilter(request, response);
    }

    private boolean isLimitedPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/change-password");
    }

    private String bucket(String path) {
        if (path.startsWith("/api/auth/login")) return "login";
        if (path.startsWith("/api/auth/register")) return "register";
        if (path.startsWith("/api/auth/change-password")) return "change-password";
        return "other";
    }

    private boolean allow(String key, int limit) {
        long now = Instant.now().getEpochSecond();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= windowSeconds) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return counter.count.get() <= limit;
    }

    private void cleanup() {
        long now = Instant.now().getEpochSecond();
        counters.entrySet().removeIf(e -> now - e.getValue().windowStart >= windowSeconds * 2);
    }

    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (remote != null && !remote.isBlank()) return remote;
        return "unknown";
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {}
}

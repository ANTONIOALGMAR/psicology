package com.psicology.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = null;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        } else if ("GET".equalsIgnoreCase(request.getMethod())) {
            String queryToken = request.getParameter("token");
            String path = request.getRequestURI();
            if (queryToken != null && !queryToken.isBlank() && isTokenQueryAllowedPath(path)) {
                token = queryToken;
            }
        }
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                String email = claims.getSubject();
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails details = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) { }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenQueryAllowedPath(String path) {
        if (path == null) return false;
        return path.contains("/audio")
                || path.contains("/export")
                || path.contains("/download")
                || path.contains("/files");
    }
}

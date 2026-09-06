package com.resumeai.controller;

import com.resumeai.dto.AuthResponse;
import com.resumeai.dto.LoginRequest;
import com.resumeai.dto.RefreshRequest;
import com.resumeai.dto.RegisterRequest;
import com.resumeai.service.AuthService;
import com.resumeai.service.RateLimiterService;
import com.resumeai.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        rateLimiterService.checkAuth(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        rateLimiterService.checkAuth(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        rateLimiterService.checkAuth(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}


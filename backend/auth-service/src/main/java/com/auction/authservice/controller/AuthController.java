package com.auction.authservice.controller;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.LogoutRequest;
import com.auction.authservice.dto.request.RefreshTokenRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return authService.refresh(refreshTokenRequest);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest);
        return Map.of("message", "Logged out successfully");
    }
}

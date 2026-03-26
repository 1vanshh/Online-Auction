package com.auction.authservice.service;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.LogoutRequest;
import com.auction.authservice.dto.request.RefreshTokenRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.RefreshToken;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.BadRequestException;
import com.auction.authservice.exception.NotFoundException;
import com.auction.authservice.exception.UnauthorizedException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.RefreshTokenRepository;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserBanService userBanService;
    private final AuditService auditService;

    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshValidityInMilliseconds;

    @Autowired
    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       JwtTokenProvider jwtTokenProvider,
                       UserBanService userBanService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userBanService = userBanService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email already in use");
        }

        User user = userMapper.toUser(registerRequest);
        user.setEmail(normalizedEmail);
        user.setFirstName(requireTrimmed(registerRequest.getFirstName(), "First name is required"));
        user.setLastName(requireTrimmed(registerRequest.getLastName(), "Last name is required"));
        user.setPhone(trimToNull(registerRequest.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setActive(true);
        user.setBanned(false);

        User savedUser = userRepository.save(user);
        RefreshToken refreshToken = createRefreshToken(savedUser);
        auditService.log(savedUser, "REGISTER", "USER", savedUser.getId(), "User registered");

        return buildAuthResponse(savedUser, refreshToken.getToken());
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(email));
        if (user.isPresent()) {
            return userMapper.toUserResponse(user.get());
        }
        throw new NotFoundException("User not found");
    }

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(normalizeEmail(loginRequest.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        userBanService.syncBanStatus(user);
        validateUserCanAuthenticate(user);

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        revokeActiveRefreshTokens(user);
        RefreshToken refreshToken = createRefreshToken(user);
        auditService.log(user, "LOGIN", "USER", user.getId(), "User logged in");
        return buildAuthResponse(user, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();
        userBanService.syncBanStatus(user);
        validateUserCanAuthenticate(user);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        RefreshToken rotatedToken = createRefreshToken(user);
        auditService.log(user, "REFRESH_TOKEN", "USER", user.getId(), "Access token refreshed");
        return buildAuthResponse(user, rotatedToken.getToken());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        auditService.log(storedToken.getUser(), "LOGOUT", "USER", storedToken.getUser().getId(), "Refresh token revoked");
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        String token = jwtTokenProvider.generateAccessToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshValidityInMilliseconds / 1000));
        return refreshTokenRepository.save(refreshToken);
    }

    private void revokeActiveRefreshTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
        }
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    private void validateUserCanAuthenticate(User user) {
        if (!user.isActive() || user.isBanned()) {
            throw new UnauthorizedException("User is not allowed to login");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireTrimmed(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(message);
        }
        return trimmed;
    }
}

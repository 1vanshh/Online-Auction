package com.auction.authservice.service;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.LogoutRequest;
import com.auction.authservice.dto.request.RefreshTokenRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.RefreshToken;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.BadRequestException;
import com.auction.authservice.exception.NotFoundException;
import com.auction.authservice.exception.UnauthorizedException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.RefreshTokenRepository;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserBanService userBanService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User mappedUser;
    private User savedUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshValidityInMilliseconds", 2_592_000_000L);

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Ivan");
        registerRequest.setLastName("Ivanov");
        registerRequest.setEmail("ivan@example.com");
        registerRequest.setPassword("secret123");
        registerRequest.setPhone("+79991234567");

        mappedUser = new User();
        mappedUser.setFirstName("Ivan");
        mappedUser.setLastName("Ivanov");
        mappedUser.setEmail("ivan@example.com");
        mappedUser.setPasswordHash("secret123");
        mappedUser.setPhone("+79991234567");
        mappedUser.setRole(Role.USER);

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFirstName("Ivan");
        savedUser.setLastName("Ivanov");
        savedUser.setEmail("ivan@example.com");
        savedUser.setPasswordHash("encoded-secret");
        savedUser.setPhone("+79991234567");
        savedUser.setRole(Role.USER);
        savedUser.setActive(true);
        savedUser.setBanned(false);

        userResponse = UserResponse.builder()
                .id(1L)
                .email("ivan@example.com")
                .firstName("Ivan")
                .role(Role.USER)
                .active(true)
                .banned(false)
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(userMapper.toUser(registerRequest)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(savedUser)).thenReturn("jwt-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.register(registerRequest);

        assertEquals("jwt-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertSame(userResponse, response.getUser());
        assertEquals("encoded-secret", mappedUser.getPasswordHash());
        verify(auditService).log(eq(savedUser), eq("REGISTER"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void shouldRejectRegistrationWhenLastNameBlank() {
        registerRequest.setLastName("   ");
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(userMapper.toUser(registerRequest)).thenReturn(mappedUser);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

        assertEquals("Last name is required", ex.getMessage());
    }

    @Test
    void shouldReturnUserByEmail() {
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        UserResponse response = authService.getByEmail("ivan@example.com");

        assertSame(userResponse, response);
    }

    @Test
    void shouldThrowNotFoundWhenUserByEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> authService.getByEmail("missing@example.com"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("secret123", "encoded-secret")).thenReturn(true);
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(savedUser)).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(savedUser)).thenReturn("jwt-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(userBanService).syncBanStatus(savedUser);
        verify(auditService).log(eq(savedUser), eq("LOGIN"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void shouldRejectLoginWhenBannedAfterBanSync() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        doAnswer(invocation -> {
            savedUser.setBanned(true);
            savedUser.setActive(false);
            return true;
        }).when(userBanService).syncBanStatus(savedUser);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("User is not allowed to login", ex.getMessage());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(savedUser);
        storedToken.setToken("refresh-token");
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        storedToken.setRevoked(false);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(savedUser)).thenReturn("new-access-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.refresh(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertNotEquals("refresh-token", response.getRefreshToken());
        assertTrue(storedToken.isRevoked());
        verify(auditService).log(eq(savedUser), eq("REFRESH_TOKEN"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(savedUser);
        storedToken.setToken("refresh-token");
        storedToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.refresh(request));

        assertEquals("Refresh token expired or revoked", ex.getMessage());
        assertTrue(storedToken.isRevoked());
    }

    @Test
    void shouldLogoutSuccessfully() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(savedUser);
        storedToken.setToken("refresh-token");
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        storedToken.setRevoked(false);

        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(storedToken)).thenReturn(storedToken);

        authService.logout(request);

        assertTrue(storedToken.isRevoked());
        verify(auditService).log(eq(savedUser), eq("LOGOUT"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void shouldRevokeOldRefreshTokensOnLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(savedUser);
        oldToken.setToken("old-token");
        oldToken.setRevoked(false);

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("secret123", "encoded-secret")).thenReturn(true);
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(savedUser)).thenReturn(List.of(oldToken));
        when(refreshTokenRepository.saveAll(
                argThat(tokens -> {
                    List<RefreshToken> list = StreamSupport
                            .stream(tokens.spliterator(), false)
                            .toList();
                    return list.size() == 1 && list.get(0).isRevoked();
                })
        )).thenReturn(List.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(savedUser)).thenReturn("jwt-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        authService.login(request);

        assertTrue(oldToken.isRevoked());
        verify(refreshTokenRepository).saveAll(any());
    }
}

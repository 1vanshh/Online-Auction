package com.auction.authservice.service;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.BadRequestException;
import com.auction.authservice.exception.UnauthorizedException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User mappedUser;
    private User savedUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
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
        when(jwtTokenProvider.generateToken(savedUser)).thenReturn("jwt-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.register(registerRequest);

        assertEquals("jwt-token", response.getAccessToken());
        assertSame(userResponse, response.getUser());
        assertEquals("encoded-secret", mappedUser.getPasswordHash());
        verify(userRepository).existsByEmail("ivan@example.com");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(mappedUser);
        verify(jwtTokenProvider).generateToken(savedUser);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

        assertEquals("Email already in use", ex.getMessage());
        verify(userMapper, never()).toUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnUserByEmail() {
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        UserResponse response = authService.getByEmail("ivan@example.com");

        assertSame(userResponse, response);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUserByEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.getByEmail("missing@example.com"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("secret123", "encoded-secret")).thenReturn(true);
        when(jwtTokenProvider.generateToken(savedUser)).thenReturn("jwt-token");
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertSame(userResponse, response.getUser());
    }

    @Test
    void shouldRejectLoginWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldRejectLoginWhenUserInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");
        savedUser.setActive(false);

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("User is not allowed to login", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldRejectLoginWhenUserBanned() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");
        savedUser.setBanned(true);

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("User is not allowed to login", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("bad-password");

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("bad-password", "encoded-secret")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(jwtTokenProvider, never()).generateToken(any());
    }
}

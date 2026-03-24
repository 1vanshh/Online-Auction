package com.auction.authservice.service;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.BadRequestException;
import com.auction.authservice.exception.UnauthorizedException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       UserMapper userMapper, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // Users registration
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Convert RegisterRequest to User
        User user = userMapper.toUser(registerRequest);

        // Encrypting the password
        String encodedPassword = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(encodedPassword);

        // Save to DB
        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);
        return AuthResponse.builder()
                .accessToken(token)
                .user(userMapper.toUserResponse(savedUser))
                .build();
    }

    // Get user by email
    public UserResponse getByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return userMapper.toUserResponse(user.get());
        }
        throw new RuntimeException("User not found");
    }

    // User login (authenticate and generate JWT)
    public AuthResponse login(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        User user = userOpt.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!user.isActive() || user.isBanned()) {
            throw new UnauthorizedException("User is not allowed to login");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .user(userMapper.toUserResponse(user))
                .build();
    }
}

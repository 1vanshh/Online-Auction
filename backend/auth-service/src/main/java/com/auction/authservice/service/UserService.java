package com.auction.authservice.service;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.BadRequestException;
import com.auction.authservice.exception.NotFoundException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserBanService userBanService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       UserBanService userBanService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userBanService = userBanService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        userBanService.syncBanStatus(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("User not found"));
        userBanService.syncBanStatus(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        applyUpdate(existingUser, request);
        userRepository.save(existingUser);
        auditService.log(existingUser, "USER_PROFILE_UPDATED", "USER", existingUser.getId(), "User updated own profile by id");
        return userMapper.toUserResponse(existingUser);
    }

    @Transactional
    public UserResponse updateByEmail(String email, UpdateUserRequest request) {
        User existingUser = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("User not found"));
        applyUpdate(existingUser, request);
        userRepository.save(existingUser);
        auditService.log(existingUser, "USER_PROFILE_UPDATED", "USER", existingUser.getId(), "User updated own profile");
        return userMapper.toUserResponse(existingUser);
    }

    @Transactional
    public UserResponse adminUpdate(Long id, AdminUpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        if (request.getActive() != null && request.getBanned() != null && request.getActive() && request.getBanned()) {
            throw new BadRequestException("User cannot be active and banned at the same time");
        }

        if (request.getBanned() != null) {
            if (request.getBanned()) {
                existingUser.setBanned(true);
                existingUser.setActive(false);

                userBanService.createBan(
                        existingUser,
                        null,
                        UserBanService.DEFAULT_ADMIN_BAN_REASON,
                        LocalDateTime.now().plusDays(UserBanService.DEFAULT_ADMIN_BAN_DAYS)
                );
            } else {
                userBanService.revokeAllActiveBans(existingUser);
                existingUser.setBanned(false);
                if (request.getActive() == null) {
                    existingUser.setActive(true);
                }
            }
        }

        if (request.getActive() != null) {
            if (request.getActive() && existingUser.isBanned()) {
                throw new BadRequestException("Banned user cannot be activated");
            }
            existingUser.setActive(request.getActive());
        }

        if (existingUser.getRole() == Role.ADMIN && !existingUser.isActive() && !existingUser.isBanned()) {
            // allowed, no-op; explicit to make admin management intent clearer
        }

        userRepository.save(existingUser);
        auditService.log(null, "ADMIN_USER_UPDATED", "USER", existingUser.getId(), buildAdminUpdateDetails(existingUser));
        return userMapper.toUserResponse(existingUser);
    }

    private void applyUpdate(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(requireTrimmed(request.getFirstName(), "First name cannot be blank"));
        }
        if (request.getLastName() != null) {
            user.setLastName(requireTrimmed(request.getLastName(), "Last name cannot be blank"));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getCountry() != null) {
            user.setCountry(trimToNull(request.getCountry()));
        }
        if (request.getCity() != null) {
            user.setCity(trimToNull(request.getCity()));
        }
        if (request.getAddressLine() != null) {
            user.setAddressLine(trimToNull(request.getAddressLine()));
        }
        if (request.getPostalCode() != null) {
            user.setPostalCode(trimToNull(request.getPostalCode()));
        }
    }

    private String buildAdminUpdateDetails(User user) {
        return "role=" + user.getRole() + ", active=" + user.isActive() + ", banned=" + user.isBanned();
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

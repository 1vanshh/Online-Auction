package com.auction.authservice.service;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.NotFoundException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        applyUpdate(existingUser, request);
        userRepository.save(existingUser);
        return userMapper.toUserResponse(existingUser);
    }

    public UserResponse updateByEmail(String email, UpdateUserRequest request) {
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        applyUpdate(existingUser, request);
        userRepository.save(existingUser);
        return userMapper.toUserResponse(existingUser);
    }

    public UserResponse adminUpdate(Long id, AdminUpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (request.getActive() != null) {
            existingUser.setActive(request.getActive());
        }
        if (request.getBanned() != null) {
            existingUser.setBanned(request.getBanned());
        }
        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }
        userRepository.save(existingUser);
        return userMapper.toUserResponse(existingUser);
    }

    private void applyUpdate(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getAddressLine() != null) {
            user.setAddressLine(request.getAddressLine());
        }
        if (request.getPostalCode() != null) {
            user.setPostalCode(request.getPostalCode());
        }
    }
}

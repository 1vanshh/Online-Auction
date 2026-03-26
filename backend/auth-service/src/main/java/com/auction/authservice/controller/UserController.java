package com.auction.authservice.controller;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.exception.UnauthorizedException;
import com.auction.authservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getByEmail(getCurrentEmail());
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateByEmail(getCurrentEmail(), request);
    }

    @GetMapping("/admin/{id}")
    public UserResponse getUserById(@PathVariable("id") Long id) {
        return userService.getById(id);
    }

    @PutMapping("/admin/{id}")
    public UserResponse adminUpdateUser(@PathVariable("id") Long id,
                                        @Valid @RequestBody AdminUpdateUserRequest request) {
        return userService.adminUpdate(id, request);
    }

    private String getCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return authentication.getName();
    }
}

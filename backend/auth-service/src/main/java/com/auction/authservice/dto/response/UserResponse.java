package com.auction.authservice.dto.response;

import com.auction.authservice.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    private LocalDate birthDate;
    private String country;
    private String city;
    private String addressLine;
    private String postalCode;

    private boolean active;
    private boolean banned;

    private Role role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
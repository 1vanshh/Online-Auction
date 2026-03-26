package com.auction.authservice.mapper;

import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .country(user.getCountry())
                .city(user.getCity())
                .addressLine(user.getAddressLine())
                .postalCode(user.getPostalCode())
                .active(user.isActive())
                .banned(user.isBanned())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toUser(RegisterRequest registerRequest) {
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(registerRequest.getPassword());
        user.setPhone(registerRequest.getPhone());
        return user;
    }
}

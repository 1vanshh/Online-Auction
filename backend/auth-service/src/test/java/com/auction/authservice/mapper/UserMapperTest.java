package com.auction.authservice.mapper;

import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void shouldMapUserToUserResponse() {
        User user = new User();
        user.setId(10L);
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setEmail("ivan@example.com");
        user.setPhone("+79991234567");
        user.setBirthDate(LocalDate.of(1995, 5, 10));
        user.setCountry("Russia");
        user.setCity("Moscow");
        user.setAddressLine("Tverskaya 1");
        user.setPostalCode("125009");
        user.setActive(true);
        user.setBanned(false);
        user.setRole(Role.ADMIN);
        user.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 10, 0));

        UserResponse response = mapper.toUserResponse(user);

        assertEquals(10L, response.getId());
        assertEquals("Ivan", response.getFirstName());
        assertEquals("Ivanov", response.getLastName());
        assertEquals("ivan@example.com", response.getEmail());
        assertEquals("+79991234567", response.getPhone());
        assertEquals(LocalDate.of(1995, 5, 10), response.getBirthDate());
        assertEquals("Russia", response.getCountry());
        assertEquals("Moscow", response.getCity());
        assertEquals("Tverskaya 1", response.getAddressLine());
        assertEquals("125009", response.getPostalCode());
        assertTrue(response.isActive());
        assertFalse(response.isBanned());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0), response.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 1, 2, 10, 0), response.getUpdatedAt());
    }

    @Test
    void shouldMapRegisterRequestToUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Anna");
        request.setLastName("Petrova");
        request.setEmail("anna@example.com");
        request.setPassword("plain-password");
        request.setPhone("+70000000000");

        User user = mapper.toUser(request);

        assertNull(user.getId());
        assertEquals("Anna", user.getFirstName());
        assertEquals("Petrova", user.getLastName());
        assertEquals("anna@example.com", user.getEmail());
        assertEquals("plain-password", user.getPasswordHash());
        assertEquals("+70000000000", user.getPhone());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isActive());
        assertFalse(user.isBanned());
    }
}

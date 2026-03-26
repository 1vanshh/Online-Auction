package com.auction.authservice.service;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.NotFoundException;
import com.auction.authservice.mapper.UserMapper;
import com.auction.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setEmail("ivan@example.com");
        user.setPhone("+79991234567");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setCountry("Russia");
        user.setCity("Moscow");
        user.setAddressLine("Old address");
        user.setPostalCode("101000");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setBanned(false);

        response = UserResponse.builder().id(1L).email("ivan@example.com").firstName("Ivan").build();
    }

    @Test
    void shouldGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        UserResponse actual = userService.getById(1L);

        assertSame(response, actual);
    }

    @Test
    void shouldThrowWhenUserByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getById(99L));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldGetUserByEmail() {
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        UserResponse actual = userService.getByEmail("ivan@example.com");

        assertSame(response, actual);
    }

    @Test
    void shouldThrowWhenUserByEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getByEmail("missing@example.com"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldUpdateUserByIdPartially() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Petr");
        request.setPhone("+70000000000");
        request.setCity("Saint Petersburg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        UserResponse actual = userService.update(1L, request);

        assertSame(response, actual);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Petr", saved.getFirstName());
        assertEquals("Ivanov", saved.getLastName());
        assertEquals("+70000000000", saved.getPhone());
        assertEquals("Saint Petersburg", saved.getCity());
        assertEquals("Old address", saved.getAddressLine());
    }

    @Test
    void shouldThrowWhenUpdatingByIdMissingUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New Name");

        assertThrows(NotFoundException.class, () -> userService.update(7L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateUserByEmailIncludingAllFields() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Anna");
        request.setLastName("Petrova");
        request.setPhone("+71111111111");
        request.setBirthDate(LocalDate.of(1999, 12, 31));
        request.setCountry("Spain");
        request.setCity("Madrid");
        request.setAddressLine("Gran Via 1");
        request.setPostalCode("28013");

        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        userService.updateByEmail("ivan@example.com", request);

        verify(userRepository).save(user);
        assertEquals("Anna", user.getFirstName());
        assertEquals("Petrova", user.getLastName());
        assertEquals("+71111111111", user.getPhone());
        assertEquals(LocalDate.of(1999, 12, 31), user.getBirthDate());
        assertEquals("Spain", user.getCountry());
        assertEquals("Madrid", user.getCity());
        assertEquals("Gran Via 1", user.getAddressLine());
        assertEquals("28013", user.getPostalCode());
    }

    @Test
    void shouldThrowWhenUpdatingByEmailMissingUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateByEmail("missing@example.com", new UpdateUserRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldAdminUpdateOnlyProvidedFields() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setActive(false);
        request.setRole(Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        userService.adminUpdate(1L, request);

        verify(userRepository).save(user);
        assertFalse(user.isActive());
        assertFalse(user.isBanned());
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    void shouldAdminUpdateBannedFlagOnly() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setBanned(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        userService.adminUpdate(1L, request);

        assertTrue(user.isBanned());
        assertTrue(user.isActive());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldThrowWhenAdminUpdatingMissingUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> userService.adminUpdate(2L, new AdminUpdateUserRequest()));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}

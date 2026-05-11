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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBanService userBanService;
    @Mock
    private AuditService auditService;
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
    void shouldListAllUsersForAdminPanel() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());
        verify(userBanService).syncBanStatus(user);
    }

    @Test
    void shouldGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        UserResponse actual = userService.getById(1L);

        assertSame(response, actual);
        verify(userBanService).syncBanStatus(user);
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
        verify(userBanService).syncBanStatus(user);
    }

    @Test
    void shouldUpdateUserByIdPartially() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("  Petr  ");
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
        verify(auditService).log(eq(user), eq("USER_PROFILE_UPDATED"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void shouldRejectBlankLastNameOnSelfUpdate() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setLastName("   ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.update(1L, request));

        assertEquals("Last name cannot be blank", ex.getMessage());
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
    void shouldAdminBanUserAndDeactivate() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setBanned(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        userService.adminUpdate(1L, request);

        verify(userBanService).createBan(eq(user), isNull(), eq(UserBanService.DEFAULT_ADMIN_BAN_REASON), any());
        verify(userRepository).save(user);
        assertTrue(user.isBanned());
        assertFalse(user.isActive());
        verify(auditService).log(isNull(), eq("ADMIN_USER_UPDATED"), eq("USER"), eq(1L), contains("banned=true"));
    }

    @Test
    void shouldRejectActivationOfBannedUser() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setActive(true);
        user.setBanned(true);
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.adminUpdate(1L, request));

        assertEquals("Banned user cannot be activated", ex.getMessage());
    }

    @Test
    void shouldUnbanUserAndActivateWhenRequested() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setBanned(false);

        user.setBanned(true);
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        userService.adminUpdate(1L, request);

        verify(userBanService).revokeAllActiveBans(user);
        assertFalse(user.isBanned());
        assertTrue(user.isActive());
    }
}

package com.auction.authservice.controller;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.entity.User;
import com.auction.authservice.exception.GlobalExceptionHandler;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.security.JwtTokenProvider;
import com.auction.authservice.security.SecurityConfig;
import com.auction.authservice.service.UserBanService;
import com.auction.authservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserBanService userBanService;

    @Test
    void shouldRejectCurrentUserEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnCurrentUserWhenTokenValid() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("Ivan")
                .role(Role.USER)
                .build();

        User user = new User();
        user.setEmail("user@example.com");
        user.setActive(true);
        user.setBanned(false);

        when(jwtTokenProvider.getEmailFromToken("user-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("user-token")).thenReturn("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userService.getByEmail("user@example.com")).thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void shouldRejectCurrentUserUpdateWhenPayloadInvalid() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("wrong");
        request.setBirthDate(LocalDate.now().plusDays(1));

        User user = new User();
        user.setEmail("user@example.com");
        user.setActive(true);
        user.setBanned(false);

        when(jwtTokenProvider.getEmailFromToken("user-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("user-token")).thenReturn("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/users/me")
                        .with(csrf())
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(userService, never()).updateByEmail(anyString(), any());
    }

    @Test
    void shouldAllowAdminUpdateForAdminRole() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setActive(false);
        request.setBanned(true);
        request.setRole(Role.ADMIN);

        UserResponse response = UserResponse.builder()
                .id(2L)
                .email("target@example.com")
                .active(false)
                .banned(true)
                .role(Role.ADMIN)
                .build();

        when(userService.adminUpdate(eq(2L), any(AdminUpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/admin/2")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true));
    }

    @Test
    void shouldRejectAdminGetForRegularUser() throws Exception {
        mockMvc.perform(get("/users/admin/1")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        verify(userService, never()).getById(anyLong());
    }
}

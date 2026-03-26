package com.auction.authservice.controller;

import com.auction.authservice.dto.request.AdminUpdateUserRequest;
import com.auction.authservice.dto.request.UpdateUserRequest;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.exception.GlobalExceptionHandler;
import com.auction.authservice.security.JwtTokenProvider;
import com.auction.authservice.security.SecurityConfig;
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

        when(jwtTokenProvider.getEmailFromToken("user-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("user-token")).thenReturn("USER");
        when(userService.getByEmail("user@example.com")).thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ivan"));
    }

    @Test
    void shouldUpdateCurrentUserWhenTokenValidAndPayloadValid() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Petr");
        request.setCity("Moscow");
        request.setBirthDate(LocalDate.of(2000, 1, 1));

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("Petr")
                .city("Moscow")
                .build();

        when(jwtTokenProvider.getEmailFromToken("user-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("user-token")).thenReturn("USER");
        when(userService.updateByEmail(eq("user@example.com"), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/me")
                        .with(csrf())
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.city").value("Moscow"));
    }

    @Test
    void shouldRejectCurrentUserUpdateWhenPayloadInvalid() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("wrong");
        request.setBirthDate(LocalDate.now().plusDays(1));

        when(jwtTokenProvider.getEmailFromToken("user-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("user-token")).thenReturn("USER");

        mockMvc.perform(put("/users/me")
                        .with(csrf())
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.phone").value("Invalid phone format"))
                .andExpect(jsonPath("$.fieldErrors.birthDate").value("Birth date must be in the past"));

        verify(userService, never()).updateByEmail(anyString(), any());
    }

    @Test
    void shouldRejectAdminGetForRegularUser() throws Exception {
        mockMvc.perform(get("/users/admin/1")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        verify(userService, never()).getById(anyLong());
    }

    @Test
    void shouldAllowAdminGetForAdminRole() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(2L)
                .email("target@example.com")
                .build();

        when(userService.getById(2L)).thenReturn(response);

        mockMvc.perform(get("/users/admin/2")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target@example.com"));
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
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.banned").value(true))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldTreatInvalidTokenAsUnauthenticated() throws Exception {
        when(jwtTokenProvider.getEmailFromToken("bad-token"))
                .thenThrow(new RuntimeException("bad token"));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }
}
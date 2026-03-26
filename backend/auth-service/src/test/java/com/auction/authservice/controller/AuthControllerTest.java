package com.auction.authservice.controller;

import com.auction.authservice.dto.request.LoginRequest;
import com.auction.authservice.dto.request.LogoutRequest;
import com.auction.authservice.dto.request.RefreshTokenRequest;
import com.auction.authservice.dto.request.RegisterRequest;
import com.auction.authservice.dto.response.AuthResponse;
import com.auction.authservice.dto.response.UserResponse;
import com.auction.authservice.entity.Role;
import com.auction.authservice.exception.GlobalExceptionHandler;
import com.auction.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");
        request.setPhone("+79991234567");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .refreshToken("refresh-token")
                .user(UserResponse.builder().id(1L).email("ivan@example.com").role(Role.USER).build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectRegisterWhenPayloadInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("");
        request.setLastName(" ");
        request.setEmail("bad-email");
        request.setPassword("123");
        request.setPhone("not-a-phone");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Invalid phone format"));
    }

    @Test
    void shouldSupportRefreshAndLogout() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("refresh-token");

        AuthResponse refreshResponse = AuthResponse.builder()
                .accessToken("new-token")
                .refreshToken("new-refresh-token")
                .user(UserResponse.builder().id(1L).email("ivan@example.com").build())
                .build();

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(refreshResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-token"));

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken("new-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void shouldLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .refreshToken("refresh-token")
                .user(UserResponse.builder().id(1L).email("ivan@example.com").build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.email").value("ivan@example.com"));
    }
}

package com.auction.authservice.controller;

import com.auction.authservice.dto.request.LoginRequest;
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
                .user(UserResponse.builder().id(1L).email("ivan@example.com").role(Role.USER).build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("ivan@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldRejectRegisterWhenPayloadInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("");
        request.setEmail("bad-email");
        request.setPassword("123");
        request.setPhone("not-a-phone");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Invalid phone format"));
    }

    @Test
    void shouldLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ivan@example.com");
        request.setPassword("secret123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .user(UserResponse.builder().id(1L).email("ivan@example.com").build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("ivan@example.com"));
    }

    @Test
    void shouldRejectLoginWhenPayloadInvalid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bad-email");
        request.setPassword("");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}

package com.auction.authservice.security;

import com.auction.authservice.entity.User;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.service.UserBanService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserBanService userBanService;
    @Mock
    private FilterChain filterChain;

    private JwtTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtTokenFilter(jwtTokenProvider, userRepository, userBanService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSetAuthenticationWhenBearerTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setEmail("user@example.com");
        user.setActive(true);
        user.setBanned(false);

        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("valid-token")).thenReturn("ADMIN");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user@example.com", authentication.getName());
        verify(userBanService).syncBanStatus(user);
    }

    @Test
    void shouldClearContextWhenBanServiceMarksUserAsBanned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setEmail("user@example.com");
        user.setActive(true);
        user.setBanned(false);

        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("user@example.com");
        when(jwtTokenProvider.getRoleFromToken("valid-token")).thenReturn("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            user.setBanned(true);
            user.setActive(false);
            return true;
        }).when(userBanService).syncBanStatus(user);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

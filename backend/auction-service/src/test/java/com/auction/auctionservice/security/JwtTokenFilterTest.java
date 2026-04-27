package com.auction.auctionservice.security;

import com.auction.auctionservice.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider provider;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private FilterChain chain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateAllowedUser() throws Exception {
        JwtTokenFilter filter = new JwtTokenFilter(provider, jdbc);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(provider.getUserIdFromToken("token")).thenReturn(10L);
        when(provider.getRoleFromToken("token")).thenReturn("USER");
        when(jdbc.queryForObject("select count(*) from auth.users where id = ? and is_active = true and is_banned = false", Integer.class, 10L)).thenReturn(1);
        when(jdbc.queryForObject("select email from auth.users where id = ?", String.class, 10L)).thenReturn("seller@example.com");

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(new AuthenticatedUser(10L, "seller@example.com", Role.USER), authentication.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateBannedOrInactiveUser() throws Exception {
        JwtTokenFilter filter = new JwtTokenFilter(provider, jdbc);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(provider.getUserIdFromToken("token")).thenReturn(10L);
        when(provider.getRoleFromToken("token")).thenReturn("USER");
        when(jdbc.queryForObject("select count(*) from auth.users where id = ? and is_active = true and is_banned = false", Integer.class, 10L)).thenReturn(0);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jdbc, never()).queryForObject("select email from auth.users where id = ?", String.class, 10L);
    }

    @Test
    void shouldIgnoreRequestWithoutBearerToken() throws Exception {
        JwtTokenFilter filter = new JwtTokenFilter(provider, jdbc);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(provider, jdbc);
        verify(chain).doFilter(request, response);
    }
}

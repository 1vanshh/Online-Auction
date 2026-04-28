package com.auction.biddingservice.service;

import com.auction.biddingservice.entity.Role;
import com.auction.biddingservice.exception.ForbiddenException;
import com.auction.biddingservice.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {
    private final CurrentUserService service = new CurrentUserService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentAuthenticatedUser() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user@test.com", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        AuthenticatedUser result = service.getCurrentUser();

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldRejectMissingAuthentication() {
        assertThatThrownBy(service::getCurrentUser)
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void shouldDetectAdminRole() {
        assertThat(service.isAdmin(new AuthenticatedUser(1L, "admin@test.com", Role.ADMIN))).isTrue();
        assertThat(service.isAdmin(new AuthenticatedUser(2L, "user@test.com", Role.USER))).isFalse();
    }
}

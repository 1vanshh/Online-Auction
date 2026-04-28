package com.auction.biddingservice.service;

import com.auction.biddingservice.entity.Role;
import com.auction.biddingservice.exception.ForbiddenException;
import com.auction.biddingservice.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Authentication is required");
        }
        return user;
    }

    public boolean isAdmin(AuthenticatedUser user) {
        return user.role() == Role.ADMIN;
    }
}

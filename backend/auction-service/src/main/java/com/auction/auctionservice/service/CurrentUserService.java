package com.auction.auctionservice.service;

import com.auction.auctionservice.entity.Role;
import com.auction.auctionservice.exception.ForbiddenException;
import com.auction.auctionservice.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public AuthenticatedUser getCurrentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthenticatedUser u))
            throw new ForbiddenException("Authentication is required");
        return u;
    }

    public boolean isAdmin(AuthenticatedUser u) {
        return u.role() == Role.ADMIN;
    }
}

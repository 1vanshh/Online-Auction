package com.auction.biddingservice.security;

import com.auction.biddingservice.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}

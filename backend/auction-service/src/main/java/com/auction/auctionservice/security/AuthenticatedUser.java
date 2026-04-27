package com.auction.auctionservice.security;

import com.auction.auctionservice.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}

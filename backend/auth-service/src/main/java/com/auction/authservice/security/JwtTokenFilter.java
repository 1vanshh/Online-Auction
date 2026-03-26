package com.auction.authservice.security;

import com.auction.authservice.entity.User;
import com.auction.authservice.repository.UserRepository;
import com.auction.authservice.service.UserBanService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserBanService userBanService;

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          UserBanService userBanService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userBanService = userBanService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtTokenProvider.getEmailFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        userBanService.syncBanStatus(user);
                        if (user.isActive() && !user.isBanned()) {
                            List<GrantedAuthority> authorities = new ArrayList<>();
                            if (role != null) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            }
                            var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            SecurityContextHolder.clearContext();
                        }
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT authentication failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

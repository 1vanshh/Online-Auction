package com.auction.biddingservice.security;

import com.auction.biddingservice.entity.Role;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenFilter.class);
    private final JwtTokenProvider provider;
    private final JdbcTemplate jdbc;

    public JwtTokenFilter(JwtTokenProvider provider, JdbcTemplate jdbc) {
        this.provider = provider;
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = getToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long id = provider.getUserIdFromToken(token);
                String roleValue = provider.getRoleFromToken(token);
                if (id != null && roleValue != null && isAllowed(id)) {
                    String email = email(id);
                    Role role = Role.valueOf(roleValue);
                    AuthenticatedUser principal = new AuthenticatedUser(id, email, role);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT authentication failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(Long id) {
        Integer count = jdbc.queryForObject("select count(*) from auth.users where id = ? and is_active = true and is_banned = false", Integer.class, id);
        return count != null && count > 0;
    }

    private String email(Long id) {
        return jdbc.queryForObject("select email from auth.users where id = ?", String.class, id);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}

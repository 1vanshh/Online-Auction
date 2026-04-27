package com.auction.auctionservice.security;

import com.auction.auctionservice.entity.Role;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
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
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String token = getToken(req);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long id = provider.getUserIdFromToken(token);
                String roleValue = provider.getRoleFromToken(token);
                if (id != null && roleValue != null && isAllowed(id)) {
                    String email = email(id);
                    Role role = Role.valueOf(roleValue);
                    AuthenticatedUser p = new AuthenticatedUser(id, email, role);
                    var a = new UsernamePasswordAuthenticationToken(p, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                    SecurityContextHolder.getContext().setAuthentication(a);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT authentication failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    private boolean isAllowed(Long id) {
        Integer c = jdbc.queryForObject("select count(*) from auth.users where id = ? and is_active = true and is_banned = false", Integer.class, id);
        return c != null && c > 0;
    }

    private String email(Long id) {
        return jdbc.queryForObject("select email from auth.users where id = ?", String.class, id);
    }

    private String getToken(HttpServletRequest r) {
        String h = r.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }
}

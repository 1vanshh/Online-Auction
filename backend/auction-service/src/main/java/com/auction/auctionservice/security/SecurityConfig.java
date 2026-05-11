package com.auction.auctionservice.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtTokenProvider provider;
    private final JdbcTemplate jdbc;

    public SecurityConfig(JwtTokenProvider provider, JdbcTemplate jdbc) {
        this.provider = provider;
        this.jdbc = jdbc;
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(provider, jdbc);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().requestMatchers(HttpMethod.GET, "/categories/**", "/lots/**", "/uploads/**").permitAll().requestMatchers("/admin/**", "/categories/admin/**").hasRole("ADMIN").anyRequest().authenticated()).addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class).build();
    }
}

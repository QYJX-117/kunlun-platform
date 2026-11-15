package com.kunlun.platform.config;

import com.kunlun.platform.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        log.debug("JwtAuthFilter processing: {} {}", request.getMethod(), path);

        if (path.startsWith("/api/auth/")) {
            log.debug("Skipping auth path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            log.debug("Missing or invalid Authorization header for: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未提供有效的认证令牌\"}");
            return;
        }

        String token = header.substring(7);

        try {
            if (JwtUtil.isTokenExpired(token)) {
                log.debug("Token expired for: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"令牌已过期\"}");
                return;
            }

            Claims claims = JwtUtil.parseToken(token);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            if (username == null || role == null) {
                log.debug("Invalid token claims for: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"令牌内容无效\"}");
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user: {} with role: {}", username, role);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT parse error for: {}: {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"令牌无效\"}");
        }
    }
}

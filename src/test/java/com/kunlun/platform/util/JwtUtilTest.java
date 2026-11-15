package com.kunlun.platform.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void testGenerateAndParseToken() {
        String username = "dispatcher";
        String role = "dispatcher";

        String token = JwtUtil.generateToken(username, role);
        assertNotNull(token);

        Claims claims = JwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals(username, claims.get("username", String.class));
        assertEquals(role, claims.get("role", String.class));
        assertEquals(username, claims.getSubject());
    }

    @Test
    void testTokenExpiration() {
        String token = JwtUtil.generateToken("test", "manager");
        assertFalse(JwtUtil.isTokenExpired(token));
    }
}

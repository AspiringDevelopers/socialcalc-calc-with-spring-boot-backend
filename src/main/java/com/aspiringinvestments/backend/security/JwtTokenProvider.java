package com.aspiringinvestments.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a 256-bit key

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;

    @Autowired
    private Environment environment;

    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("dev"))) {
                throw new IllegalStateException(
                    "JWT_SECRET is not set. Set the JWT_SECRET environment variable "
                    + "(at least " + MIN_SECRET_BYTES + " bytes), or run with the 'dev' profile "
                    + "to use a random per-process key.");
            }
            System.err.println("WARNING: JWT_SECRET not set; using a random key (dev profile). "
                + "Tokens will not survive a restart.");
            signingKey = Jwts.SIG.HS256.key().build();
            return;
        }
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes, got " + secretBytes.length + ".");
        }
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String createToken(String userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT token compact of handler are invalid: " + e.getMessage());
        }
        return false;
    }
}
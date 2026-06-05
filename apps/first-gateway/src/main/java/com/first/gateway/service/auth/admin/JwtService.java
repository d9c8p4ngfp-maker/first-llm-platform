package com.first.gateway.service.auth.admin;

import com.first.gateway.config.AuthProperties;
import com.first.gateway.domain.enums.TenantRole;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final AuthProperties authProperties;
    private final SecretKey secretKey;
    private final JwtBlacklistStore blacklistStore;

    public JwtService(AuthProperties authProperties, JwtBlacklistStore blacklistStore) {
        this.authProperties = authProperties;
        byte[] keyBytes = authProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.blacklistStore = blacklistStore;
    }

    public String createToken(AdminPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(authProperties.getExpireHours(), ChronoUnit.HOURS);
        return Jwts.builder()
            .id(java.util.UUID.randomUUID().toString())
            .subject(String.valueOf(principal.userId()))
            .claim("tenantId", principal.tenantId())
            .claim("username", principal.username())
            .claim("role", principal.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();
    }

    public AdminPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return new AdminPrincipal(
                Long.parseLong(claims.getSubject()),
                claims.get("tenantId", Number.class).longValue(),
                claims.get("username", String.class),
                TenantRole.valueOf(claims.get("role", String.class))
            );
        } catch (Exception ex) {
            throw new GatewayException(GatewayError.INVALID_JWT);
        }
    }

    public long expiresInSeconds() {
        return authProperties.getExpireHours() * 3600L;
    }

    public void blacklist(String token) {
        try {
            Claims claims = parseClaims(token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                blacklistStore.push(claims.getId(), ttl);
            }
        } catch (GatewayException e) {
            throw e;
        } catch (Exception ignored) {
            // token already invalid, nothing to blacklist
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Claims claims = parseClaims(token);
            return blacklistStore.contains(claims.getId());
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
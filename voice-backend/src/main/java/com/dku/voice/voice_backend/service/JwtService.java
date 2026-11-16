package com.dku.voice.voice_backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private static final String BLACKLIST_PREFIX = "blacklist:jwt:";

    /**
     * JWT 토큰 생성
     * - subject: username
     * - claim: role
     * - 만료 시간: application.yml 설정값 (기본 24시간)
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰에서 username 추출
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 role 추출
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 토큰 유효성 검증
     * - 만료 여부 확인
     * - 블랙리스트 확인 (로그아웃된 토큰)
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            if (claims.getExpiration().before(new Date())) {
                return false;
            }
            // 블랙리스트 확인
            return !isBlacklisted(token);
        } catch (Exception e) {
            log.warn("[JWT] 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 블랙리스트 등록 (로그아웃 시 호출)
     * - TTL: 토큰 남은 유효시간
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = getClaims(token);
            long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token,
                        "logout",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                );
                log.info("[JWT] 토큰 블랙리스트 등록 완료");
            }
        } catch (Exception e) {
            log.warn("[JWT] 블랙리스트 등록 실패: {}", e.getMessage());
        }
    }

    /**
     * 블랙리스트 확인
     */
    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
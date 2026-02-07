package com.paymentSystem.project.utils;

import com.paymentSystem.project.config.RedisConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.security.KeyRep.Type.SECRET;

/** Future Scope
 * refresh token
 */

@Component
public class JwtUtil {

    private final String SECRET = "################";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));//to be changes in production
    private final long EXPIRATION = 1000 * 60 * 15; // 15 minutes

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);

        String jti = UUID.randomUUID().toString();

        // will add roles later
        return Jwts.builder()
                .setId(jti)  // for logout
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();

    }

    public Long extractUserId(String token) {
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(SECRET.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public String extractJti(String token) {
        return
                Jwts.parserBuilder()
                        .setSigningKey(SECRET.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getId();
    }

    public void logout(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        String jti = claims.getId();
        Date expiration = claims.getExpiration();

        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + jti, "true", ttl, TimeUnit.MILLISECONDS);
        }
    }
}

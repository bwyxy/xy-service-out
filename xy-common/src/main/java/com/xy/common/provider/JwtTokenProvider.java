package com.xy.common.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${jwt.header}")
    private String jwtHeader;

    @Value("${jwt.prefix}")
    private String jwtPrefix;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT 密钥长度不足 ({} bytes)，自动补齐至 32 bytes", keyBytes.length);
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getJwtHeader() {
        return jwtHeader;
    }
    public String getJwtPrefix() {
        return jwtPrefix;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), jwtExpiration);
    }

    /**
     * 创建客户端小程序登录 token
     *
     * @param subject 主题，格式为「客户端前缀 + 用户主键」，如待办的 todo_10000001、聊天的 chat_10000001
     */
    public String createWeChatToken(String subject) {
        return createToken(new HashMap<>(), subject, jwtExpiration);
    }

    /**
     * 创建指定有效期和主题的 token
     *
     * @param subject          主题（用户标识）
     * @param expirationMillis 有效期（毫秒）
     */
    public String createToken(String subject, long expirationMillis) {
        return createToken(new HashMap<>(), subject, expirationMillis);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        log.debug("生成 JWT: subject={}, 过期时间={}", subject, expirationDate);

        return Jwts.builder()
                .setClaims(claims)
                // jti：保证每次登录生成的 token 唯一。
                // 同一用户在同一秒内重复登录时，若不含 jti，JWT 内容完全一致，
                // 会导致多端登录管控「按 token 精确踢下线」误伤新会话。
                .setId(UUID.randomUUID().toString().replace("-", ""))
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 获取 token 的唯一标识（jti）
     */
    public String getJtiFromToken(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getRemainingExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expiration = claims.getExpiration();
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期：{}", e.getMessage());
            return e.getClaims();
        } catch (Exception e) {
            log.error("JWT 解析失败：{}", e.getMessage());
            throw new JwtException("JWT 解析失败：" + e.getMessage());
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);

            if (username == null || username.isEmpty()) {
                log.warn("JWT 中用户名为空");
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("JWT 已过期");
                return false;
            }

            boolean usernameMatches = username.equals(userDetails.getUsername());
            if (!usernameMatches) {
                log.warn("JWT 用户名不匹配：{} != {}", username, userDetails.getUsername());
            }
            return usernameMatches;

        } catch (ExpiredJwtException e) {
            log.error("JWT 已过期：{}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("JWT 签名验证失败：{}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("JWT 格式错误：{}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT 令牌为空：{}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 验证异常：{}", e.getMessage(), e);
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);

            if (expirationDate == null) {
                log.warn("JWT 中没有过期时间声明");
                return true;
            }

            return expirationDate.before(new Date());
        } catch (Exception e) {
            log.error("检查 JWT 过期时间失败：{}", e.getMessage());
            return true;
        }
    }

    public String refreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            claims.setIssuedAt(new Date(System.currentTimeMillis()));
            claims.setExpiration(new Date(System.currentTimeMillis() + refreshExpiration));
            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(getSigningKey())
                    .compact();
        } catch (ExpiredJwtException e) {
            throw new JwtException("刷新令牌已过期，请重新登录");
        }
    }

    public Boolean isTokenExpiringSoon(String token, long minutesThreshold) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            if (expirationDate == null) {
                return true;
            }
            long now = System.currentTimeMillis();
            long threshold = minutesThreshold * 60 * 1000;
            return (expirationDate.getTime() - now) < threshold;
        } catch (Exception e) {
            log.error("检查 JWT 即将过期失败：{}", e.getMessage());
            return true;
        }
    }
}

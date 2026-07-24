package com.xy.framework.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    public String createWeChatToken(String openId) {
        return createToken(new HashMap<>(), openId, jwtExpiration);
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
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)  // ✅ 确保设置了过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getRemainingExpiration(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
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
            // Token 过期时，返回 Claims（包含过期时间）
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

            // 验证用户名匹配
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

            // ✅ 检查是否为 null
            if (expirationDate == null) {
                log.warn("JWT 中没有过期时间声明");
                return true; // 没有过期时间，视为已过期
            }

            return expirationDate.before(new Date());
        } catch (Exception e) {
            log.error("检查 JWT 过期时间失败：{}", e.getMessage());
            return true; // 解析失败，视为已过期
        }
    }

    public String refreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            claims.setIssuedAt(new Date(System.currentTimeMillis()));
            claims.setExpiration(new Date(System.currentTimeMillis() + refreshExpiration));
            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
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

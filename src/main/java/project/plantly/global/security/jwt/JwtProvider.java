package project.plantly.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import project.plantly.domain.user.enums.UserRole;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private final String secretKey; // 보통 Base64 인코딩
    private final long expirationMillis = 1000 * 60 * 60 * 24; // 24시간
    private final Key key;

    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
        this.secretKey = secretKey;
    }

    public String createToken(Long userId, UserRole userRole) {
        Claims claims = Jwts.claims().setSubject(userId.toString());
        claims.put("UserRole", userRole.name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isValidToken(String token) {
        try {
            getClaims(token); // parsing 시 예외 발생 여부로 유효성 체크
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(getClaims(token).get("UserRole", String.class));
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)  // secretKey
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();             //subject 꺼냄
    }
}

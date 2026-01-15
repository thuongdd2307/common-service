package com.example.commonserviceofficial.security;

import com.example.commonserviceofficial.properties.JwtProperties;
import com.example.commonserviceofficial.security.jwt.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtTokenProvider {

    private JwtProperties jwtProperties;
    private Key key;

    public JwtTokenProvider() {
        this.jwtProperties = new JwtProperties();
    }

    public JwtTokenProvider(String secret) {
        this.jwtProperties = new JwtProperties();
        this.jwtProperties.setSecret(secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public void setSecret(String secret) {
        this.jwtProperties.setSecret(secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, List<String> roleCodes, Map<String, Object> additionalClaims) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(jwtProperties.getExpirationSeconds());

        Claims claims = Jwts.claims().setSubject(username);
        claims.put(JwtConstants.CLAIM_ROLE_CODES, roleCodes);
        
        if (additionalClaims != null) {
            additionalClaims.forEach(claims::put);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String username, List<String> roleCodes) {
        return generateToken(username, roleCodes, null);
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(jwtProperties.getRefreshExpirationSeconds());

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtClaims parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseClaimsJws(token)
                .getBody();

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setUsername(claims.getSubject());
        jwtClaims.setRoleCodes(claims.get(JwtConstants.CLAIM_ROLE_CODES, List.class));

        return jwtClaims;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}

package com.locaobra.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String tipo, String cargo) {
        var builder = Jwts.builder()
                .setSubject(email)
                .claim("tipo", tipo);
        if (cargo != null) {
            builder.claim("cargo", cargo);
        }
        return builder
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractCargo(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("cargo", String.class) : null;
    }

    public String extractEmail(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String extractTipo(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("tipo", String.class) : null;
    }

    // Seu método novo integrado e protegido contra NullPointerException
    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        // Garante que o email não é nulo antes de comparar e verifica a expiração
        return (extractedEmail != null && extractedEmail.equals(email) && !isTokenExpired(token));
    }

    // Seu método novo adaptado para usar o getClaims que você já tinha
    private Boolean isTokenExpired(String token) {
        Claims claims = getClaims(token);
        if (claims == null) {
            return true; // Se não conseguiu ler os claims, o token é considerado inválido/expirado
        }
        return claims.getExpiration().before(new Date());
    }

    public boolean isValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // O jjwt lança exceção aqui se o token estiver expirado ou com assinatura errada
            return null; 
        }
    }
}
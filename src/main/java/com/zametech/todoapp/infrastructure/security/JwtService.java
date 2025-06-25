package com.zametech.todoapp.infrastructure.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.todoapp.application.service.JwksService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    private final JwksService jwksService;
    private final SecretKey secretKey;
    private final Duration tokenExpiration;

    public JwtService(JwksService jwksService, JwtConfiguration jwtConfiguration) {
        this.jwksService = jwksService;
        this.secretKey = Keys.hmacShaKeyFor(jwtConfiguration.getSecretKey().getBytes());
        this.tokenExpiration = Duration.ofMillis(jwtConfiguration.getExpiration());
    }

    public String extractUsername(String token) {
        try {
            // Primary: Try to get email claim directly
            String email = extractClaim(token, claims -> (String) claims.get("email"));
            if (email != null) {
                log.debug("Extracted email from token: {}", email);
                return email;
            }
            
            // Secondary: Use subject (which should also be email in our tokens)
            String subject = extractClaim(token, Claims::getSubject);
            if (subject != null && subject.contains("@")) {
                log.debug("Using subject as email: {}", subject);
                return subject;
            }
            
            // Legacy support: For old tokens that might have username instead of email
            String username = extractClaim(token, claims -> (String) claims.get("username"));
            if (username != null && username.contains("@")) {
                log.debug("Legacy token with email in username claim: {}", username);
                return username;
            }
            
            // Final fallback
            log.warn("Token doesn't contain email, falling back to subject: {}", subject);
            return subject;
        } catch (Exception e) {
            log.warn("Error extracting email from token: {}", e.getMessage());
            return extractClaim(token, Claims::getSubject);
        }
    }

    public List<String> extractAuthorities(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("authorities"));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, tokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, Duration.ofDays(7));
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, Duration expiration) {
        Instant now = Instant.now();
        
        // Add authorities to claims
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // userDetails.getUsername() actually contains email in our implementation
        String email = userDetails.getUsername();
        
        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)  // Use email as subject
                .claim("email", email)  // Also explicitly add email claim
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isValid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        if (!isValid) {
            log.debug("Token validation failed - Token username: {}, UserDetails username: {}, Expired: {}", 
                username, userDetails.getUsername(), isTokenExpired(token));
        }
        return isValid;
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            // First try to parse as RS256 token (from OIDC service)
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Check if it's an RS256 token by looking at the algorithm in header
            if ("RS256".equals(signedJWT.getHeader().getAlgorithm().getName())) {
                // Verify RS256 signature using RSA public key
                JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) jwksService.getKeyPair().getPublic());
                if (signedJWT.verify(verifier)) {
                    // Convert to JJWT Claims for compatibility
                    Map<String, Object> payloadMap = signedJWT.getJWTClaimsSet().toJSONObject();
                    return Jwts.claims(payloadMap);
                } else {
                    throw new io.jsonwebtoken.JwtException("RS256 signature verification failed");
                }
            }
        } catch (Exception e) {
            log.debug("Token is not RS256 format, trying HS256: {}", e.getMessage());
        }
        
        // Fall back to HS256 token parsing (legacy tokens)
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token with both RS256 and HS256", e);
            throw new io.jsonwebtoken.JwtException("Invalid JWT token", e);
        }
    }
}
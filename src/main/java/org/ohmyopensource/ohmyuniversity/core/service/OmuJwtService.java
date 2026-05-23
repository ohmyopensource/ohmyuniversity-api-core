package org.ohmyopensource.ohmyuniversity.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates OhMyUniversity JWT access tokens.
 *
 * These tokens contain OhMyU-specific claims and
 * are signed with our own secret key.
 *
 * Token lifetime: 15 minutes (access token).
 * Refresh is handled via the refresh token stored in Redis.
 */
@Service
public class OmuJwtService {

  private final SecretKey signingKey;
  private final long expirationMs;

  public OmuJwtService(
      @Value("${omu.jwt.secret}") String secret,
      @Value("${omu.jwt.expiration-ms:900000}") long expirationMs) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  /**
   * Issues a new OhMyU access token for the given user and active profile.
   *
   * @param omuUserId    UUID of the OhMyU user
   * @param codiceFiscale tax code of the user
   * @param universityId active university identifier
   * @param stuId        active Cineca career ID
   * @param matId        active Cineca career segment ID
   * @param matricola    active registration number
   * @return signed JWT string
   */
  public String issue(
      String omuUserId,
      String codiceFiscale,
      String universityId,
      Long stuId,
      Long matId,
      String matricola) {

    Instant now = Instant.now();
    Instant expiry = now.plusMillis(expirationMs);

    return Jwts.builder()
        .subject(omuUserId)
        .claim("cf", codiceFiscale)
        .claim("uni", universityId)
        .claim("stuId", stuId)
        .claim("matId", matId)
        .claim("matricola", matricola)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  /**
   * Validates a JWT and returns its claims.
   * Throws a JwtException if the token is invalid or expired.
   */
  public Claims validate(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Generates a cryptographically random refresh token.
   */
  public String generateRefreshToken() {
    return UUID.randomUUID().toString().replace("-", "")
        + UUID.randomUUID().toString().replace("-", "");
  }
}
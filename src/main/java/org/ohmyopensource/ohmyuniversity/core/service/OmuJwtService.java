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
 * Service responsible for issuing and validating OhMyUniversity! JWT tokens.
 *
 * <p>Access tokens contain user identity and active university profile claims. Tokens are signed
 * using an HMAC secret key configured through application properties.
 *
 * <p>Access token lifetime defaults to 15 minutes. Refresh token lifecycle management is delegated
 * to the dedicated Redis-based refresh token layer.
 */
@Service
public class OmuJwtService {

  private final SecretKey signingKey;
  private final long expirationMs;

  // ============ Constructor ============

  /**
   * Creates a new JWT service instance.
   *
   * @param secret       JWT signing secret
   * @param expirationMs access token expiration time in milliseconds
   */
  public OmuJwtService(
      @Value("${omu.jwt.secret}") String secret,
      @Value("${omu.jwt.expiration-ms:900000}") long expirationMs) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  // ============ Override Methods ============

  // ============ Getters | Setters | Bool ============

  // ============ Class Methods ============

  /**
   * Issues a signed JWT access token for the provided user context.
   *
   * <p>The generated token contains the authenticated OhMyUniversity user identifier together with
   * the currently selected academic profile.
   *
   * @param omuUserId     unique OhMyUniversity user identifier
   * @param codiceFiscale user tax code
   * @param universityId  active university identifier
   * @param stuId         active Cineca career identifier
   * @param matId         active Cineca career segment identifier
   * @param matricola     active student registration number
   * @return signed JWT access token
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
        .claim("hasCarriera", stuId != null && matId != null)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  /**
   * Validates the provided JWT access token.
   *
   * <p>If validation succeeds, the parsed token claims are returned. The underlying JWT library
   * throws an exception when the token is malformed, expired, or signed with an invalid key.
   *
   * @param token JWT token to validate
   * @return validated token claims
   */
  public Claims validate(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Generates a cryptographically random refresh token identifier.
   *
   * <p>The generated value is composed of two UUIDs without dashes in order to increase entropy and
   * reduce predictability.
   *
   * @return generated refresh token
   */
  public String generateRefreshToken() {
    return UUID.randomUUID().toString().replace("-", "")
        + UUID.randomUUID().toString().replace("-", "");
  }
}
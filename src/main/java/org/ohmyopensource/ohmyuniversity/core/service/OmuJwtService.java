package org.ohmyopensource.ohmyuniversity.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Validates OhMyUniversity JWT access tokens issued by the auth service.
 *
 * <p>The core service no longer issues tokens — that responsibility moved entirely to
 * {@code ohmyuniversity-auth}. This class is validation-only, kept for
 * {@link org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter}. It
 * must share the same {@code omu.jwt.secret} value as the auth service, or every
 * request will fail validation.
 */
@Service
public class OmuJwtService {

  private final SecretKey signingKey;

  public OmuJwtService(@Value("${omu.jwt.secret}") String secret) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Validates a JWT access token, throwing if it is malformed, expired, or signed with
   * an invalid key.
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
}
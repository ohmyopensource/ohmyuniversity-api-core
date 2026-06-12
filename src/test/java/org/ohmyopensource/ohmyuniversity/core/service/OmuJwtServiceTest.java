package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OmuJwtService}.
 *
 * <p>Instantiated directly without a Spring context. Covers token issuance,
 * claim round-trips, expiration, signature validation, and refresh token generation.
 */
class OmuJwtServiceTest {

  /** HMAC-SHA256 requires a secret of at least 32 characters. */
  private static final String SECRET =
      "omu_test_jwt_secret_at_least_32_chars_ok";

  /** Standard access token lifetime used across happy-path tests. */
  private static final long EXPIRATION_MS = 900_000L;

  private OmuJwtService jwtService;

  /**
   * Initialises a fresh {@link OmuJwtService} instance before each test
   * to guarantee test isolation.
   */
  @BeforeEach
  void setUp() {
    jwtService = new OmuJwtService(SECRET, EXPIRATION_MS);
  }

  /**
   * Verifies that a token issued by {@link OmuJwtService#issue} contains
   * the expected subject and string claims after a validate round-trip.
   */
  @Test
  @DisplayName("issue: token contains all expected claims")
  void issue_containsAllClaims() {
    String token = jwtService.issue(
        "user-uuid-123",
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026");

    Claims claims = jwtService.validate(token);

    assertThat(claims.getSubject()).isEqualTo("user-uuid-123");
    assertThat(claims.get("cf", String.class)).isEqualTo("TSTXXX00A00X000X");
    assertThat(claims.get("uni", String.class)).isEqualTo("UNIMOL");
    assertThat(claims.get("matricola", String.class)).isEqualTo("178026");
  }

  /**
   * Verifies that {@code stuId} and {@code matId} survive a validate round-trip
   * as {@link Long} values when read via {@link Number#longValue()}.
   *
   * <p>JJWT 0.12.x may deserialise numeric claims as {@link Integer} internally;
   * this test guards against silent precision loss.
   */
  @Test
  @DisplayName("issue: stuId and matId survive round-trip as Long via Number cast")
  void issue_stuIdAndMatIdRoundTrip() {
    String token = jwtService.issue(
        "user-uuid-123",
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026");

    Claims claims = jwtService.validate(token);

    Number stuId = (Number) claims.get("stuId");
    Number matId = (Number) claims.get("matId");

    assertThat(stuId).isNotNull();
    assertThat(matId).isNotNull();
    assertThat(stuId.longValue()).isEqualTo(89486L);
    assertThat(matId.longValue()).isEqualTo(106279L);
  }

  /**
   * Verifies that {@code stuId}, {@code matId}, and {@code matricola} are
   * accepted as {@code null}, supporting the refresh-token issuance flow
   * where no active academic profile is required.
   */
  @Test
  @DisplayName("issue: null stuId and matId are allowed (refresh token flow)")
  void issue_nullStuIdAndMatId() {
    String token = jwtService.issue(
        "user-uuid-123",
        "TSTXXX00A00X000X",
        "UNIMOL",
        null,
        null,
        null);

    Claims claims = jwtService.validate(token);

    assertThat(claims.get("stuId")).isNull();
    assertThat(claims.get("matId")).isNull();
    assertThat(claims.get("matricola")).isNull();
  }

  /**
   * Verifies that a freshly issued token is not considered expired,
   * confirmed by {@link OmuJwtService#validate} returning non-null claims.
   */
  @Test
  @DisplayName("issue: token is not expired immediately after creation")
  void issue_tokenNotExpiredImmediately() {
    String token = jwtService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    Claims claims = jwtService.validate(token);
    assertThat(claims).isNotNull();
  }

  /**
   * Verifies that {@link OmuJwtService#validate} throws {@link JwtException}
   * when the token signature has been tampered with by altering the last character
   * of the signature segment.
   */
  @Test
  @DisplayName("validate: throws JwtException on tampered token")
  void validate_throwsOnTamperedToken() {
    String token = jwtService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    String[] parts = token.split("\\.");
    String tamperedPayload = parts[1] + "TAMPERED";
    String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

    assertThatThrownBy(() -> jwtService.validate(tampered))
        .isInstanceOf(JwtException.class);
  }

  /**
   * Verifies that {@link OmuJwtService#validate} throws {@link JwtException}
   * when the token was signed with a different secret key.
   */
  @Test
  @DisplayName("validate: throws JwtException on token signed with different secret")
  void validate_throwsOnWrongSecret() {
    OmuJwtService otherService = new OmuJwtService(
        "completely_different_secret_also_32_chars", EXPIRATION_MS);

    String token = otherService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    assertThatThrownBy(() -> jwtService.validate(token))
        .isInstanceOf(JwtException.class);
  }

  /**
   * Verifies that {@link OmuJwtService#validate} throws {@link JwtException}
   * when the token expiration is set to {@code -1ms}, causing it to expire
   * before validation is attempted.
   */
  @Test
  @DisplayName("validate: throws JwtException on expired token")
  void validate_throwsOnExpiredToken() {
    OmuJwtService shortLivedService = new OmuJwtService(SECRET, -1L);

    String token = shortLivedService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    assertThatThrownBy(() -> jwtService.validate(token))
        .isInstanceOf(JwtException.class);
  }

  /**
   * Verifies that {@link OmuJwtService#validate} throws {@link JwtException}
   * when presented with a syntactically malformed token string.
   */
  @Test
  @DisplayName("validate: throws JwtException on malformed token")
  void validate_throwsOnMalformedToken() {
    assertThatThrownBy(() -> jwtService.validate("not.a.jwt"))
        .isInstanceOf(JwtException.class);
  }

  /**
   * Verifies that {@link OmuJwtService#validate} throws an exception
   * when called with an empty string.
   */
  @Test
  @DisplayName("validate: throws JwtException on empty string")
  void validate_throwsOnEmptyString() {
    assertThatThrownBy(() -> jwtService.validate(""))
        .isInstanceOf(Exception.class);
  }

  /**
   * Verifies that {@link OmuJwtService#generateRefreshToken} returns a
   * 64-character string, composed of two UUID values with dashes removed.
   */
  @Test
  @DisplayName("generateRefreshToken: returns 64-char hex string")
  void generateRefreshToken_length() {
    String token = jwtService.generateRefreshToken();
    assertThat(token).hasSize(64);
  }

  /**
   * Verifies that the refresh token contains no dash characters,
   * confirming that UUID formatting has been correctly stripped.
   */
  @Test
  @DisplayName("generateRefreshToken: contains no dashes")
  void generateRefreshToken_noDashes() {
    String token = jwtService.generateRefreshToken();
    assertThat(token).doesNotContain("-");
  }

  /**
   * Verifies that two consecutively generated refresh tokens are not equal,
   * confirming that the generation relies on a random source.
   */
  @Test
  @DisplayName("generateRefreshToken: two consecutive tokens are different")
  void generateRefreshToken_isRandom() {
    String t1 = jwtService.generateRefreshToken();
    String t2 = jwtService.generateRefreshToken();
    assertThat(t1).isNotEqualTo(t2);
  }
}
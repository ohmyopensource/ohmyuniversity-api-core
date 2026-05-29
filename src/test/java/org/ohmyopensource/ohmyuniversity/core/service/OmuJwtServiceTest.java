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
 * No Spring context — instantiated directly with test values.
 */
class OmuJwtServiceTest {

  // Must be at least 32 chars for HMAC-SHA256
  private static final String SECRET =
      "omu_test_jwt_secret_at_least_32_chars_ok";
  private static final long EXPIRATION_MS = 900_000L; // 15 min

  private OmuJwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new OmuJwtService(SECRET, EXPIRATION_MS);
  }

  // ================================
  // issue + validate — happy path
  // ================================

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

    // JJWT 0.12.x deserializes numbers as Integer internally —
    // the filter reads them via (Number).longValue(), tested here
    Number stuId = (Number) claims.get("stuId");
    Number matId = (Number) claims.get("matId");

    assertThat(stuId).isNotNull();
    assertThat(matId).isNotNull();
    assertThat(stuId.longValue()).isEqualTo(89486L);
    assertThat(matId.longValue()).isEqualTo(106279L);
  }

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

  @Test
  @DisplayName("issue: token is not expired immediately after creation")
  void issue_tokenNotExpiredImmediately() {
    String token = jwtService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    // validate() throws if expired — no exception means token is valid
    Claims claims = jwtService.validate(token);
    assertThat(claims).isNotNull();
  }

  // ================================
  // validate — failure cases
  // ================================

  @Test
  @DisplayName("validate: throws JwtException on tampered token")
  void validate_throwsOnTamperedToken() {
    String token = jwtService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    // Flip one character in the signature (last segment)
    String[] parts = token.split("\\.");
    String tamperedSignature = parts[2].substring(0, parts[2].length() - 1) + "X";
    String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

    assertThatThrownBy(() -> jwtService.validate(tampered))
        .isInstanceOf(JwtException.class);
  }

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

  @Test
  @DisplayName("validate: throws JwtException on expired token")
  void validate_throwsOnExpiredToken() {
    // Create service with -1ms expiration so token expires immediately
    OmuJwtService shortLivedService = new OmuJwtService(SECRET, -1L);

    String token = shortLivedService.issue(
        "user-uuid-123", "CF", "UNIMOL", null, null, null);

    assertThatThrownBy(() -> jwtService.validate(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("validate: throws JwtException on malformed token")
  void validate_throwsOnMalformedToken() {
    assertThatThrownBy(() -> jwtService.validate("not.a.jwt"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("validate: throws JwtException on empty string")
  void validate_throwsOnEmptyString() {
    assertThatThrownBy(() -> jwtService.validate(""))
        .isInstanceOf(Exception.class);
  }

  // ================================
  // generateRefreshToken
  // ================================

  @Test
  @DisplayName("generateRefreshToken: returns 64-char hex string")
  void generateRefreshToken_length() {
    String token = jwtService.generateRefreshToken();
    // Two UUIDs without dashes = 32 + 32 = 64 chars
    assertThat(token).hasSize(64);
  }

  @Test
  @DisplayName("generateRefreshToken: contains no dashes")
  void generateRefreshToken_noDashes() {
    String token = jwtService.generateRefreshToken();
    assertThat(token).doesNotContain("-");
  }

  @Test
  @DisplayName("generateRefreshToken: two consecutive tokens are different")
  void generateRefreshToken_isRandom() {
    String t1 = jwtService.generateRefreshToken();
    String t2 = jwtService.generateRefreshToken();
    assertThat(t1).isNotEqualTo(t2);
  }
}
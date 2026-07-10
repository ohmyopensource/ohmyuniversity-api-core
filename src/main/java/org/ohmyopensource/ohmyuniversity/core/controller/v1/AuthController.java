package org.ohmyopensource.ohmyuniversity.core.controller.v1;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.AuthSessionResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse;
import org.ohmyopensource.ohmyuniversity.core.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller for OhMyUniversity.
 *
 * <p>This controller exposes public endpoints used to:
 * - authenticate a student via Cineca ESSE3 (login) - refresh JWT access tokens using a refresh
 * token - logout and invalidate server-side sessions (Redis)
 *
 * <p>Important: All endpoints are publicly accessible — authentication is delegated to Cineca or
 * refresh-token validation, not Spring Security here.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthService authService;

  // ============ Constructor ============

  /**
   * Creates the AuthController and injects the authentication service.
   *
   * <p>This controller depends on AuthService to handle all authentication logic, including Cineca
   * login, token generation, and session management.
   *
   * @param authService service responsible for authentication flows and token management
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  // ============ Classe Methods ============

  /**
   * Authenticates a student against Cineca ESSE3.
   *
   * <p>Flow:
   * 1. Validate request payload 2. Delegate authentication to AuthService 3. Retrieve Cineca
   * session + JWT 4. Build OhMyUniversity access + refresh tokens
   *
   * @param request login payload containing universityId, username, password
   * @return 200 OK with LoginResponse if successful 401 if Cineca credentials are invalid 404 if
   * universityId is not registered 503 if Cineca service is unreachable
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    try {
      String ipAddress = resolveClientIp(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");
      LoginResponse response = authService.login(request, ipAddress, userAgent);
      return ResponseEntity.ok(response);
    } catch (CinecaAuthException e) {
      log.warn("AuthController: login failed — invalid credentials: {}",
          e.getMessage().replaceAll("[\r\n]", ""));
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("AuthController: Cineca unavailable: {}", e.getMessage());
      return ResponseEntity.status(503).build();
    } catch (IllegalArgumentException e) {
      log.warn("AuthController: login failed — {}", e.getMessage());
      return ResponseEntity.status(404).build();
    }
  }

  /**
   * Refreshes an access token using a valid refresh token.
   *
   * <p>This endpoint does NOT contact Cineca directly. It only validates and rotates
   * OhMyUniversity! tokens.
   *
   * @param refreshToken valid refresh token issued at login
   * @param universityId target university session scope
   * @return 200 OK with new access token 401 if refresh token is invalid or expired
   */
  @PostMapping("/refresh")
  public ResponseEntity<String> refresh(
      @RequestParam String refreshToken,
      @RequestParam String universityId) {
    try {
      String newAccessToken = authService.refresh(refreshToken, universityId);
      return ResponseEntity.ok(newAccessToken);
    } catch (IllegalArgumentException e) {
      log.warn("AuthController: refresh failed — {}", e.getMessage());
      return ResponseEntity.status(401).build();
    }
  }

  /**
   * Logs out the user by invalidating all active sessions.
   *
   * <p>Actions performed:
   * - Deletes refresh token from Redis - Clears Cineca session tokens
   *
   * @param refreshToken refresh token to invalidate
   * @param universityId university session scope
   * @return 204 No Content
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestParam String refreshToken,
      @RequestParam String universityId) {
    authService.logout(refreshToken, universityId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Switches the active career for the authenticated user.
   *
   * @param stuId     new student career ID
   * @param matId     new matricola ID
   * @param matricola new matricola string
   * @param principal authenticated user
   * @return new access token
   */
  @PostMapping("/switch-carriera")
  public ResponseEntity<Map<String, String>> switchCarriera(
      @RequestParam Long stuId,
      @RequestParam Long matId,
      @RequestParam String matricola,
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      String newToken = authService.switchCarriera(
          principal.omuUserId(), principal.universityId(),
          stuId, matId, matricola, principal.sessionId());
      return ResponseEntity.ok(Map.of("accessToken", newToken));
    } catch (Exception e) {
      log.warn("AuthController: switch carriera failed — {}", e.getMessage());
      return ResponseEntity.status(400).build();
    }
  }

  /**
   * Switches the active university context for the authenticated user. Returns 409 if no active
   * Cineca session exists for the target university (client must prompt for login credentials).
   *
   * @param targetUniversityId university to switch to
   * @param refreshToken       current refresh token
   * @param principal          authenticated user
   * @return new access token or 409 if re-login required
   */
  @PostMapping("/switch-university")
  public ResponseEntity<Map<String, String>> switchUniversity(
      @RequestParam String targetUniversityId,
      @RequestParam String refreshToken,
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      String newToken = authService.switchUniversity(
          principal.omuUserId(), targetUniversityId, refreshToken);
      return ResponseEntity.ok(Map.of("accessToken", newToken));
    } catch (IllegalArgumentException e) {
      log.warn("AuthController: switch university failed — {}", e.getMessage());
      return ResponseEntity.status(409).build();
    }
  }

  /**
   * Resolves the client's IP address, preferring the first entry of X-Forwarded-For (set by the
   * gateway/reverse proxy in front of api-core) over the direct connection address.
   *
   * @param request incoming HTTP request
   * @return best-effort client IP address
   */
  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  /**
   * Lists the authenticated user's currently active sessions.
   *
   * @param principal authenticated user
   * @return 200 OK with the list of active sessions
   */
  @GetMapping("/sessions")
  public ResponseEntity<List<AuthSessionResponse>> getSessions(
      @AuthenticationPrincipal OmuPrincipal principal) {
    java.util.List<AuthSessionResponse> sessions =
        authService.listSessions(principal.omuUserId()).stream()
            .map(s -> new AuthSessionResponse(
                s.sessionId(), s.ipAddress(), s.userAgent(), s.createdAt(), s.lastUsedAt(),
                s.sessionId().equals(principal.sessionId())))
            .toList();
    return ResponseEntity.ok(sessions);
  }

  /**
   * Revokes a specific session, logging that device out.
   *
   * @param sessionId session identifier to revoke
   * @param principal authenticated user
   * @return 204 No Content
   */
  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<Void> revokeSession(
      @PathVariable String sessionId,
      @AuthenticationPrincipal OmuPrincipal principal) {
    authService.revokeSession(principal.omuUserId(), sessionId);
    return ResponseEntity.noContent().build();
  }
}
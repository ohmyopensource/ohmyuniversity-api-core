package org.ohmyopensource.ohmyuniversity.core.controller;

import jakarta.validation.Valid;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse;
import org.ohmyopensource.ohmyuniversity.core.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller for OhMyUniversity.
 *
 * <p>This controller exposes public endpoints used to:
 * - authenticate a student via Cineca ESSE3 (login)
 * - refresh JWT access tokens using a refresh token
 * - logout and invalidate server-side sessions (Redis)
 *
 * <p>Important: All endpoints are publicly accessible — authentication is delegated to Cineca or
 * refresh-token validation, not Spring Security here.
 */
@RestController
@RequestMapping("/api/auth")
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
   * 1. Validate request payload
   * 2. Delegate authentication to AuthService
   * 3. Retrieve Cineca session + JWT
   * 4. Build OhMyUniversity access + refresh tokens
   *
   * @param request login payload containing universityId, username, password
   * @return 200 OK with LoginResponse if successful 401 if Cineca credentials are invalid 404 if
   *     universityId is not registered 503 if Cineca service is unreachable
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    try {
      LoginResponse response = authService.login(request);
      return ResponseEntity.ok(response);
    } catch (CinecaAuthException e) {
      log.warn("AuthController: login failed — invalid credentials: {}", e.getMessage());
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
   * <p>This endpoint does NOT contact Cineca directly. It only validates and rotates OhMyUniversity
   * tokens.
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
   * - Deletes refresh token from Redis
   * - Clears Cineca session tokens
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
}
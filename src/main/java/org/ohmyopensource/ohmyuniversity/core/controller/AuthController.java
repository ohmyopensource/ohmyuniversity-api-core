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
 * REST controller for OhMyUniversity authentication.
 *
 * All endpoints are public — no authentication required to reach them.
 * The login endpoint proxies credentials to the university's Cineca ESSE3 instance.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Authenticates a student against their university's Cineca ESSE3 instance.
   *
   * @param request Cineca credentials + universityId
   * @return 200 with login data, 401 if credentials invalid,
   *         404 if university unknown, 503 if Cineca is unreachable
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
   * Issues a new access token using a valid refresh token.
   *
   * @param refreshToken the OhMyU refresh token (from login response)
   * @param universityId the university to refresh the session for
   * @return 200 with new access token, 401 if refresh token invalid or expired
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
   * Logs out the user by invalidating the refresh token and clearing
   * the Cineca session from Redis.
   *
   * @param refreshToken the OhMyU refresh token to invalidate
   * @param universityId the university session to clear
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
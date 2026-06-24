package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.CarrieraInfoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.ProfiloResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing profile-related endpoints for the authenticated user.
 *
 * <p>All endpoints proxy live data from Cineca ESSE3 via {@link ProfileService}.
 * No academic or personal data is persisted by this service.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/profile/persona} — full personal data (anagrafica)</li>
 *   <li>{@code GET /v1/profile/info} — career metadata (tipoCorsoCod, cdsDes, etc.)</li>
 *   <li>{@code GET /v1/profile/avatar} — profile photo as JPEG</li>
 *   <li>{@code GET /v1/profile/badge} — university badge</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController extends AbstractEsse3Controller {

  private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

  private final ProfileService profileService;

  // ============ Constructor ============

  /**
   * Constructs a new ProfileController with the specified profile service.
   *
   * @param profileService the service responsible for managing user profile information
   */
  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  // ============ Class Methods ============

  /**
   * Returns full personal data for the authenticated user.
   *
   * <p>Falls back to JWT/Redis claims if persId is not cached in session.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with personal data, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/persona")
  public ResponseEntity<ProfiloResponse> getPersona(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> profileService.getPersona(principal));
  }

  /**
   * Returns career metadata (degree type, course name, faculty, enrollment year, etc.).
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with career info, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/info")
  public ResponseEntity<CarrieraInfoResponse> getInfo(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> profileService.getInfo(principal));
  }

  /**
   * Returns the profile avatar as a JPEG image.
   *
   * <p>This endpoint cannot use the standard {@code execute} helper because it
   * requires a custom content type and null-check on the response body.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with JPEG bytes, {@code 404} if no avatar exists,
   *         {@code 401} if session expired, {@code 503} if Cineca is unavailable
   */
  @GetMapping(value = "/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> getAvatar(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      byte[] avatar = profileService.getAvatar(principal);
      if (avatar == null) return ResponseEntity.notFound().build();
      return ResponseEntity.ok()
          .contentType(MediaType.IMAGE_JPEG)
          .body(avatar);
    } catch (CinecaAuthException e) {
      log.warn("ProfileController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("ProfileController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Returns the university badge for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with badge data, {@code 404} if no badge exists,
   *         {@code 401} if session expired, {@code 503} if Cineca is unavailable
   */
  @GetMapping("/badge")
  public ResponseEntity<BadgeResponse> getBadge(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      BadgeResponse badge = profileService.getBadge(principal);
      if (badge == null) return ResponseEntity.notFound().build();
      return ResponseEntity.ok(badge);
    } catch (CinecaAuthException e) {
      log.warn("ProfileController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("ProfileController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }
}
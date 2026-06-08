package org.ohmyopensource.ohmyuniversity.core.controller.v1;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.dto.UniversityConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes external university service URLs to authenticated clients.
 *
 * <p>Returns the WebView URLs needed by the frontend to embed university
 * services (Moodle, library) without leaving the application.
 * URLs are resolved from {@link UniversityRegistry} based on the
 * authenticated student's active university.
 */
@RestController
@RequestMapping("/api/v1/university")
public class ExternalServicesController {

  private static final Logger log = LoggerFactory.getLogger(ExternalServicesController.class);

  private final UniversityRegistry universityRegistry;

  // ============ Constructor ============

  /**
   * Creates the controller and injects the university registry.
   *
   * @param universityRegistry registry containing per-university service URLs
   */
  public ExternalServicesController(UniversityRegistry universityRegistry) {
    this.universityRegistry = universityRegistry;
  }

  // ============ Class Methods ============

  /**
   * Returns external service URLs for the authenticated student's university.
   *
   * <p>The client uses the returned URLs to open embedded WebViews for
   * Moodle and the library portal, avoiding external browser redirects.
   *
   * @param principal authenticated OhMyU principal injected by Spring Security
   * @return 200 OK with {@link UniversityConfigResponse} containing Moodle and library URLs,
   *         404 Not Found if the university is not registered in configuration
   */
  @GetMapping("/external-services")
  public ResponseEntity<UniversityConfigResponse> getExternalServices(
      @AuthenticationPrincipal OmuPrincipal principal) {

    return universityRegistry.resolve(principal.universityId())
        .map(config -> {
          UniversityConfigResponse response = new UniversityConfigResponse();
          response.setUniversityId(principal.universityId());
          response.setName(config.name());
          response.setMoodleUrl(config.moodleUrl());
          response.setLibraryUrl(config.libraryUrl());
          return ResponseEntity.ok(response);
        })
        .orElseGet(() -> {
          log.warn("ExternalServicesController: unknown university={}",
              principal.universityId());
          return ResponseEntity.notFound().build();
        });
  }
}
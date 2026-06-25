package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InternshipApplicationResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.InternshipsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing internship-related endpoints for the authenticated student.
 *
 * <p>All endpoints proxy live data from Cineca ESSE3 via {@link InternshipsService}.
 * No internship data is persisted by this service.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/internships/applications} — list of internship applications</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/internships")
public class InternshipsController extends AbstractEsse3Controller {

  private static final Logger log = LoggerFactory.getLogger(InternshipsController.class);

  private final InternshipsService internshipsService;

  // ============ Constructor ============

  public InternshipsController(InternshipsService internshipsService) {
    this.internshipsService = internshipsService;
  }

  // ============ Class Methods ============

  /**
   * Returns all internship applications for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with applications, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/applications")
  public ResponseEntity<InternshipApplicationResponse> getApplications(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> internshipsService.getApplications(principal));
  }
}
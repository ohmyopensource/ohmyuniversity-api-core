package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveysResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.ExamsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing exam-related endpoints for the authenticated student.
 *
 * <p>All endpoints proxy live data from Cineca ESSE3 via {@link ExamsService}.
 * No exam data is persisted by this service.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET  /v1/exams/sessions} — available exam sessions (calesa)</li>
 *   <li>{@code GET  /v1/exams/bookable} — bookable sessions (libretto)</li>
 *   <li>{@code GET  /v1/exams/bookings} — active upcoming bookings</li>
 *   <li>{@code POST /v1/exams/bookings/legacy} — full booking history (Basic Auth)</li>
 *   <li>{@code GET  /v1/exams/surveys} — teaching evaluation surveys</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/exams")
public class ExamsController extends AbstractEsse3Controller {

  private static final Logger log = LoggerFactory.getLogger(ExamsController.class);

  private final ExamsService examsService;

  // ============ Constructor ============

  /**
   * Creates a new controller instance with the required dependency.
   *
   * @param examsService the service used to manage exam-related business logic
   */
  public ExamsController(ExamsService examsService) {
    this.examsService = examsService;
  }

  // ============ Class Methods ============

  /**
   * Returns available exam sessions for a specific teaching activity from calesa-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @param cdsId     course of study identifier
   * @param adId      teaching activity identifier
   * @return {@code 200 OK} with available sessions, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/sessions")
  public ResponseEntity<SessionsResponse> getSessions(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestParam Long cdsId,
      @RequestParam Long adId) {
    return execute(principal, () -> examsService.getSessions(principal, cdsId, adId));
  }

  /**
   * Returns bookable exam sessions from libretto-service-v2.
   *
   * <p>Accessible to STUDENTE role via {@code checkMatId}, bypassing the
   * {@code checkAbildocStu} restriction that blocks calesa-service on some
   * universities (e.g. UNIMOL).
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with bookable sessions, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/bookable")
  public ResponseEntity<BookableSessionsResponse> getBookableSessions(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> examsService.getBookableSessions(principal));
  }

  /**
   * Returns active upcoming exam bookings from libretto-service-v2.
   *
   * <p>Filters out past exams and those already passed or withdrawn.
   * Does not require Cineca password.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with active bookings, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/bookings")
  public ResponseEntity<BookingsResponse> getBookings(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> examsService.getBookings(principal));
  }

  /**
   * Returns full booking history from calesa-service-v1 using Basic Auth.
   *
   * <p>Requires the Cineca password in the request body. The password is
   * never persisted or logged.
   *
   * <p>Uses POST to avoid exposing the password as a query parameter.
   *
   * @param principal authenticated OhMyU principal
   * @param request   request body containing the Cineca password
   * @return {@code 200 OK} with full history, {@code 400} if password is missing,
   *         {@code 401} if Cineca auth fails, {@code 503} if Cineca is unavailable
   */
  @PostMapping("/bookings/legacy")
  public ResponseEntity<LegacyBookingsResponse> getLegacyBookings(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestBody LegacyBookingRequest request) {
    if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
      log.warn("ExamsController: missing password for legacy bookings user={}",
          principal.omuUserId());
      return ResponseEntity.badRequest().build();
    }
    return execute(principal, () -> examsService.getLegacyBookings(principal,
        request.getPassword()));
  }

  /**
   * Returns teaching evaluation surveys split into pending and completed.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with survey status, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/surveys")
  public ResponseEntity<SurveysResponse> getSurveys(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> examsService.getSurveys(principal));
  }
}
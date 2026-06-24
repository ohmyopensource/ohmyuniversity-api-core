package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.StoricoEsamiResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.SuggerimentiResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.CareerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing career academic data endpoints for the authenticated student.
 *
 * <p>All endpoints proxy live data from Cineca ESSE3 via {@link CareerService}.
 * No academic data is persisted by this service.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/career/transcript} — exam records with grades and status</li>
 *   <li>{@code GET /v1/career/grades} — grade averages and CFU statistics</li>
 *   <li>{@code GET /v1/career/study-plan} — planned academic activities</li>
 *   <li>{@code GET /v1/career/exam-history} — all exam attempts grouped by activity</li>
 *   <li>{@code GET /v1/career/recommendations} — prioritised pending exams</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/career")
public class CareerController extends AbstractEsse3Controller {

  private final CareerService careerService;

  // ============ Constructor ============

  /**
   * Creates a new controller instance with the required dependencies.
   *
   * @param careerService the service used to manage career-related business logic
   */
  public CareerController(CareerService careerService) {
    this.careerService = careerService;
  }

  // ============ Class Methods ============

  /**
   * Returns the student transcript — all exam records with grades, status and CFU.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with transcript rows, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/transcript")
  public ResponseEntity<LibrettoResponse> getTranscript(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> careerService.getTranscript(principal));
  }

  /**
   * Returns grade averages (arithmetic, weighted, base-110) and CFU progress statistics.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with grade statistics, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/grades")
  public ResponseEntity<MediaResponse> getGrades(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> careerService.getGrades(principal));
  }

  /**
   * Returns the student's study plan — all planned activities with CFU and year.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with study plan rows, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/study-plan")
  public ResponseEntity<PianoStudioResponse> getStudyPlan(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> careerService.getStudyPlan(principal));
  }

  /**
   * Returns the complete exam attempt history grouped by course activity.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with grouped history, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/exam-history")
  public ResponseEntity<StoricoEsamiResponse> getExamHistory(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> careerService.getExamHistory(principal));
  }

  /**
   * Returns an ordered list of recommended exams to attempt next.
   *
   * <p>Excludes already-passed activities and ranks by year of course and CFU weight.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with ordered recommendations, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/recommendations")
  public ResponseEntity<SuggerimentiResponse> getRecommendations(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> careerService.getRecommendations(principal));
  }
}
package org.ohmyopensource.ohmyuniversity.core.controller.v1.coursecatalogue;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3.AbstractEsse3Controller;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CoursePlanResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CourseSyllabusResponse;
import org.ohmyopensource.ohmyuniversity.core.service.coursecatalogue.CourseCatalogueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing Cineca Course Catalogue data — a separate Cineca
 * product from ESSE3 (see {@link CourseCatalogueService}) — used to enrich
 * the career view beyond what the student's own libretto provides.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/course-catalogue/plan} — full multi-year course plan for the student's cohort</li>
 *   <li>{@code GET /v1/course-catalogue/syllabus} — prerequisites and CFU breakdown for one activity</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/course-catalogue")
public class CourseCatalogueController extends AbstractEsse3Controller {

  private final CourseCatalogueService courseCatalogueService;

  public CourseCatalogueController(CourseCatalogueService courseCatalogueService) {
    this.courseCatalogueService = courseCatalogueService;
  }

  /**
   * Returns the full multi-year teaching plan for the authenticated
   * student's degree course and cohort.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with the course plan (empty if not resolvable), {@code 401} if session
   * expired, {@code 503} if Cineca is unavailable
   */
  @GetMapping("/plan")
  public ResponseEntity<CoursePlanResponse> getCoursePlan(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> courseCatalogueService.getCoursePlan(principal));
  }

  /**
   * Returns the syllabus (prerequisites and per-module CFU breakdown) for a
   * single teaching activity.
   *
   * @param principal authenticated OhMyU principal
   * @param adCod     ESSE3 teaching activity code
   * @return {@code 200 OK} with the syllabus (fields {@code null} if not compiled by the
   * university), {@code 401} if session expired, {@code 503} if Cineca is unavailable
   */
  @GetMapping("/syllabus")
  public ResponseEntity<CourseSyllabusResponse> getCourseSyllabus(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestParam String adCod) {
    return execute(principal, () -> courseCatalogueService.getCourseSyllabus(principal, adCod));
  }
}
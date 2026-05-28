package org.ohmyopensource.ohmyuniversity.core.controller;

import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.AppelloResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse;
import org.ohmyopensource.ohmyuniversity.core.service.CarrieraService;
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
 * Controller responsible for exposing all student career-related APIs.
 *
 * <p>This layer is a thin orchestration layer:
 * - extracts identity from JWT (OmuPrincipal)
 * - delegates business logic to CarrieraService
 * - maps Cineca failures into HTTP responses
 *
 * <p>Important architectural note: All data is fetched in real-time from Cineca ESSE3. No academic
 * data is persisted in this service.
 */
@RestController
@RequestMapping("/api/carriera")
public class CarrieraController {

  private static final Logger log = LoggerFactory.getLogger(CarrieraController.class);
  private final CarrieraService carrieraService;

  // ============ Constructor ============

  /**
   * Creates the CarrieraController and injects the career service layer.
   *
   * @param carrieraService service responsible for all Cineca career data retrieval
   */
  public CarrieraController(CarrieraService carrieraService) {
    this.carrieraService = carrieraService;
  }

  // ============ Class Methods ============

  /**
   * Retrieves the student's transcript (libretto).
   *
   * <p>This endpoint calls Cineca ESSE3 in real-time and returns all exam records associated with
   * the authenticated student (derived from JWT principal).
   *
   * @param principal authenticated user extracted from JWT containing:
   *                  - stuId
   *                  - matId
   *                  - universityId
   * @return 200 OK with {@link LibrettoResponse} containing all transcript rows 401 Unauthorized if
   *     Cineca session has expired or JWT is invalid 503 Service Unavailable if Cineca ESSE3 is
   *     unreachable
   */
  @GetMapping("/libretto")
  public ResponseEntity<LibrettoResponse> getLibretto(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      return ResponseEntity.ok(carrieraService.getLibretto(principal));
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Retrieves computed academic averages for the authenticated student.
   *
   * <p>Includes:
   * - arithmetic mean
   * - weighted mean (ECTS-based)
   * - projected graduation score
   * - CFU progress metrics
   *
   * @param principal authenticated user extracted from JWT
   * @return 200 OK with {@link MediaResponse} 401 if Cineca session expired 503 if Cineca service
   *     is unavailable
   */
  @GetMapping("/medie")
  public ResponseEntity<MediaResponse> getMedia(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      return ResponseEntity.ok(carrieraService.getMedia(principal));
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Retrieves the student's study plan (piano di studi).
   *
   * <p>Returns all planned courses and their status (passed, pending, etc.) as provided by Cineca
   * piani-service.
   *
   * @param principal authenticated user extracted from JWT
   * @return 200 OK with {@link PianoStudioResponse} 401 if Cineca session expired or JWT invalid
   *     503 if Cineca service is unreachable
   */
  @GetMapping("/piano")
  public ResponseEntity<PianoStudioResponse> getPiano(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      return ResponseEntity.ok(carrieraService.getPiano(principal));
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Retrieves available exam sessions (appelli) for a specific course.
   *
   * <p>This endpoint requires:
   * - cdsId (degree course ID)
   * - adId (teaching activity ID)
   *
   * <p>These are used to query Cineca calesa-service for available exam dates.
   *
   * @param principal authenticated user extracted from JWT
   * @param cdsId     degree course identifier (Cineca cdsId)
   * @param adId      teaching activity identifier (Cineca adId)
   * @return 200 OK with {@link AppelloResponse} 401 if Cineca session expired 503 if Cineca service
   *     is unavailable
   */
  @GetMapping("/appelli")
  public ResponseEntity<AppelloResponse> getAppelli(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestParam Long cdsId,
      @RequestParam Long adId) {
    try {
      return ResponseEntity.ok(carrieraService.getAppelli(principal, cdsId, adId));
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Retrieves full exam booking history for the student.
   *
   * <p>This endpoint requires the Cineca password because the underlying calesa-service does not
   * support JWT-only authentication and requires session-level authentication (JSESSIONID + Basic
   * Auth).
   *
   * <p>SECURITY NOTE: The password is transmitted in the request body and is never persisted,
   * cached, or logged.
   *
   * @param principal authenticated user extracted from JWT
   * @param request   request body containing Cineca password
   * @return 200 OK with {@link PrenotazioneResponse} 400 Bad Request if password is missing or
   *     empty 401 Unauthorized if Cineca authentication fails 503 Service Unavailable if Cineca is
   *     unreachable
   */
  @PostMapping("/prenotazioni")
  public ResponseEntity<PrenotazioneResponse> getPrenotazioni(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestBody PrenotazioneRequest request) {
    if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
      log.warn("CarrieraController: missing password for prenotazioni");
      return ResponseEntity.badRequest().build();
    }
    try {
      return ResponseEntity.ok(carrieraService.getPrenotazioni(principal, request.getPassword()));
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca auth failed for prenotazioni user={}",
          principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Retrieves student badge information if available.
   *
   * <p>The badge contains university-issued identification data such as RFID, enrollment status,
   * and course metadata.
   *
   * @param principal authenticated user extracted from JWT
   * @return 200 OK with {@link BadgeResponse} 404 Not Found if no badge is associated with the
   *     student 401 if Cineca session expired 503 if Cineca service is unavailable
   */
  @GetMapping("/badge")
  public ResponseEntity<BadgeResponse> getBadge(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      BadgeResponse badge = carrieraService.getBadge(principal);
      if (badge == null) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(badge);
    } catch (CinecaAuthException e) {
      log.warn("CarrieraController: Cineca session expired for user={}", principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("CarrieraController: Cineca unavailable — {}", e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }
}
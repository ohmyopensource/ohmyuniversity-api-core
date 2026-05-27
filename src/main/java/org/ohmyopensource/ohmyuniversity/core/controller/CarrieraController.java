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
 * REST controller for student career data.
 *
 * All endpoints require a valid OhMyU JWT (validated by JwtAuthenticationFilter).
 * The student identity (matId, stuId, universityId) is read directly from the JWT
 * principal — no path variables needed for personal data.
 *
 * Data is fetched live from Cineca on every request.
 * If the Cineca session is expired (JWT older than 15 min), returns 401
 * so the client knows it must call POST /api/auth/refresh.
 */
@RestController
@RequestMapping("/api/carriera")
public class CarrieraController {

  private static final Logger log = LoggerFactory.getLogger(CarrieraController.class);

  private final CarrieraService carrieraService;

  public CarrieraController(CarrieraService carrieraService) {
    this.carrieraService = carrieraService;
  }

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
   * Returns the full booking history for the student.
   *
   * Requires the Cineca password in the request body (JSON: {"password": "..."}).
   * calesa-service-v1 on Cineca requires Basic Auth + JSESSIONID — the JWT
   * alone is not sufficient. The password is used only for this request
   * and is never stored. Sent in the body (not header) to avoid appearing
   * in server access logs.
   *
   * @param principal OhMyU JWT claims injected by Spring Security
   * @param request   request body containing the Cineca password
   * @return 200 with booking list, 400 if password missing,
   *         401 if Cineca session expired, 503 if Cineca down
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
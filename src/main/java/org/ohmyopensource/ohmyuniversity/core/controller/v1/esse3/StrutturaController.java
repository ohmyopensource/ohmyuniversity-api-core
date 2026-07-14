package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SedeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.StrutturaResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.StrutturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing faculty/department/location data for the Contatti page.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/struttura/facolta} — all faculties/departments (with nested locations)</li>
 *   <li>{@code GET /v1/struttura/sedi/{sedeId}} — single location detail</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/struttura")
public class StrutturaController extends AbstractEsse3Controller {

  private final StrutturaService strutturaService;

  public StrutturaController(StrutturaService strutturaService) {
    this.strutturaService = strutturaService;
  }

  @GetMapping("/facolta")
  public ResponseEntity<List<StrutturaResponse>> getFacolta(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> strutturaService.getFacolta(principal));
  }

  @GetMapping("/sedi/{sedeId}")
  public ResponseEntity<SedeResponse> getSede(
      @PathVariable Long sedeId,
      @AuthenticationPrincipal OmuPrincipal principal) {
    SedeResponse sede = strutturaService.getSede(principal, sedeId);
    return sede != null ? ResponseEntity.ok(sede) : ResponseEntity.notFound().build();
  }
}
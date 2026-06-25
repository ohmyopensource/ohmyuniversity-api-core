package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.FeeStatusResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InvoiceResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.RefundResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.FeesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing fee-related endpoints for the authenticated student.
 *
 * <p>All endpoints proxy live data from Cineca ESSE3 tasse-service-v1
 * via {@link FeesService}. No fee data is persisted by this service.
 *
 * <p>Exposed endpoints:
 * <ul>
 *   <li>{@code GET /v1/fees/status}   — overall payment status (semaforo) + charges</li>
 *   <li>{@code GET /v1/fees/invoices} — issued invoices</li>
 *   <li>{@code GET /v1/fees/refunds}  — refund records</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/fees")
public class FeesController extends AbstractEsse3Controller {

  private final FeesService feesService;

  public FeesController(FeesService feesService) {
    this.feesService = feesService;
  }

  /**
   * Returns the aggregated fee status for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with fee status and charges, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/status")
  public ResponseEntity<FeeStatusResponse> getStatus(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getStatus(principal));
  }

  /**
   * Returns issued invoices for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with invoice list, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/invoices")
  public ResponseEntity<InvoiceResponse> getInvoices(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getInvoices(principal));
  }

  /**
   * Returns refund records for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with refund list, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/refunds")
  public ResponseEntity<RefundResponse> getRefunds(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getRefunds(principal));
  }
}
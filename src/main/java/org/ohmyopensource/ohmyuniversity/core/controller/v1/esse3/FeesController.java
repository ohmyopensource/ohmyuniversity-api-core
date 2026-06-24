package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaInvoice;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaPayment;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaRefund;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;
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
 *   <li>{@code GET /v1/fees/payments} — PagoPA transaction history</li>
 * </ul>
 *
 * <p>Note: {@code /invoices}, {@code /refunds} and {@code /payments} currently return
 * raw Cineca types pending the introduction of dedicated response DTOs.
 * These will be replaced with typed responses in a follow-up step.
 */
@RestController
@RequestMapping("/api/v1/fees")
public class FeesController extends AbstractEsse3Controller {

  private final FeesService feesService;

  // ============ Constructor ============

  /**
   * Constructs a new FeesController with the specified fees service.
   *
   * @param feesService the service responsible for managing tuition fees and payment-related operations
   */
  public FeesController(FeesService feesService) {
    this.feesService = feesService;
  }

  // ============ Class Methods ============

  /**
   * Returns the aggregated fee status for the authenticated student.
   *
   * <p>Combines the semaforo indicator (overall payment standing) with
   * the full list of accounting charges into a single response.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with fee status and charges, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/status")
  public ResponseEntity<TasseResponse> getStatus(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getStatus(principal));
  }

  /**
   * Returns issued invoices for the authenticated student.
   *
   * <p>TODO: replace raw {@link CinecaInvoice} return type with a dedicated
   * {@code InvoiceResponse} DTO once introduced.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with invoice list, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/invoices")
  public ResponseEntity<List<CinecaInvoice>> getInvoices(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getInvoices(principal));
  }

  /**
   * Returns refund records for the authenticated student.
   *
   * <p>TODO: replace raw {@link CinecaRefund} return type with a dedicated
   * {@code RefundResponse} DTO once introduced.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with refund list, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/refunds")
  public ResponseEntity<List<CinecaRefund>> getRefunds(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getRefunds(principal));
  }

  /**
   * Returns PagoPA payment transaction history for the authenticated student.
   *
   * <p>TODO: replace raw {@link CinecaPayment} return type with a dedicated
   * {@code PaymentResponse} DTO once introduced.
   *
   * @param principal authenticated OhMyU principal
   * @return {@code 200 OK} with payment list, {@code 401} if session expired,
   *         {@code 503} if Cineca is unavailable
   */
  @GetMapping("/payments")
  public ResponseEntity<List<CinecaPayment>> getPayments(
      @AuthenticationPrincipal OmuPrincipal principal) {
    return execute(principal, () -> feesService.getPayments(principal));
  }
}
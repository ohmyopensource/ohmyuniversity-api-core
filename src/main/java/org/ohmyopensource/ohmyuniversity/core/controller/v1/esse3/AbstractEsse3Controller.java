package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import java.util.function.Supplier;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaBookingException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Abstract base controller for all ESSE3-backed endpoints.
 *
 * <p>Provides a single {@code execute} helper that wraps every Cineca-backed
 * handler with consistent exception handling and HTTP status mapping, eliminating the duplicated
 * try/catch blocks across all concrete controllers.
 *
 * <p>HTTP status mapping:
 * <ul>
 *   <li>{@link CinecaAuthException} → {@code 401 Unauthorized}</li>
 *   <li>{@link CinecaUnavailableException} → {@code 503 Service Unavailable}</li>
 * </ul>
 */
public abstract class AbstractEsse3Controller {

  private static final Logger log = LoggerFactory.getLogger(AbstractEsse3Controller.class);

  // ============ Class Methods ============

  /**
   * Executes a Cineca-backed supplier and maps exceptions to HTTP responses.
   *
   * <p>The concrete controller class name is included in log output automatically,
   * so no additional context is needed in the caller.
   *
   * @param principal the authenticated OhMyU user (used for logging)
   * @param action    supplier producing the response body
   * @param <T>       response body type
   * @return {@code 200 OK} with the produced body, {@code 401} if the Cineca session has expired,
   * or {@code 503} if Cineca is unreachable
   */
  protected <T> ResponseEntity<T> execute(OmuPrincipal principal, Supplier<T> action) {
    try {
      return ResponseEntity.ok(action.get());
    } catch (CinecaAuthException e) {
      log.warn("{}: Cineca session expired for user={}",
          getClass().getSimpleName(), principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("{}: Cineca unavailable — {}",
          getClass().getSimpleName(), e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Executes a Cineca-backed booking action that returns no body, mapping a
   * {@link CinecaBookingException} to {@code 422 Unprocessable Entity} with the raw Cineca message
   * so it can be shown to the user.
   *
   * @param principal the authenticated OhMyU user (used for logging)
   * @param action    runnable performing the booking/cancellation
   * @return {@code 204 No Content} on success, {@code 422} with the Cineca message if rejected,
   * {@code 401} if the session expired, or {@code 503} if Cineca is unreachable
   */
  protected ResponseEntity<String> executeBooking(OmuPrincipal principal, Runnable action) {
    try {
      action.run();
      return ResponseEntity.noContent().build();
    } catch (CinecaBookingException e) {
      log.info("{}: booking rejected by Cineca for user={} — {}",
          getClass().getSimpleName(), principal.omuUserId(), e.getMessage());
      return ResponseEntity.unprocessableEntity().body(e.getMessage());
    } catch (CinecaAuthException e) {
      log.warn("{}: Cineca session expired for user={}",
          getClass().getSimpleName(), principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("{}: Cineca unavailable — {}",
          getClass().getSimpleName(), e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }

  /**
   * Executes a Cineca-backed supplier that may be rejected with a business error, mapping a
   * {@link CinecaBookingException} to {@code 422 Unprocessable Entity} with the raw Cineca message
   * while still returning the typed body on success.
   *
   * <p>Used by questionnaire compilation endpoints, where the happy path returns
   * a payload but Cineca may reject the request (e.g. questionnaire not available, invalid session,
   * validation errors on submitted answers).
   *
   * @param principal the authenticated OhMyU user (used for logging)
   * @param action    supplier producing the response body
   * @param <T>       response body type
   * @return {@code 200 OK} with the produced body, {@code 422} with the Cineca message if rejected,
   * {@code 401} if the session expired, or {@code 503} if Cineca is unreachable
   */
  protected <T> ResponseEntity<?> executeWithBusinessError(
      OmuPrincipal principal, Supplier<T> action) {
    try {
      return ResponseEntity.ok(action.get());
    } catch (CinecaBookingException e) {
      log.info("{}: request rejected by Cineca for user={} — {}",
          getClass().getSimpleName(), principal.omuUserId(), e.getMessage());
      return ResponseEntity.unprocessableEntity().body(e.getMessage());
    } catch (CinecaAuthException e) {
      log.warn("{}: Cineca session expired for user={}",
          getClass().getSimpleName(), principal.omuUserId());
      return ResponseEntity.status(401).build();
    } catch (CinecaUnavailableException e) {
      log.error("{}: Cineca unavailable — {}",
          getClass().getSimpleName(), e.getMessage());
      return ResponseEntity.status(503).build();
    }
  }
}
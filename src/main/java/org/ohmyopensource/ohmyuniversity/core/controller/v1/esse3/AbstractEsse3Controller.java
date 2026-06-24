package org.ohmyopensource.ohmyuniversity.core.controller.v1.esse3;

import java.util.function.Supplier;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Abstract base controller for all ESSE3-backed endpoints.
 *
 * <p>Provides a single {@code execute} helper that wraps every Cineca-backed
 * handler with consistent exception handling and HTTP status mapping,
 * eliminating the duplicated try/catch blocks across all concrete controllers.
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
   * @return {@code 200 OK} with the produced body, {@code 401} if the Cineca
   *         session has expired, or {@code 503} if Cineca is unreachable
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
}
package org.ohmyopensource.ohmyuniversity.core.cineca;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for the Cineca ESSE3 REST API.
 *
 * Each university has its own ESSE3 instance at a different base URL.
 * This client is stateless — the base URL and credentials are passed per call.
 *
 * Authentication against Cineca uses HTTP Basic Auth (RFC2617).
 * The student's Cineca username and password are passed directly — we act
 * as a proxy between the student and their university's ESSE3 instance.
 * Credentials are never stored — only the resulting session token is cached
 * in Redis with TTL.
 */
@Component
public class CinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaClient.class);

  // Request the JWT in the login response so we can use it for subsequent calls
  private static final String LOGIN_PATH = "/login?optionalFields=jwt";

  // JWT refresh endpoint — no session auth required, only a valid Cineca JWT
  private static final String JWT_REFRESH_PATH = "/jwt/refresh";

  private final WebClient webClient;

  public CinecaClient() {
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
        .build();
  }

  /**
   * Authenticates a student against their university's ESSE3 instance.
   *
   * Calls {@code GET /login?optionalFields=jwt} with HTTP Basic Auth.
   * Returns the full login response including career segments and JWT token.
   *
   * @param cinecaBaseUrl base URL of the university's ESSE3 API,
   *                      e.g. "https://unimol.esse3.cineca.it/e3rest/api"
   * @param username      Cineca username, e.g. "a.delmuto"
   * @param password      Cineca password
   * @return login response with authToken, JWT and career segments
   * @throws CinecaAuthException if credentials are invalid (401) or
   *                             the account is not enabled for REST (400)
   * @throws CinecaUnavailableException if the ESSE3 instance is unreachable
   */
  public CinecaLoginResponse login(
      String cinecaBaseUrl,
      String username,
      String password) {

    String url = cinecaBaseUrl + LOGIN_PATH;
    String basicAuth = basicAuth(username, password);

    log.info("CinecaClient: login attempt for user '{}' at {}", username, cinecaBaseUrl);

    try {
      return webClient.get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, basicAuth)
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, response ->
              response.statusCode().value() == 401
                  ? Mono.error(new CinecaAuthException("Invalid Cineca credentials for user: "
                  + username))
                  : Mono.error(new CinecaAuthException("Cineca rejected request with status: "
                      + response.statusCode().value())))
          .onStatus(HttpStatusCode::is5xxServerError, response ->
              Mono.error(new CinecaUnavailableException("Cineca ESSE3 unavailable at: "
                  + cinecaBaseUrl)))
          .bodyToMono(CinecaLoginResponse.class)
          .block();
    } catch (CinecaAuthException | CinecaUnavailableException e) {
      throw e;
    } catch (Exception e) {
      log.error("CinecaClient: unexpected error during login for user '{}': {}",
          username, e.getMessage());
      throw new CinecaUnavailableException("Failed to reach Cineca at: " + cinecaBaseUrl);
    }
  }

  /**
   * Refreshes an existing Cineca JWT token.
   *
   * Calls {@code GET /jwt/refresh} with the current JWT in the Authorization header.
   * This is a public endpoint on Cineca — no session required, only a valid JWT.
   *
   * @param cinecaBaseUrl base URL of the university's ESSE3 API
   * @param currentJwt    the current (possibly near-expiry) Cineca JWT
   * @return new Cineca JWT with refreshed expiry
   * @throws CinecaAuthException if the current JWT is invalid or expired
   * @throws CinecaUnavailableException if the ESSE3 instance is unreachable
   */
  public String refreshJwt(String cinecaBaseUrl, String currentJwt) {
    String url = cinecaBaseUrl + JWT_REFRESH_PATH;

    log.debug("CinecaClient: refreshing JWT at {}", cinecaBaseUrl);

    try {
      return webClient.get()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentJwt)
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, response ->
              Mono.error(new CinecaAuthException("Cannot refresh Cineca JWT — token invalid")))
          .onStatus(HttpStatusCode::is5xxServerError, response ->
              Mono.error(new CinecaUnavailableException("Cineca ESSE3 unavailable at: "
                  + cinecaBaseUrl)))
          .bodyToMono(String.class)
          .block();
    } catch (CinecaAuthException | CinecaUnavailableException e) {
      throw e;
    } catch (Exception e) {
      log.error("CinecaClient: failed to refresh JWT at {}: {}", cinecaBaseUrl, e.getMessage());
      throw new CinecaUnavailableException("Failed to reach Cineca at: " + cinecaBaseUrl);
    }
  }

  // ================================
  // Private helpers
  // ================================

  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  // ================================
  // Exceptions
  // ================================

  public static class CinecaAuthException extends RuntimeException {
    public CinecaAuthException(String message) {
      super(message);
    }
  }

  public static class CinecaUnavailableException extends RuntimeException {
    public CinecaUnavailableException(String message) {
      super(message);
    }
  }
}
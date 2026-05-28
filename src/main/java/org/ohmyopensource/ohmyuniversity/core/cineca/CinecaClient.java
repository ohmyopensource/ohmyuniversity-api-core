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
 * HTTP client for interacting with the Cineca ESSE3 REST API.
 *
 * This client acts as a stateless proxy between OhMyUniversity and the
 * university ESSE3 systems.
 *
 * Key characteristics:
 * - Each university has its own base URL
 * - Authentication uses HTTP Basic Auth (username/password)
 * - No credentials are persisted
 * - Session tokens are stored externally in Redis
 *
 * All calls are reactive (WebClient) but executed in blocking mode
 * at service layer boundaries.
 */
@Component
public class CinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaClient.class);

  /** Login endpoint requesting also JWT in response payload */
  private static final String LOGIN_PATH = "/login?optionalFields=jwt";

  /** Endpoint used to refresh an existing Cineca JWT */
  private static final String JWT_REFRESH_PATH = "/jwt/refresh";

  private final WebClient webClient;

  // ============ Constructor ============

  /**
   * Initializes the WebClient instance used to communicate with Cineca ESSE3 APIs.
   *
   * Configuration details:
   * - Increases max in-memory buffer size to 5MB to safely handle large Cineca responses
   *   (e.g. login payloads containing career data and nested structures)
   * - Uses default WebClient builder since base URL is dynamic per request
   *
   * This client is stateless by design — no base URL is preconfigured here.
   * Each request provides its own target ESSE3 instance.
   */
  public CinecaClient() {
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
        .build();
  }

  // ============ Class Methods ============

  /**
   * Authenticates a student against a specific Cineca ESSE3 instance.
   *
   * This method performs:
   * - HTTP GET /login?optionalFields=jwt
   * - HTTP Basic Authentication (username/password)
   * - Parsing of CinecaLoginResponse
   *
   * @param cinecaBaseUrl base URL of ESSE3 API (e.g. https://.../e3rest/api)
   * @param username Cineca username
   * @param password Cineca password
   * @return CinecaLoginResponse containing JWT, authToken, and user data
   *
   * @throws CinecaAuthException if credentials are invalid (401/400)
   * @throws CinecaUnavailableException if Cineca service is unreachable
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
   * Used when the current JWT is close to expiration.
   *
   * @param cinecaBaseUrl base URL of ESSE3 API
   * @param currentJwt valid Cineca JWT
   * @return refreshed JWT string
   *
   * @throws CinecaAuthException if JWT is invalid or expired
   * @throws CinecaUnavailableException if Cineca service is unreachable
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

  /**
   * Builds HTTP Basic Authentication header value.
   *
   * @param username Cineca username
   * @param password Cineca password
   * @return formatted "Basic base64(username:password)" header
   */
  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Thrown when Cineca rejects authentication or JWT is invalid.
   */
  public static class CinecaAuthException extends RuntimeException {
    public CinecaAuthException(String message) {
      super(message);
    }
  }

  /**
   * Thrown when Cineca ESSE3 is unreachable or returns 5xx errors.
   */
  public static class CinecaUnavailableException extends RuntimeException {
    public CinecaUnavailableException(String message) {
      super(message);
    }
  }
}
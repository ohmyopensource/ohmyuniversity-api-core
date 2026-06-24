package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Abstract base class for all Cineca ESSE3 HTTP clients.
 *
 * <p>Provides a shared {@link WebClient} instance and utility methods
 * common to all concrete clients, avoiding duplication across the ESSE3 integration layer.
 *
 * <p>The WebClient is configured with an increased in-memory buffer
 * to safely handle large Cineca payloads without memory exceptions.
 */
public abstract class AbstractCinecaClient {

  protected static final int BUFFER_SIZE_MB = 10;

  protected final WebClient webClient;

  // ============ Constructor ============

  protected AbstractCinecaClient() {
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs()
            .maxInMemorySize(BUFFER_SIZE_MB * 1024 * 1024))
        .build();
  }

  // ============ Class Methods ============

  /**
   * Builds a Bearer Authorization header value from a Cineca JWT.
   *
   * @param jwt Cineca JWT token
   * @return formatted {@code Authorization: Bearer <jwt>} header value
   */
  protected String bearer(String jwt) {
    return "Bearer " + jwt;
  }

  /**
   * Builds an HTTP Basic Authentication header value.
   *
   * <p>Credentials are encoded as {@code Base64(username:password)}
   * as required by RFC 7617.
   *
   * @param username Cineca username
   * @param password Cineca password
   * @return formatted {@code Authorization: Basic <base64>} header value
   */
  protected String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Extracts the hostname from a full ESSE3 base URL.
   *
   * <p>Used when building URIs programmatically via {@code UriBuilder}
   * rather than concatenating strings.
   *
   * @param baseUrl full base URL (e.g. {@code https://unimol.esse3.cineca.it/e3rest/api})
   * @return hostname only (e.g. {@code unimol.esse3.cineca.it})
   */
  protected String extractHost(String baseUrl) {
    return baseUrl.replace("https://", "").split("/")[0];
  }

  /**
   * Returns the standard Authorization header name.
   *
   * <p>Convenience accessor to avoid importing {@link HttpHeaders} in subclasses.
   */
  protected String authHeader() {
    return HttpHeaders.AUTHORIZATION;
  }
}
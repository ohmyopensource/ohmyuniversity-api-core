package org.ohmyopensource.ohmyuniversity.core.cineca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link CinecaClient}.
 *
 * <p>{@link MockWebServer} intercepts outbound HTTP calls at the network level,
 * eliminating the need for a real Cineca ESSE3 instance. Tests cover the happy path,
 * {@code 4xx}/{@code 5xx} error mapping to domain exceptions, request header and path assertions,
 * and connection failure handling.
 */
class CinecaClientTest {

  /**
   * Shared {@link ObjectMapper} instance for constructing expected JSON payloads.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockWebServer server;
  private CinecaClient client;
  private String baseUrl;

  /**
   * Starts a fresh {@link MockWebServer} and constructs the base URL and {@link CinecaClient}
   * before each test.
   */
  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    baseUrl = server.url("/e3rest/api").toString();
    baseUrl = "http://" + server.getHostName() + ":" + server.getPort() + "/e3rest/api";
    client = new CinecaClient();
  }

  /**
   * Shuts down the {@link MockWebServer} after each test to release the bound port and avoid
   * resource leaks.
   */
  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  /**
   * Verifies the HTTP contract of {@link CinecaClient#login}, covering successful authentication,
   * request structure, and error code mapping.
   */
  @Nested
  @DisplayName("login()")
  class Login {

    /**
     * Verifies that a {@code 200 OK} response with a valid JSON body is parsed into a
     * {@link CinecaLoginResponse} with the expected field values.
     */
    @Test
    @DisplayName("200 → returns parsed CinecaLoginResponse")
    void success() throws Exception {
      String json = """
          {
            "authToken": "auth-token-abc",
            "jwt": "jwt-token-abc"
          }
          """;

      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody(json));

      CinecaLoginResponse result = client.login(baseUrl, "user", "pass");

      assertThat(result.getAuthToken()).isEqualTo("auth-token-abc");
      assertThat(result.getJwt()).isEqualTo("jwt-token-abc");
    }

    /**
     * Verifies that the {@code Authorization} header sent to Cineca is a correctly encoded HTTP
     * Basic Auth value derived from the supplied username and password.
     */
    @Test
    @DisplayName("200 → Authorization header is correct Basic Auth")
    void sendsCorrectBasicAuthHeader() throws Exception {
      String json = """
          {
            "authToken": "auth-token-abc",
            "jwt": "jwt-token-abc"
          }
          """;

      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody(json));

      client.login(baseUrl, "myuser", "mypassword");

      RecordedRequest recorded = server.takeRequest();
      String expectedHeader = "Basic " + Base64.getEncoder()
          .encodeToString("myuser:mypassword".getBytes());
      assertThat(recorded.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo(expectedHeader);
    }

    /**
     * Verifies that the login request targets the {@code /login} path and includes the
     * {@code optionalFields=jwt} query parameter required by the Cineca ESSE3 API to include a JWT
     * in the response.
     */
    @Test
    @DisplayName("200 → request hits /login?optionalFields=jwt")
    void hitsCorrectPath() throws Exception {
      String json = """
          {
            "authToken": "auth-token-abc",
            "jwt": "jwt-token-abc"
          }
          """;

      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody(json));

      client.login(baseUrl, "user", "pass");

      RecordedRequest recorded = server.takeRequest();
      assertThat(recorded.getPath()).contains("/login")
          .contains("optionalFields=jwt");
    }

    /**
     * Verifies that a {@code 401 Unauthorized} response from Cineca is mapped to a
     * {@link CinecaAuthException}.
     */
    @Test
    @DisplayName("401 → throws CinecaAuthException")
    void unauthorized() {
      server.enqueue(new MockResponse().setResponseCode(401));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "wrongpass"))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that a {@code 400 Bad Request} response from Cineca is mapped to a
     * {@link CinecaAuthException}.
     */
    @Test
    @DisplayName("400 → throws CinecaAuthException")
    void badRequest() {
      server.enqueue(new MockResponse().setResponseCode(400));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that a {@code 500 Internal Server Error} response from Cineca is mapped to a
     * {@link CinecaUnavailableException}.
     */
    @Test
    @DisplayName("500 → throws CinecaUnavailableException")
    void serverError() {
      server.enqueue(new MockResponse().setResponseCode(500));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaUnavailableException.class);
    }

    /**
     * Verifies that a connection failure — simulated by shutting down the server before the call —
     * is mapped to a {@link CinecaUnavailableException}.
     */
    @Test
    @DisplayName("connection refused → throws CinecaUnavailableException")
    void connectionRefused() throws Exception {
      server.shutdown();

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }

  /**
   * Verifies the HTTP contract of {@link CinecaClient#refreshJwt}, covering successful token
   * refresh, request structure, and error code mapping.
   */
  @Nested
  @DisplayName("refreshJwt()")
  class RefreshJwt {

    /**
     * Verifies that a {@code 200 OK} response with a plain-text body is returned as the refreshed
     * JWT string.
     */
    @Test
    @DisplayName("200 → returns refreshed JWT string")
    void success() {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
          .setBody("new-jwt-token"));

      String result = client.refreshJwt(baseUrl, "old-jwt");

      assertThat(result).isEqualTo("new-jwt-token");
    }

    /**
     * Verifies that the {@code Authorization} header sent to Cineca uses the Bearer scheme carrying
     * the current JWT value.
     */
    @Test
    @DisplayName("200 → Authorization header is Bearer <currentJwt>")
    void sendsBearerHeader() throws Exception {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
          .setBody("new-jwt-token"));

      client.refreshJwt(baseUrl, "my-current-jwt");

      RecordedRequest recorded = server.takeRequest();
      assertThat(recorded.getHeader(HttpHeaders.AUTHORIZATION))
          .isEqualTo("Bearer my-current-jwt");
    }

    /**
     * Verifies that the refresh request targets the {@code /jwt/refresh} path as required by the
     * Cineca ESSE3 API.
     */
    @Test
    @DisplayName("200 → request hits /jwt/refresh")
    void hitsCorrectPath() throws Exception {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
          .setBody("new-jwt-token"));

      client.refreshJwt(baseUrl, "jwt");

      RecordedRequest recorded = server.takeRequest();
      assertThat(recorded.getPath()).contains("/jwt/refresh");
    }

    /**
     * Verifies that a {@code 401 Unauthorized} response from Cineca is mapped to a
     * {@link CinecaAuthException}, indicating the current JWT has expired.
     */
    @Test
    @DisplayName("401 → throws CinecaAuthException")
    void unauthorized() {
      server.enqueue(new MockResponse().setResponseCode(401));

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "expired-jwt"))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that a {@code 500 Internal Server Error} response from Cineca is mapped to a
     * {@link CinecaUnavailableException}.
     */
    @Test
    @DisplayName("500 → throws CinecaUnavailableException")
    void serverError() {
      server.enqueue(new MockResponse().setResponseCode(500));

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "jwt"))
          .isInstanceOf(CinecaUnavailableException.class);
    }

    /**
     * Verifies that a connection failure — simulated by shutting down the server before the call —
     * is mapped to a {@link CinecaUnavailableException}.
     */
    @Test
    @DisplayName("connection refused → throws CinecaUnavailableException")
    void connectionRefused() throws Exception {
      server.shutdown();

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "jwt"))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }
}
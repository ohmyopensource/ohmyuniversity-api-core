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
 * MockWebServer intercepts HTTP calls at network level — no real Cineca instance required.
 * Tests cover happy path, 4xx/5xx error mapping, and connection failure handling.
 */
class CinecaClientTest {

  private MockWebServer server;
  private CinecaClient client;
  private String baseUrl;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    server  = new MockWebServer();
    server.start();
    baseUrl = server.url("/e3rest/api").toString();
    // Strip trailing path — CinecaClient appends its own paths
    baseUrl = "http://" + server.getHostName() + ":" + server.getPort() + "/e3rest/api";
    client  = new CinecaClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  // ============================================================
  // login()
  // ============================================================

  @Nested
  @DisplayName("login()")
  class Login {

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

    @Test
    @DisplayName("401 → throws CinecaAuthException")
    void unauthorized() {
      server.enqueue(new MockResponse().setResponseCode(401));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "wrongpass"))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("400 → throws CinecaAuthException")
    void badRequest() {
      server.enqueue(new MockResponse().setResponseCode(400));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("500 → throws CinecaUnavailableException")
    void serverError() {
      server.enqueue(new MockResponse().setResponseCode(500));

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaUnavailableException.class);
    }

    @Test
    @DisplayName("connection refused → throws CinecaUnavailableException")
    void connectionRefused() throws Exception {
      // Shut down server before calling — simulates unreachable Cineca
      server.shutdown();

      assertThatThrownBy(() -> client.login(baseUrl, "user", "pass"))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }

  // ============================================================
  // refreshJwt()
  // ============================================================

  @Nested
  @DisplayName("refreshJwt()")
  class RefreshJwt {

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

    @Test
    @DisplayName("401 → throws CinecaAuthException")
    void unauthorized() {
      server.enqueue(new MockResponse().setResponseCode(401));

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "expired-jwt"))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("500 → throws CinecaUnavailableException")
    void serverError() {
      server.enqueue(new MockResponse().setResponseCode(500));

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "jwt"))
          .isInstanceOf(CinecaUnavailableException.class);
    }

    @Test
    @DisplayName("connection refused → throws CinecaUnavailableException")
    void connectionRefused() throws Exception {
      server.shutdown();

      assertThatThrownBy(() -> client.refreshJwt(baseUrl, "jwt"))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }
}
package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.controller.v1.EmailController;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.service.EmailService;
import org.ohmyopensource.ohmyuniversity.core.service.EmailService.EmailNotConnectedException;
import org.ohmyopensource.ohmyuniversity.core.service.email.MicrosoftEmailProvider.EmailProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Unit tests for {@link EmailController} using {@link MockMvc}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link EmailService} is replaced by a Mockito mock; the authenticated {@link OmuPrincipal} is
 * injected into each request through {@link SecurityMockMvcRequestPostProcessors#authentication}.
 */
@WebMvcTest(controllers = EmailController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailControllerTest {

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private MockMvc mockMvc;

  @MockitoBean
  private EmailService emailService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  private OmuPrincipal principal;

  /**
   * Initialises a test {@link OmuPrincipal} injected into the {@link MockMvc} security context
   * before each test via {@link #auth()}.
   */
  @BeforeEach
  void setUp() {
    principal = new OmuPrincipal(
        UUID.randomUUID().toString(),
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026", true);
  }

  /**
   * Returns a {@link RequestPostProcessor} that wraps {@link #principal} in a
   * {@link UsernamePasswordAuthenticationToken} and registers it as the current security context
   * principal for the outgoing {@link MockMvc} request.
   *
   * @return a post-processor that injects the test principal into the request
   */
  private RequestPostProcessor auth() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENTE"))));
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/email/auth/url}, covering successful retrieval
   * of the OAuth2 authorization URL.
   */
  @Nested
  @DisplayName("GET /api/v1/email/auth/url")
  class GetAuthUrl {

    /**
     * Verifies that a successful call to {@link EmailService#getAuthorizationUrl} produces a
     * {@code 200 OK} response whose body contains the expected Microsoft authorization URL.
     */
    @Test
    @DisplayName("returns 200 with authorization URL")
    void returns200() throws Exception {
      when(emailService.getAuthorizationUrl(any()))
          .thenReturn("https://login.microsoftonline.com/oauth2/authorize");

      mockMvc.perform(get("/api/v1/email/auth/url").with(auth()))
          .andExpect(status().isOk())
          .andExpect(content().string("https://login.microsoftonline.com/oauth2/authorize"));
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/email/auth/callback}, covering successful
   * token exchange and provider failure.
   */
  @Nested
  @DisplayName("GET /api/v1/email/auth/callback")
  class HandleCallback {

    /**
     * Verifies that a successful call to {@link EmailService#handleCallback} produces a
     * {@code 204 No Content} response with no body.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(emailService).handleCallback(anyString(), anyString());

      mockMvc.perform(get("/api/v1/email/auth/callback")
              .param("code", "auth-code-123")
              .param("state", UUID.randomUUID().toString()))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EmailProviderException} thrown by {@link EmailService#handleCallback}
     * — indicating a failed token exchange — is mapped to a {@code 400 Bad Request} response.
     */
    @Test
    @DisplayName("returns 400 when token exchange fails")
    void returns400OnProviderFailure() throws Exception {
      doThrow(new EmailProviderException("exchange failed"))
          .when(emailService).handleCallback(anyString(), anyString());

      mockMvc.perform(get("/api/v1/email/auth/callback")
              .param("code", "bad-code")
              .param("state", UUID.randomUUID().toString()))
          .andExpect(status().isBadRequest());
    }
  }

  /**
   * Verifies the HTTP contract of {@code DELETE /api/v1/email/auth/disconnect}, covering successful
   * disconnection.
   */
  @Nested
  @DisplayName("DELETE /api/v1/email/auth/disconnect")
  class Disconnect {

    /**
     * Verifies that a successful call to {@link EmailService#disconnect} produces a
     * {@code 204 No Content} response with no body.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(emailService).disconnect(any());

      mockMvc.perform(delete("/api/v1/email/auth/disconnect").with(auth()))
          .andExpect(status().isNoContent());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/email/inbox}, covering successful retrieval,
   * email not connected, and provider failure.
   */
  @Nested
  @DisplayName("GET /api/v1/email/inbox")
  class GetInbox {

    /**
     * Verifies that a successful call to {@link EmailService#getInbox} produces a {@code 200 OK}
     * response whose JSON body contains the expected inbox fields.
     */
    @Test
    @DisplayName("returns 200 with inbox")
    void returns200() throws Exception {
      EmailInboxResponse inbox = new EmailInboxResponse();
      inbox.setMessages(List.of());
      inbox.setTotalCount(0);

      when(emailService.getInbox(any())).thenReturn(inbox);

      mockMvc.perform(get("/api/v1/email/inbox").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalCount").value(0));
    }

    /**
     * Verifies that an {@link EmailNotConnectedException} thrown by {@link EmailService#getInbox} —
     * indicating the user has not connected their email — is mapped to a {@code 409 Conflict}
     * response.
     */
    @Test
    @DisplayName("returns 409 when email not connected")
    void returns409WhenNotConnected() throws Exception {
      when(emailService.getInbox(any()))
          .thenThrow(new EmailNotConnectedException("not connected"));

      mockMvc.perform(get("/api/v1/email/inbox").with(auth()))
          .andExpect(status().isConflict());
    }

    /**
     * Verifies that an {@link EmailProviderException} thrown by {@link EmailService#getInbox} —
     * indicating the email provider is unavailable — is mapped to a {@code 502 Bad Gateway}
     * response.
     */
    @Test
    @DisplayName("returns 502 when provider fails")
    void returns502OnProviderError() throws Exception {
      when(emailService.getInbox(any()))
          .thenThrow(new EmailProviderException("provider down"));

      mockMvc.perform(get("/api/v1/email/inbox").with(auth()))
          .andExpect(status().isBadGateway());
    }
  }

  /**
   * Verifies the HTTP contract of {@code GET /api/v1/email/{messageId}}, covering successful
   * retrieval, email not connected, and provider failure.
   */
  @Nested
  @DisplayName("GET /api/v1/email/{messageId}")
  class GetMessage {

    /**
     * Verifies that a successful call to {@link EmailService#getMessage} produces a {@code 200 OK}
     * response whose JSON body contains the expected message fields.
     */
    @Test
    @DisplayName("returns 200 with message")
    void returns200() throws Exception {
      EmailMessageResponse message = new EmailMessageResponse();
      message.setId("msg-123");
      message.setSubject("Test email");
      message.setBodyText("Hello!");

      when(emailService.getMessage(any(), anyString())).thenReturn(message);

      mockMvc.perform(get("/api/v1/email/msg-123").with(auth()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("msg-123"))
          .andExpect(jsonPath("$.subject").value("Test email"));
    }

    /**
     * Verifies that an {@link EmailNotConnectedException} thrown by {@link EmailService#getMessage}
     * is mapped to a {@code 409 Conflict} response.
     */
    @Test
    @DisplayName("returns 409 when email not connected")
    void returns409WhenNotConnected() throws Exception {
      when(emailService.getMessage(any(), anyString()))
          .thenThrow(new EmailNotConnectedException("not connected"));

      mockMvc.perform(get("/api/v1/email/msg-123").with(auth()))
          .andExpect(status().isConflict());
    }

    /**
     * Verifies that an {@link EmailProviderException} thrown by {@link EmailService#getMessage} is
     * mapped to a {@code 502 Bad Gateway} response.
     */
    @Test
    @DisplayName("returns 502 when provider fails")
    void returns502OnProviderError() throws Exception {
      when(emailService.getMessage(any(), anyString()))
          .thenThrow(new EmailProviderException("provider down"));

      mockMvc.perform(get("/api/v1/email/msg-123").with(auth()))
          .andExpect(status().isBadGateway());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/v1/email/send}, covering successful send, email
   * not connected, and provider failure.
   */
  @Nested
  @DisplayName("POST /api/v1/email/send")
  class SendEmail {

    /**
     * Verifies that a successful call to {@link EmailService#sendEmail} produces a
     * {@code 204 No Content} response with no body.
     */
    @Test
    @DisplayName("returns 204 on success")
    void returns204() throws Exception {
      doNothing().when(emailService).sendEmail(any(), any());

      mockMvc.perform(post("/api/v1/email/send")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"to\":\"test@unimol.it\",\"subject\":\"Ciao\",\"body\":\"Test\"}"))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that an {@link EmailNotConnectedException} thrown by {@link EmailService#sendEmail}
     * is mapped to a {@code 409 Conflict} response.
     */
    @Test
    @DisplayName("returns 409 when email not connected")
    void returns409WhenNotConnected() throws Exception {
      doThrow(new EmailNotConnectedException("not connected"))
          .when(emailService).sendEmail(any(), any());

      mockMvc.perform(post("/api/v1/email/send")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"to\":\"test@unimol.it\",\"subject\":\"Ciao\",\"body\":\"Test\"}"))
          .andExpect(status().isConflict());
    }

    /**
     * Verifies that an {@link EmailProviderException} thrown by {@link EmailService#sendEmail} is
     * mapped to a {@code 502 Bad Gateway} response.
     */
    @Test
    @DisplayName("returns 502 when provider fails")
    void returns502OnProviderError() throws Exception {
      doThrow(new EmailProviderException("provider down"))
          .when(emailService).sendEmail(any(), any());

      mockMvc.perform(post("/api/v1/email/send")
              .with(auth())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"to\":\"test@unimol.it\",\"subject\":\"Ciao\",\"body\":\"Test\"}"))
          .andExpect(status().isBadGateway());
    }
  }
}
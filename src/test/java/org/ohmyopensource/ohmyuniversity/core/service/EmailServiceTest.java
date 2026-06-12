package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailSendRequest;
import org.ohmyopensource.ohmyuniversity.core.service.EmailService.EmailNotConnectedException;
import org.ohmyopensource.ohmyuniversity.core.service.email.EmailProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for {@link EmailService}.
 *
 * <p>All external dependencies — {@link EmailProvider} and {@link StringRedisTemplate} — are
 * replaced by Mockito mocks so that no Redis instance or Microsoft Graph connection is required.
 * Each test group covers a single public method of the service, verifying both the happy path and
 * the relevant exception paths.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

  @Mock
  private EmailProvider emailProvider;

  @Mock
  private StringRedisTemplate redis;

  @Mock
  private ValueOperations<String, String> valueOps;

  @InjectMocks
  private EmailService emailService;

  private OmuPrincipal principal;
  private String userId;

  /**
   * Initialises a test {@link OmuPrincipal} reused across all test cases and stubs the Redis value
   * operations accessor used by the service.
   */
  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID().toString();
    principal = new OmuPrincipal(
        userId,
        "TSTXXX00A00X000X",
        "UNIMOL",
        89486L,
        106279L,
        "178026");
    when(redis.opsForValue()).thenReturn(valueOps);
  }

  /**
   * Verifies {@link EmailService#getAuthorizationUrl} delegates to the provider and returns the URL
   * unchanged.
   */
  @Nested
  @DisplayName("getAuthorizationUrl")
  class GetAuthorizationUrl {

    /**
     * Verifies that {@link EmailService#getAuthorizationUrl} calls
     * {@link EmailProvider#buildAuthorizationUrl} with the authenticated user's id and returns the
     * resulting URL.
     */
    @Test
    @DisplayName("returns authorization URL from provider")
    void returnsUrl() {
      when(emailProvider.buildAuthorizationUrl(userId))
          .thenReturn("https://login.microsoftonline.com/oauth2/authorize");

      String result = emailService.getAuthorizationUrl(principal);

      assertThat(result).isEqualTo("https://login.microsoftonline.com/oauth2/authorize");
    }
  }

  /**
   * Verifies {@link EmailService#handleCallback} exchanges the code and stores the token in Redis.
   */
  @Nested
  @DisplayName("handleCallback")
  class HandleCallback {

    /**
     * Verifies that {@link EmailService#handleCallback} calls
     * {@link EmailProvider#exchangeCodeForToken}, then stores the returned token in Redis under the
     * expected key with the configured TTL.
     */
    @Test
    @DisplayName("exchanges code and stores token in Redis")
    void exchangesCodeAndStoresToken() {
      when(emailProvider.exchangeCodeForToken("code-123", userId)).thenReturn("access-token-abc");

      emailService.handleCallback("code-123", userId);

      verify(emailProvider).exchangeCodeForToken("code-123", userId);
      verify(valueOps).set(
          eq("email:token:" + userId),
          eq("access-token-abc"),
          any());
    }
  }

  /**
   * Verifies {@link EmailService#disconnect} deletes the token from Redis.
   */
  @Nested
  @DisplayName("disconnect")
  class Disconnect {

    /**
     * Verifies that {@link EmailService#disconnect} calls {@link StringRedisTemplate#delete} with
     * the correct token key for the authenticated user.
     */
    @Test
    @DisplayName("deletes token from Redis")
    void deletesToken() {
      emailService.disconnect(principal);

      verify(redis).delete("email:token:" + userId);
    }
  }

  /**
   * Verifies {@link EmailService#isConnected} checks Redis for the token key.
   */
  @Nested
  @DisplayName("isConnected")
  class IsConnected {

    /**
     * Verifies that {@link EmailService#isConnected} returns {@code true} when Redis reports the
     * token key exists.
     */
    @Test
    @DisplayName("returns true when token exists in Redis")
    void returnsTrueWhenConnected() {
      when(redis.hasKey("email:token:" + userId)).thenReturn(true);

      assertThat(emailService.isConnected(principal)).isTrue();
    }

    /**
     * Verifies that {@link EmailService#isConnected} returns {@code false} when Redis reports the
     * token key does not exist.
     */
    @Test
    @DisplayName("returns false when token not in Redis")
    void returnsFalseWhenNotConnected() {
      when(redis.hasKey("email:token:" + userId)).thenReturn(false);

      assertThat(emailService.isConnected(principal)).isFalse();
    }
  }

  /**
   * Verifies {@link EmailService#getInbox} covering successful retrieval and email not connected.
   */
  @Nested
  @DisplayName("getInbox")
  class GetInbox {

    /**
     * Verifies that {@link EmailService#getInbox} resolves the token from Redis, delegates to
     * {@link EmailProvider#getInbox}, and returns the result.
     */
    @Test
    @DisplayName("returns inbox from provider")
    void returnsInbox() {
      when(valueOps.get("email:token:" + userId)).thenReturn("access-token-abc");

      EmailInboxResponse inbox = new EmailInboxResponse();
      inbox.setMessages(List.of());
      inbox.setTotalCount(0);
      when(emailProvider.getInbox("access-token-abc")).thenReturn(inbox);

      EmailInboxResponse result = emailService.getInbox(principal);

      assertThat(result.getTotalCount()).isZero();
      verify(emailProvider).getInbox("access-token-abc");
    }

    /**
     * Verifies that {@link EmailService#getInbox} throws {@link EmailNotConnectedException} when no
     * token is found in Redis for the authenticated user.
     */
    @Test
    @DisplayName("throws EmailNotConnectedException when token not in Redis")
    void throwsWhenNotConnected() {
      when(valueOps.get("email:token:" + userId)).thenReturn(null);

      assertThatThrownBy(() -> emailService.getInbox(principal))
          .isInstanceOf(EmailNotConnectedException.class);
    }
  }

  /**
   * Verifies {@link EmailService#getMessage} covering successful retrieval and email not
   * connected.
   */
  @Nested
  @DisplayName("getMessage")
  class GetMessage {

    /**
     * Verifies that {@link EmailService#getMessage} resolves the token from Redis, delegates to
     * {@link EmailProvider#getMessage}, and returns the result.
     */
    @Test
    @DisplayName("returns message from provider")
    void returnsMessage() {
      when(valueOps.get("email:token:" + userId)).thenReturn("access-token-abc");

      EmailMessageResponse message = new EmailMessageResponse();
      message.setId("msg-123");
      message.setSubject("Test");
      when(emailProvider.getMessage("access-token-abc", "msg-123")).thenReturn(message);

      EmailMessageResponse result = emailService.getMessage(principal, "msg-123");

      assertThat(result.getId()).isEqualTo("msg-123");
      verify(emailProvider).getMessage("access-token-abc", "msg-123");
    }

    /**
     * Verifies that {@link EmailService#getMessage} throws {@link EmailNotConnectedException} when
     * no token is found in Redis.
     */
    @Test
    @DisplayName("throws EmailNotConnectedException when token not in Redis")
    void throwsWhenNotConnected() {
      when(valueOps.get("email:token:" + userId)).thenReturn(null);

      assertThatThrownBy(() -> emailService.getMessage(principal, "msg-123"))
          .isInstanceOf(EmailNotConnectedException.class);
    }
  }

  /**
   * Verifies {@link EmailService#sendEmail} covering successful send and email not connected.
   */
  @Nested
  @DisplayName("sendEmail")
  class SendEmail {

    /**
     * Verifies that {@link EmailService#sendEmail} resolves the token from Redis and delegates to
     * {@link EmailProvider#sendEmail}.
     */
    @Test
    @DisplayName("sends email via provider")
    void sendsEmail() {
      when(valueOps.get("email:token:" + userId)).thenReturn("access-token-abc");

      EmailSendRequest request = new EmailSendRequest();
      emailService.sendEmail(principal, request);

      verify(emailProvider).sendEmail(eq("access-token-abc"), eq(request));
    }

    /**
     * Verifies that {@link EmailService#sendEmail} throws {@link EmailNotConnectedException} when
     * no token is found in Redis.
     */
    @Test
    @DisplayName("throws EmailNotConnectedException when token not in Redis")
    void throwsWhenNotConnected() {
      when(valueOps.get("email:token:" + userId)).thenReturn(null);

      assertThatThrownBy(() -> emailService.sendEmail(principal, new EmailSendRequest()))
          .isInstanceOf(EmailNotConnectedException.class);
    }
  }
}
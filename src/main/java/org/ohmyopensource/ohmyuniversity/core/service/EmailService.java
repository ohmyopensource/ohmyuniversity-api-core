package org.ohmyopensource.ohmyuniversity.core.service;

import java.time.Duration;
import java.util.Optional;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailSendRequest;
import org.ohmyopensource.ohmyuniversity.core.service.email.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Orchestrates institutional email operations for authenticated students.
 *
 * <p>Manages the OAuth2 token lifecycle in Redis and delegates all
 * provider-specific operations to the injected {@link EmailProvider}.
 * Currently uses
 * {@link org.ohmyopensource.ohmyuniversity.core.service.email.MicrosoftEmailProvider}
 * but is provider-agnostic by design.
 *
 * <p>Token storage key pattern: {@code email:token:{omuUserId}}
 * TTL: 55 minutes (Microsoft access tokens expire in 60 minutes).
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private static final String TOKEN_KEY = "email:token:%s";
  private static final Duration TOKEN_TTL = Duration.ofMinutes(55);

  private final EmailProvider emailProvider;
  private final StringRedisTemplate redis;

  // ============ Constructor ============

  /**
   * Creates the email service with the active provider and Redis.
   *
   * @param emailProvider the active institutional email provider
   * @param redis         Redis template for token storage
   */
  public EmailService(EmailProvider emailProvider, StringRedisTemplate redis) {
    this.emailProvider = emailProvider;
    this.redis = redis;
  }

  // ============ Class Methods ============

  /**
   * Returns the OAuth2 authorization URL the client must open in a WebView.
   *
   * @param principal authenticated OhMyU principal
   * @return authorization URL
   */
  public String getAuthorizationUrl(OmuPrincipal principal) {
    return emailProvider.buildAuthorizationUrl(principal.omuUserId());
  }

  /**
   * Handles the OAuth2 callback — exchanges the code for a token and stores it in Redis.
   *
   * @param code  authorization code from the provider
   * @param state omuUserId passed as state parameter
   */
  public void handleCallback(String code, String state) {
    log.info("EmailService: handling OAuth2 callback for user={}", state);
    String accessToken = emailProvider.exchangeCodeForToken(code, state);
    String key = String.format(TOKEN_KEY, state);
    redis.opsForValue().set(key, accessToken, TOKEN_TTL);
    log.info("EmailService: email token stored for user={}", state);
  }

  /**
   * Disconnects the user's email account by deleting the token from Redis.
   *
   * @param principal authenticated OhMyU principal
   */
  public void disconnect(OmuPrincipal principal) {
    redis.delete(String.format(TOKEN_KEY, principal.omuUserId()));
    log.info("EmailService: email disconnected for user={}", principal.omuUserId());
  }

  /**
   * Returns true if the user has a valid email token in Redis.
   *
   * @param principal authenticated OhMyU principal
   * @return true if connected
   */
  public boolean isConnected(OmuPrincipal principal) {
    return Boolean.TRUE.equals(
        redis.hasKey(String.format(TOKEN_KEY, principal.omuUserId())));
  }

  /**
   * Retrieves the inbox for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return inbox response
   * @throws EmailNotConnectedException if the user has not connected their email
   */
  public EmailInboxResponse getInbox(OmuPrincipal principal) {
    String token = resolveToken(principal);
    return emailProvider.getInbox(token);
  }

  /**
   * Retrieves a single email message.
   *
   * @param principal authenticated OhMyU principal
   * @param messageId provider-specific message identifier
   * @return full message content
   * @throws EmailNotConnectedException if the user has not connected their email
   */
  public EmailMessageResponse getMessage(OmuPrincipal principal, String messageId) {
    String token = resolveToken(principal);
    return emailProvider.getMessage(token, messageId);
  }

  /**
   * Sends an email on behalf of the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @param request   message to send
   * @throws EmailNotConnectedException if the user has not connected their email
   */
  public void sendEmail(OmuPrincipal principal, EmailSendRequest request) {
    String token = resolveToken(principal);
    emailProvider.sendEmail(token, request);
  }

  /**
   * Resolves the active messaging access token associated with the
   * authenticated user.
   *
   * @param principal the authenticated user context
   * @return the access token required for messaging operations
   * @throws EmailNotConnectedException if no active messaging integration
   *         is available for the user
   */
  private String resolveToken(OmuPrincipal principal) {
    String key = String.format(TOKEN_KEY, principal.omuUserId());
    return Optional.ofNullable(redis.opsForValue().get(key))
        .orElseThrow(() -> new EmailNotConnectedException(
            "Email not connected for user=" + principal.omuUserId()));
  }

  /**
   * Exception indicating that the requested messaging operation cannot
   * be completed because no email account is currently associated with
   * the user session.
   */
  public static class EmailNotConnectedException extends RuntimeException {
    public EmailNotConnectedException(String message) {
      super(message);
    }
  }
}
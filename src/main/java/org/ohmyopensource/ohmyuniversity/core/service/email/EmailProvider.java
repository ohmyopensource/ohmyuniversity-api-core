package org.ohmyopensource.ohmyuniversity.core.service.email;

import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailSendRequest;

/**
 * Abstraction layer for institutional email providers.
 *
 * <p>OhMyUniversity! supports multiple email backends — currently Microsoft 365
 * via Microsoft Graph API. Future providers (e.g. Gmail, generic IMAP) can be added
 * by implementing this interface without changing the controller or service layer.
 *
 * <p>Each method receives the OAuth2 access token for the authenticated user,
 * retrieved from Redis by {@link org.ohmyopensource.ohmyuniversity.core.service.EmailService}.
 */
public interface EmailProvider {

  /**
   * Generates the OAuth2 authorization URL the client must redirect to.
   *
   * @param omuUserId the OhMyUniversity user identifier, used as OAuth2 state parameter
   * @return the full authorization URL
   */
  String buildAuthorizationUrl(String omuUserId);

  /**
   * Exchanges an OAuth2 authorization code for an access token and returns it.
   *
   * @param code  the authorization code received from the provider callback
   * @param state the state parameter (omuUserId) for CSRF validation
   * @return the access token to store in Redis
   */
  String exchangeCodeForToken(String code, String state);

  /**
   * Retrieves the inbox message list for the authenticated user.
   *
   * @param accessToken the user's email provider OAuth2 access token
   * @return inbox response containing message summaries
   */
  EmailInboxResponse getInbox(String accessToken);

  /**
   * Retrieves a single message by its provider-specific identifier.
   *
   * @param accessToken the user's email provider OAuth2 access token
   * @param messageId   the provider-specific message identifier
   * @return full message content
   */
  EmailMessageResponse getMessage(String accessToken, String messageId);

  /**
   * Sends an email on behalf of the authenticated user.
   *
   * @param accessToken the user's email provider OAuth2 access token
   * @param request     the message to send
   */
  void sendEmail(String accessToken, EmailSendRequest request);
}
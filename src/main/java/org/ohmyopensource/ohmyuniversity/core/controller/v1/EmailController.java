package org.ohmyopensource.ohmyuniversity.core.controller.v1;

import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailSendRequest;
import org.ohmyopensource.ohmyuniversity.core.service.EmailService;
import org.ohmyopensource.ohmyuniversity.core.service.EmailService.EmailNotConnectedException;
import org.ohmyopensource.ohmyuniversity.core.service.email.MicrosoftEmailProvider.EmailProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing institutional email functionalities.
 *
 * <p>Acts as the entry point for email-related services, coordinating
 * authentication flows, account connectivity, and message management
 * operations through the underlying application layer.
 */
@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

  private static final Logger log = LoggerFactory.getLogger(EmailController.class);

  private final EmailService emailService;

  // ============ Constructor ============

  /**
   * Creates the controller and injects the email service.
   *
   * @param emailService service orchestrating email operations
   */
  public EmailController(EmailService emailService) {
    this.emailService = emailService;
  }

  // ============ Class Methods ============

  /**
   * Returns the OAuth2 authorization URL the client must open in a WebView.
   *
   * <p>The client opens this URL in an embedded WebView. After the user
   * authenticates with Microsoft, the provider redirects to the callback endpoint.
   *
   * @param principal authenticated OhMyU principal
   * @return 200 OK with the authorization URL as a plain string
   */
  @GetMapping("/auth/url")
  public ResponseEntity<String> getAuthUrl(
      @AuthenticationPrincipal OmuPrincipal principal) {
    String url = emailService.getAuthorizationUrl(principal);
    return ResponseEntity.ok(url);
  }

  /**
   * Handles the OAuth2 callback from Microsoft.
   *
   * <p>Exchanges the authorization code for an access token and stores it in Redis.
   * The client is redirected here by Microsoft after successful authentication.
   * Returns 204 on success — the client should close the WebView.
   *
   * @param code  authorization code from Microsoft
   * @param state omuUserId passed as OAuth2 state parameter
   * @return 204 No Content on success, 400 if code exchange fails
   */
  @GetMapping("/auth/callback")
  public ResponseEntity<Void> handleCallback(
      @RequestParam String code,
      @RequestParam String state) {
    try {
      emailService.handleCallback(code, state);
      return ResponseEntity.noContent().build();
    } catch (EmailProviderException e) {
      log.warn("EmailController: callback failed — {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Disconnects the user's institutional email account.
   *
   * <p>Deletes the email access token from Redis. The user must re-authenticate
   * via the auth/url flow to reconnect.
   *
   * @param principal authenticated OhMyU principal
   * @return 204 No Content
   */
  @DeleteMapping("/auth/disconnect")
  public ResponseEntity<Void> disconnect(
      @AuthenticationPrincipal OmuPrincipal principal) {
    emailService.disconnect(principal);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns the inbox message list for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return 200 OK with {@link EmailInboxResponse},
   *         409 Conflict if email account not connected,
   *         502 Bad Gateway if the email provider is unavailable
   */
  @GetMapping("/inbox")
  public ResponseEntity<EmailInboxResponse> getInbox(
      @AuthenticationPrincipal OmuPrincipal principal) {
    try {
      return ResponseEntity.ok(emailService.getInbox(principal));
    } catch (EmailNotConnectedException e) {
      log.warn("EmailController: email not connected for user={}", principal.omuUserId());
      return ResponseEntity.status(409).build();
    } catch (EmailProviderException e) {
      log.error("EmailController: provider error — {}", e.getMessage());
      return ResponseEntity.status(502).build();
    }
  }

  /**
   * Returns a single email message by its provider-specific identifier.
   *
   * @param principal authenticated OhMyU principal
   * @param messageId provider-specific message identifier
   * @return 200 OK with {@link EmailMessageResponse},
   *         409 Conflict if email account not connected,
   *         502 Bad Gateway if the email provider is unavailable
   */
  @GetMapping("/{messageId}")
  public ResponseEntity<EmailMessageResponse> getMessage(
      @AuthenticationPrincipal OmuPrincipal principal,
      @PathVariable String messageId) {
    try {
      return ResponseEntity.ok(emailService.getMessage(principal, messageId));
    } catch (EmailNotConnectedException e) {
      log.warn("EmailController: email not connected for user={}", principal.omuUserId());
      return ResponseEntity.status(409).build();
    } catch (EmailProviderException e) {
      log.error("EmailController: provider error — {}", e.getMessage());
      return ResponseEntity.status(502).build();
    }
  }

  /**
   * Sends an email on behalf of the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @param request   message to send
   * @return 204 No Content on success,
   *         409 Conflict if email account not connected,
   *         502 Bad Gateway if the email provider is unavailable
   */
  @PostMapping("/send")
  public ResponseEntity<Void> sendEmail(
      @AuthenticationPrincipal OmuPrincipal principal,
      @RequestBody EmailSendRequest request) {
    try {
      emailService.sendEmail(principal, request);
      return ResponseEntity.noContent().build();
    } catch (EmailNotConnectedException e) {
      log.warn("EmailController: email not connected for user={}", principal.omuUserId());
      return ResponseEntity.status(409).build();
    } catch (EmailProviderException e) {
      log.error("EmailController: provider error — {}", e.getMessage());
      return ResponseEntity.status(502).build();
    }
  }
}
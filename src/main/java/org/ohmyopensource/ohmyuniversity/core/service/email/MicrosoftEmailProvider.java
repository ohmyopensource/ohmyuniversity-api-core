package org.ohmyopensource.ohmyuniversity.core.service.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailInboxResponse.EmailSummary;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailMessageResponse.AttachmentInfo;
import org.ohmyopensource.ohmyuniversity.core.dto.email.EmailSendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Microsoft 365 implementation of {@link EmailProvider} using Microsoft Graph API.
 *
 * <p>Uses OAuth2 Authorization Code flow to obtain access tokens.
 * Tokens are exchanged and returned to the caller for Redis storage —
 * this class does not manage persistence.
 */
@Component
public class MicrosoftEmailProvider implements EmailProvider {

  private static final Logger log = LoggerFactory.getLogger(MicrosoftEmailProvider.class);

  private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
  private static final String AUTH_BASE = "https://login.microsoftonline.com";
  private static final String SCOPES = "openid email Mail.Read Mail.Send User.Read offline_access";

  private final String clientId;
  private final String clientSecret;
  private final String tenantId;
  private final String redirectUri;
  private final WebClient webClient;

  // ============ Constructor ============

  /**
   * Creates the Microsoft email provider with Azure App Registration credentials.
   *
   * @param clientId    Azure App Registration client ID
   * @param clientSecret Azure App Registration client secret
   * @param tenantId    Azure directory (tenant) ID
   * @param redirectUri OAuth2 redirect URI registered in Azure
   */
  public MicrosoftEmailProvider(
      @Value("${MICROSOFT_CLIENT_ID:not-configured}") String clientId,
      @Value("${MICROSOFT_CLIENT_SECRET:not-configured}") String clientSecret,
      @Value("${MICROSOFT_TENANT_ID:common}") String tenantId,
      @Value("${MICROSOFT_REDIRECT_URI:http://localhost:8083/api/v1/email/auth/callback}") String redirectUri) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tenantId = tenantId;
    this.redirectUri = redirectUri;
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();
    log.info("MicrosoftEmailProvider: initialized for tenant={}", tenantId);
  }

  // ============ Override Methods ============

  /**
   * {@inheritDoc}
   *
   * <p>Builds the Microsoft authorization URL with the required scopes.
   * The {@code state} parameter carries the omuUserId for CSRF validation
   * and user identification in the callback.
   */
  @Override
  public String buildAuthorizationUrl(String omuUserId) {
    return AUTH_BASE + "/common/oauth2/v2.0/authorize"
        + "?client_id=" + clientId
        + "&response_type=code"
        + "&redirect_uri=" + redirectUri
        + "&scope=" + SCOPES.replace(" ", "%20")
        + "&state=" + omuUserId
        + "&prompt=select_account";
  }

  /**
   * {@inheritDoc}
   *
   * <p>Exchanges the authorization code for an access token using
   * the Microsoft token endpoint. Returns the access token for Redis storage.
   */
  @Override
  public String exchangeCodeForToken(String code, String state) {
    log.debug("MicrosoftEmailProvider: exchanging code for token, state={}", state);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("code", code);
    formData.add("redirect_uri", redirectUri);
    formData.add("grant_type", "authorization_code");
    formData.add("scope", SCOPES);

    MicrosoftTokenResponse response = webClient.post()
        .uri(AUTH_BASE + "/common/oauth2/v2.0/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("MicrosoftEmailProvider: token exchange failed: {}", body);
              return Mono.error(new EmailProviderException("Token exchange failed: " + body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new EmailProviderException("Microsoft auth server error")))
        .bodyToMono(MicrosoftTokenResponse.class)
        .block();

    if (response == null || response.accessToken == null) {
      throw new EmailProviderException("Empty token response from Microsoft");
    }

    return response.accessToken;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Fetches the 50 most recent inbox messages ordered by received date descending.
   */
  @Override
  public EmailInboxResponse getInbox(String accessToken) {
    log.debug("MicrosoftEmailProvider: fetching inbox");

    MicrosoftMessagesResponse response = webClient.get()
        .uri(GRAPH_BASE + "/me/messages"
            + "?$select=id,subject,from,receivedDateTime,isRead,hasAttachments,bodyPreview"
            + "&$orderby=receivedDateTime desc"
            + "&$top=50")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new EmailProviderException("Unauthorized — email token may be expired")))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new EmailProviderException("Microsoft Graph unavailable")))
        .bodyToMono(MicrosoftMessagesResponse.class)
        .block();

    EmailInboxResponse result = new EmailInboxResponse();
    if (response == null || response.value == null) {
      result.setMessages(List.of());
      result.setTotalCount(0);
      return result;
    }

    List<EmailSummary> summaries = response.value.stream().map(m -> {
      EmailSummary s = new EmailSummary();
      s.setId(m.id);
      s.setSubject(m.subject);
      s.setFromName(m.from != null && m.from.emailAddress != null
          ? m.from.emailAddress.name : null);
      s.setFromAddress(m.from != null && m.from.emailAddress != null
          ? m.from.emailAddress.address : null);
      s.setReceivedAt(m.receivedDateTime);
      s.setIsRead(m.isRead);
      s.setHasAttachments(m.hasAttachments);
      s.setPreview(m.bodyPreview);
      return s;
    }).toList();

    result.setMessages(summaries);
    result.setTotalCount(summaries.size());
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Fetches a single message with full body and attachment metadata.
   */
  @Override
  public EmailMessageResponse getMessage(String accessToken, String messageId) {
    log.debug("MicrosoftEmailProvider: fetching message id={}", messageId);

    MicrosoftMessage message = webClient.get()
        .uri(GRAPH_BASE + "/me/messages/" + messageId
            + "?$select=id,subject,from,toRecipients,ccRecipients,"
            + "receivedDateTime,isRead,hasAttachments,body,attachments")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new EmailProviderException("Message not found or unauthorized")))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new EmailProviderException("Microsoft Graph unavailable")))
        .bodyToMono(MicrosoftMessage.class)
        .block();

    if (message == null) {
      throw new EmailProviderException("Empty response for message id=" + messageId);
    }

    EmailMessageResponse response = new EmailMessageResponse();
    response.setId(message.id);
    response.setSubject(message.subject);
    response.setFromName(message.from != null && message.from.emailAddress != null
        ? message.from.emailAddress.name : null);
    response.setFromAddress(message.from != null && message.from.emailAddress != null
        ? message.from.emailAddress.address : null);
    response.setReceivedAt(message.receivedDateTime);
    response.setIsRead(message.isRead);
    response.setHasAttachments(message.hasAttachments);

    if (message.body != null) {
      if ("html".equalsIgnoreCase(message.body.contentType)) {
        response.setBodyHtml(message.body.content);
      } else {
        response.setBodyText(message.body.content);
      }
    }

    if (message.toRecipients != null) {
      response.setToAddresses(message.toRecipients.stream()
          .filter(r -> r.emailAddress != null)
          .map(r -> r.emailAddress.address)
          .toList());
    }

    if (message.ccRecipients != null) {
      response.setCcAddresses(message.ccRecipients.stream()
          .filter(r -> r.emailAddress != null)
          .map(r -> r.emailAddress.address)
          .toList());
    }

    if (message.attachments != null) {
      response.setAttachments(message.attachments.stream().map(a -> {
        AttachmentInfo info = new AttachmentInfo();
        info.setName(a.name);
        info.setContentType(a.contentType);
        info.setSizeBytes(a.size);
        return info;
      }).toList());
    }

    return response;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Sends an email using the Microsoft Graph sendMail endpoint.
   */
  @Override
  public void sendEmail(String accessToken, EmailSendRequest request) {
    log.debug("MicrosoftEmailProvider: sending email to {}", request.getToAddresses());

    Map<String, Object> body = Map.of(
        "message", Map.of(
            "subject", request.getSubject(),
            "body", Map.of(
                "contentType", request.getHtmlBody() != null ? "HTML" : "Text",
                "content", request.getHtmlBody() != null
                    ? request.getHtmlBody() : request.getTextBody()
            ),
            "toRecipients", request.getToAddresses().stream()
                .map(addr -> Map.of("emailAddress", Map.of("address", addr)))
                .collect(Collectors.toList())
        ),
        "saveToSentItems", true
    );

    webClient.post()
        .uri(GRAPH_BASE + "/me/sendMail")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(b -> {
              log.error("MicrosoftEmailProvider: sendMail failed: {}", b);
              return Mono.error(new EmailProviderException("Send failed: " + b));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new EmailProviderException("Microsoft Graph unavailable")))
        .bodyToMono(Void.class)
        .block();
  }


  /**
   * Internal model representing the result of a token acquisition or refresh operation.
   *
   * <p>Used to deserialize authentication responses returned by the external
   * messaging provider.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class MicrosoftTokenResponse {
    @JsonProperty("access_token") String accessToken;
    @JsonProperty("refresh_token") String refreshToken;
    @JsonProperty("expires_in") Integer expiresIn;
  }

  /**
   * Internal model representing a collection of message resources returned
   * by the external messaging provider.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class MicrosoftMessagesResponse {
    @JsonProperty("value") List<MicrosoftMessage> value;
  }

  /**
   * Internal model representing a message resource as exposed by the
   * external messaging provider.
   *
   * <p>Contains message metadata, participants, content, and attachment
   * information required by the integration layer.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class MicrosoftMessage {
    @JsonProperty("id") String id;
    @JsonProperty("subject") String subject;
    @JsonProperty("from") Recipient from;
    @JsonProperty("toRecipients") List<Recipient> toRecipients;
    @JsonProperty("ccRecipients") List<Recipient> ccRecipients;
    @JsonProperty("receivedDateTime") String receivedDateTime;
    @JsonProperty("isRead") Boolean isRead;
    @JsonProperty("hasAttachments") Boolean hasAttachments;
    @JsonProperty("bodyPreview") String bodyPreview;
    @JsonProperty("body") Body body;
    @JsonProperty("attachments") List<Attachment> attachments;
  }

  /**
   * Internal model representing a message participant.
   *
   * <p>Encapsulates the addressing information associated with a sender
   * or recipient.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Recipient {
    @JsonProperty("emailAddress") EmailAddress emailAddress;
  }

  /**
   * Internal model representing an electronic mail address.
   *
   * <p>Contains the display information associated with a messaging endpoint.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class EmailAddress {
    @JsonProperty("name") String name;
    @JsonProperty("address") String address;
  }

  /**
   * Internal model representing the content of a message.
   *
   * <p>Provides the payload and format information required for message
   * rendering.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Body {
    @JsonProperty("contentType") String contentType;
    @JsonProperty("content") String content;
  }

  /**
   * Internal model representing attachment metadata associated with a message.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Attachment {
    @JsonProperty("name") String name;
    @JsonProperty("contentType") String contentType;
    @JsonProperty("size") Long size;
  }

  /**
   * Exception thrown when an interaction with the external messaging provider
   * cannot be completed successfully.
   *
   * <p>Used to encapsulate provider-specific failures such as authentication,
   * authorization, communication, or resource access errors.
   */
  public static class EmailProviderException extends RuntimeException {
    public EmailProviderException(String message) {
      super(message);
    }
  }
}
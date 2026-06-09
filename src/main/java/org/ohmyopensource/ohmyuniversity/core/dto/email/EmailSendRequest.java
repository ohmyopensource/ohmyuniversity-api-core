package org.ohmyopensource.ohmyuniversity.core.dto.email;

import java.util.List;

/**
 * Request DTO containing the information necessary for message delivery.
 *
 * <p>Encapsulates the content, recipients, and related attributes
 * required to process an outbound communication request.
 */
public class EmailSendRequest {

  private List<String> toAddresses;
  private List<String> ccAddresses;
  private String subject;
  private String textBody;
  private String htmlBody;

  // ============ Getters | Setters | Bool ============

  public List<String> getToAddresses() { return toAddresses; }
  public void setToAddresses(List<String> toAddresses) { this.toAddresses = toAddresses; }

  public List<String> getCcAddresses() { return ccAddresses; }
  public void setCcAddresses(List<String> ccAddresses) { this.ccAddresses = ccAddresses; }

  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }

  public String getTextBody() { return textBody; }
  public void setTextBody(String textBody) { this.textBody = textBody; }

  public String getHtmlBody() { return htmlBody; }
  public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
}
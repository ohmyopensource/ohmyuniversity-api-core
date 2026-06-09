package org.ohmyopensource.ohmyuniversity.core.dto.email;

import java.util.List;

/**
 * Response DTO representing a message resource.
 *
 * <p>Encapsulates the information associated with a single message,
 * including its content, participants, and related metadata.
 */
public class EmailMessageResponse {

  private String id;
  private String subject;
  private String fromName;
  private String fromAddress;
  private List<String> toAddresses;
  private List<String> ccAddresses;
  private String receivedAt;
  private Boolean isRead;
  private String bodyHtml;
  private String bodyText;
  private Boolean hasAttachments;
  private List<AttachmentInfo> attachments;

  // ============ Getters | Setters | Bool ============

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }

  public String getFromName() { return fromName; }
  public void setFromName(String fromName) { this.fromName = fromName; }

  public String getFromAddress() { return fromAddress; }
  public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

  public List<String> getToAddresses() { return toAddresses; }
  public void setToAddresses(List<String> toAddresses) { this.toAddresses = toAddresses; }

  public List<String> getCcAddresses() { return ccAddresses; }
  public void setCcAddresses(List<String> ccAddresses) { this.ccAddresses = ccAddresses; }

  public String getReceivedAt() { return receivedAt; }
  public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }

  public Boolean getIsRead() { return isRead; }
  public void setIsRead(Boolean isRead) { this.isRead = isRead; }

  public String getBodyHtml() { return bodyHtml; }
  public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }

  public String getBodyText() { return bodyText; }
  public void setBodyText(String bodyText) { this.bodyText = bodyText; }

  public Boolean getHasAttachments() { return hasAttachments; }
  public void setHasAttachments(Boolean hasAttachments) { this.hasAttachments = hasAttachments; }

  public List<AttachmentInfo> getAttachments() { return attachments; }
  public void setAttachments(List<AttachmentInfo> attachments) { this.attachments = attachments; }

  /**
   * Metadata for a single email attachment.
   * Content is not included inline — the client downloads attachments separately.
   */
  public static class AttachmentInfo {

    private String name;
    private String contentType;
    private Long sizeBytes;

    // ============ Getters | Setters | Bool ============

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
  }
}
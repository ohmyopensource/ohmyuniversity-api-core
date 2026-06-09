package org.ohmyopensource.ohmyuniversity.core.dto.email;

import java.util.List;

/**
 * DTO representing a collection of mailbox items.
 *
 * <p>Encapsulates the information required to expose a paginated view
 * of message resources available within the institutional messaging
 * service.
 */
public class EmailInboxResponse {

  private List<EmailSummary> messages;
  private Integer totalCount;

  // ============ Getters | Setters | Bool ============

  public List<EmailSummary> getMessages() { return messages; }
  public void setMessages(List<EmailSummary> messages) { this.messages = messages; }

  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

  /**
   * Summary of a single email message shown in the inbox list.
   */
  public static class EmailSummary {

    private String id;
    private String subject;
    private String fromName;
    private String fromAddress;
    private String receivedAt;
    private Boolean isRead;
    private Boolean hasAttachments;
    private String preview;

    // ============ Getters | Setters | Bool ============

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String receivedAt) { this.receivedAt = receivedAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Boolean getHasAttachments() { return hasAttachments; }
    public void setHasAttachments(Boolean hasAttachments) { this.hasAttachments = hasAttachments; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
  }
}
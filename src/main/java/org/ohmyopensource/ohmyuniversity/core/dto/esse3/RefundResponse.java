package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for {@code GET /v1/fees/refunds}.
 *
 * <p>Contains the list of refunds for the authenticated student,
 * mapped from Cineca {@code tasse-service-v1/lista-rimborsi/{persId}}.
 *
 * <p>Note: field names are best-effort based on Cineca YAML spec —
 * verify against real API response when refunds are available.
 */
public class RefundResponse {

  private List<Refund> refunds;

  public List<Refund> getRefunds() {
    return refunds;
  }

  public void setRefunds(List<Refund> refunds) {
    this.refunds = refunds;
  }

  /**
   * Represents a single refund record.
   */
  public static class Refund {

    /** Cineca invoice identifier linked to this refund. */
    private Long fattId;

    /** Description of the fee being refunded. */
    private String feeDes;

    /** Refund amount. */
    private String refundAmount;

    /** Refund date. */
    private String refundDate;

    /** Refund status. */
    private String refundStatus;

    public Long getFattId() { return fattId; }
    public void setFattId(Long fattId) { this.fattId = fattId; }
    public String getFeeDes() { return feeDes; }
    public void setFeeDes(String feeDes) { this.feeDes = feeDes; }
    public String getRefundAmount() { return refundAmount; }
    public void setRefundAmount(String refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundDate() { return refundDate; }
    public void setRefundDate(String refundDate) { this.refundDate = refundDate; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
  }
}
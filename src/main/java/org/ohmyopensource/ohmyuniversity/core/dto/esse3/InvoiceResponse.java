package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for {@code GET /v1/fees/invoices}.
 *
 * <p>Contains the list of invoices issued to the authenticated student,
 * mapped from Cineca {@code tasse-service-v1/lista-fatture}.
 */
public class InvoiceResponse {

  private List<Invoice> invoices;

  public List<Invoice> getInvoices() {
    return invoices;
  }

  public void setInvoices(List<Invoice> invoices) {
    this.invoices = invoices;
  }

  /**
   * Represents a single invoice issued to the student.
   */
  public static class Invoice {

    /** Cineca invoice identifier. */
    private Long fattId;

    /** Academic year (e.g. 2025). */
    private Integer academicYear;

    /** Invoice description (e.g. "Contributo universitario"). */
    private String description;

    /** Invoice amount. */
    private Double amount;

    /** Amount actually paid. */
    private Double paidAmount;

    /** Issue date (dd/MM/yyyy). */
    private String issueDate;

    /** Payment deadline (dd/MM/yyyy). */
    private String deadline;

    /** Payment date (dd/MM/yyyy), or null if not paid. */
    private String paymentDate;

    /** Whether the invoice has been paid (1 = yes, 0 = no). */
    private Integer paidFlg;

    /** Whether the invoice has been cancelled (0 = no). */
    private Integer cancelledFlg;

    /** IUV payment identifier. */
    private String iuv;

    /** PagoPA notice code. */
    private String noticeCode;

    /** PagoPA payment date. */
    private String pagopaPaymentDate;

    /** Collection source (e.g. "PA"). */
    private String collectedFrom;

    /** Whether this invoice was PagoPA enabled. */
    private Integer pagopaAvviso;

    public Long getFattId() { return fattId; }
    public void setFattId(Long fattId) { this.fattId = fattId; }
    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public Integer getPaidFlg() { return paidFlg; }
    public void setPaidFlg(Integer paidFlg) { this.paidFlg = paidFlg; }
    public Integer getCancelledFlg() { return cancelledFlg; }
    public void setCancelledFlg(Integer cancelledFlg) { this.cancelledFlg = cancelledFlg; }
    public String getIuv() { return iuv; }
    public void setIuv(String iuv) { this.iuv = iuv; }
    public String getNoticeCode() { return noticeCode; }
    public void setNoticeCode(String noticeCode) { this.noticeCode = noticeCode; }
    public String getPagopaPaymentDate() { return pagopaPaymentDate; }
    public void setPagopaPaymentDate(String pagopaPaymentDate) {
      this.pagopaPaymentDate = pagopaPaymentDate;
    }
    public String getCollectedFrom() { return collectedFrom; }
    public void setCollectedFrom(String collectedFrom) { this.collectedFrom = collectedFrom; }
    public Integer getPagopaAvviso() { return pagopaAvviso; }
    public void setPagopaAvviso(Integer pagopaAvviso) { this.pagopaAvviso = pagopaAvviso; }
  }
}
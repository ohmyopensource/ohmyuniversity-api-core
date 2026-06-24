package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca ESSE3 fees-related services.
 *
 * <p>Covers {@code tasse-service-v1}:
 * <ul>
 *   <li>{@code /semaforo/{stuId}} — payment status indicator</li>
 *   <li>{@code /addebiti-studente} — detailed charges</li>
 *   <li>{@code /lista-fatture} — issued invoices</li>
 *   <li>{@code /lista-rimborsi/{persId}} — refunds</li>
 *   <li>{@code /pagopa/transazioni} — PagoPA payment history</li>
 *   <li>{@code /parametri-iscrizioni-per-tasse/{stuId}} — enrollment fee parameters</li>
 * </ul>
 *
 * <p>All endpoints are accessible with STUDENTE role via Bearer JWT.
 */
@Component
public class CinecaFeesClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaFeesClient.class);

  private static final String TASSE_BASE = "/tasse-service-v1";

  // ============ Class Methods ============

  /**
   * Retrieves the payment status indicator (semaforo) for a student.
   *
   * <p>The semaforo reflects the student's overall fee standing
   * (e.g. regular, warning, blocked) as configured by the university.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return semaforo status, or {@code null} if not available
   */
  public CinecaFeeStatus getFeeStatus(String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaFeesClient: GET fee status stuId={}", stuId);
    return webClient.get()
        .uri(baseUrl + TASSE_BASE + "/semaforo/" + stuId)
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for fee status stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on fee status")))
        .bodyToMono(CinecaFeeStatus.class)
        .block();
  }

  /**
   * Retrieves the detailed list of charges (addebiti) for a student.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return list of charges; empty if none available
   */
  public List<CinecaCharge> getCharges(String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaFeesClient: GET charges stuId={}", stuId);
    List<CinecaCharge> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "tasse-service-v1", "addebiti-studente")
            .queryParam("stuId", stuId)
            .queryParam("limit", 100)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for charges stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on charges")))
        .bodyToFlux(CinecaCharge.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves issued invoices (fatture) for a student.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return list of invoices; empty if none available
   */
  public List<CinecaInvoice> getInvoices(String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaFeesClient: GET invoices stuId={}", stuId);
    List<CinecaInvoice> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "tasse-service-v1", "lista-fatture")
            .queryParam("stuId", stuId)
            .queryParam("limit", 100)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for invoices stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on invoices")))
        .bodyToFlux(CinecaInvoice.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves refunds (rimborsi) for a person.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param persId  Cineca person identifier
   * @return list of refunds; empty if none available
   */
  public List<CinecaRefund> getRefunds(String baseUrl, String jwt, Long persId) {
    log.debug("CinecaFeesClient: GET refunds persId={}", persId);
    List<CinecaRefund> result = webClient.get()
        .uri(baseUrl + TASSE_BASE + "/lista-rimborsi/" + persId)
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for refunds persId=" + persId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on refunds")))
        .bodyToFlux(CinecaRefund.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves PagoPA payment transactions for a student.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return list of PagoPA transactions; empty if none available
   */
  public List<CinecaPayment> getPayments(String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaFeesClient: GET payments stuId={}", stuId);
    List<CinecaPayment> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "tasse-service-v1", "pagopa", "transazioni")
            .queryParam("stuId", stuId)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for payments stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on payments")))
        .bodyToFlux(CinecaPayment.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  // ============ DTOs ============

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaFeeStatus {

    @JsonProperty("semaforo")
    private String status;
    @JsonProperty("importoDovuto")
    private String amountDue;
    @JsonProperty("tasseScadute")
    private List<CinecaFeeItem> overdueItems;
    @JsonProperty("tasseDovute")
    private List<CinecaFeeItem> dueItems;

    public String getStatus() {
      return status;
    }

    public String getAmountDue() {
      return amountDue;
    }

    public List<CinecaFeeItem> getOverdueItems() {
      return overdueItems != null ? overdueItems : List.of();
    }

    public List<CinecaFeeItem> getDueItems() {
      return dueItems != null ? dueItems : List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaFeeItem {

    @JsonProperty("fattId")
    private Long invoiceId;
    @JsonProperty("tassaId")
    private Long feeId;
    @JsonProperty("tassaCod")
    private String feeCod;
    @JsonProperty("tassaDes")
    private String feeDes;
    @JsonProperty("voceId")
    private Long itemId;
    @JsonProperty("voceCod")
    private String itemCod;
    @JsonProperty("voceDes")
    private String itemDes;
    @JsonProperty("importoVoce")
    private String amount;
    @JsonProperty("dataScadenza")
    private String dueDate;
    @JsonProperty("dataPagTollerataMax")
    private String toleratedDueDate;

    public Long getInvoiceId() {
      return invoiceId;
    }

    public Long getFeeId() {
      return feeId;
    }

    public String getFeeCod() {
      return feeCod;
    }

    public String getFeeDes() {
      return feeDes;
    }

    public Long getItemId() {
      return itemId;
    }

    public String getItemCod() {
      return itemCod;
    }

    public String getItemDes() {
      return itemDes;
    }

    public String getAmount() {
      return amount;
    }

    public String getDueDate() {
      return dueDate;
    }

    public String getToleratedDueDate() {
      return toleratedDueDate;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCharge {

    @JsonProperty("aaId")
    private Long academicYear;
    @JsonProperty("tassaDes")
    private String feeDes;
    @JsonProperty("tassaCod")
    private String feeCod;
    @JsonProperty("tipoTaxCod")
    private String feeTypeCod;
    @JsonProperty("voceDes")
    private String itemDes;
    @JsonProperty("importoVoce")
    private Double amount;
    @JsonProperty("scadenzaAddebito")
    private String chargeDeadline;
    @JsonProperty("scadutoFlg")
    private Integer overdueFlg;
    @JsonProperty("fattId")
    private Long invoiceId;
    @JsonProperty("scadFattura")
    private String invoiceDeadline;
    @JsonProperty("fattScadutaFlg")
    private Integer invoiceOverdueFlg;
    @JsonProperty("importoFattura")
    private String invoiceAmount;
    @JsonProperty("dataEmissione")
    private String issueDate;
    @JsonProperty("pagatoFlg")
    private Integer paidFlg;
    @JsonProperty("dataPagamento")
    private String paymentDate;
    @JsonProperty("importoPag")
    private Double paidAmount;
    @JsonProperty("annullataFlg")
    private Integer cancelledFlg;
    @JsonProperty("rataDes")
    private String installmentDes;
    @JsonProperty("iuv")
    private String iuv;
    @JsonProperty("codiceAvviso")
    private String noticeCode;

    public Long getAcademicYear() {
      return academicYear;
    }

    public String getFeeDes() {
      return feeDes;
    }

    public String getFeeCod() {
      return feeCod;
    }

    public String getFeeTypeCod() {
      return feeTypeCod;
    }

    public String getItemDes() {
      return itemDes;
    }

    public Double getAmount() {
      return amount;
    }

    public String getChargeDeadline() {
      return chargeDeadline;
    }

    public Integer getOverdueFlg() {
      return overdueFlg;
    }

    public Long getInvoiceId() {
      return invoiceId;
    }

    public String getInvoiceDeadline() {
      return invoiceDeadline;
    }

    public Integer getInvoiceOverdueFlg() {
      return invoiceOverdueFlg;
    }

    public String getInvoiceAmount() {
      return invoiceAmount;
    }

    public String getIssueDate() {
      return issueDate;
    }

    public Integer getPaidFlg() {
      return paidFlg;
    }

    public String getPaymentDate() {
      return paymentDate;
    }

    public Double getPaidAmount() {
      return paidAmount;
    }

    public Integer getCancelledFlg() {
      return cancelledFlg;
    }

    public String getInstallmentDes() {
      return installmentDes;
    }

    public String getIuv() {
      return iuv;
    }

    public String getNoticeCode() {
      return noticeCode;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaInvoice {

    @JsonProperty("fattId")
    private Long invoiceId;
    @JsonProperty("aaId")
    private Integer academicYear;
    @JsonProperty("tassaDes")
    private String feeDes;
    @JsonProperty("importoFattura")
    private String amount;
    @JsonProperty("dataEmissione")
    private String issueDate;
    @JsonProperty("scadFattura")
    private String deadline;
    @JsonProperty("pagatoFlg")
    private Integer paidFlg;
    @JsonProperty("dataPagamento")
    private String paymentDate;
    @JsonProperty("annullataFlg")
    private Integer cancelledFlg;
    @JsonProperty("iuv")
    private String iuv;
    @JsonProperty("codiceAvviso")
    private String noticeCode;

    public Long getInvoiceId() {
      return invoiceId;
    }

    public Integer getAcademicYear() {
      return academicYear;
    }

    public String getFeeDes() {
      return feeDes;
    }

    public String getAmount() {
      return amount;
    }

    public String getIssueDate() {
      return issueDate;
    }

    public String getDeadline() {
      return deadline;
    }

    public Integer getPaidFlg() {
      return paidFlg;
    }

    public String getPaymentDate() {
      return paymentDate;
    }

    public Integer getCancelledFlg() {
      return cancelledFlg;
    }

    public String getIuv() {
      return iuv;
    }

    public String getNoticeCode() {
      return noticeCode;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaRefund {

    @JsonProperty("fattId")
    private Long invoiceId;
    @JsonProperty("tassaDes")
    private String feeDes;
    @JsonProperty("importoRimborso")
    private String refundAmount;
    @JsonProperty("dataRimborso")
    private String refundDate;
    @JsonProperty("statoRimborso")
    private String refundStatus;

    public Long getInvoiceId() {
      return invoiceId;
    }

    public String getFeeDes() {
      return feeDes;
    }

    public String getRefundAmount() {
      return refundAmount;
    }

    public String getRefundDate() {
      return refundDate;
    }

    public String getRefundStatus() {
      return refundStatus;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPayment {

    @JsonProperty("transId")
    private String transactionId;
    @JsonProperty("cartStatusCod")
    private String cartStatus;
    @JsonProperty("cartStatusDes")
    private String cartStatusDes;
    @JsonProperty("importoPagato")
    private Double paidAmount;
    @JsonProperty("dataPagamento")
    private String paymentDate;
    @JsonProperty("esisteIncasso")
    private Integer hasCollection;
    @JsonProperty("incassoDaGestire")
    private Integer collectionPending;
    @JsonProperty("codiceContestoPagamento")
    private String paymentContext;
    @JsonProperty("aaId")
    private Integer academicYear;
    @JsonProperty("pagatoFlg")
    private Integer paidFlg;
    @JsonProperty("quietanzaStampabile")
    private Integer receiptPrintable;

    public String getTransactionId() {
      return transactionId;
    }

    public String getCartStatus() {
      return cartStatus;
    }

    public String getCartStatusDes() {
      return cartStatusDes;
    }

    public Double getPaidAmount() {
      return paidAmount;
    }

    public String getPaymentDate() {
      return paymentDate;
    }

    public Integer getHasCollection() {
      return hasCollection;
    }

    public Integer getCollectionPending() {
      return collectionPending;
    }

    public String getPaymentContext() {
      return paymentContext;
    }

    public Integer getAcademicYear() {
      return academicYear;
    }

    public Integer getPaidFlg() {
      return paidFlg;
    }

    public Integer getReceiptPrintable() {
      return receiptPrintable;
    }
  }
}
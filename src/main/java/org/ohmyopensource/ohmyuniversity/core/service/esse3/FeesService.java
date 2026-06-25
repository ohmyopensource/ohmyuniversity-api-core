package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaCharge;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaFeeItem;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaFeeStatus;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaRefund;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.FeeStatusResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.FeeStatusResponse.Addebito;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.FeeStatusResponse.VoceTassa;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InvoiceResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InvoiceResponse.Invoice;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.RefundResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.RefundResponse.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for fee-related operations against Cineca ESSE3 tasse-service-v1.
 */
@Service
public class FeesService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(FeesService.class);

  private final CinecaFeesClient feesClient;

  public FeesService(
      CinecaFeesClient feesClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.feesClient = feesClient;
  }

  /**
   * Retrieves the aggregated fee status for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return aggregated fee status and charges
   */
  public FeeStatusResponse getStatus(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaFeeStatus status = feesClient.getFeeStatus(baseUrl, jwt, principal.stuId());
    List<CinecaCharge> charges = feesClient.getCharges(baseUrl, jwt, principal.stuId());

    log.debug("FeesService: fee status={} charges={} for stuId={}",
        status != null ? status.getStatus() : "null", charges.size(), principal.stuId());

    FeeStatusResponse response = new FeeStatusResponse();
    if (status != null) {
      response.setSemaforo(status.getStatus());
      response.setImportoDovuto(status.getAmountDue());
      response.setTasseScadute(status.getOverdueItems().stream().map(this::toVoceTassa).toList());
      response.setTasseDovute(status.getDueItems().stream().map(this::toVoceTassa).toList());
    }
    response.setAddebiti(charges.stream().map(this::toAddebito).toList());
    return response;
  }

  /**
   * Retrieves issued invoices for the authenticated student.
   *
   * <p>Requires both stuId (from JWT) and persId (from Redis session).
   *
   * @param principal authenticated OhMyU principal
   * @return invoice response
   */
  public InvoiceResponse getInvoices(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    Long persId = resolvePersId(principal);

    var invoices = feesClient.getInvoices(baseUrl, jwt, principal.stuId(), persId);
    log.debug("FeesService: fetched {} invoices for stuId={}", invoices.size(), principal.stuId());

    InvoiceResponse response = new InvoiceResponse();
    response.setInvoices(invoices.stream().map(this::toInvoice).toList());
    return response;
  }

  /**
   * Retrieves refunds for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return refund response
   */
  public RefundResponse getRefunds(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    Long persId = resolvePersId(principal);

    List<CinecaRefund> refunds = feesClient.getRefunds(baseUrl, jwt, persId);
    log.debug("FeesService: fetched {} refunds for persId={}", refunds.size(), persId);

    RefundResponse response = new RefundResponse();
    response.setRefunds(refunds.stream().map(this::toRefund).toList());
    return response;
  }

  // ============ Mappers ============

  private VoceTassa toVoceTassa(CinecaFeeItem item) {
    VoceTassa v = new VoceTassa();
    v.setFattId(item.getInvoiceId());
    v.setTassaId(item.getFeeId());
    v.setTassaCod(item.getFeeCod());
    v.setTassaDes(item.getFeeDes());
    v.setVoceId(item.getItemId());
    v.setVoceCod(item.getItemCod());
    v.setVoceDes(item.getItemDes());
    v.setImportoVoce(item.getAmount());
    v.setDataScadenza(item.getDueDate());
    v.setDataPagTollerataMax(item.getToleratedDueDate());
    return v;
  }

  private Addebito toAddebito(CinecaCharge c) {
    Addebito a = new Addebito();
    a.setAaId(c.getAcademicYear());
    a.setTassaDes(c.getFeeDes());
    a.setTassaCod(c.getFeeCod());
    a.setTipoTaxCod(c.getFeeTypeCod());
    a.setVoceDes(c.getItemDes());
    a.setImportoVoce(c.getAmount());
    a.setScadenzaAddebito(c.getChargeDeadline());
    a.setScadutoFlg(c.getOverdueFlg());
    a.setFattId(c.getInvoiceId());
    a.setScadFattura(c.getInvoiceDeadline());
    a.setFattScadutaFlg(c.getInvoiceOverdueFlg());
    a.setImportoFattura(c.getInvoiceAmount());
    a.setDataEmissione(c.getIssueDate());
    a.setPagatoFlg(c.getPaidFlg());
    a.setDataPagamento(c.getPaymentDate());
    a.setImportoPag(c.getPaidAmount());
    a.setAnnullataFlg(c.getCancelledFlg());
    a.setRataDes(c.getInstallmentDes());
    a.setIuv(c.getIuv());
    a.setCodiceAvviso(c.getNoticeCode());
    return a;
  }

  private Invoice toInvoice(CinecaFeesClient.CinecaInvoice c) {
    Invoice i = new Invoice();
    i.setFattId(c.getFattId());
    i.setAcademicYear(c.getAcademicYear());
    i.setDescription(c.getDescription());
    i.setAmount(c.getAmount());
    i.setPaidAmount(c.getPaidAmount());
    i.setIssueDate(c.getIssueDate());
    i.setDeadline(c.getDeadline());
    i.setPaymentDate(c.getPaymentDate());
    i.setPagopaPaymentDate(c.getPagopaPaymentDate());
    i.setPaidFlg(c.getPaidFlg());
    i.setCancelledFlg(c.getCancelledFlg());
    i.setIuv(c.getIuv());
    i.setNoticeCode(c.getNoticeCode());
    i.setCollectedFrom(c.getCollectedFrom());
    i.setPagopaAvviso(c.getPagopaAvviso());
    return i;
  }

  private Refund toRefund(CinecaRefund c) {
    Refund r = new Refund();
    r.setFattId(c.getInvoiceId());
    r.setFeeDes(c.getFeeDes());
    r.setRefundAmount(c.getRefundAmount());
    r.setRefundDate(c.getRefundDate());
    r.setRefundStatus(c.getRefundStatus());
    return r;
  }
}
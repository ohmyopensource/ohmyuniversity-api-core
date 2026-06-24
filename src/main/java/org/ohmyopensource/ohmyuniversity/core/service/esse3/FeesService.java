package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaCharge;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaFeeItem;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaFeeStatus;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaInvoice;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaPayment;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaRefund;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse.Addebito;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse.VoceTassa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for fee-related operations against Cineca ESSE3 tasse-service-v1.
 *
 * <p>Covers:
 * <ul>
 *   <li>fee status (semaforo) — overall payment standing indicator</li>
 *   <li>charges (addebiti) — detailed billing entries</li>
 *   <li>invoices (fatture) — issued invoices</li>
 *   <li>refunds (rimborsi) — refund records</li>
 *   <li>payments (pagamenti) — PagoPA transaction history</li>
 * </ul>
 *
 * <p>All data is fetched in real-time from Cineca; nothing is persisted locally.
 *
 * <p>Note: {@code getStatus} aggregates semaforo + charges in a single response
 * to preserve backward compatibility with the existing {@link TasseResponse} DTO.
 * Individual endpoints (invoices, refunds, payments) return their own dedicated responses
 * once the corresponding DTOs are introduced.
 */
@Service
public class FeesService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(FeesService.class);

  private final CinecaFeesClient feesClient;

  // ============ Constructor ============

  public FeesService(
      CinecaFeesClient feesClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.feesClient = feesClient;
  }

  // ============ Public Methods ============

  /**
   * Retrieves the aggregated fee status for the authenticated student.
   *
   * <p>Combines the semaforo indicator with the full list of charges
   * into a single {@link TasseResponse}, preserving backward compatibility.
   *
   * @param principal authenticated OhMyU principal
   * @return aggregated fee status and charges
   */
  public TasseResponse getStatus(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaFeeStatus status = feesClient.getFeeStatus(baseUrl, jwt, principal.stuId());
    List<CinecaCharge> charges = feesClient.getCharges(baseUrl, jwt, principal.stuId());

    log.debug("FeesService: fee status={} charges={} for stuId={}",
        status != null ? status.getStatus() : "null", charges.size(), principal.stuId());

    TasseResponse response = new TasseResponse();
    if (status != null) {
      response.setSemaforo(status.getStatus());
      response.setImportoDovuto(status.getAmountDue());
      response.setTasseScadute(status.getOverdueItems().stream()
          .map(this::toVoceTassa).toList());
      response.setTasseDovute(status.getDueItems().stream()
          .map(this::toVoceTassa).toList());
    }
    response.setAddebiti(charges.stream().map(this::toAddebito).toList());
    return response;
  }

  /**
   * Retrieves the list of issued invoices for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return list of Cineca invoices (raw — dedicated DTO to be introduced)
   */
  public List<CinecaInvoice> getInvoices(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaInvoice> invoices = feesClient.getInvoices(baseUrl, jwt, principal.stuId());
    log.debug("FeesService: fetched {} invoices for stuId={}", invoices.size(), principal.stuId());
    return invoices;
  }

  /**
   * Retrieves the list of refunds for the authenticated student.
   *
   * <p>Uses persId — resolved from Redis session.
   *
   * @param principal authenticated OhMyU principal
   * @return list of Cineca refunds (raw — dedicated DTO to be introduced)
   */
  public List<CinecaRefund> getRefunds(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    Long persId = resolvePersId(principal);

    List<CinecaRefund> refunds = feesClient.getRefunds(baseUrl, jwt, persId);
    log.debug("FeesService: fetched {} refunds for persId={}", refunds.size(), persId);
    return refunds;
  }

  /**
   * Retrieves PagoPA payment transactions for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return list of Cineca payments (raw — dedicated DTO to be introduced)
   */
  public List<CinecaPayment> getPayments(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaPayment> payments = feesClient.getPayments(baseUrl, jwt, principal.stuId());
    log.debug("FeesService: fetched {} payments for stuId={}", payments.size(), principal.stuId());
    return payments;
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
}
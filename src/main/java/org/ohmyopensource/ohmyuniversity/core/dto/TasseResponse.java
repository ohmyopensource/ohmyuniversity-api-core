package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/tasse.
 *
 * <p>Aggregates tuition fee information retrieved from Cineca tasse-service-v1:
 * - semaforo: overall payment status indicator
 * - importoDovuto: total amount due
 * - tasseScadute: overdue fee items contributing to the semaforo state
 * - tasseDovute: pending (not yet overdue) fee items
 * - addebiti: complete list of billing entries for the student
 */
public class TasseResponse {

  /**
   * Fee status indicator returned by Cineca (e.g., VERDE, GIALLO, ROSSO).
   */
  private String semaforo;

  /**
   * Total amount due as provided by Cineca (formatted string).
   */
  private String importoDovuto;

  /**
   * List of overdue fee items.
   */
  private List<VoceTassa> tasseScadute;

  /**
   * List of non-overdue fee items still to be paid.
   */
  private List<VoceTassa> tasseDovute;

  /**
   * Full list of billing entries (charges) for the student.
   */
  private List<Addebito> addebiti;

  // ============ Getters | Setters | Bool ============

  public String getSemaforo() {
    return semaforo;
  }

  public void setSemaforo(String semaforo) {
    this.semaforo = semaforo;
  }

  public String getImportoDovuto() {
    return importoDovuto;
  }

  public void setImportoDovuto(String importoDovuto) {
    this.importoDovuto = importoDovuto;
  }

  public List<VoceTassa> getTasseScadute() {
    return tasseScadute;
  }

  public void setTasseScadute(List<VoceTassa> tasseScadute) {
    this.tasseScadute = tasseScadute;
  }

  public List<VoceTassa> getTasseDovute() {
    return tasseDovute;
  }

  public void setTasseDovute(List<VoceTassa> tasseDovute) {
    this.tasseDovute = tasseDovute;
  }

  public List<Addebito> getAddebiti() {
    return addebiti;
  }

  public void setAddebiti(List<Addebito> addebiti) {
    this.addebiti = addebiti;
  }

  /**
   * Represents a single fee item returned by the semaforo endpoint. Can be either overdue or still
   * payable.
   */
  public static class VoceTassa {

    private Long fattId;
    private Long tassaId;
    private String tassaCod;
    private String tassaDes;
    private Long voceId;
    private String voceCod;
    private String voceDes;
    private String importoVoce;
    private String dataScadenza;
    private String dataPagTollerataMax;

    // ============ Getters | Setters | Bool ============

    public Long getFattId() {
      return fattId;
    }

    public void setFattId(Long fattId) {
      this.fattId = fattId;
    }

    public Long getTassaId() {
      return tassaId;
    }

    public void setTassaId(Long tassaId) {
      this.tassaId = tassaId;
    }

    public String getTassaCod() {
      return tassaCod;
    }

    public void setTassaCod(String tassaCod) {
      this.tassaCod = tassaCod;
    }

    public String getTassaDes() {
      return tassaDes;
    }

    public void setTassaDes(String tassaDes) {
      this.tassaDes = tassaDes;
    }

    public Long getVoceId() {
      return voceId;
    }

    public void setVoceId(Long voceId) {
      this.voceId = voceId;
    }

    public String getVoceCod() {
      return voceCod;
    }

    public void setVoceCod(String voceCod) {
      this.voceCod = voceCod;
    }

    public String getVoceDes() {
      return voceDes;
    }

    public void setVoceDes(String voceDes) {
      this.voceDes = voceDes;
    }

    public String getImportoVoce() {
      return importoVoce;
    }

    public void setImportoVoce(String importoVoce) {
      this.importoVoce = importoVoce;
    }

    public String getDataScadenza() {
      return dataScadenza;
    }

    public void setDataScadenza(String dataScadenza) {
      this.dataScadenza = dataScadenza;
    }

    public String getDataPagTollerataMax() {
      return dataPagTollerataMax;
    }

    public void setDataPagTollerataMax(String dataPagTollerataMax) {
      this.dataPagTollerataMax = dataPagTollerataMax;
    }
  }

  /**
   * Represents a single billing entry (addebito) from /addebiti-studente.
   */
  public static class Addebito {

    private Long aaId;
    private String tassaDes;
    private String tassaCod;
    private String tipoTaxCod;
    private String voceDes;
    private Double importoVoce;
    private String scadenzaAddebito;
    private Integer scadutoFlg;
    private Long fattId;
    private String scadFattura;
    private Integer fattScadutaFlg;
    private String importoFattura;
    private String dataEmissione;
    private Integer pagatoFlg;
    private String dataPagamento;
    private Double importoPag;
    private Integer annullataFlg;
    private String rataDes;
    private String iuv;
    private String codiceAvviso;

    // ============ Getters | Setters | Bool ============

    public Long getAaId() {
      return aaId;
    }

    public void setAaId(Long aaId) {
      this.aaId = aaId;
    }

    public String getTassaDes() {
      return tassaDes;
    }

    public void setTassaDes(String tassaDes) {
      this.tassaDes = tassaDes;
    }

    public String getTassaCod() {
      return tassaCod;
    }

    public void setTassaCod(String tassaCod) {
      this.tassaCod = tassaCod;
    }

    public String getTipoTaxCod() {
      return tipoTaxCod;
    }

    public void setTipoTaxCod(String tipoTaxCod) {
      this.tipoTaxCod = tipoTaxCod;
    }

    public String getVoceDes() {
      return voceDes;
    }

    public void setVoceDes(String voceDes) {
      this.voceDes = voceDes;
    }

    public Double getImportoVoce() {
      return importoVoce;
    }

    public void setImportoVoce(Double importoVoce) {
      this.importoVoce = importoVoce;
    }

    public String getScadenzaAddebito() {
      return scadenzaAddebito;
    }

    public void setScadenzaAddebito(String scadenzaAddebito) {
      this.scadenzaAddebito = scadenzaAddebito;
    }

    public Integer getScadutoFlg() {
      return scadutoFlg;
    }

    public void setScadutoFlg(Integer scadutoFlg) {
      this.scadutoFlg = scadutoFlg;
    }

    public Long getFattId() {
      return fattId;
    }

    public void setFattId(Long fattId) {
      this.fattId = fattId;
    }

    public String getScadFattura() {
      return scadFattura;
    }

    public void setScadFattura(String scadFattura) {
      this.scadFattura = scadFattura;
    }

    public Integer getFattScadutaFlg() {
      return fattScadutaFlg;
    }

    public void setFattScadutaFlg(Integer fattScadutaFlg) {
      this.fattScadutaFlg = fattScadutaFlg;
    }

    public String getImportoFattura() {
      return importoFattura;
    }

    public void setImportoFattura(String importoFattura) {
      this.importoFattura = importoFattura;
    }

    public String getDataEmissione() {
      return dataEmissione;
    }

    public void setDataEmissione(String dataEmissione) {
      this.dataEmissione = dataEmissione;
    }

    public Integer getPagatoFlg() {
      return pagatoFlg;
    }

    public void setPagatoFlg(Integer pagatoFlg) {
      this.pagatoFlg = pagatoFlg;
    }

    public String getDataPagamento() {
      return dataPagamento;
    }

    public void setDataPagamento(String dataPagamento) {
      this.dataPagamento = dataPagamento;
    }

    public Double getImportoPag() {
      return importoPag;
    }

    public void setImportoPag(Double importoPag) {
      this.importoPag = importoPag;
    }

    public Integer getAnnullataFlg() {
      return annullataFlg;
    }

    public void setAnnullataFlg(Integer annullataFlg) {
      this.annullataFlg = annullataFlg;
    }

    public String getRataDes() {
      return rataDes;
    }

    public void setRataDes(String rataDes) {
      this.rataDes = rataDes;
    }

    public String getIuv() {
      return iuv;
    }

    public void setIuv(String iuv) {
      this.iuv = iuv;
    }

    public String getCodiceAvviso() {
      return codiceAvviso;
    }

    public void setCodiceAvviso(String codiceAvviso) {
      this.codiceAvviso = codiceAvviso;
    }
  }
}
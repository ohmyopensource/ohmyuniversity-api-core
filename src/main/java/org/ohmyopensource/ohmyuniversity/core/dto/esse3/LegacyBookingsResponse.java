package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/prenotazioni.
 *
 * <p>This object represents the full exam booking history for a student, including both past and
 * future exam sessions.
 *
 * <p>Data is retrieved from Cineca calesa-service-v1 and includes booking metadata, exam results,
 * and grading outcomes when available.
 *
 * <p>Each entry represents a single exam booking linked to the student's academic career and
 * transcript.
 */
public class LegacyBookingsResponse {

  private List<Prenotazione> prenotazioni;

  // ============ Getters | Setters | Bool ============

  public List<Prenotazione> getPrenotazioni() {
    return prenotazioni;
  }

  public void setPrenotazioni(List<Prenotazione> prenotazioni) {
    this.prenotazioni = prenotazioni;
  }

  /**
   * Represents a single exam booking entry.
   *
   * <p>Each record contains the booking metadata and, if available, the exam outcome information.
   */
  public static class Prenotazione {

    /**
     * Unique booking ID (applistaId from Cineca).
     */
    private Long applistaId;

    /**
     * Teaching activity numeric ID (adId from Cineca).
     */
    private Long adId;

    /**
     * Teaching activity code (e.g. "411114").
     */
    private String adStuCod;

    /**
     * Teaching activity description (e.g. "Architettura degli elaboratori").
     */
    private String adStuDes;

    /**
     * Transcript row ID (adsceId) — links to libretto.
     */
    private Long adsceId;

    /**
     * Degree course ID.
     */
    private Long cdsId;

    /**
     * Progressive exam ID within (cdsId, adId).
     */
    private Long appId;

    /**
     * Turn progressive ID within the exam.
     */
    private Long appLogId;

    /**
     * Student career track ID.
     */
    private Long stuId;

    /**
     * Booking date (DD/MM/YYYY HH:MM:SS).
     */
    private String dataIns;

    /**
     * Exam date if set (DD/MM/YYYY).
     */
    private String dataEsa;

    /**
     * Last rejection date for the student.
     */
    private String dataRifEsitoStu;

    /**
     * CFU weight of the activity.
     */
    private Double pesoAd;

    /**
     * Student's position in the enrolled list ordered by booking date.
     */
    private Integer posizApp;

    /**
     * Grade outcome.
     */
    private EsitoPrenotazione esito;

    /**
     * Result acknowledgement status: N=not seen, V=seen, R=rejected, A=accepted.
     */
    private String presaVisione;

    /**
     * Teaching activity status in libretto: S=superata, F=frequentata.
     */
    private String statoAdsce;

    /**
     * Whether the result has been published (pubblId != null).
     */
    private Boolean esitoPubblicato;

    /**
     * Exam questions text, if available.
     */
    private String domandeEsame;

    // ============ Getters | Setters | Bool ============

    public Long getApplistaId() {
      return applistaId;
    }

    public void setApplistaId(Long applistaId) {
      this.applistaId = applistaId;
    }

    public Long getAdId() {
      return adId;
    }

    public void setAdId(Long adId) {
      this.adId = adId;
    }

    public String getAdStuCod() {
      return adStuCod;
    }

    public void setAdStuCod(String adStuCod) {
      this.adStuCod = adStuCod;
    }

    public String getAdStuDes() {
      return adStuDes;
    }

    public void setAdStuDes(String adStuDes) {
      this.adStuDes = adStuDes;
    }

    public Long getAdsceId() {
      return adsceId;
    }

    public void setAdsceId(Long adsceId) {
      this.adsceId = adsceId;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public void setCdsId(Long cdsId) {
      this.cdsId = cdsId;
    }

    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long appId) {
      this.appId = appId;
    }

    public Long getAppLogId() {
      return appLogId;
    }

    public void setAppLogId(Long appLogId) {
      this.appLogId = appLogId;
    }

    public Long getStuId() {
      return stuId;
    }

    public void setStuId(Long stuId) {
      this.stuId = stuId;
    }

    public String getDataIns() {
      return dataIns;
    }

    public void setDataIns(String dataIns) {
      this.dataIns = dataIns;
    }

    public String getDataEsa() {
      return dataEsa;
    }

    public void setDataEsa(String dataEsa) {
      this.dataEsa = dataEsa;
    }

    public String getDataRifEsitoStu() {
      return dataRifEsitoStu;
    }

    public void setDataRifEsitoStu(String dataRifEsitoStu) {
      this.dataRifEsitoStu = dataRifEsitoStu;
    }

    public Double getPesoAd() {
      return pesoAd;
    }

    public void setPesoAd(Double pesoAd) {
      this.pesoAd = pesoAd;
    }

    public Integer getPosizApp() {
      return posizApp;
    }

    public void setPosizApp(Integer posizApp) {
      this.posizApp = posizApp;
    }

    public EsitoPrenotazione getEsito() {
      return esito;
    }

    public void setEsito(EsitoPrenotazione esito) {
      this.esito = esito;
    }

    public String getPresaVisione() {
      return presaVisione;
    }

    public void setPresaVisione(String presaVisione) {
      this.presaVisione = presaVisione;
    }

    public String getStatoAdsce() {
      return statoAdsce;
    }

    public void setStatoAdsce(String statoAdsce) {
      this.statoAdsce = statoAdsce;
    }

    public Boolean getEsitoPubblicato() {
      return esitoPubblicato;
    }

    public void setEsitoPubblicato(Boolean esitoPubblicato) {
      this.esitoPubblicato = esitoPubblicato;
    }

    public String getDomandeEsame() {
      return domandeEsame;
    }

    public void setDomandeEsame(String domandeEsame) {
      this.domandeEsame = domandeEsame;
    }
  }

  /**
   * Represents the outcome of an exam booking.
   */
  public static class EsitoPrenotazione {

    private Integer votoEsa;
    private Boolean superato;
    private Boolean assente;
    private Boolean ritirato;
    private String tipoGiudCod;
    private String tipoGiudizioDes;

    // ============ Getters | Setters | Bool ============

    public Integer getVotoEsa() {
      return votoEsa;
    }

    public void setVotoEsa(Integer votoEsa) {
      this.votoEsa = votoEsa;
    }

    public Boolean getSuperato() {
      return superato;
    }

    public void setSuperato(Boolean superato) {
      this.superato = superato;
    }

    public Boolean getAssente() {
      return assente;
    }

    public void setAssente(Boolean assente) {
      this.assente = assente;
    }

    public Boolean getRitirato() {
      return ritirato;
    }

    public void setRitirato(Boolean ritirato) {
      this.ritirato = ritirato;
    }

    public String getTipoGiudCod() {
      return tipoGiudCod;
    }

    public void setTipoGiudCod(String tipoGiudCod) {
      this.tipoGiudCod = tipoGiudCod;
    }

    public String getTipoGiudizioDes() {
      return tipoGiudizioDes;
    }

    public void setTipoGiudizioDes(String tipoGiudizioDes) {
      this.tipoGiudizioDes = tipoGiudizioDes;
    }
  }
}
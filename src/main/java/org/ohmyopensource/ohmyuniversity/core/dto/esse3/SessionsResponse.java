package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/appelli.
 *
 * <p>This DTO represents a collection of available exam sessions retrieved from Cineca
 * calesa-service-v1.
 *
 * <p>It is designed as an API contract object and is independent of persistence and external
 * service models to ensure stability of the internal API layer.
 */
public class SessionsResponse {

  private List<Appello> appelli;

  // ============ Getters | Setters | Bool ============

  public List<Appello> getAppelli() {
    return appelli;
  }

  public void setAppelli(List<Appello> appelli) {
    this.appelli = appelli;
  }

  /**
   * Represents a single exam session (appello) available for booking.
   *
   * <p>This object maps Cineca calesa-service-v1 exam session data into a stable internal API
   * representation.
   */
  public static class Appello {

    private Long appId;
    private String adCod;
    private String adDes;
    private String dataInizio;
    private String dataFine;
    private String dataScadPrenotazione;
    private String tipo;
    private String tipoDes;
    private String docente;
    private String aula;
    private Boolean prenotabile;
    private Boolean prenotato;
    private Long prenotazioneId;

    // ============ Getters | Setters | Bool ============

    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long appId) {
      this.appId = appId;
    }

    public String getAdCod() {
      return adCod;
    }

    public void setAdCod(String adCod) {
      this.adCod = adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public void setAdDes(String adDes) {
      this.adDes = adDes;
    }

    public String getDataInizio() {
      return dataInizio;
    }

    public void setDataInizio(String dataInizio) {
      this.dataInizio = dataInizio;
    }

    public String getDataFine() {
      return dataFine;
    }

    public void setDataFine(String dataFine) {
      this.dataFine = dataFine;
    }

    public String getDataScadPrenotazione() {
      return dataScadPrenotazione;
    }

    public void setDataScadPrenotazione(String d) {
      this.dataScadPrenotazione = d;
    }

    public String getTipo() {
      return tipo;
    }

    public void setTipo(String tipo) {
      this.tipo = tipo;
    }

    public String getTipoDes() {
      return tipoDes;
    }

    public void setTipoDes(String tipoDes) {
      this.tipoDes = tipoDes;
    }

    public String getDocente() {
      return docente;
    }

    public void setDocente(String docente) {
      this.docente = docente;
    }

    public String getAula() {
      return aula;
    }

    public void setAula(String aula) {
      this.aula = aula;
    }

    public Boolean getPrenotabile() {
      return prenotabile;
    }

    public void setPrenotabile(Boolean prenotabile) {
      this.prenotabile = prenotabile;
    }

    public Boolean getPrenotato() {
      return prenotato;
    }

    public void setPrenotato(Boolean prenotato) {
      this.prenotato = prenotato;
    }

    public Long getPrenotazioneId() {
      return prenotazioneId;
    }

    public void setPrenotazioneId(Long prenotazioneId) {
      this.prenotazioneId = prenotazioneId;
    }
  }
}
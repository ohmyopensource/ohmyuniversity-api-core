package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/appelli.
 * Maps available exam sessions from Cineca calesa-service-v1.
 */
public class AppelloResponse {

  private List<Appello> appelli;

  public List<Appello> getAppelli() { return appelli; }
  public void setAppelli(List<Appello> appelli) { this.appelli = appelli; }

  /**
   * Single available exam session.
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

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getAdCod() { return adCod; }
    public void setAdCod(String adCod) { this.adCod = adCod; }
    public String getAdDes() { return adDes; }
    public void setAdDes(String adDes) { this.adDes = adDes; }
    public String getDataInizio() { return dataInizio; }
    public void setDataInizio(String dataInizio) { this.dataInizio = dataInizio; }
    public String getDataFine() { return dataFine; }
    public void setDataFine(String dataFine) { this.dataFine = dataFine; }
    public String getDataScadPrenotazione() { return dataScadPrenotazione; }
    public void setDataScadPrenotazione(String d) { this.dataScadPrenotazione = d; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTipoDes() { return tipoDes; }
    public void setTipoDes(String tipoDes) { this.tipoDes = tipoDes; }
    public String getDocente() { return docente; }
    public void setDocente(String docente) { this.docente = docente; }
    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }
    public Boolean getPrenotabile() { return prenotabile; }
    public void setPrenotabile(Boolean prenotabile) { this.prenotabile = prenotabile; }
    public Boolean getPrenotato() { return prenotato; }
    public void setPrenotato(Boolean prenotato) { this.prenotato = prenotato; }
    public Long getPrenotazioneId() { return prenotazioneId; }
    public void setPrenotazioneId(Long prenotazioneId) { this.prenotazioneId = prenotazioneId; }
  }
}
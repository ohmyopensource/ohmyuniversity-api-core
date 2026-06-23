package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/v1/carriera/storico-esami. Returns complete exam attempt history
 * grouped by course activity.
 */
public class StoricoEsamiResponse {

  private List<EsameConStorico> esami;

  public List<EsameConStorico> getEsami() {
    return esami;
  }

  public void setEsami(List<EsameConStorico> esami) {
    this.esami = esami;
  }

  /**
   * A single course activity with all its exam attempts.
   */
  public static class EsameConStorico {

    private String adCod;
    private String adDes;
    private Long adsceId;
    private Double cfu;
    private List<Tentativo> tentativi;

    public String getAdCod() {
      return adCod;
    }

    public void setAdCod(String v) {
      this.adCod = v;
    }

    public String getAdDes() {
      return adDes;
    }

    public void setAdDes(String v) {
      this.adDes = v;
    }

    public Long getAdsceId() {
      return adsceId;
    }

    public void setAdsceId(Long v) {
      this.adsceId = v;
    }

    public Double getCfu() {
      return cfu;
    }

    public void setCfu(Double v) {
      this.cfu = v;
    }

    public List<Tentativo> getTentativi() {
      return tentativi;
    }

    public void setTentativi(List<Tentativo> v) {
      this.tentativi = v;
    }
  }

  /**
   * A single exam attempt.
   */
  public static class Tentativo {

    private Long applistaId;
    private String dataOraTurno;
    private String dataInizioIscr;
    private String dataFineIscr;
    private String tipoIscrCod;
    private String domandeEsame;
    private Integer posizApp;
    private boolean superato;
    private boolean ritirato;
    private boolean assente;
    private boolean futuro;
    private Integer votoEsa;
    private String tipoGiudCod;
    private String tipoGiudizioDes;

    public Long getApplistaId() {
      return applistaId;
    }

    public void setApplistaId(Long v) {
      this.applistaId = v;
    }

    public String getDataOraTurno() {
      return dataOraTurno;
    }

    public void setDataOraTurno(String v) {
      this.dataOraTurno = v;
    }

    public String getDataInizioIscr() {
      return dataInizioIscr;
    }

    public void setDataInizioIscr(String v) {
      this.dataInizioIscr = v;
    }

    public String getDataFineIscr() {
      return dataFineIscr;
    }

    public void setDataFineIscr(String v) {
      this.dataFineIscr = v;
    }

    public String getTipoIscrCod() {
      return tipoIscrCod;
    }

    public void setTipoIscrCod(String v) {
      this.tipoIscrCod = v;
    }

    public String getDomandeEsame() {
      return domandeEsame;
    }

    public void setDomandeEsame(String v) {
      this.domandeEsame = v;
    }

    public Integer getPosizApp() {
      return posizApp;
    }

    public void setPosizApp(Integer v) {
      this.posizApp = v;
    }

    public boolean isSuperato() {
      return superato;
    }

    public void setSuperato(boolean v) {
      this.superato = v;
    }

    public boolean isRitirato() {
      return ritirato;
    }

    public void setRitirato(boolean v) {
      this.ritirato = v;
    }

    public boolean isAssente() {
      return assente;
    }

    public void setAssente(boolean v) {
      this.assente = v;
    }

    public boolean isFuturo() {
      return futuro;
    }

    public void setFuturo(boolean v) {
      this.futuro = v;
    }

    public Integer getVotoEsa() {
      return votoEsa;
    }

    public void setVotoEsa(Integer v) {
      this.votoEsa = v;
    }

    public String getTipoGiudCod() {
      return tipoGiudCod;
    }

    public void setTipoGiudCod(String v) {
      this.tipoGiudCod = v;
    }

    public String getTipoGiudizioDes() {
      return tipoGiudizioDes;
    }

    public void setTipoGiudizioDes(String v) {
      this.tipoGiudizioDes = v;
    }
  }
}
package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/v1/carriera/appelli-prenotabili.
 *
 * <p>Contains exam sessions retrieved from libretto-service-v2,
 * accessible to STUDENTE role without checkAbildocStu restrictions.
 */
public class AppelliLibrettoResponse {

  private List<AppelloLibretto> appelli;

  public List<AppelloLibretto> getAppelli() {
    return appelli;
  }

  public void setAppelli(List<AppelloLibretto> appelli) {
    this.appelli = appelli;
  }

  /**
   * A single bookable exam session linked to the student's libretto.
   */
  public static class AppelloLibretto {

    private Long appId;
    private Long appelloId;
    private Long cdsId;
    private Long adId;
    private String adCod;
    private String adDes;
    private Long adsceId;
    private String dataInizioApp;
    private String dataInizioIscr;
    private String dataFineIscr;
    private String oraEsa;
    private String stato;
    private String statoDes;
    private String docente;
    private String note;
    private Integer numIscritti;
    private String tipoIscrCod;
    private String desApp;

    // getters e setters per tutti
    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long appId) {
      this.appId = appId;
    }

    public Long getAppelloId() {
      return appelloId;
    }

    public void setAppelloId(Long appelloId) {
      this.appelloId = appelloId;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public void setCdsId(Long cdsId) {
      this.cdsId = cdsId;
    }

    public Long getAdId() {
      return adId;
    }

    public void setAdId(Long adId) {
      this.adId = adId;
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

    public Long getAdsceId() {
      return adsceId;
    }

    public void setAdsceId(Long adsceId) {
      this.adsceId = adsceId;
    }

    public String getDataInizioApp() {
      return dataInizioApp;
    }

    public void setDataInizioApp(String d) {
      this.dataInizioApp = d;
    }

    public String getDataInizioIscr() {
      return dataInizioIscr;
    }

    public void setDataInizioIscr(String d) {
      this.dataInizioIscr = d;
    }

    public String getDataFineIscr() {
      return dataFineIscr;
    }

    public void setDataFineIscr(String d) {
      this.dataFineIscr = d;
    }

    public String getOraEsa() {
      return oraEsa;
    }

    public void setOraEsa(String oraEsa) {
      this.oraEsa = oraEsa;
    }

    public String getStato() {
      return stato;
    }

    public void setStato(String stato) {
      this.stato = stato;
    }

    public String getStatoDes() {
      return statoDes;
    }

    public void setStatoDes(String statoDes) {
      this.statoDes = statoDes;
    }

    public String getDocente() {
      return docente;
    }

    public void setDocente(String docente) {
      this.docente = docente;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public Integer getNumIscritti() {
      return numIscritti;
    }

    public void setNumIscritti(Integer numIscritti) {
      this.numIscritti = numIscritti;
    }

    public String getTipoIscrCod() {
      return tipoIscrCod;
    }

    public void setTipoIscrCod(String tipoIscrCod) {
      this.tipoIscrCod = tipoIscrCod;
    }

    public String getDesApp() {
      return desApp;
    }

    public void setDesApp(String desApp) {
      this.desApp = desApp;
    }
  }
}
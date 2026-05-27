package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/piano.
 * Maps the study plan from Cineca piani-service-v1.
 */
public class PianoStudioResponse {

  private List<RigaPiano> righe;

  public List<RigaPiano> getRighe() { return righe; }
  public void setRighe(List<RigaPiano> righe) { this.righe = righe; }

  /**
   * Single row of the study plan — one course activity.
   */
  public static class RigaPiano {

    private Long adsceId;
    private String adCod;
    private String adDes;
    private Integer annoCorso;
    private Double cfu;
    private String tipoInsCod;
    private String tipoInsDes;
    private Boolean obbligatorio;
    private String stato;
    private String statoDes;
    private Boolean superata;

    public Long getAdsceId() { return adsceId; }
    public void setAdsceId(Long adsceId) { this.adsceId = adsceId; }
    public String getAdCod() { return adCod; }
    public void setAdCod(String adCod) { this.adCod = adCod; }
    public String getAdDes() { return adDes; }
    public void setAdDes(String adDes) { this.adDes = adDes; }
    public Integer getAnnoCorso() { return annoCorso; }
    public void setAnnoCorso(Integer annoCorso) { this.annoCorso = annoCorso; }
    public Double getCfu() { return cfu; }
    public void setCfu(Double cfu) { this.cfu = cfu; }
    public String getTipoInsCod() { return tipoInsCod; }
    public void setTipoInsCod(String tipoInsCod) { this.tipoInsCod = tipoInsCod; }
    public String getTipoInsDes() { return tipoInsDes; }
    public void setTipoInsDes(String tipoInsDes) { this.tipoInsDes = tipoInsDes; }
    public Boolean getObbligatorio() { return obbligatorio; }
    public void setObbligatorio(Boolean obbligatorio) { this.obbligatorio = obbligatorio; }
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    public String getStatoDes() { return statoDes; }
    public void setStatoDes(String statoDes) { this.statoDes = statoDes; }
    public Boolean getSuperata() { return superata; }
    public void setSuperata(Boolean superata) { this.superata = superata; }
  }
}
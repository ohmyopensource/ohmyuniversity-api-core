package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/carriera/libretto.
 *
 * <p>This object represents the student transcript as returned by Cineca libretto-service-v2,
 * mapped into a stable internal API model.
 *
 * <p>It is used as a read-only transport structure and does not contain any business logic or
 * persistence behavior.
 *
 * <p>The purpose of this DTO is to decouple the internal API contract from external Cineca data
 * structures, ensuring long-term stability.
 */
public class LibrettoResponse {

  private List<RigaLibretto> righe;

  // ============ Getters | Setters | Bool ============

  public List<RigaLibretto> getRighe() {
    return righe;
  }

  public void setRighe(List<RigaLibretto> righe) {
    this.righe = righe;
  }

  /**
   * Represents a single row in the student transcript (libretto).
   *
   * <p>Each row corresponds to an exam or academic activity and includes:
   * - exam metadata (code, description, year)
   * - outcome information (grade, lode, status)
   * - credit and exam attempt data
   */
  public static class RigaLibretto {

    private Long adsceId;
    private String adCod;
    private String adDes;
    private Integer annoCorso;
    private String stato;
    private String statoDes;
    private Double peso;
    private String tipoInsCod;
    private String tipoInsDes;
    private Integer voto;
    private Boolean lode;
    private String dataEsame;
    private Boolean superata;
    private Integer numAppelliPrenotabili;

    // ============ Getters | Setters | Bool ============

    public Long getAdsceId() {
      return adsceId;
    }

    public void setAdsceId(Long adsceId) {
      this.adsceId = adsceId;
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

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public void setAnnoCorso(Integer annoCorso) {
      this.annoCorso = annoCorso;
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

    public Double getPeso() {
      return peso;
    }

    public void setPeso(Double peso) {
      this.peso = peso;
    }

    public String getTipoInsCod() {
      return tipoInsCod;
    }

    public void setTipoInsCod(String tipoInsCod) {
      this.tipoInsCod = tipoInsCod;
    }

    public String getTipoInsDes() {
      return tipoInsDes;
    }

    public void setTipoInsDes(String tipoInsDes) {
      this.tipoInsDes = tipoInsDes;
    }

    public Integer getVoto() {
      return voto;
    }

    public void setVoto(Integer voto) {
      this.voto = voto;
    }

    public Boolean getLode() {
      return lode;
    }

    public void setLode(Boolean lode) {
      this.lode = lode;
    }

    public String getDataEsame() {
      return dataEsame;
    }

    public void setDataEsame(String dataEsame) {
      this.dataEsame = dataEsame;
    }

    public Boolean getSuperata() {
      return superata;
    }

    public void setSuperata(Boolean superata) {
      this.superata = superata;
    }

    public Integer getNumAppelliPrenotabili() {
      return numAppelliPrenotabili;
    }

    public void setNumAppelliPrenotabili(Integer n) {
      this.numAppelliPrenotabili = n;
    }
  }
}
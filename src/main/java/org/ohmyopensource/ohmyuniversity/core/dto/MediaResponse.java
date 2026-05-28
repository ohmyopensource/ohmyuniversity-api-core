package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Response DTO for GET /api/carriera/medie.
 *
 * This object represents aggregated academic performance metrics
 * calculated from the Cineca libretto-service-v2 endpoints.
 *
 * It includes weighted and arithmetic averages, credit accumulation,
 * exam progress, and graduation projection data.
 *
 * All values are derived at runtime from live Cineca data and are not persisted.
 */
public class MediaResponse {

  private Double mediaAritmetica;
  private Double mediaPesata;
  private Double baseMax110;
  private Double cfu;
  private Double cfuTotali;
  private Integer esamiSuperati;
  private Integer esamiTotali;
  private Double percentualeCfu;

  // ============ Getters | Setters | Bool ============

  public Double getMediaAritmetica() { return mediaAritmetica; }
  public void setMediaAritmetica(Double mediaAritmetica) {
    this.mediaAritmetica = mediaAritmetica;
  }

  public Double getMediaPesata() { return mediaPesata; }
  public void setMediaPesata(Double mediaPesata) { this.mediaPesata = mediaPesata; }

  public Double getBaseMax110() { return baseMax110; }
  public void setBaseMax110(Double baseMax110) { this.baseMax110 = baseMax110; }

  public Double getCfu() { return cfu; }
  public void setCfu(Double cfu) { this.cfu = cfu; }

  public Double getCfuTotali() { return cfuTotali; }
  public void setCfuTotali(Double cfuTotali) { this.cfuTotali = cfuTotali; }

  public Integer getEsamiSuperati() { return esamiSuperati; }
  public void setEsamiSuperati(Integer esamiSuperati) { this.esamiSuperati = esamiSuperati; }

  public Integer getEsamiTotali() { return esamiTotali; }
  public void setEsamiTotali(Integer esamiTotali) { this.esamiTotali = esamiTotali; }

  public Double getPercentualeCfu() { return percentualeCfu; }
  public void setPercentualeCfu(Double percentualeCfu) { this.percentualeCfu = percentualeCfu; }
}
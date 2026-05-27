package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Response DTO for GET /api/carriera/medie.
 * Maps the calculated averages from Cineca libretto-service-v2 medie endpoint.
 */
public class MediaResponse {

  /** Arithmetic average of all passed exams. */
  private Double mediaAritmetica;

  /** Weighted average (by ECTS credits). */
  private Double mediaPesata;

  /** Projected graduation base score out of 110. */
  private Double baseMax110;

  /** Total ECTS credits acquired. */
  private Double cfu;

  /** Total ECTS credits required for graduation. */
  private Double cfuTotali;

  /** Number of exams passed. */
  private Integer esamiSuperati;

  /** Total number of exams in the study plan. */
  private Integer esamiTotali;

  /** Percentage of ECTS acquired vs total required. */
  private Double percentualeCfu;

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
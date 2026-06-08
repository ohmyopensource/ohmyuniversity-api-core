package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for GET /api/v1/carriera/esami-suggeriti.
 *
 * <p>Contains an ordered list of exam suggestions for the authenticated student,
 * sorted from most to least convenient to attempt based on a scoring algorithm
 * that considers year of course and ECTS weight.
 */
public class SuggerimentiResponse {

  /** Ordered list of suggested exams, from most to least convenient. */
  private List<EsameSuggerito> esami;

  // ============ Getters | Setters | Bool ============

  public List<EsameSuggerito> getEsami() { return esami; }
  public void setEsami(List<EsameSuggerito> esami) { this.esami = esami; }

  /**
   * A single exam suggestion with its convenience score.
   *
   * <p>Score formula: {@code (annoCorso * 100) + cfu}
   * Lower score means more convenient to attempt first.
   */
  public static class EsameSuggerito {

    /** Cineca teaching activity code. */
    private String adCod;

    /** Human-readable exam name. */
    private String adDes;

    /** ECTS credits (CFU). */
    private Double cfu;

    /** Year of course the exam belongs to (1, 2, 3...). */
    private Integer annoCorso;

    /**
     * Convenience score — lower is better.
     * Computed as {@code (annoCorso * 100) + cfu}.
     */
    private Integer score;

    // ============ Getters | Setters | Bool ============

    public String getAdCod() { return adCod; }
    public void setAdCod(String adCod) { this.adCod = adCod; }

    public String getAdDes() { return adDes; }
    public void setAdDes(String adDes) { this.adDes = adDes; }

    public Double getCfu() { return cfu; }
    public void setCfu(Double cfu) { this.cfu = cfu; }

    public Integer getAnnoCorso() { return annoCorso; }
    public void setAnnoCorso(Integer annoCorso) { this.annoCorso = annoCorso; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
  }
}
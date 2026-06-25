package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/v1/carriera/questionari. Contains questionnaire status for all libretto
 * activities.
 */
public class SurveysResponse {

  private List<QuestionarioEsame> daCompilare;
  private List<QuestionarioEsame> compilati;

  public List<QuestionarioEsame> getDaCompilare() {
    return daCompilare;
  }

  public void setDaCompilare(List<QuestionarioEsame> v) {
    this.daCompilare = v;
  }

  public List<QuestionarioEsame> getCompilati() {
    return compilati;
  }

  public void setCompilati(List<QuestionarioEsame> v) {
    this.compilati = v;
  }

  /**
   * A single exam with its questionnaire status. statoLink: 0=nessuno, 1=tutti compilati, 2=alcuni
   * da compilare, 3=tutti da compilare
   */
  public static class QuestionarioEsame {

    private String adCod;
    private String adDes;
    private Long adsceId;
    private Integer annoCorso;
    private Double cfu;
    private Integer statoLink;

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

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public void setAnnoCorso(Integer v) {
      this.annoCorso = v;
    }

    public Double getCfu() {
      return cfu;
    }

    public void setCfu(Double v) {
      this.cfu = v;
    }

    public Integer getStatoLink() {
      return statoLink;
    }

    public void setStatoLink(Integer v) {
      this.statoLink = v;
    }
  }
}
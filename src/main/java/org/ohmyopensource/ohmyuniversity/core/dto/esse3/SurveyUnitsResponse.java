package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/v1/exams/surveys/{adsceId}/units.
 *
 * <p>Represents the "box" of teaching-evaluation questionnaires attached to a booklet
 * activity (adsceId). A box may contain multiple modules (e.g. Lezione, Laboratorio, or
 * different teachers), each a separate questionnaire identified by its own {@code tags}.
 */
public class SurveyUnitsResponse {

  private Long adsceId;
  private Long questionarioId;
  private Long questConfigId;
  private Integer anonimoFlg;
  private String questionarioDes;
  private List<SurveyModule> moduli;

  public Long getAdsceId() {
    return adsceId;
  }

  public void setAdsceId(Long v) {
    this.adsceId = v;
  }

  public Long getQuestionarioId() {
    return questionarioId;
  }

  public void setQuestionarioId(Long v) {
    this.questionarioId = v;
  }

  public Long getQuestConfigId() {
    return questConfigId;
  }

  public void setQuestConfigId(Long v) {
    this.questConfigId = v;
  }

  public Integer getAnonimoFlg() {
    return anonimoFlg;
  }

  public void setAnonimoFlg(Integer v) {
    this.anonimoFlg = v;
  }

  public String getQuestionarioDes() {
    return questionarioDes;
  }

  public void setQuestionarioDes(String v) {
    this.questionarioDes = v;
  }

  public List<SurveyModule> getModuli() {
    return moduli;
  }

  public void setModuli(List<SurveyModule> v) {
    this.moduli = v;
  }

  public static class SurveyModule {

    private String adDes;
    private String udDes;
    private String docente;
    private String moduloLabel;
    private Integer statoLink;
    private String tags;

    public String getAdDes() {
      return adDes;
    }

    public void setAdDes(String v) {
      this.adDes = v;
    }

    public String getUdDes() {
      return udDes;
    }

    public void setUdDes(String v) {
      this.udDes = v;
    }

    public String getDocente() {
      return docente;
    }

    public void setDocente(String v) {
      this.docente = v;
    }

    public String getModuloLabel() {
      return moduloLabel;
    }

    public void setModuloLabel(String v) {
      this.moduloLabel = v;
    }

    public Integer getStatoLink() {
      return statoLink;
    }

    public void setStatoLink(Integer v) {
      this.statoLink = v;
    }

    public String getTags() {
      return tags;
    }

    public void setTags(String v) {
      this.tags = v;
    }
  }
}
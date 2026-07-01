package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

public class SurveySummaryRequest {

  private Long adsceId;
  private Long questionarioId;
  private Long questCompId;
  private Long questConfigId;
  private Long userCompId;

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

  public Long getQuestCompId() {
    return questCompId;
  }

  public void setQuestCompId(Long v) {
    this.questCompId = v;
  }

  public Long getQuestConfigId() {
    return questConfigId;
  }

  public void setQuestConfigId(Long v) {
    this.questConfigId = v;
  }

  public Long getUserCompId() {
    return userCompId;
  }

  public void setUserCompId(Long v) {
    this.userCompId = v;
  }
}
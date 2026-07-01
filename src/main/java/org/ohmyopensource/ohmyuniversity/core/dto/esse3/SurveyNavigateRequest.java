package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Request DTO for POST /api/v1/exams/surveys/navigate.
 *
 * <p>Carries the session identifiers needed to fetch an adjacent page (next or
 * prev) within a compilation session.
 */
public class SurveyNavigateRequest {

  private Long adsceId;
  private Long questionarioId;
  private Long questCompId;
  private Long pageId;
  private Long userCompId;
  private String direction;

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

  public Long getPageId() {
    return pageId;
  }

  public void setPageId(Long v) {
    this.pageId = v;
  }

  public Long getUserCompId() {
    return userCompId;
  }

  public void setUserCompId(Long v) {
    this.userCompId = v;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String v) {
    this.direction = v;
  }
}
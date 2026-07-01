package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Request DTO for POST /api/v1/exams/surveys/save.
 *
 * <p>Carries the session identifiers needed to address a compilation session and
 * the list of answers for the page being saved.
 */
public class SurveySaveRequest {

  private Long questionarioId;
  private Long questCompId;
  private Long pageId;
  private List<SurveyAnswerRequest> answers;

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

  public List<SurveyAnswerRequest> getAnswers() {
    return answers;
  }

  public void setAnswers(List<SurveyAnswerRequest> v) {
    this.answers = v;
  }
}
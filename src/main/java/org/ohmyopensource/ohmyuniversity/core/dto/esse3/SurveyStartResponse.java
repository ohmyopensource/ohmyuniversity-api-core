package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for POST /api/v1/exams/surveys/start.
 *
 * <p>Represents the first page of a questionnaire compilation session, along with
 * the session identifiers ({@code questCompId}, {@code userCompId}) that the client must echo back
 * on every subsequent step (save, next, prev, summary, confirm).
 */
public class SurveyStartResponse {

  private Long questCompId;
  private Long userCompId;
  private Long questionarioId;
  private Long questConfigId;
  private Integer anonimoFlg;
  private String questionarioDes;
  private SurveyPage page;

  public Long getQuestCompId() {
    return questCompId;
  }

  public void setQuestCompId(Long v) {
    this.questCompId = v;
  }

  public Long getUserCompId() {
    return userCompId;
  }

  public void setUserCompId(Long v) {
    this.userCompId = v;
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

  public SurveyPage getPage() {
    return page;
  }

  public void setPage(SurveyPage v) {
    this.page = v;
  }

  /**
   * A single questionnaire page with its paragraphs.
   */
  public static class SurveyPage {

    private Long paginaId;
    private Long prevPageId;
    private Long nextPageId;
    private String des;
    private List<SurveyParagraph> paragrafi;

    public Long getPaginaId() {
      return paginaId;
    }

    public void setPaginaId(Long v) {
      this.paginaId = v;
    }

    public Long getPrevPageId() {
      return prevPageId;
    }

    public void setPrevPageId(Long v) {
      this.prevPageId = v;
    }

    public Long getNextPageId() {
      return nextPageId;
    }

    public void setNextPageId(Long v) {
      this.nextPageId = v;
    }

    public String getDes() {
      return des;
    }

    public void setDes(String v) {
      this.des = v;
    }

    public List<SurveyParagraph> getParagrafi() {
      return paragrafi;
    }

    public void setParagrafi(List<SurveyParagraph> v) {
      this.paragrafi = v;
    }
  }

  /**
   * A paragraph grouping a set of questions, optionally titled.
   */
  public static class SurveyParagraph {

    private Long paragrafoId;
    private String des;
    private String note;
    private List<SurveyQuestion> domande;

    public Long getParagrafoId() {
      return paragrafoId;
    }

    public void setParagrafoId(Long v) {
      this.paragrafoId = v;
    }

    public String getDes() {
      return des;
    }

    public void setDes(String v) {
      this.des = v;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String v) {
      this.note = v;
    }

    public List<SurveyQuestion> getDomande() {
      return domande;
    }

    public void setDomande(List<SurveyQuestion> v) {
      this.domande = v;
    }
  }

  /**
   * A single question with its available answers.
   */
  public static class SurveyQuestion {

    private Long domandaId;
    private String des;
    private boolean mandatory;
    private Integer maxChoices;
    private String formatCod;
    private List<SurveyAnswer> risposte;

    public Long getDomandaId() {
      return domandaId;
    }

    public void setDomandaId(Long v) {
      this.domandaId = v;
    }

    public String getDes() {
      return des;
    }

    public void setDes(String v) {
      this.des = v;
    }

    public boolean isMandatory() {
      return mandatory;
    }

    public void setMandatory(boolean v) {
      this.mandatory = v;
    }

    public Integer getMaxChoices() {
      return maxChoices;
    }

    public void setMaxChoices(Integer v) {
      this.maxChoices = v;
    }

    public String getFormatCod() {
      return formatCod;
    }

    public void setFormatCod(String v) {
      this.formatCod = v;
    }

    public List<SurveyAnswer> getRisposte() {
      return risposte;
    }

    public void setRisposte(List<SurveyAnswer> v) {
      this.risposte = v;
    }
  }

  /**
   * A single available answer for a question.
   */
  public static class SurveyAnswer {

    private Long rispostaId;
    private String des;
    private String formatCod;

    public Long getRispostaId() {
      return rispostaId;
    }

    public void setRispostaId(Long v) {
      this.rispostaId = v;
    }

    public String getDes() {
      return des;
    }

    public void setDes(String v) {
      this.des = v;
    }

    public String getFormatCod() {
      return formatCod;
    }

    public void setFormatCod(String v) {
      this.formatCod = v;
    }
  }
}
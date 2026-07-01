package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Request DTO representing a single answer submitted for a questionnaire question.
 *
 * <p>For fixed-text answers only {@code rispostaId} is provided and
 * {@code corpoRisposta} stays an empty string. For free-text answers {@code corpoRisposta} carries
 * the typed text. For multiple-choice questions, several instances share the same
 * {@code domandaId}, one per selected {@code rispostaId}.
 *
 * <p>Cineca requires {@code corpoRisposta} to always be present: a null or absent
 * value triggers a NO_TEXT validation error even for fixed-text answers. The field therefore
 * defaults to an empty string and is always serialized.
 */
public class SurveyAnswerRequest {

  private Long domandaId;
  private Long rispostaId;
  private String corpoRisposta = "";

  public Long getDomandaId() {
    return domandaId;
  }

  public void setDomandaId(Long v) {
    this.domandaId = v;
  }

  public Long getRispostaId() {
    return rispostaId;
  }

  public void setRispostaId(Long v) {
    this.rispostaId = v;
  }

  public String getCorpoRisposta() {
    return corpoRisposta;
  }

  public void setCorpoRisposta(String v) {
    this.corpoRisposta = (v == null ? "" : v);
  }
}
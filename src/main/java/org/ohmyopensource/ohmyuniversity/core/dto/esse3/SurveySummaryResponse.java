package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

public class SurveySummaryResponse {

  private String questionarioDes;
  private List<SummaryPage> pagine;

  public String getQuestionarioDes() {
    return questionarioDes;
  }

  public void setQuestionarioDes(String v) {
    this.questionarioDes = v;
  }

  public List<SummaryPage> getPagine() {
    return pagine;
  }

  public void setPagine(List<SummaryPage> v) {
    this.pagine = v;
  }

  public static class SummaryPage {

    private Long paginaId;
    private List<SummaryItem> items;

    public Long getPaginaId() {
      return paginaId;
    }

    public void setPaginaId(Long v) {
      this.paginaId = v;
    }

    public List<SummaryItem> getItems() {
      return items;
    }

    public void setItems(List<SummaryItem> v) {
      this.items = v;
    }
  }

  public static class SummaryItem {

    private String paragrafoDes;
    private String domandaDes;
    private String rispostaDes;
    private String testoLibero;

    public String getParagrafoDes() {
      return paragrafoDes;
    }

    public void setParagrafoDes(String v) {
      this.paragrafoDes = v;
    }

    public String getDomandaDes() {
      return domandaDes;
    }

    public void setDomandaDes(String v) {
      this.domandaDes = v;
    }

    public String getRispostaDes() {
      return rispostaDes;
    }

    public void setRispostaDes(String v) {
      this.rispostaDes = v;
    }

    public String getTestoLibero() {
      return testoLibero;
    }

    public void setTestoLibero(String v) {
      this.testoLibero = v;
    }
  }
}
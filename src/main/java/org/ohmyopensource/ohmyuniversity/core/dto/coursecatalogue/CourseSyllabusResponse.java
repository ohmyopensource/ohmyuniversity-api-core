package org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue;

import java.util.List;

/**
 * Response DTO for GET /api/v1/course-catalogue/syllabus.
 *
 * <p>Sourced from Cineca Course Catalogue, which — unlike ESSE3 — some
 * universities actually populate with real prerequisites and per-module
 * course content. All fields may be {@code null}/empty when the university
 * has not compiled this data for the given activity.
 */
public class CourseSyllabusResponse {

  private String prerequisites;
  private List<CfuBreakdownItem> cfuBreakdown;

  public String getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(String v) {
    this.prerequisites = v;
  }

  public List<CfuBreakdownItem> getCfuBreakdown() {
    return cfuBreakdown;
  }

  public void setCfuBreakdown(List<CfuBreakdownItem> v) {
    this.cfuBreakdown = v;
  }

  public static class CfuBreakdownItem {

    private String moduleName;
    private Integer cfu;
    private String content;
    private List<String> professors;

    public String getModuleName() {
      return moduleName;
    }

    public void setModuleName(String v) {
      this.moduleName = v;
    }

    public Integer getCfu() {
      return cfu;
    }

    public void setCfu(Integer v) {
      this.cfu = v;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String v) {
      this.content = v;
    }

    public List<String> getProfessors() {
      return professors;
    }

    public void setProfessors(List<String> v) {
      this.professors = v;
    }
  }
}
package org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue;

import java.util.List;

/**
 * Response DTO for GET /api/v1/course-catalogue/plan.
 *
 * <p>The full multi-year teaching plan for the authenticated student's
 * degree course and cohort, sourced from Cineca Course Catalogue rather
 * than the student's own libretto — so it includes years the student has
 * not reached yet.
 */
public class CoursePlanResponse {

  private List<CoursePlanExam> exams;

  public List<CoursePlanExam> getExams() {
    return exams;
  }

  public void setExams(List<CoursePlanExam> v) {
    this.exams = v;
  }

  public static class CoursePlanExam {

    private String adCod;
    private String name;
    private Integer cfu;
    private Integer academicYear;
    private String period;
    private Boolean mandatory;

    public String getAdCod() {
      return adCod;
    }

    public void setAdCod(String v) {
      this.adCod = v;
    }

    public String getName() {
      return name;
    }

    public void setName(String v) {
      this.name = v;
    }

    public Integer getCfu() {
      return cfu;
    }

    public void setCfu(Integer v) {
      this.cfu = v;
    }

    public Integer getAcademicYear() {
      return academicYear;
    }

    public void setAcademicYear(Integer v) {
      this.academicYear = v;
    }

    public String getPeriod() {
      return period;
    }

    public void setPeriod(String v) {
      this.period = v;
    }

    public Boolean getMandatory() {
      return mandatory;
    }

    public void setMandatory(Boolean v) {
      this.mandatory = v;
    }
  }
}
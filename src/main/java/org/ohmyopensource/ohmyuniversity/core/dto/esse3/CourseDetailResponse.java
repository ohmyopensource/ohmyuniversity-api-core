package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

public class CourseDetailResponse {

  private String period;
  private String location;
  private String teachingStartDate;
  private String teachingEndDate;
  private String teachingLanguage;
  private String examType;
  private String evaluationType;
  private Boolean mandatory;
  private String coursePageUrl;

  public String getPeriod() { return period; }
  public void setPeriod(String v) { this.period = v; }

  public String getLocation() { return location; }
  public void setLocation(String v) { this.location = v; }

  public String getTeachingStartDate() { return teachingStartDate; }
  public void setTeachingStartDate(String v) { this.teachingStartDate = v; }

  public String getTeachingEndDate() { return teachingEndDate; }
  public void setTeachingEndDate(String v) { this.teachingEndDate = v; }

  public String getTeachingLanguage() { return teachingLanguage; }
  public void setTeachingLanguage(String v) { this.teachingLanguage = v; }

  public String getExamType() { return examType; }
  public void setExamType(String v) { this.examType = v; }

  public String getEvaluationType() { return evaluationType; }
  public void setEvaluationType(String v) { this.evaluationType = v; }

  public Boolean getMandatory() { return mandatory; }
  public void setMandatory(Boolean v) { this.mandatory = v; }

  public String getCoursePageUrl() { return coursePageUrl; }
  public void setCoursePageUrl(String v) { this.coursePageUrl = v; }
}
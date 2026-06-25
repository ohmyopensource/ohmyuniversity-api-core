package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for {@code GET /v1/internships/applications}.
 *
 * <p>Contains the list of internship applications for the authenticated student,
 * mapped from Cineca {@code tirocini-service-v1/tirocini/{stuId}/domande}.
 */
public class InternshipApplicationResponse {

  private List<InternshipApplication> applications;

  public List<InternshipApplication> getApplications() { return applications; }
  public void setApplications(List<InternshipApplication> applications) {
    this.applications = applications;
  }

  /**
   * Represents a single internship application (domanda di tirocinio).
   */
  public static class InternshipApplication {

    /** Cineca internship application identifier. */
    private Long domTiroId;

    /** Progressive number of the application. */
    private Long domTiroPrg;

    /** Academic year identifier. */
    private Long academicYear;

    /** Application status code (PRE, CON, AVV, CHI, ANN, RIF, NAS). */
    private String statusCode;

    /** Application status description. */
    private String statusDescription;

    /** Internship type code. */
    private String internshipTypeCode;

    /** Internship type description. */
    private String internshipTypeDescription;

    /** Host organization identifier. */
    private Long organizationId;

    /** Host organization name. */
    private String organizationName;

    /** Opportunity title. */
    private String opportunityTitle;

    /** Opportunity description. */
    private String opportunityDescription;

    /** Internship start date (dd/MM/yyyy). */
    private String startDate;

    /** Duration in months. */
    private Integer durationMonths;

    /** Whether CFU recognition is enabled (1 = yes). */
    private Integer cfuRecognitionEnabled;

    public Long getDomTiroId() { return domTiroId; }
    public void setDomTiroId(Long domTiroId) { this.domTiroId = domTiroId; }
    public Long getDomTiroPrg() { return domTiroPrg; }
    public void setDomTiroPrg(Long domTiroPrg) { this.domTiroPrg = domTiroPrg; }
    public Long getAcademicYear() { return academicYear; }
    public void setAcademicYear(Long academicYear) { this.academicYear = academicYear; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) {
      this.statusDescription = statusDescription;
    }
    public String getInternshipTypeCode() { return internshipTypeCode; }
    public void setInternshipTypeCode(String internshipTypeCode) {
      this.internshipTypeCode = internshipTypeCode;
    }
    public String getInternshipTypeDescription() { return internshipTypeDescription; }
    public void setInternshipTypeDescription(String internshipTypeDescription) {
      this.internshipTypeDescription = internshipTypeDescription;
    }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) {
      this.organizationName = organizationName;
    }
    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) {
      this.opportunityTitle = opportunityTitle;
    }
    public String getOpportunityDescription() { return opportunityDescription; }
    public void setOpportunityDescription(String opportunityDescription) {
      this.opportunityDescription = opportunityDescription;
    }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) {
      this.durationMonths = durationMonths;
    }
    public Integer getCfuRecognitionEnabled() { return cfuRecognitionEnabled; }
    public void setCfuRecognitionEnabled(Integer cfuRecognitionEnabled) {
      this.cfuRecognitionEnabled = cfuRecognitionEnabled;
    }
  }
}
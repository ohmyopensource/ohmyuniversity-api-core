package org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue;

import java.util.List;

/**
 * Response DTO for GET /api/v1/course-catalogue/docenti/{docenteId}.
 */
public class DocenteDetailResponse {

  private String name;
  private String role;
  private String email;
  private String department;
  private String profilePageUrl;
  private String biography;
  private String publications;
  private String notes;
  private List<TaughtCourse> courses;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getProfilePageUrl() {
    return profilePageUrl;
  }

  public void setProfilePageUrl(String profilePageUrl) {
    this.profilePageUrl = profilePageUrl;
  }

  public String getBiography() {
    return biography;
  }

  public void setBiography(String biography) {
    this.biography = biography;
  }

  public String getPublications() {
    return publications;
  }

  public void setPublications(String publications) {
    this.publications = publications;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<TaughtCourse> getCourses() {
    return courses;
  }

  public void setCourses(List<TaughtCourse> courses) {
    this.courses = courses;
  }

  /**
   * A single course taught by this professor, flagged if it's in the requesting student's own
   * career.
   */
  public static class TaughtCourse {

    private String name;
    private String degreeCourseName;
    private String degreeCourseType;
    private boolean inStudentCareer;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDegreeCourseName() {
      return degreeCourseName;
    }

    public void setDegreeCourseName(String degreeCourseName) {
      this.degreeCourseName = degreeCourseName;
    }

    public String getDegreeCourseType() {
      return degreeCourseType;
    }

    public void setDegreeCourseType(String degreeCourseType) {
      this.degreeCourseType = degreeCourseType;
    }

    public boolean isInStudentCareer() {
      return inStudentCareer;
    }

    public void setInStudentCareer(boolean inStudentCareer) {
      this.inStudentCareer = inStudentCareer;
    }
  }
}
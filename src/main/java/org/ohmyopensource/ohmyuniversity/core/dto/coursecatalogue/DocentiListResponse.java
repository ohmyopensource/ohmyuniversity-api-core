package org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue;

import java.util.List;

/**
 * Response DTO for GET /api/v1/course-catalogue/docenti.
 */
public class DocentiListResponse {

  private List<DocenteSummary> docenti;

  public List<DocenteSummary> getDocenti() {
    return docenti;
  }

  public void setDocenti(List<DocenteSummary> docenti) {
    this.docenti = docenti;
  }

  /**
   * Minimal professor entry for the list view — full detail is fetched lazily on expand.
   */
  public static class DocenteSummary {

    private String id;
    private String name;
    private String profilePageUrl;
    private boolean hasDetail;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getProfilePageUrl() {
      return profilePageUrl;
    }

    public void setProfilePageUrl(String profilePageUrl) {
      this.profilePageUrl = profilePageUrl;
    }

    public boolean isHasDetail() {
      return hasDetail;
    }

    public void setHasDetail(boolean hasDetail) {
      this.hasDetail = hasDetail;
    }
  }
}
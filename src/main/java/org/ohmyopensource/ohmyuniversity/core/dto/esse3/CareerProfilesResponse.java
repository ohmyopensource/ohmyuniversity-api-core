package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response wrapper for {@code GET /api/v1/career/profiles} — every cached career profile known for
 * the authenticated user, across all universities and platform vendors.
 */
public class CareerProfilesResponse {

  // ============ Getters | Setters | Bool ============

  private List<CareerProfileResponse> profili;

  public List<CareerProfileResponse> getProfili() {
    return profili;
  }

  public void setProfili(List<CareerProfileResponse> profili) {
    this.profili = profili;
  }
}
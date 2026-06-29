package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Request body for booking or cancelling an exam session.
 *
 * <p>Carries the Cineca password required for Basic Auth against
 * {@code calesa-service-v1}. The password is never persisted or logged.
 */
public class BookExamRequest {

  private String password;

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
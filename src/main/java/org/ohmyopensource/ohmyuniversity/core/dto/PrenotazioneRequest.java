package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Request DTO for POST /api/carriera/prenotazioni.
 *
 * <p>This object carries the Cineca password required to retrieve the student's booking history
 * from ESSE3 services.
 *
 * <p>The password is provided at request time and is never persisted. It is transmitted over HTTPS
 * and used only for the current request lifecycle.
 *
 * <p>Note: it is intentionally placed in the request body to reduce the risk of exposure in server
 * access logs (e.g. query parameters or headers).
 */
public class PrenotazioneRequest {

  private String password;

  // ============ Getters | Setters | Bool ============

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
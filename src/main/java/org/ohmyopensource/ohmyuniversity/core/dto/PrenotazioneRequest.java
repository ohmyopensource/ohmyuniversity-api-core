package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Request DTO for POST /api/carriera/prenotazioni.
 * The Cineca password is sent in the request body over HTTPS
 * to avoid it appearing in server access logs (headers are often logged).
 */
public class PrenotazioneRequest {

  private String password;

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}
package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for POST /api/auth/login.
 *
 * <p>This object represents the authentication response returned after a successful login against a
 * Cineca ESSE3 university instance.
 *
 * <p>It contains:
 * - a short-lived access token used for authenticated requests
 * - a long-lived refresh token used to renew sessions
 * - the list of available academic profiles for the authenticated user
 *
 * <p>Each profile corresponds to a specific university and degree course, allowing multi-university
 * and multi-career support without re-authentication.
 */
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String nome;
    private String cognome;
    private List<ProfiloCarriera> profili;


    // ============ Getters | Setters | Bool ============

  public String getAccessToken() {
    return accessToken;
  }
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getNome() { return nome; }
  public void setNome(String nome) { this.nome = nome; }

  public String getCognome() { return cognome; }
  public void setCognome(String cognome) { this.cognome = cognome; }

  public List<ProfiloCarriera> getProfili() {
    return profili;
  }
  public void setProfili(List<ProfiloCarriera> profili) {
    this.profili = profili;
  }

  /**
   * Represents a single academic career profile.
   *
   * <p>Each profile corresponds to a specific:
   * - university
   * - degree course
   * - student enrollment (matricola context)
   *
   * <p>This structure allows the user to switch between multiple academic contexts without
   * re-authentication.
   */
  public static class ProfiloCarriera {

    private String universityId;
    private String universityName;
    private Long stuId;
    private Long matId;
    private String matricola;
    private String corsoNome;
    private String corsoCodice;

    /**
     * Cineca degree course identifier used for academic operations. Required for endpoints such as
     * exam sessions (appelli).
     */
    private Long cdsId;

    private String tipoCorsoCod;
    private String statusStudente;
    private String statusDescrizione;
    private Integer annoCorso;
    private Integer durataAnni;
    private Integer annoAccademico;
    private boolean attivo;

    // ============ Getters | Setters | Bool ============

    public String getUniversityId() {
      return universityId;
    }

    public void setUniversityId(String universityId) {
      this.universityId = universityId;
    }

    public String getUniversityName() {
      return universityName;
    }

    public void setUniversityName(String universityName) {
      this.universityName = universityName;
    }

    public Long getStuId() {
      return stuId;
    }

    public void setStuId(Long stuId) {
      this.stuId = stuId;
    }

    public Long getMatId() {
      return matId;
    }

    public void setMatId(Long matId) {
      this.matId = matId;
    }

    public String getMatricola() {
      return matricola;
    }

    public void setMatricola(String matricola) {
      this.matricola = matricola;
    }

    public String getCorsoNome() {
      return corsoNome;
    }

    public void setCorsoNome(String corsoNome) {
      this.corsoNome = corsoNome;
    }

    public String getCorsoCodice() {
      return corsoCodice;
    }

    public void setCorsoCodice(String corsoCodice) {
      this.corsoCodice = corsoCodice;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public void setCdsId(Long cdsId) {
      this.cdsId = cdsId;
    }

    public String getTipoCorsoCod() {
      return tipoCorsoCod;
    }

    public void setTipoCorsoCod(String tipoCorsoCod) {
      this.tipoCorsoCod = tipoCorsoCod;
    }

    public String getStatusStudente() {
      return statusStudente;
    }

    public void setStatusStudente(String statusStudente) {
      this.statusStudente = statusStudente;
    }

    public String getStatusDescrizione() {
      return statusDescrizione;
    }

    public void setStatusDescrizione(String statusDescrizione) {
      this.statusDescrizione = statusDescrizione;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public void setAnnoCorso(Integer annoCorso) {
      this.annoCorso = annoCorso;
    }

    public Integer getDurataAnni() {
      return durataAnni;
    }

    public void setDurataAnni(Integer durataAnni) {
      this.durataAnni = durataAnni;
    }

    public Integer getAnnoAccademico() {
      return annoAccademico;
    }

    public void setAnnoAccademico(Integer annoAccademico) {
      this.annoAccademico = annoAccademico;
    }

    public boolean isAttivo() {
      return attivo;
    }

    public void setAttivo(boolean attivo) {
      this.attivo = attivo;
    }
  }
}
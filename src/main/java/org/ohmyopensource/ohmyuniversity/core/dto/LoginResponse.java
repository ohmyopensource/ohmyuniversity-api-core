package org.ohmyopensource.ohmyuniversity.core.dto;

import java.util.List;

/**
 * Response DTO for POST /api/auth/login.
 *
 * Returns the OhMyUniversity access token and refresh token,
 * plus the list of all university profiles available for this user.
 * The student can then switch between profiles without re-logging in.
 */
public class LoginResponse {

  /** OhMyUniversity JWT access token — short-lived (15 minutes). */
  private String accessToken;

  /** OhMyUniversity refresh token — long-lived (7 days), stored in Redis. */
  private String refreshToken;

  /** All career profiles available for this user across all connected universities. */
  private List<ProfiloCarriera> profili;

  public String getAccessToken() { return accessToken; }
  public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

  public String getRefreshToken() { return refreshToken; }
  public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

  public List<ProfiloCarriera> getProfili() { return profili; }
  public void setProfili(List<ProfiloCarriera> profili) { this.profili = profili; }

  /**
   * Represents a single career profile — one degree course at one university.
   * A student can have multiple profiles (e.g. L-31 + LM-18, or two universities).
   */
  public static class ProfiloCarriera {

    private String universityId;
    private String universityName;
    private Long stuId;
    private Long matId;
    private String matricola;
    private String corsoNome;
    private String corsoCodice;
    private String tipoCorsoCod;
    private String statusStudente;
    private String statusDescrizione;
    private Integer annoCorso;
    private Integer durataAnni;
    private Integer annoAccademico;
    private boolean attivo;

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public Long getStuId() { return stuId; }
    public void setStuId(Long stuId) { this.stuId = stuId; }

    public Long getMatId() { return matId; }
    public void setMatId(Long matId) { this.matId = matId; }

    public String getMatricola() { return matricola; }
    public void setMatricola(String matricola) { this.matricola = matricola; }

    public String getCorsoNome() { return corsoNome; }
    public void setCorsoNome(String corsoNome) { this.corsoNome = corsoNome; }

    public String getCorsoCodice() { return corsoCodice; }
    public void setCorsoCodice(String corsoCodice) { this.corsoCodice = corsoCodice; }

    public String getTipoCorsoCod() { return tipoCorsoCod; }
    public void setTipoCorsoCod(String tipoCorsoCod) { this.tipoCorsoCod = tipoCorsoCod; }

    public String getStatusStudente() { return statusStudente; }
    public void setStatusStudente(String statusStudente) {
      this.statusStudente = statusStudente;
    }

    public String getStatusDescrizione() { return statusDescrizione; }
    public void setStatusDescrizione(String statusDescrizione) {
      this.statusDescrizione = statusDescrizione;
    }

    public Integer getAnnoCorso() { return annoCorso; }
    public void setAnnoCorso(Integer annoCorso) { this.annoCorso = annoCorso; }

    public Integer getDurataAnni() { return durataAnni; }
    public void setDurataAnni(Integer durataAnni) { this.durataAnni = durataAnni; }

    public Integer getAnnoAccademico() { return annoAccademico; }
    public void setAnnoAccademico(Integer annoAccademico) {
      this.annoAccademico = annoAccademico;
    }

    public boolean isAttivo() { return attivo; }
    public void setAttivo(boolean attivo) { this.attivo = attivo; }
  }
}
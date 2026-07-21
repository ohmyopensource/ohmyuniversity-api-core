package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * A single cached career profile for one university/vendor account, sourced from
 * {@code cached_profilo_carriera} rather than a live Cineca call.
 *
 * <p>Mirrors the shape of the pre-auth-extraction {@code LoginResponse.ProfiloCarriera},
 * which used to be embedded directly in the login response. That aggregation moved here because the
 * auth service has no access to this service's cache.
 */
public class CareerProfileResponse {

  private String universityId;
  private String universityName;
  private Long stuId;
  private Long matId;
  private String matricola;
  private String corsoNome;
  private String corsoCodice;
  private Long cdsId;
  private String tipoCorsoCod;
  private String statusStudente;
  private String statusDescrizione;
  private Integer annoCorso;
  private Integer durataAnni;
  private Integer annoAccademico;
  private boolean attivo;
  private boolean laureato;

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

  public boolean isLaureato() {
    return laureato;
  }

  public void setLaureato(boolean laureato) {
    this.laureato = laureato;
  }
}
package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Data Transfer Object for the endpoint GET /api/carriera/badge.
 *
 * This DTO represents the normalized view of a student badge
 * retrieved from Cineca badge-service-v1.
 *
 * It is used exclusively for API responses and does not contain
 * any business logic or persistence behavior.
 *
 * The structure mirrors external Cineca data but is decoupled
 * to ensure internal API stability even if external schemas change.
 */
public class BadgeResponse {

  private Long bdgId;
  private String matricola;
  private String cognome;
  private String nome;
  private String codFis;
  private String codCds;
  private String desCds;
  private String codFac;
  private String desFac;
  private Integer aaIscrAnn;
  private String rfid;
  private String universita;
  private String staStuCod;
  private String dataIni;
  private String dataFin;
  private Boolean frontImagePresent;
  private Boolean rearImagePresent;
  private Long badgeBlbId;

  // ============ Getters | Setters | Bool ============

  public Long getBdgId() { return bdgId; }
  public void setBdgId(Long bdgId) { this.bdgId = bdgId; }

  public String getMatricola() { return matricola; }
  public void setMatricola(String matricola) { this.matricola = matricola; }

  public String getCognome() { return cognome; }
  public void setCognome(String cognome) { this.cognome = cognome; }

  public String getNome() { return nome; }
  public void setNome(String nome) { this.nome = nome; }

  public String getCodFis() { return codFis; }
  public void setCodFis(String codFis) { this.codFis = codFis; }

  public String getCodCds() { return codCds; }
  public void setCodCds(String codCds) { this.codCds = codCds; }

  public String getDesCds() { return desCds; }
  public void setDesCds(String desCds) { this.desCds = desCds; }

  public String getCodFac() { return codFac; }
  public void setCodFac(String codFac) { this.codFac = codFac; }

  public String getDesFac() { return desFac; }
  public void setDesFac(String desFac) { this.desFac = desFac; }

  public Integer getAaIscrAnn() { return aaIscrAnn; }
  public void setAaIscrAnn(Integer aaIscrAnn) { this.aaIscrAnn = aaIscrAnn; }

  public String getRfid() { return rfid; }
  public void setRfid(String rfid) { this.rfid = rfid; }

  public String getUniversita() { return universita; }
  public void setUniversita(String universita) { this.universita = universita; }

  public String getStaStuCod() { return staStuCod; }
  public void setStaStuCod(String staStuCod) { this.staStuCod = staStuCod; }

  public String getDataIni() { return dataIni; }
  public void setDataIni(String dataIni) { this.dataIni = dataIni; }

  public String getDataFin() { return dataFin; }
  public void setDataFin(String dataFin) { this.dataFin = dataFin; }

  public Boolean getFrontImagePresent() { return frontImagePresent; }
  public void setFrontImagePresent(Boolean frontImagePresent) {
    this.frontImagePresent = frontImagePresent;
  }

  public Boolean getRearImagePresent() { return rearImagePresent; }
  public void setRearImagePresent(Boolean rearImagePresent) {
    this.rearImagePresent = rearImagePresent;
  }

  public Long getBadgeBlbId() { return badgeBlbId; }
  public void setBadgeBlbId(Long badgeBlbId) { this.badgeBlbId = badgeBlbId; }
}
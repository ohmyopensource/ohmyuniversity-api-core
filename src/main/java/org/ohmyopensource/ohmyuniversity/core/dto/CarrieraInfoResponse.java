package org.ohmyopensource.ohmyuniversity.core.dto;

/**
 * Response DTO for GET /api/v1/carriera/info.
 * Contains full career information from carriere-service-v1.
 */
public class CarrieraInfoResponse {
  private Long stuId;
  private Long matId;
  private String matricola;
  private Long persId;
  private String nome;
  private String cognome;
  private String codFis;
  private String dataNascita;
  private String sesso;
  private String email;
  private String emailAte;
  private String emailCertificata;
  private String tipoCorsoCod;
  private String tipoCorsoDes;
  private String cdsCod;
  private String cdsDes;
  private String facCod;
  private String facDes;
  private Integer annoCorso;
  private Integer aaIscrId;
  private Integer aaOrdId;
  private String dataImm;
  private String dataIscr;
  private String dataFineCarriera;
  private String staStuCod;
  private String statiStuDes;
  private String profstuDes;
  private Integer ptFlg;
  private Integer sospFlg;
  private Integer attlauFlg;
  private String sediDes;
  private Long sedeId;
  private String userId;

  public Long getStuId() { return stuId; }
  public void setStuId(Long v) { this.stuId = v; }
  public Long getMatId() { return matId; }
  public void setMatId(Long v) { this.matId = v; }
  public String getMatricola() { return matricola; }
  public void setMatricola(String v) { this.matricola = v; }
  public Long getPersId() { return persId; }
  public void setPersId(Long v) { this.persId = v; }
  public String getNome() { return nome; }
  public void setNome(String v) { this.nome = v; }
  public String getCognome() { return cognome; }
  public void setCognome(String v) { this.cognome = v; }
  public String getCodFis() { return codFis; }
  public void setCodFis(String v) { this.codFis = v; }
  public String getDataNascita() { return dataNascita; }
  public void setDataNascita(String v) { this.dataNascita = v; }
  public String getSesso() { return sesso; }
  public void setSesso(String v) { this.sesso = v; }
  public String getEmail() { return email; }
  public void setEmail(String v) { this.email = v; }
  public String getEmailAte() { return emailAte; }
  public void setEmailAte(String v) { this.emailAte = v; }
  public String getEmailCertificata() { return emailCertificata; }
  public void setEmailCertificata(String v) { this.emailCertificata = v; }
  public String getTipoCorsoCod() { return tipoCorsoCod; }
  public void setTipoCorsoCod(String v) { this.tipoCorsoCod = v; }
  public String getTipoCorsoDes() { return tipoCorsoDes; }
  public void setTipoCorsoDes(String v) { this.tipoCorsoDes = v; }
  public String getCdsCod() { return cdsCod; }
  public void setCdsCod(String v) { this.cdsCod = v; }
  public String getCdsDes() { return cdsDes; }
  public void setCdsDes(String v) { this.cdsDes = v; }
  public String getFacCod() { return facCod; }
  public void setFacCod(String v) { this.facCod = v; }
  public String getFacDes() { return facDes; }
  public void setFacDes(String v) { this.facDes = v; }
  public Integer getAnnoCorso() { return annoCorso; }
  public void setAnnoCorso(Integer v) { this.annoCorso = v; }
  public Integer getAaIscrId() { return aaIscrId; }
  public void setAaIscrId(Integer v) { this.aaIscrId = v; }
  public Integer getAaOrdId() { return aaOrdId; }
  public void setAaOrdId(Integer v) { this.aaOrdId = v; }
  public String getDataImm() { return dataImm; }
  public void setDataImm(String v) { this.dataImm = v; }
  public String getDataIscr() { return dataIscr; }
  public void setDataIscr(String v) { this.dataIscr = v; }
  public String getDataFineCarriera() { return dataFineCarriera; }
  public void setDataFineCarriera(String v) { this.dataFineCarriera = v; }
  public String getStaStuCod() { return staStuCod; }
  public void setStaStuCod(String v) { this.staStuCod = v; }
  public String getStatiStuDes() { return statiStuDes; }
  public void setStatiStuDes(String v) { this.statiStuDes = v; }
  public String getProfstuDes() { return profstuDes; }
  public void setProfstuDes(String v) { this.profstuDes = v; }
  public Integer getPtFlg() { return ptFlg; }
  public void setPtFlg(Integer v) { this.ptFlg = v; }
  public Integer getSospFlg() { return sospFlg; }
  public void setSospFlg(Integer v) { this.sospFlg = v; }
  public Integer getAttlauFlg() { return attlauFlg; }
  public void setAttlauFlg(Integer v) { this.attlauFlg = v; }
  public String getSediDes() { return sediDes; }
  public void setSediDes(String v) { this.sediDes = v; }
  public Long getSedeId() { return sedeId; }
  public void setSedeId(Long v) { this.sedeId = v; }
  public String getUserId() { return userId; }
  public void setUserId(String v) { this.userId = v; }
}
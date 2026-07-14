package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/v1/struttura/facolta.
 */
public class StrutturaResponse {

  private Long facId;
  private String istatCod;
  private String facCod;
  private String facDes;
  private String facDesEng;
  private Long ateneoId;
  private String citta;
  private String via;
  private String prov;
  private String cap;
  private String codFis;
  private Integer aaAttId;
  private Integer aaDisId;
  private String urlSitoWeb;
  private Integer webViewFlg;
  private String tel;
  private String fax;
  private String email;
  private String codStatMiur;
  private String csaCod;
  private String sdrTip;
  private String areaDiscCod;
  private String areaDiscDesEng;
  private List<SedeResponse> sedi;
  private List<TipoCorsoStrutturaResponse> tipiCorso;

  // ============ Getters | Setters | Bool ============

  public Long getFacId() {
    return facId;
  }

  public void setFacId(Long facId) {
    this.facId = facId;
  }

  public String getIstatCod() {
    return istatCod;
  }

  public void setIstatCod(String istatCod) {
    this.istatCod = istatCod;
  }

  public String getFacCod() {
    return facCod;
  }

  public void setFacCod(String facCod) {
    this.facCod = facCod;
  }

  public String getFacDes() {
    return facDes;
  }

  public void setFacDes(String facDes) {
    this.facDes = facDes;
  }

  public String getFacDesEng() {
    return facDesEng;
  }

  public void setFacDesEng(String facDesEng) {
    this.facDesEng = facDesEng;
  }

  public Long getAteneoId() {
    return ateneoId;
  }

  public void setAteneoId(Long ateneoId) {
    this.ateneoId = ateneoId;
  }

  public String getCitta() {
    return citta;
  }

  public void setCitta(String citta) {
    this.citta = citta;
  }

  public String getVia() {
    return via;
  }

  public void setVia(String via) {
    this.via = via;
  }

  public String getProv() {
    return prov;
  }

  public void setProv(String prov) {
    this.prov = prov;
  }

  public String getCap() {
    return cap;
  }

  public void setCap(String cap) {
    this.cap = cap;
  }

  public String getCodFis() {
    return codFis;
  }

  public void setCodFis(String codFis) {
    this.codFis = codFis;
  }

  public Integer getAaAttId() {
    return aaAttId;
  }

  public void setAaAttId(Integer aaAttId) {
    this.aaAttId = aaAttId;
  }

  public Integer getAaDisId() {
    return aaDisId;
  }

  public void setAaDisId(Integer aaDisId) {
    this.aaDisId = aaDisId;
  }

  public String getUrlSitoWeb() {
    return urlSitoWeb;
  }

  public void setUrlSitoWeb(String urlSitoWeb) {
    this.urlSitoWeb = urlSitoWeb;
  }

  public Integer getWebViewFlg() {
    return webViewFlg;
  }

  public void setWebViewFlg(Integer webViewFlg) {
    this.webViewFlg = webViewFlg;
  }

  public String getTel() {
    return tel;
  }

  public void setTel(String tel) {
    this.tel = tel;
  }

  public String getFax() {
    return fax;
  }

  public void setFax(String fax) {
    this.fax = fax;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCodStatMiur() {
    return codStatMiur;
  }

  public void setCodStatMiur(String codStatMiur) {
    this.codStatMiur = codStatMiur;
  }

  public String getCsaCod() {
    return csaCod;
  }

  public void setCsaCod(String csaCod) {
    this.csaCod = csaCod;
  }

  public String getSdrTip() {
    return sdrTip;
  }

  public void setSdrTip(String sdrTip) {
    this.sdrTip = sdrTip;
  }

  public String getAreaDiscCod() {
    return areaDiscCod;
  }

  public void setAreaDiscCod(String areaDiscCod) {
    this.areaDiscCod = areaDiscCod;
  }

  public String getAreaDiscDesEng() {
    return areaDiscDesEng;
  }

  public void setAreaDiscDesEng(String areaDiscDesEng) {
    this.areaDiscDesEng = areaDiscDesEng;
  }

  public List<SedeResponse> getSedi() {
    return sedi;
  }

  public void setSedi(List<SedeResponse> sedi) {
    this.sedi = sedi;
  }

  public List<TipoCorsoStrutturaResponse> getTipiCorso() {
    return tipiCorso;
  }

  public void setTipiCorso(List<TipoCorsoStrutturaResponse> tipiCorso) {
    this.tipiCorso = tipiCorso;
  }
}
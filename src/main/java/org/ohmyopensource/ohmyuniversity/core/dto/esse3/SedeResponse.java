package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Response DTO for GET /api/v1/struttura/sedi/{sedeId}, and nested inside StrutturaResponse.
 */
public class SedeResponse {

  private Long sedeId;
  private Long ateneoId;
  private String sedeDes;
  private String sedeDesEng;
  private String cap;
  private String via;
  private String direttore;
  private String citta;
  private String cistra;
  private String istatCod;
  private String tel;
  private String fax;
  private String email;
  private String urlSitoWeb;
  private Long facId;
  private Integer defAmmFlg;

  // ============ Getters | Setters | Bool ============

  public Long getSedeId() {
    return sedeId;
  }

  public void setSedeId(Long sedeId) {
    this.sedeId = sedeId;
  }

  public Long getFacId() {
    return facId;
  }

  public void setFacId(Long facId) {
    this.facId = facId;
  }

  public Integer getDefAmmFlg() {
    return defAmmFlg;
  }

  public void setDefAmmFlg(Integer defAmmFlg) {
    this.defAmmFlg = defAmmFlg;
  }

  public Long getAteneoId() {
    return ateneoId;
  }

  public void setAteneoId(Long ateneoId) {
    this.ateneoId = ateneoId;
  }

  public String getSedeDes() {
    return sedeDes;
  }

  public void setSedeDes(String sedeDes) {
    this.sedeDes = sedeDes;
  }

  public String getSedeDesEng() {
    return sedeDesEng;
  }

  public void setSedeDesEng(String sedeDesEng) {
    this.sedeDesEng = sedeDesEng;
  }

  public String getCap() {
    return cap;
  }

  public void setCap(String cap) {
    this.cap = cap;
  }

  public String getVia() {
    return via;
  }

  public void setVia(String via) {
    this.via = via;
  }

  public String getDirettore() {
    return direttore;
  }

  public void setDirettore(String direttore) {
    this.direttore = direttore;
  }

  public String getCitta() {
    return citta;
  }

  public void setCitta(String citta) {
    this.citta = citta;
  }

  public String getCistra() {
    return cistra;
  }

  public void setCistra(String cistra) {
    this.cistra = cistra;
  }

  public String getIstatCod() {
    return istatCod;
  }

  public void setIstatCod(String istatCod) {
    this.istatCod = istatCod;
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

  public String getUrlSitoWeb() {
    return urlSitoWeb;
  }

  public void setUrlSitoWeb(String urlSitoWeb) {
    this.urlSitoWeb = urlSitoWeb;
  }
}
package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Degree program type offered under a faculty/department, nested inside StrutturaResponse.
 */
public class TipoCorsoStrutturaResponse {

  private Long cdsId;
  private Long facId;
  private String tipoCorsoCod;
  private String tipoCorsoDes;

  // ============ Getters | Setters | Bool ============

  public Long getCdsId() {
    return cdsId;
  }

  public void setCdsId(Long cdsId) {
    this.cdsId = cdsId;
  }

  public Long getFacId() {
    return facId;
  }

  public void setFacId(Long facId) {
    this.facId = facId;
  }

  public String getTipoCorsoCod() {
    return tipoCorsoCod;
  }

  public void setTipoCorsoCod(String tipoCorsoCod) {
    this.tipoCorsoCod = tipoCorsoCod;
  }

  public String getTipoCorsoDes() {
    return tipoCorsoDes;
  }

  public void setTipoCorsoDes(String tipoCorsoDes) {
    this.tipoCorsoDes = tipoCorsoDes;
  }
}
package org.ohmyopensource.ohmyuniversity.core.cineca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO mapping the response from Cineca ESSE3 GET /login.
 *
 * Only the fields we actually use are mapped — all unknown fields
 * are ignored via {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinecaLoginResponse {

  @JsonProperty("authToken")
  private String authToken;

  @JsonProperty("jwt")
  private String jwt;

  @JsonProperty("user")
  private CinecaUser user;

  public String getAuthToken() {
    return authToken;
  }

  public String getJwt() {
    return jwt;
  }

  public CinecaUser getUser() {
    return user;
  }

  // ================================
  // Nested DTOs
  // ================================

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaUser {

    @JsonProperty("codFis")
    private String codiceFiscale;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("persId")
    private Long persId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("trattiCarriera")
    private List<TrattoCarriera> trattiCarriera;

    public String getCodiceFiscale() { return codiceFiscale; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Long getPersId() { return persId; }
    public String getUserId() { return userId; }
    public List<TrattoCarriera> getTrattiCarriera() { return trattiCarriera; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class TrattoCarriera {

    @JsonProperty("stuId")
    private Long stuId;

    @JsonProperty("matId")
    private Long matId;

    @JsonProperty("matricola")
    private String matricola;

    @JsonProperty("cdsId")
    private Long cdsId;

    @JsonProperty("cdsDes")
    private String cdsDes;

    @JsonProperty("staStuCod")
    private String staStuCod;

    @JsonProperty("staStuDes")
    private String staStuDes;

    @JsonProperty("staMatCod")
    private String staMatCod;

    @JsonProperty("staMatDes")
    private String staMatDes;

    @JsonProperty("dettaglioTratto")
    private DettaglioTratto dettaglioTratto;

    public Long getStuId() { return stuId; }
    public Long getMatId() { return matId; }
    public String getMatricola() { return matricola; }
    public Long getCdsId() { return cdsId; }
    public String getCdsDes() { return cdsDes; }
    public String getStaStuCod() { return staStuCod; }
    public String getStaStuDes() { return staStuDes; }
    public String getStaMatCod() { return staMatCod; }
    public String getStaMatDes() { return staMatDes; }
    public DettaglioTratto getDettaglioTratto() { return dettaglioTratto; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DettaglioTratto {

    @JsonProperty("cdsCod")
    private String cdsCod;

    @JsonProperty("tipoCorsoCod")
    private String tipoCorsoCod;

    @JsonProperty("annoCorso")
    private Integer annoCorso;

    @JsonProperty("durataAnni")
    private Integer durataAnni;

    @JsonProperty("aaIscrId")
    private Integer aaIscrId;

    @JsonProperty("ultimoAnnoFlg")
    private Integer ultimoAnnoFlg;

    @JsonProperty("facCod")
    private String facCod;

    public String getCdsCod() { return cdsCod; }
    public String getTipoCorsoCod() { return tipoCorsoCod; }
    public Integer getAnnoCorso() { return annoCorso; }
    public Integer getDurataAnni() { return durataAnni; }
    public Integer getAaIscrId() { return aaIscrId; }
    public Integer getUltimoAnnoFlg() { return ultimoAnnoFlg; }
    public String getFacCod() { return facCod; }
  }
}
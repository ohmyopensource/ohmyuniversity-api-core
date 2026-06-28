package org.ohmyopensource.ohmyuniversity.core.cineca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO mapping the response from Cineca ESSE3 GET /login.
 *
 * <p>This is the root authentication response returned by Cineca after a successful login. It is a
 * pure integration-layer DTO and must NOT contain business logic.
 *
 * <p>It includes:
 * - authToken: session-based authentication token (legacy ESSE3 session mechanism)
 * - jwt: Cineca-issued JWT used for stateless authentication
 * - user: authenticated user profile containing personal data and career tracks
 *
 * <p>All unknown fields returned by Cineca are ignored via:
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 *
 * <p>This object is strictly transient and must never be persisted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinecaLoginResponse {

  @JsonProperty("authToken")
  private String authToken;

  @JsonProperty("jwt")
  private String jwt;

  @JsonProperty("user")
  private CinecaUser user;

  // ============ Getters | Setters | Bool ============

  public String getAuthToken() {
    return authToken;
  }

  public String getJwt() {
    return jwt;
  }

  public CinecaUser getUser() {
    return user;
  }

  /**
   * Represents the authenticated user returned by Cineca ESSE3 login.
   *
   * <p>This is NOT a local application user, but a remote identity coming directly from the
   * university system.
   *
   * <p>It contains personal data and academic career tracks.
   */
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

    // ============ Getters | Setters | Bool ============

    public String getCodiceFiscale() {
      return codiceFiscale;
    }

    public String getFirstName() {
      return firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public Long getPersId() {
      return persId;
    }

    public String getUserId() {
      return userId;
    }

    public List<TrattoCarriera> getTrattiCarriera() {
      return trattiCarriera;
    }
  }

  /**
   * Represents a single academic career track (enrollment) in Cineca ESSE3.
   *
   * <p>A user can have multiple tracks:
   * - different degree programs
   * - multiple enrollments over time
   *
   * <p>Each track contains identifiers used to build internal application profiles.
   */
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

    @JsonProperty("attlauFlg")
    private Integer attlauFlg;

    @JsonProperty("dettaglioTratto")
    private DettaglioTratto dettaglioTratto;

    // ============ Getters | Setters | Bool ============

    public Long getStuId() {
      return stuId;
    }

    public Long getMatId() {
      return matId;
    }

    public String getMatricola() {
      return matricola;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public String getCdsDes() {
      return cdsDes;
    }

    public String getStaStuCod() {
      return staStuCod;
    }

    public String getStaStuDes() {
      return staStuDes;
    }

    public String getStaMatCod() {
      return staMatCod;
    }

    public String getStaMatDes() {
      return staMatDes;
    }

    public Integer getAttlauFlg() { return attlauFlg; }

    public DettaglioTratto getDettaglioTratto() {
      return dettaglioTratto;
    }
  }

  /**
   * Contains detailed metadata about the degree program within a career track.
   *
   * <p>This includes structural academic information such as:
   * - course code
   * - course type
   * - academic year progression
   * - duration of the program
   */
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

    // ============ Getters | Setters | Bool ============

    public String getCdsCod() {
      return cdsCod;
    }

    public String getTipoCorsoCod() {
      return tipoCorsoCod;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public Integer getDurataAnni() {
      return durataAnni;
    }

    public Integer getAaIscrId() {
      return aaIscrId;
    }

    public Integer getUltimoAnnoFlg() {
      return ultimoAnnoFlg;
    }

    public String getFacCod() {
      return facCod;
    }
  }
}
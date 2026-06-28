package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca ESSE3 profile-related services.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code anagrafica-service-v2} — persona data and avatar</li>
 *   <li>{@code carriere-service-v1} — career metadata</li>
 *   <li>{@code badge-service-v1} — university badge</li>
 * </ul>
 */
@Component
public class CinecaProfileClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaProfileClient.class);

  // ============ Class Methods ============

  /**
   * Retrieves personal data for a student from {@code anagrafica-service-v2}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param persId  Cineca person identifier
   * @return persona data, or {@code null} if not found
   */
  public CinecaPersona getPersona(String baseUrl, String jwt, Long persId) {
    log.debug("CinecaProfileClient: GET persona persId={}", persId);
    return webClient.get()
        .uri(baseUrl + "/anagrafica-service-v2/persone/" + persId + "/")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for persona persId=" + persId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on persona")))
        .bodyToMono(CinecaPersona.class)
        .block();
  }

  /**
   * Retrieves the profile avatar bytes from {@code anagrafica-service-v2}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param persId  Cineca person identifier
   * @return JPEG image bytes, or {@code null} if not available
   */
  public byte[] getAvatar(String baseUrl, String jwt, Long persId) {
    log.debug("CinecaProfileClient: GET avatar persId={}", persId);
    return webClient.get()
        .uri(baseUrl + "/anagrafica-service-v2/persone/" + persId + "/foto")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for avatar persId=" + persId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on avatar")))
        .bodyToMono(byte[].class)
        .block();
  }

  /**
   * Retrieves career metadata from {@code carriere-service-v1}.
   *
   * <p>Returns all careers for the authenticated user; the caller is
   * responsible for filtering by {@code matId} to select the active one.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param matId   career segment identifier used to select the correct career
   * @return matching career, or {@code null} if none found
   */
  public CinecaCarriera getCarriera(String baseUrl, String jwt, Long matId) {
    log.debug("CinecaProfileClient: GET carriera matId={}", matId);
    List<CinecaCarriera> result = webClient.get()
        .uri(baseUrl + "/carriere-service-v1/carriere"
            + "?optionalFields=tipoCorsoCod,tipoCorsoDes")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException("Unauthorized for carriera")))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on carriera")))
        .bodyToFlux(CinecaCarriera.class)
        .collectList()
        .block();

    if (result == null || result.isEmpty()) {
      return null;
    }

    return result.stream()
        .filter(c -> matId != null && matId.equals(c.getMatId()))
        .findFirst()
        .orElseGet(() -> result.stream()
            .filter(c -> "A".equals(c.getStaStuCod()))
            .findFirst()
            .orElse(result.get(0)));
  }

  /**
   * Retrieves all career entries from {@code carriere-service-v1} without filtering.
   *
   * <p>Used at login time to enrich career profiles with fields not available
   * in the login response (e.g. {@code attlauFlg}).
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @return list of all career entries; empty if none available
   */
  public List<CinecaCarriera> getAllCarriere(String baseUrl, String jwt) {
    log.debug("CinecaProfileClient: GET all carriere");
    List<CinecaCarriera> result = webClient.get()
        .uri(baseUrl + "/carriere-service-v1/carriere"
            + "?optionalFields=tipoCorsoCod,tipoCorsoDes,attlauFlg")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException("Unauthorized for carriere")))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on carriere")))
        .bodyToFlux(CinecaCarriera.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves the university badge for a student from {@code badge-service-v1}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return list of badges; empty if none associated
   */
  public List<CinecaBadge> getBadges(String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaProfileClient: GET badges stuId={}", stuId);
    List<CinecaBadge> result = webClient.get()
        .uri(baseUrl + "/badge-service-v1/badges?stuId=" + stuId)
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException("Unauthorized for badge")))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on badge")))
        .bodyToFlux(CinecaBadge.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  // ============ DTOs ============

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPersona {

    @JsonProperty("persId")
    private Long persId;
    @JsonProperty("nome")
    private String nome;
    @JsonProperty("cognome")
    private String cognome;
    @JsonProperty("codFis")
    private String codFis;
    @JsonProperty("dataNascita")
    private String dataNascita;
    @JsonProperty("sesso")
    private String sesso;
    @JsonProperty("comuNascDes")
    private String comuNascDes;
    @JsonProperty("comuNascSigla")
    private String comuNascSigla;
    @JsonProperty("provNascDes")
    private String provNascDes;
    @JsonProperty("desCittadinanza")
    private String desCittadinanza;
    @JsonProperty("email")
    private String email;
    @JsonProperty("emailAte")
    private String emailAte;
    @JsonProperty("emailCertificata")
    private String emailCertificata;
    @JsonProperty("cellulare")
    private String cellulare;
    @JsonProperty("telRes")
    private String telRes;
    @JsonProperty("viaRes")
    private String viaRes;
    @JsonProperty("numCivRes")
    private String numCivRes;
    @JsonProperty("capRes")
    private String capRes;
    @JsonProperty("comuResDes")
    private String comuResDes;
    @JsonProperty("comuResSigla")
    private String comuResSigla;
    @JsonProperty("provResDes")
    private String provResDes;
    @JsonProperty("naziResDes")
    private String naziResDes;
    @JsonProperty("viaDom")
    private String viaDom;
    @JsonProperty("numCivDom")
    private String numCivDom;
    @JsonProperty("capDom")
    private String capDom;
    @JsonProperty("comuDomDes")
    private String comuDomDes;
    @JsonProperty("comuDomSigla")
    private String comuDomSigla;
    @JsonProperty("naziDomDes")
    private String naziDomDes;
    @JsonProperty("domComeResFlg")
    private Integer domComeResFlg;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("statoCivileDes")
    private String statoCivileDes;
    @JsonProperty("professione")
    private String professione;
    @JsonProperty("emergNome")
    private String emergNome;
    @JsonProperty("emergCognome")
    private String emergCognome;
    @JsonProperty("emergTel")
    private String emergTel;
    @JsonProperty("emergEmail")
    private String emergEmail;
    @JsonProperty("emergRapporto")
    private String emergRapporto;

    public Long getPersId() {
      return persId;
    }

    public String getNome() {
      return nome;
    }

    public String getCognome() {
      return cognome;
    }

    public String getCodFis() {
      return codFis;
    }

    public String getDataNascita() {
      return dataNascita;
    }

    public String getSesso() {
      return sesso;
    }

    public String getComuNascDes() {
      return comuNascDes;
    }

    public String getComuNascSigla() {
      return comuNascSigla;
    }

    public String getProvNascDes() {
      return provNascDes;
    }

    public String getDesCittadinanza() {
      return desCittadinanza;
    }

    public String getEmail() {
      return email;
    }

    public String getEmailAte() {
      return emailAte;
    }

    public String getEmailCertificata() {
      return emailCertificata;
    }

    public String getCellulare() {
      return cellulare;
    }

    public String getTelRes() {
      return telRes;
    }

    public String getViaRes() {
      return viaRes;
    }

    public String getNumCivRes() {
      return numCivRes;
    }

    public String getCapRes() {
      return capRes;
    }

    public String getComuResDes() {
      return comuResDes;
    }

    public String getComuResSigla() {
      return comuResSigla;
    }

    public String getProvResDes() {
      return provResDes;
    }

    public String getNaziResDes() {
      return naziResDes;
    }

    public String getViaDom() {
      return viaDom;
    }

    public String getNumCivDom() {
      return numCivDom;
    }

    public String getCapDom() {
      return capDom;
    }

    public String getComuDomDes() {
      return comuDomDes;
    }

    public String getComuDomSigla() {
      return comuDomSigla;
    }

    public String getNaziDomDes() {
      return naziDomDes;
    }

    public Integer getDomComeResFlg() {
      return domComeResFlg;
    }

    public String getUserId() {
      return userId;
    }

    public String getStatoCivileDes() {
      return statoCivileDes;
    }

    public String getProfessione() {
      return professione;
    }

    public String getEmergNome() {
      return emergNome;
    }

    public String getEmergCognome() {
      return emergCognome;
    }

    public String getEmergTel() {
      return emergTel;
    }

    public String getEmergEmail() {
      return emergEmail;
    }

    public String getEmergRapporto() {
      return emergRapporto;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCarriera {

    @JsonProperty("stuId")
    private Long stuId;
    @JsonProperty("matId")
    private Long matId;
    @JsonProperty("matricola")
    private String matricola;
    @JsonProperty("persId")
    private Long persId;
    @JsonProperty("nome")
    private String nome;
    @JsonProperty("cognome")
    private String cognome;
    @JsonProperty("codFis")
    private String codFis;
    @JsonProperty("dataNascita")
    private String dataNascita;
    @JsonProperty("sesso")
    private String sesso;
    @JsonProperty("email")
    private String email;
    @JsonProperty("emailAte")
    private String emailAte;
    @JsonProperty("emailCertificata")
    private String emailCertificata;
    @JsonProperty("tipoCorsoCod")
    private String tipoCorsoCod;
    @JsonProperty("tipoCorsoDes")
    private String tipoCorsoDes;
    @JsonProperty("p06CdsCod")
    private String cdsCod;
    @JsonProperty("p06CdsDes")
    private String cdsDes;
    @JsonProperty("facCod")
    private String facCod;
    @JsonProperty("facDes")
    private String facDes;
    @JsonProperty("annoCorso")
    private Integer annoCorso;
    @JsonProperty("aaIscrId")
    private Integer aaIscrId;
    @JsonProperty("aaOrdId")
    private Integer aaOrdId;
    @JsonProperty("dataImm")
    private String dataImm;
    @JsonProperty("dataIscr")
    private String dataIscr;
    @JsonProperty("dataFineCarriera")
    private String dataFineCarriera;
    @JsonProperty("staStuCod")
    private String staStuCod;
    @JsonProperty("statiStuDes")
    private String statiStuDes;
    @JsonProperty("profstuDes")
    private String profstuDes;
    @JsonProperty("ptFlg")
    private Integer ptFlg;
    @JsonProperty("sospFlg")
    private Integer sospFlg;
    @JsonProperty("attlauFlg")
    private Integer attlauFlg;
    @JsonProperty("sediDes")
    private String sediDes;
    @JsonProperty("sedeId")
    private Long sedeId;
    @JsonProperty("userId")
    private String userId;

    public Long getStuId() {
      return stuId;
    }

    public Long getMatId() {
      return matId;
    }

    public String getMatricola() {
      return matricola;
    }

    public Long getPersId() {
      return persId;
    }

    public String getNome() {
      return nome;
    }

    public String getCognome() {
      return cognome;
    }

    public String getCodFis() {
      return codFis;
    }

    public String getDataNascita() {
      return dataNascita;
    }

    public String getSesso() {
      return sesso;
    }

    public String getEmail() {
      return email;
    }

    public String getEmailAte() {
      return emailAte;
    }

    public String getEmailCertificata() {
      return emailCertificata;
    }

    public String getTipoCorsoCod() {
      return tipoCorsoCod;
    }

    public String getTipoCorsoDes() {
      return tipoCorsoDes;
    }

    public String getCdsCod() {
      return cdsCod;
    }

    public String getCdsDes() {
      return cdsDes;
    }

    public String getFacCod() {
      return facCod;
    }

    public String getFacDes() {
      return facDes;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public Integer getAaIscrId() {
      return aaIscrId;
    }

    public Integer getAaOrdId() {
      return aaOrdId;
    }

    public String getDataImm() {
      return dataImm;
    }

    public String getDataIscr() {
      return dataIscr;
    }

    public String getDataFineCarriera() {
      return dataFineCarriera;
    }

    public String getStaStuCod() {
      return staStuCod;
    }

    public String getStatiStuDes() {
      return statiStuDes;
    }

    public String getProfstuDes() {
      return profstuDes;
    }

    public Integer getPtFlg() {
      return ptFlg;
    }

    public Integer getSospFlg() {
      return sospFlg;
    }

    public Integer getAttlauFlg() {
      return attlauFlg;
    }

    public String getSediDes() {
      return sediDes;
    }

    public Long getSedeId() {
      return sedeId;
    }

    public String getUserId() {
      return userId;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaBadge {

    @JsonProperty("bdgId")
    private Long bdgId;
    @JsonProperty("matricola")
    private String matricola;
    @JsonProperty("cognome")
    private String cognome;
    @JsonProperty("nome")
    private String nome;
    @JsonProperty("codFis")
    private String codFis;
    @JsonProperty("codCds")
    private String codCds;
    @JsonProperty("desCds")
    private String desCds;
    @JsonProperty("codFac")
    private String codFac;
    @JsonProperty("desFac")
    private String desFac;
    @JsonProperty("aaIscrAnn")
    private Integer aaIscrAnn;
    @JsonProperty("rfid")
    private String rfid;
    @JsonProperty("universita")
    private String universita;
    @JsonProperty("staStuCod")
    private String staStuCod;
    @JsonProperty("dataIni")
    private String dataIni;
    @JsonProperty("dataFin")
    private String dataFin;
    @JsonProperty("frontImagePresent")
    private Integer frontImagePresent;
    @JsonProperty("rearImagePresent")
    private Integer rearImagePresent;
    @JsonProperty("badgeBlbId")
    private Long badgeBlbId;

    public Long getBdgId() {
      return bdgId;
    }

    public String getMatricola() {
      return matricola;
    }

    public String getCognome() {
      return cognome;
    }

    public String getNome() {
      return nome;
    }

    public String getCodFis() {
      return codFis;
    }

    public String getCodCds() {
      return codCds;
    }

    public String getDesCds() {
      return desCds;
    }

    public String getCodFac() {
      return codFac;
    }

    public String getDesFac() {
      return desFac;
    }

    public Integer getAaIscrAnn() {
      return aaIscrAnn;
    }

    public String getRfid() {
      return rfid;
    }

    public String getUniversita() {
      return universita;
    }

    public String getStaStuCod() {
      return staStuCod;
    }

    public String getDataIni() {
      return dataIni;
    }

    public String getDataFin() {
      return dataFin;
    }

    public Integer getFrontImagePresent() {
      return frontImagePresent;
    }

    public Integer getRearImagePresent() {
      return rearImagePresent;
    }

    public Long getBadgeBlbId() {
      return badgeBlbId;
    }
  }
}
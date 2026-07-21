package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.exception.CinecaUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca ESSE3 {@code struttura-service-v1}.
 *
 * <p>Covers faculties/departments and their physical locations (sedi):
 * <ul>
 *   <li>{@code /strutture} — all faculties/departments</li>
 *   <li>{@code /strutture/{facId}/} — single faculty/department detail</li>
 *   <li>{@code /sedi/{sedeId}} — single location detail</li>
 * </ul>
 *
 * <p>Every endpoint in this domain is {@code common-public-permissions} (kind
 * {@code ALL}) — no authentication is actually required by Cineca. The JWT is
 * still passed for consistency with the rest of the client layer and in case
 * a given university's deployment tightens this in the future.
 */
@Component
public class CinecaStrutturaClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaStrutturaClient.class);

  private static final String STRUTTURA_BASE = "/struttura-service-v1";

  // ============ Class Methods ============

  /**
   * Retrieves all faculties/departments for the university.
   *
   * <p>Requests {@code optionalFields=ALL} to maximize the returned data — the
   * base response omits several fields by default per Cineca convention.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @return list of faculties/departments; empty if none available
   */
  public List<CinecaStruttura> getStrutture(String baseUrl, String jwt) {
    log.debug("CinecaStrutturaClient: GET strutture");
    List<CinecaStruttura> result = webClient.get()
        .uri(baseUrl + STRUTTURA_BASE + "/strutture?optionalFields=ALL")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on strutture")))
        .bodyToFlux(CinecaStruttura.class)
        .collectList()
        .onErrorResume(e -> e instanceof CinecaUnavailableException
            ? Mono.error(e)
            : Mono.just(List.of()))
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves a single faculty/department by ID, with all optional fields.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param facId   faculty/department identifier
   * @return faculty/department detail, or {@code null} if not found
   */
  public CinecaStruttura getStruttura(String baseUrl, String jwt, Long facId) {
    log.debug("CinecaStrutturaClient: GET struttura facId={}", facId);
    return webClient.get()
        .uri(baseUrl + STRUTTURA_BASE + "/strutture/" + facId + "/?optionalFields=ALL")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on struttura detail")))
        .bodyToMono(CinecaStruttura.class)
        .onErrorResume(e -> e instanceof CinecaUnavailableException
            ? Mono.error(e)
            : Mono.empty())
        .block();
  }

  /**
   * Retrieves a single location (sede) by ID, with all optional fields.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param sedeId  location identifier
   * @return location detail, or {@code null} if not found
   */
  public CinecaSede getSede(String baseUrl, String jwt, Long sedeId) {
    log.debug("CinecaStrutturaClient: GET sede sedeId={}", sedeId);
    return webClient.get()
        .uri(baseUrl + STRUTTURA_BASE + "/sedi/" + sedeId + "?optionalFields=ALL")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on sede detail")))
        .bodyToMono(CinecaSede.class)
        .onErrorResume(e -> e instanceof CinecaUnavailableException
            ? Mono.error(e)
            : Mono.empty())
        .block();
  }

  // ============ DTOs ============

  /**
   * Maps {@code StrutturaDidattica} (faculty/department). Fields per the Cineca OpenAPI schema;
   * {@code @JsonIgnoreProperties(ignoreUnknown = true)} means any field present in a real response
   * but not yet listed here is silently skipped rather than causing a parse failure — TODO: verify
   * against a real payload (same approach used for the Course Catalogue / attendance
   * investigations) to confirm nothing is missing, since the source documentation was truncated
   * around {@code areaDiscCod}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaStruttura {

    @JsonProperty("facId")
    private Long facId;
    @JsonProperty("istatCod")
    private String istatCod;
    @JsonProperty("facCod")
    private String facCod;
    @JsonProperty("facDes")
    private String facDes;
    @JsonProperty("facDesEng")
    private String facDesEng;
    @JsonProperty("ateneoId")
    private Long ateneoId;
    @JsonProperty("citta")
    private String citta;
    @JsonProperty("via")
    private String via;
    @JsonProperty("prov")
    private String prov;
    @JsonProperty("cap")
    private String cap;
    @JsonProperty("codFis")
    private String codFis;
    @JsonProperty("aaAttId")
    private Integer aaAttId;
    @JsonProperty("aaDisId")
    private Integer aaDisId;
    @JsonProperty("urlSitoWeb")
    private String urlSitoWeb;
    @JsonProperty("webViewFlg")
    private Integer webViewFlg;
    @JsonProperty("tel")
    private String tel;
    @JsonProperty("fax")
    private String fax;
    @JsonProperty("email")
    private String email;
    @JsonProperty("codStatMiur")
    private String codStatMiur;
    @JsonProperty("csaCod")
    private String csaCod;
    @JsonProperty("sdrTip")
    private String sdrTip;
    @JsonProperty("areaDiscCod")
    private String areaDiscCod;
    @JsonProperty("areaDiscDesEng")
    private String areaDiscDesEng;
    @JsonProperty("sediStruttura")
    private List<CinecaSede> sediStruttura;
    @JsonProperty("tipiCorsoStruttura")
    private List<CinecaTipoCorsoStruttura> tipiCorsoStruttura;

    public Long getFacId() {
      return facId;
    }

    public String getIstatCod() {
      return istatCod;
    }

    public String getFacCod() {
      return facCod;
    }

    public String getFacDes() {
      return facDes;
    }

    public String getFacDesEng() {
      return facDesEng;
    }

    public Long getAteneoId() {
      return ateneoId;
    }

    public String getCitta() {
      return citta;
    }

    public String getVia() {
      return via;
    }

    public String getProv() {
      return prov;
    }

    public String getCap() {
      return cap;
    }

    public String getCodFis() {
      return codFis;
    }

    public Integer getAaAttId() {
      return aaAttId;
    }

    public Integer getAaDisId() {
      return aaDisId;
    }

    public String getUrlSitoWeb() {
      return urlSitoWeb;
    }

    public Integer getWebViewFlg() {
      return webViewFlg;
    }

    public String getTel() {
      return tel;
    }

    public String getFax() {
      return fax;
    }

    public String getEmail() {
      return email;
    }

    public String getCodStatMiur() {
      return codStatMiur;
    }

    public String getCsaCod() {
      return csaCod;
    }

    public String getSdrTip() {
      return sdrTip;
    }

    public String getAreaDiscCod() {
      return areaDiscCod;
    }

    public String getAreaDiscDesEng() {
      return areaDiscDesEng;
    }

    public List<CinecaSede> getSediStruttura() {
      return sediStruttura != null ? sediStruttura : List.of();
    }

    public List<CinecaTipoCorsoStruttura> getTipiCorsoStruttura() {
      return tipiCorsoStruttura != null ? tipiCorsoStruttura : List.of();
    }
  }

  /**
   * Maps {@code Sede} (physical location). Same verification caveat as {@link CinecaStruttura} —
   * documentation truncated around {@code urlSitoWeb}.
   *
   * <p>Fields sedeId/sedeDes/sedeDesEng/defAmmFlg/facId confirmed present when nested
   * inside CinecaStruttura.sediStruttura (real payload, UNIMOL). The remaining fields
   * (tel/fax/email/via/direttore/etc.) are per the OpenAPI schema for the standalone GET
   * /sedi/{sedeId} endpoint but NOT YET verified against a real response
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSede {

    @JsonProperty("sedeId")
    private Long sedeId;
    @JsonProperty("facId")
    private Long facId;
    @JsonProperty("defAmmFlg")
    private Integer defAmmFlg;
    @JsonProperty("ateneoId")
    private Long ateneoId;
    @JsonProperty("sedeDes")
    private String sedeDes;
    @JsonProperty("sedeDesEng")
    private String sedeDesEng;
    @JsonProperty("cap")
    private String cap;
    @JsonProperty("via")
    private String via;
    @JsonProperty("direttore")
    private String direttore;
    @JsonProperty("citta")
    private String citta;
    @JsonProperty("cistra")
    private String cistra;
    @JsonProperty("istatCod")
    private String istatCod;
    @JsonProperty("tel")
    private String tel;
    @JsonProperty("fax")
    private String fax;
    @JsonProperty("email")
    private String email;
    @JsonProperty("urlSitoWeb")
    private String urlSitoWeb;

    public Long getSedeId() {
      return sedeId;
    }

    public Long getFacId() {
      return facId;
    }

    public Integer getDefAmmFlg() {
      return defAmmFlg;
    }

    public Long getAteneoId() {
      return ateneoId;
    }

    public String getSedeDes() {
      return sedeDes;
    }

    public String getSedeDesEng() {
      return sedeDesEng;
    }

    public String getCap() {
      return cap;
    }

    public String getVia() {
      return via;
    }

    public String getDirettore() {
      return direttore;
    }

    public String getCitta() {
      return citta;
    }

    public String getCistra() {
      return cistra;
    }

    public String getIstatCod() {
      return istatCod;
    }

    public String getTel() {
      return tel;
    }

    public String getFax() {
      return fax;
    }

    public String getEmail() {
      return email;
    }

    public String getUrlSitoWeb() {
      return urlSitoWeb;
    }
  }

  /**
   * Degree program type offered under a given faculty/department.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaTipoCorsoStruttura {

    @JsonProperty("cdsId")
    private Long cdsId;
    @JsonProperty("facId")
    private Long facId;
    @JsonProperty("tipoCorsoCod")
    private String tipoCorsoCod;
    @JsonProperty("tipoCorsoDes")
    private String tipoCorsoDes;

    public Long getCdsId() {
      return cdsId;
    }

    public Long getFacId() {
      return facId;
    }

    public String getTipoCorsoCod() {
      return tipoCorsoCod;
    }

    public String getTipoCorsoDes() {
      return tipoCorsoDes;
    }
  }
}
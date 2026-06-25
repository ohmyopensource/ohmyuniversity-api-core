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
 * HTTP client for Cineca ESSE3 internship-related services.
 *
 * <p>Covers {@code tirocini-service-v1}:
 * <ul>
 *   <li>GET /tirocini/{stuId}/domande — list of internship applications</li>
 * </ul>
 *
 * <p>Accessible with STUDENTE role via {@code checkStuId}.
 */
@Component
public class CinecaInternshipsClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaInternshipsClient.class);

  /**
   * Retrieves the list of internship applications for a student.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student career identifier
   * @return list of internship applications; empty if none
   */
  public List<CinecaInternshipApplication> getApplications(
      String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaInternshipsClient: GET applications stuId={}", stuId);
    List<CinecaInternshipApplication> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "tirocini-service-v1",
                "tirocini", stuId.toString(), "domande")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaInternshipsClient: applications 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for internship applications stuId=" + stuId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on internship applications")))
        .bodyToFlux(CinecaInternshipApplication.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  // ============ DTOs ============

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaInternshipApplication {

    @JsonProperty("domTiroId")
    private Long domTiroId;

    @JsonProperty("stuId")
    private Long stuId;

    @JsonProperty("domTiroPrg")
    private Long domTiroPrg;

    @JsonProperty("aaId")
    private Long aaId;

    @JsonProperty("statoDomTiroCod")
    private String statoDomTiroCod;

    @JsonProperty("statoDomTiroDes")
    private String statoDomTiroDes;

    @JsonProperty("tipoTirocCod")
    private String tipoTirocCod;

    @JsonProperty("tipoTirocDes")
    private String tipoTirocDes;

    @JsonProperty("enteId")
    private Long enteId;

    @JsonProperty("enteDes")
    private String enteDes;

    @JsonProperty("piva")
    private String piva;

    @JsonProperty("cf")
    private String cf;

    @JsonProperty("oppTitolo")
    private String oppTitolo;

    @JsonProperty("oppDes")
    private String oppDes;

    @JsonProperty("dataIniTiro")
    private String startDate;

    @JsonProperty("durataMesi")
    private Integer durationMonths;

    @JsonProperty("abilRicCfu")
    private Integer abilRicCfu;

    @JsonProperty("candVisEnteFlg")
    private Integer candVisEnteFlg;

    public Long getDomTiroId() { return domTiroId; }
    public Long getStuId() { return stuId; }
    public Long getDomTiroPrg() { return domTiroPrg; }
    public Long getAaId() { return aaId; }
    public String getStatoDomTiroCod() { return statoDomTiroCod; }
    public String getStatoDomTiroDes() { return statoDomTiroDes; }
    public String getTipoTirocCod() { return tipoTirocCod; }
    public String getTipoTirocDes() { return tipoTirocDes; }
    public Long getEnteId() { return enteId; }
    public String getEnteDes() { return enteDes; }
    public String getPiva() { return piva; }
    public String getCf() { return cf; }
    public String getOppTitolo() { return oppTitolo; }
    public String getOppDes() { return oppDes; }
    public String getStartDate() { return startDate; }
    public Integer getDurationMonths() { return durationMonths; }
    public Integer getAbilRicCfu() { return abilRicCfu; }
    public Integer getCandVisEnteFlg() { return candVisEnteFlg; }
  }
}
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
 * HTTP client for Cineca ESSE3 career-related services.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code libretto-service-v2} — transcript rows and grade averages</li>
 *   <li>{@code piani-service-v1} — study plan headers and details</li>
 * </ul>
 *
 * <p>All endpoints in this client are accessible with STUDENTE role via Bearer JWT.
 */
@Component
public class CinecaCareerClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaCareerClient.class);

  // ============ Class Methods ============

  /**
   * Retrieves all transcript rows from {@code libretto-service-v2}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param matId   student career segment identifier
   * @return list of transcript rows; empty if none available
   */
  public List<CinecaTranscriptRow> getTranscript(String baseUrl, String jwt, Long matId) {
    log.debug("CinecaCareerClient: GET transcript matId={}", matId);
    List<CinecaTranscriptRow> result = webClient.get()
        .uri(baseUrl + "/libretto-service-v2/libretti/" + matId + "/righe/")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for transcript matId=" + matId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on transcript")))
        .bodyToFlux(CinecaTranscriptRow.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves grade averages from {@code libretto-service-v2}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param matId   student career segment identifier
   * @return list of grade average entries; empty if none available
   */
  public List<CinecaGrade> getGrades(String baseUrl, String jwt, Long matId) {
    log.debug("CinecaCareerClient: GET grades matId={}", matId);
    List<CinecaGrade> result = webClient.get()
        .uri(baseUrl + "/libretto-service-v2/libretti/" + matId + "/medie")
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for grades matId=" + matId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on grades")))
        .bodyToFlux(CinecaGrade.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves study plan headers from {@code piani-service-v1}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student identifier
   * @return list of study plan headers; empty if none available
   */
  public List<CinecaStudyPlanHeader> getStudyPlanHeaders(
      String baseUrl, String jwt, Long stuId) {
    log.debug("CinecaCareerClient: GET study plan headers stuId={}", stuId);
    List<CinecaStudyPlanHeader> result = webClient.get()
        .uri(baseUrl + "/piani-service-v1/piani/" + stuId)
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for study plan headers stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on study plan headers")))
        .bodyToFlux(CinecaStudyPlanHeader.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves the full study plan detail from {@code piani-service-v1}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param stuId   student identifier
   * @param pianoId study plan identifier
   * @return study plan detail, or {@code null} if not available
   */
  public CinecaStudyPlanDetail getStudyPlanDetail(
      String baseUrl, String jwt, Long stuId, Long pianoId) {
    log.debug("CinecaCareerClient: GET study plan detail stuId={} pianoId={}", stuId, pianoId);
    return webClient.get()
        .uri(baseUrl + "/piani-service-v1/piani/" + stuId + "/" + pianoId)
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for study plan detail stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on study plan detail")))
        .bodyToMono(CinecaStudyPlanDetail.class)
        .block();
  }

  // ============ DTOs ============

  /**
   * Shared value wrapper for Cineca enum-like fields.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaValueWrapper {

    @JsonProperty("value")
    private String value;

    public String getValue() {
      return value;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaTranscriptRow {

    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("annoCorso")
    private Integer annoCorso;
    @JsonProperty("stato")
    private CinecaValueWrapper stato;
    @JsonProperty("statoDes")
    private String statoDes;
    @JsonProperty("peso")
    private Double peso;
    @JsonProperty("tipoInsCod")
    private String tipoInsCod;
    @JsonProperty("tipoInsDes")
    private String tipoInsDes;
    @JsonProperty("esito")
    private CinecaExamResult esito;
    @JsonProperty("numAppelliPrenotabili")
    private Integer numAppelliPrenotabili;

    public Long getAdsceId() {
      return adsceId;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public String getStato() {
      return stato != null ? stato.getValue() : null;
    }

    public String getStatoDes() {
      return statoDes;
    }

    public Double getPeso() {
      return peso;
    }

    public String getTipoInsCod() {
      return tipoInsCod;
    }

    public String getTipoInsDes() {
      return tipoInsDes;
    }

    public CinecaExamResult getEsito() {
      return esito;
    }

    public Integer getNumAppelliPrenotabili() {
      return numAppelliPrenotabili;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaExamResult {

    @JsonProperty("voto")
    private Double voto;
    @JsonProperty("lodeFlg")
    private Integer lodeFlg;
    @JsonProperty("dataEsa")
    private String dataEsa;
    @JsonProperty("tipoGiudCod")
    private String tipoGiudCod;
    @JsonProperty("tipoGiudDes")
    private String tipoGiudDes;

    public Double getVoto() {
      return voto;
    }

    public Integer getLodeFlg() {
      return lodeFlg;
    }

    public String getDataEsa() {
      return dataEsa;
    }

    public String getTipoGiudCod() {
      return tipoGiudCod;
    }

    public String getTipoGiudDes() {
      return tipoGiudDes;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaGrade {

    @JsonProperty("media")
    private Double media;
    @JsonProperty("base")
    private Integer base;
    @JsonProperty("tipoMediaCod")
    private CinecaValueWrapper tipoMediaCod;
    @JsonProperty("definizioneBase")
    private CinecaValueWrapper definizioneBase;
    @JsonProperty("tipoOk")
    private Integer tipoOk;

    public Double getMedia() {
      return media;
    }

    public Integer getBase() {
      return base;
    }

    public String getTipoMediaCod() {
      return tipoMediaCod != null ? tipoMediaCod.getValue() : null;
    }

    public String getDefinizioneBase() {
      return definizioneBase != null ? definizioneBase.getValue() : null;
    }

    public Integer getTipoOk() {
      return tipoOk;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaStudyPlanHeader {

    @JsonProperty("stuId")
    private Long stuId;
    @JsonProperty("pianoId")
    private Long pianoId;
    @JsonProperty("stato")
    private CinecaValueWrapper stato;
    @JsonProperty("statoDes")
    private String statoDes;

    public Long getStuId() {
      return stuId;
    }

    public Long getPianoId() {
      return pianoId;
    }

    public String getStato() {
      return stato != null ? stato.getValue() : null;
    }

    public String getStatoDes() {
      return statoDes;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaStudyPlanDetail {

    @JsonProperty("stuId")
    private Long stuId;
    @JsonProperty("pianoId")
    private Long pianoId;
    @JsonProperty("attivita")
    private List<CinecaStudyPlanActivity> activities;

    public Long getStuId() {
      return stuId;
    }

    public Long getPianoId() {
      return pianoId;
    }

    public List<CinecaStudyPlanActivity> getActivities() {
      return activities != null ? activities : List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaStudyPlanActivity {

    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("adLibCod")
    private String adLibCod;
    @JsonProperty("adLibDes")
    private String adLibDes;
    @JsonProperty("peso")
    private Double cfu;
    @JsonProperty("annoCorso")
    private Integer annoCorso;
    @JsonProperty("chiaveADContestualizzata")
    private CinecaActivityKey activityKey;
    @JsonProperty("sceltaFlg")
    private Integer sceltaFlg;

    public Long getAdsceId() {
      return adsceId;
    }

    public Double getCfu() {
      return cfu;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public boolean isRequired() {
      return sceltaFlg == null || sceltaFlg == 0;
    }

    public String getAdDes() {
      if (adLibDes != null && !adLibDes.isBlank()) {
        return adLibDes;
      }
      return activityKey != null ? activityKey.getAdDes() : null;
    }

    public String getAdCod() {
      if (adLibCod != null && !adLibCod.isBlank()) {
        return adLibCod;
      }
      return activityKey != null ? activityKey.getAdCod() : null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaActivityKey {

    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }
  }
}
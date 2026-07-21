package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.exception.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.exception.CinecaUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca ESSE3 course logistics and teaching-load data.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code logistica-service-v1} — course scheduling/location metadata and per-module
 *   teaching loads</li>
 *   <li>{@code offerta-service-v1} — offered activity metadata (language, exam type, mandatory
 *   flag)</li>
 * </ul>
 */
@Component
public class CinecaCourseCatalogClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaCourseCatalogClient.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  // ============ Class Methods ============

  /**
   * Retrieves scheduling/location metadata for a teaching activity from
   * {@code logistica-service-v1}.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param adCod   activity code
   * @param cdsCod  course of study code
   * @param aaOffId offering year identifier, or {@code null} to omit the filter
   * @return list of logistics entries; empty if none available
   */
  public List<CinecaLogistics> getLogistics(
      String baseUrl, String adCod, String cdsCod, Long aaOffId) {
    log.debug("CinecaCourseCatalogClient: GET logistics adCod={} cdsCod={} aaOffId={}",
        adCod, cdsCod, aaOffId);
    List<CinecaLogistics> result = webClient.get()
        .uri(uriBuilder -> {
          uriBuilder
              .scheme("https")
              .host(extractHost(baseUrl))
              .pathSegment("e3rest", "api", "logistica-service-v1", "logistica")
              .queryParam("adCod", adCod)
              .queryParam("cdsCod", cdsCod);
          if (aaOffId != null) {
            uriBuilder.queryParam("aaOffId", aaOffId);
          }
          return uriBuilder.build();
        })
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaAuthException(
                "Unauthorized for logistics adCod=" + adCod)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on logistics")))
        .bodyToFlux(CinecaLogistics.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves offered-activity metadata (teaching language, exam type, mandatory flag) from
   * {@code offerta-service-v1}.
   *
   * @param baseUrl  Cineca ESSE3 base URL
   * @param aaOffId  offering year identifier
   * @param cdsOffId course offering identifier
   * @param adCod    activity code
   * @return list of offered activity entries; empty if none available
   */
  public List<CinecaOfferedActivity> getOfferedActivities(
      String baseUrl, Long aaOffId, Long cdsOffId, String adCod) {
    log.debug("CinecaCourseCatalogClient: GET offered activities aaOffId={} cdsOffId={} adCod={}",
        aaOffId, cdsOffId, adCod);
    List<CinecaOfferedActivity> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "offerta-service-v1", "offerte",
                aaOffId.toString(), cdsOffId.toString(), "attivita")
            .queryParam("adCod", adCod)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaAuthException(
                "Unauthorized for offered activities adCod=" + adCod)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on offered activities")))
        .bodyToFlux(CinecaOfferedActivity.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves the per-module teaching assignments (docenti + hours + role) for a given logistics
   * grouping. A course with multiple modules (UD) returns one entry per (docente, modulo) pair — a
   * docente teaching several modules of the same course appears once per module, each with its own
   * tipoCreCod/ore/frazioneCarico.
   *
   * <p>Note: docenteEMail is present in the OpenAPI schema but was
   * confirmed empty for every docente tested (UNIMOL/UNICH), even with optionalFields=ALL — see
   * HANDOFF for the full investigation.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param adLogId logistics grouping identifier (obtained from getLogistics)
   * @return list of teaching assignments; empty if none available
   */
  public List<CinecaCaricoDocente> getCaricoDocenti(String baseUrl, Long adLogId) {
    log.debug("CinecaCourseCatalogClient: GET carico docenti adLogId={}", adLogId);
    String raw = webClient.get()
        .uri(baseUrl + "/logistica-service-v1/logistica/" + adLogId + "/udLogConDettagli"
            + "?optionalFields=ALL")
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaAuthException(
                "Unauthorized for carico docenti adLogId=" + adLogId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaUnavailableException(
                "Cineca error on carico docenti")))
        .bodyToMono(String.class)
        .block();

    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    try {
      JsonNode root = objectMapper.readTree(raw);
      List<CinecaCaricoDocente> result = new java.util.ArrayList<>();
      for (JsonNode udBlock : root) {
        JsonNode carico = udBlock.get("CaricoDocenti");
        if (carico == null) {
          continue;
        }
        for (JsonNode entry : carico) {
          result.add(objectMapper.treeToValue(entry, CinecaCaricoDocente.class));
        }
      }
      return result;
    } catch (Exception e) {
      log.error("CinecaCourseCatalogClient: failed to parse carico docenti adLogId={}", adLogId, e);
      return List.of();
    }
  }

  // ============ DTOs ============

  /**
   * Scheduling/location metadata for a single teaching activity occurrence.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaLogistics {

    @JsonProperty("chiavePartizione")
    private CinecaPartitionKey partitionKey;
    @JsonProperty("dataInizio")
    private String startDate;
    @JsonProperty("dataFine")
    private String endDate;
    @JsonProperty("sedeDes")
    private String locationDes;

    public String getPeriodDes() {
      return partitionKey != null ? partitionKey.getPartDes() : null;
    }

    public String getStartDate() {
      return startDate;
    }

    public String getEndDate() {
      return endDate;
    }

    public String getLocationDes() {
      return locationDes;
    }
  }

  /**
   * Teaching period label wrapper for a logistics entry.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPartitionKey {

    @JsonProperty("partDes")
    private String partDes;

    public String getPartDes() {
      return partDes;
    }
  }

  /**
   * Offered-activity metadata: language, exam/evaluation type, mandatory status, course page link.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaOfferedActivity {

    @JsonProperty("linguaInsDes")
    private String teachingLanguage;
    @JsonProperty("tipoEsaDes")
    private String examTypeDes;
    @JsonProperty("tipoValDes")
    private String evaluationTypeDes;
    @JsonProperty("tipoInsCod")
    private String activityTypeCod;
    @JsonProperty("urlSitoWeb")
    private String coursePageUrl;

    public String getTeachingLanguage() {
      return teachingLanguage;
    }

    public String getExamTypeDes() {
      return examTypeDes;
    }

    public String getEvaluationTypeDes() {
      return evaluationTypeDes;
    }

    public String getCoursePageUrl() {
      return coursePageUrl;
    }

    public boolean isMandatory() {
      return "OBB".equals(activityTypeCod) || "1".equals(activityTypeCod);
    }
  }

  /**
   * A single (docente, modulo) teaching-load assignment within a logistics grouping.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCaricoDocente {

    @JsonProperty("docenteId")
    private Long docenteId;
    @JsonProperty("docenteMatricola")
    private String docenteMatricola;
    @JsonProperty("docenteNome")
    private String docenteNome;
    @JsonProperty("docenteCognome")
    private String docenteCognome;
    @JsonProperty("docenteEMail")
    private String docenteEMail;
    @JsonProperty("udLogId")
    private Long udLogId;
    @JsonProperty("tipoCreCod")
    private String tipoCreCod;
    @JsonProperty("tipoCreDes")
    private String tipoCreDes;
    @JsonProperty("ore")
    private Double ore;
    @JsonProperty("frazioneCarico")
    private Double frazioneCarico;
    @JsonProperty("valDidFlg")
    private Integer valDidFlg;
    @JsonProperty("oreAttSuppDid")
    private Double oreAttSuppDid;
    @JsonProperty("tipoCoperturaCod")
    private String tipoCoperturaCod;
    @JsonProperty("tipoCoperturaDes")
    private String tipoCoperturaDes;

    public Long getDocenteId() {
      return docenteId;
    }

    public String getDocenteMatricola() {
      return docenteMatricola;
    }

    public String getDocenteNome() {
      return docenteNome;
    }

    public String getDocenteCognome() {
      return docenteCognome;
    }

    public String getDocenteEMail() {
      return docenteEMail;
    }

    public Long getUdLogId() {
      return udLogId;
    }

    public String getTipoCreCod() {
      return tipoCreCod;
    }

    public String getTipoCreDes() {
      return tipoCreDes;
    }

    public Double getOre() {
      return ore;
    }

    public Double getFrazioneCarico() {
      return frazioneCarico;
    }

    public Integer getValDidFlg() {
      return valDidFlg;
    }

    public Double getOreAttSuppDid() {
      return oreAttSuppDid;
    }

    public String getTipoCoperturaCod() {
      return tipoCoperturaCod;
    }

    public String getTipoCoperturaDes() {
      return tipoCoperturaDes;
    }

    public boolean isValutabile() {
      return valDidFlg != null && valDidFlg == 1;
    }
  }
}
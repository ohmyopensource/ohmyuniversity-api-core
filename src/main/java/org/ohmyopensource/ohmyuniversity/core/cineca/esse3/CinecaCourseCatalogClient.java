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

@Component
public class CinecaCourseCatalogClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaCourseCatalogClient.class);

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
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for logistics adCod=" + adCod)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on logistics")))
        .bodyToFlux(CinecaLogistics.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

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
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for offered activities adCod=" + adCod)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on offered activities")))
        .bodyToFlux(CinecaOfferedActivity.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPartitionKey {

    @JsonProperty("partDes")
    private String partDes;

    public String getPartDes() {
      return partDes;
    }
  }

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
}
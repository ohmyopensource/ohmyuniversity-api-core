package org.ohmyopensource.ohmyuniversity.core.cineca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for integration with Cineca ESSE3 services.
 *
 * <p>This component handles all HTTP calls to student career-related services
 * (libretto, study plans, grades, exam sessions, bookings, badges, and fees) using Spring
 * WebClient.
 *
 * <p>It supports two authentication mechanisms:
 * - Bearer JWT for most ESSE3 services
 * - Basic Auth + JSESSIONID for calesa-service-v1 endpoints
 *
 * <p>Notes on Cineca API behavior:
 * - Most endpoints return JSON arrays even when a single element is expected
 * - Some fields are wrapped as objects in the form { value: "..." }
 * - Certain responses contain nested or non-uniform structures
 *
 * <p>This class acts as a low-level HTTP integration layer and does not contain business logic.
 */
@Component
public class CinecaCarrieraClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaCarrieraClient.class);
  private final WebClient webClient;

  // ============ Constructor ============

  /**
   * Creates a WebClient instance configured with an increased in-memory buffer size.
   *
   * <p>This is required to safely handle large payloads returned by Cineca services
   * without triggering memory limit exceptions.
   */
  public CinecaCarrieraClient() {
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();
  }

  // ============ Classe Methods ============

  /**
   * Retrieves the student libretto entries (exam records and course status).
   *
   * <p>This method calls the Cineca libretto-service-v2 API using the student matricola ID
   * and returns the full list of exam/career rows.
   *
   * <p>Authentication is performed via Bearer JWT.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * @param cinecaBaseUrl base URL of the Cineca ESSE3 instance
   * @param cinecaJwt     JWT token used for Bearer authentication
   * @param matId         student matricola / career ID
   * @return list of libretto rows; empty list if no data is returned or response is null
   */
  public List<CinecaRigaLibretto> getRigheLibretto(
      String cinecaBaseUrl, String cinecaJwt, Long matId) {

    String url = cinecaBaseUrl + "/libretto-service-v2/libretti/" + matId + "/righe/";
    log.debug("CinecaCarrieraClient: GET righe libretto matId={}", matId);

    List<CinecaRigaLibretto> result = webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for libretto matId=" + matId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on libretto")))
        .bodyToFlux(CinecaRigaLibretto.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Retrieves the list of student grade averages from the Cineca service.
   *
   * <p>Performs an HTTP GET request to the remote service using WebClient,
   * authenticated via JWT, in order to fetch grade average information associated with the student
   * identified by {@code matId}.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * <p>If the response is null, an empty list is returned.
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param matId         student identifier
   * @return list of {@link CinecaMedia} associated with the student, or an empty list if no data is
   *     available
   */
  public List<CinecaMedia> getMedia(String cinecaBaseUrl, String cinecaJwt, Long matId) {
    String url = cinecaBaseUrl + "/libretto-service-v2/libretti/" + matId + "/medie";
    log.debug("CinecaCarrieraClient: GET medie matId={}", matId);

    List<CinecaMedia> result = webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for medie matId=" + matId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on medie")))
        .bodyToFlux(CinecaMedia.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Retrieves the study plan headers for a specific student from the Cineca service.
   *
   * <p>Performs an HTTP GET request to the remote piani-service using WebClient,
   * authenticated via JWT, in order to fetch the study plan header information associated with the
   * student identified by {@code stuId}.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * <p>If the response is null, an empty list is returned.
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param stuId         student identifier
   * @return list of {@link CinecaTestataPiano} representing the student's study plan headers, or an
   *     empty list if no data is available
   */
  public List<CinecaTestataPiano> getPianoHeaders(
      String cinecaBaseUrl, String cinecaJwt, Long stuId) {

    String url = cinecaBaseUrl + "/piani-service-v1/piani/" + stuId;
    log.debug("CinecaCarrieraClient: GET piano headers stuId={}", stuId);

    List<CinecaTestataPiano> result = webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for piani stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on piani")))
        .bodyToFlux(CinecaTestataPiano.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Retrieves the detailed study plan information for a specific student and plan from the Cineca
   * service.
   *
   * <p>Performs an HTTP GET request to the remote piani-service using WebClient,
   * authenticated via JWT, in order to fetch the detailed information of a study plan identified by
   * {@code stuId} and {@code pianoId}.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param stuId         student identifier
   * @param pianoId       study plan identifier
   * @return detailed study plan information as {@link CinecaPianoDettaglio}, or {@code null} if not
   *     available
   */
  public CinecaPianoDettaglio getPianoDettaglio(
      String cinecaBaseUrl, String cinecaJwt, Long stuId, Long pianoId) {

    String url = cinecaBaseUrl + "/piani-service-v1/piani/" + stuId + "/" + pianoId;
    log.debug("CinecaCarrieraClient: GET piano dettaglio stuId={} pianoId={}", stuId, pianoId);

    return webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for piano dettaglio stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on piano dettaglio")))
        .bodyToMono(CinecaPianoDettaglio.class)
        .block();
  }

  /**
   * Retrieves available exam sessions (appelli) for a specific course of study and student from the
   * Cineca service.
   *
   * <p>Performs an HTTP GET request to the remote calesa-service using WebClient,
   * authenticated via JWT and additional auth token, in order to fetch the list of available exam
   * sessions for the specified course and exam identifiers.
   *
   * <p>The request is built dynamically using URI components, including:
   * - cdsId: course of study identifier
   * - adId: teaching activity identifier
   * - stuId: student identifier
   * - authToken: Cineca service authentication token
   * - fixed query parameter q=APPELLI_PRENOTABILI
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: the response body is logged and an authentication/authorization exception is
   * thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * <p>If the response is null, an empty list is returned.
   *
   * @param cinecaBaseUrl   base URL of the Cineca service
   * @param cinecaJwt       JWT token used to authenticate the request
   * @param cinecaAuthToken additional authentication token required by Cineca
   * @param cdsId           course of study identifier
   * @param adId            teaching activity identifier
   * @param stuId           student identifier
   * @return list of available exam sessions as {@link CinecaAppello}, or an empty list if no data
   *     is available
   */
  public List<CinecaAppello> getAppelli(
      String cinecaBaseUrl,
      String cinecaJwt,
      String cinecaAuthToken,
      Long cdsId,
      Long adId,
      Long stuId) {

    log.debug("CinecaCarrieraClient: GET appelli cdsId={} adId={} stuId={}", cdsId, adId, stuId);

    List<CinecaAppello> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(cinecaBaseUrl.replace("https://", "").split("/")[0])
            .pathSegment("e3rest", "api", "calesa-service-v1", "appelli",
                cdsId.toString(), adId.toString())
            .queryParam("q", "APPELLI_PRENOTABILI")
            .queryParam("stuId", stuId)
            .queryParam("authToken", cinecaAuthToken)
            .build())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("Cineca appelli 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for appelli: " + body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on appelli")))
        .bodyToFlux(CinecaAppello.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Fetches the full booking history for a student from calesa-service-v1.
   *
   * <p>This endpoint requires both Basic Authentication and a valid JSESSIONID cookie.
   * JWT authentication alone is not sufficient, as the service relies on an active Cineca session
   * context.
   *
   * <p>Authentication model:
   * - Basic Auth is used to authenticate the user against Cineca
   * - JSESSIONID is required to maintain an active session context
   * - Requests without a valid session are rejected by SecurityCalesa
   *
   * <p>Security behavior notes:
   * - Endpoint {@code prenotazioni/{matId}} uses a different authorization flow compared to other
   * calesa-service endpoints
   * - The checkAbildocStu constraint is not compatible with anonymous sessions
   * - A valid session must be established externally and provided via Redis token
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: the response body is logged and an authentication/authorization exception is
   * thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * @param cinecaBaseUrl   base URL of the university ESSE3 instance
   * @param cinecaUsername  Cineca username (from UniversityConnection)
   * @param cinecaPassword  Cineca password provided at request time
   * @param cinecaAuthToken Cineca session token (JSESSIONID) retrieved from Redis
   * @param matId           student career track identifier
   * @return list of booking history entries as {@link CinecaPrenotazione}, or an empty list if none
   *     available
   */
  public List<CinecaPrenotazione> getPrenotazioni(
      String cinecaBaseUrl,
      String cinecaUsername,
      String cinecaPassword,
      String cinecaAuthToken,
      Long matId) {

    String url = cinecaBaseUrl + "/calesa-service-v1/prenotazioni/" + matId;
    log.debug("CinecaCarrieraClient: GET prenotazioni matId={}", matId);

    List<CinecaPrenotazione> result = webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, basicAuth(cinecaUsername, cinecaPassword))
        .header("Cookie", "JSESSIONID=" + cinecaAuthToken)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("Cineca prenotazioni 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for prenotazioni matId=" + matId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on prenotazioni")))
        .bodyToFlux(CinecaPrenotazione.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Retrieves the list of badges associated with a student from the Cineca badge-service.
   *
   * <p>Performs an HTTP GET request to the remote badge-service using WebClient,
   * authenticated via JWT, in order to fetch all badge information linked to the student identified
   * by {@code stuId}.
   *
   * <p>The student identifier is sent as a query parameter in the request.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * <p>If the response is null, an empty list is returned.
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param stuId         student identifier
   * @return list of {@link CinecaBadge} associated with the student, or an empty list if no data is
   *     available
   */
  public List<CinecaBadge> getBadges(String cinecaBaseUrl, String cinecaJwt, Long stuId) {
    String url = cinecaBaseUrl + "/badge-service-v1/badges?stuId=" + stuId;
    log.debug("CinecaCarrieraClient: GET badge stuId={}", stuId);

    List<CinecaBadge> result = webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
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

  /**
   * Builds a Basic Authentication header value from username and password.
   *
   * <p>The credentials are concatenated in the format {@code username:password},
   * then encoded using Base64 as required by the HTTP Basic Authentication standard.
   *
   * <p>The resulting string can be used directly as the value of the
   * {@code Authorization} HTTP header.
   *
   * @param username the Cineca username
   * @param password the Cineca password
   * @return a formatted Basic Auth header value
   */
  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Retrieves the financial status indicator (semaforo) for a student from the Cineca
   * tasse-service.
   *
   * <p>This method performs an HTTP GET request to the remote tasse-service using WebClient,
   * authenticated via JWT, in order to retrieve the student's payment/administrative status
   * represented as a "semaphore" indicator.
   *
   * <p>The semaforo typically reflects the student's administrative standing (e.g. regular,
   * warning, blocked), depending on Cineca configuration.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param stuId         student identifier
   * @return semaforo status as {@link CinecaSemaforo}, or {@code null} if not available
   */
  public CinecaSemaforo getSemaforo(String cinecaBaseUrl, String cinecaJwt, Long stuId) {
    String url = cinecaBaseUrl + "/tasse-service-v1/semaforo/" + stuId;
    log.debug("CinecaCarrieraClient: GET semaforo stuId={}", stuId);

    return webClient.get()
        .uri(url)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for semaforo stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on semaforo")))
        .bodyToMono(CinecaSemaforo.class)
        .block();
  }

  /**
   * Retrieves the list of financial charges (addebiti) for a student from the Cineca
   * tasse-service.
   *
   * <p>This method performs an HTTP GET request to the remote tasse-service using WebClient,
   * authenticated via JWT, in order to fetch all financial charges associated with the student
   * identified by {@code stuId}.
   *
   * <p>The request is limited to a maximum number of results (limit = 100) to avoid
   * excessive payload size.
   *
   * <p>In case of remote errors:
   * - HTTP 4xx: an authentication/authorization exception is thrown
   * - HTTP 5xx: a service unavailability exception is thrown
   *
   * @param cinecaBaseUrl base URL of the Cineca service
   * @param cinecaJwt     JWT token used to authenticate the request
   * @param stuId         student identifier
   * @return list of {@link CinecaAddebito} representing financial charges, or an empty list if none
   *     are available
   */
  public List<CinecaAddebito> getAddebiti(String cinecaBaseUrl, String cinecaJwt, Long stuId) {
    log.debug("CinecaCarrieraClient: GET addebiti stuId={}", stuId);

    List<CinecaAddebito> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(cinecaBaseUrl.replace("https://", "").split("/")[0])
            .pathSegment("e3rest", "api", "tasse-service-v1", "addebiti-studente")
            .queryParam("stuId", stuId)
            .queryParam("limit", 100)
            .build())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cinecaJwt)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for addebiti stuId=" + stuId)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on addebiti")))
        .bodyToFlux(CinecaAddebito.class)
        .collectList()
        .block();

    return result != null ? result : List.of();
  }

  /**
   * Wrapper class used to deserialize Cineca enum-like structures.
   *
   * <p>Cineca APIs often represent enum-like values as JSON objects in the form:
   * {@code { "value": "..." }} instead of plain strings.
   *
   * <p>This class is used to correctly map such structures during JSON deserialization,
   * extracting the underlying {@code value} field.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaValueWrapper {

    @JsonProperty("value")
    private String value;

    public String getValue() {
      return value;
    }
  }

  /**
   * Represents a single row of a student's academic transcript (libretto) retrieved from Cineca.
   *
   * <p>This DTO maps the structure returned by the Cineca libretto-service and contains
   * information about a specific teaching activity, including status, results, and metadata.
   *
   * <p>Notes:
   * - Some fields (e.g. {@code stato}) are wrapped in Cineca-specific value objects
   * - Enum-like structures are represented using {@link CinecaValueWrapper}
   * - Unknown fields from the API are ignored during deserialization
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaRigaLibretto {

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
    private CinecaEsito esito;
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

    public CinecaEsito getEsito() {
      return esito;
    }

    public Integer getNumAppelliPrenotabili() {
      return numAppelliPrenotabili;
    }
  }

  /**
   * Represents the exam outcome (esito) of a teaching activity in the Cineca system.
   *
   * <p>This object contains grading information, including the final score,
   * honors flag, exam date, and judgment metadata as returned by the Cineca API.
   *
   * <p>Notes:
   * - The exam date is provided as a raw string in the API response
   * - The honors flag indicates whether the exam result includes laude
   * - Judgment fields describe the evaluation type and its description
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaEsito {

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

  /**
   * Represents a student's grade average information retrieved from the Cineca service.
   *
   * <p>This DTO maps the media-service response and contains statistical information
   * about the student's academic performance, including weighted averages and metadata describing
   * how the average is calculated.
   *
   * <p>Notes:
   * - Some fields are wrapped using Cineca-specific value objects
   * - Enum-like values are represented through {@link CinecaValueWrapper}
   * - Null-safe accessors are used for wrapped fields
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaMedia {

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

  /**
   * Represents the header information of a student's study plan retrieved from Cineca.
   *
   * <p>This DTO maps the study plan overview returned by the piani-service and includes
   * identifiers and status information for a specific study plan associated with a student.
   *
   * <p>Notes:
   * - The {@code stato} field is wrapped using a Cineca-specific value object
   * - Null-safe access is used for wrapped enum-like values
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaTestataPiano {

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

  /**
   * Represents the detailed structure of a student's study plan retrieved from Cineca.
   *
   * <p>This DTO maps the response of the piani-service detail endpoint and contains
   * the full list of planned academic activities associated with a specific study plan.
   *
   * <p>Notes:
   * - If the {@code attivita} field is null, an empty list is returned to avoid null handling
   * - The structure is designed to be safe for direct use in service layers
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPianoDettaglio {

    @JsonProperty("stuId")
    private Long stuId;
    @JsonProperty("pianoId")
    private Long pianoId;
    @JsonProperty("attivita")
    private List<CinecaAttivitaPiano> attivita;

    public Long getStuId() {
      return stuId;
    }

    public Long getPianoId() {
      return pianoId;
    }

    public List<CinecaAttivitaPiano> getAttivita() {
      return attivita != null ? attivita : List.of();
    }
  }

  /**
   * Represents a single academic activity within a student's study plan in the Cineca system.
   *
   * <p>This DTO maps the study plan activity structure returned by the piani-service
   * and contains both descriptive and structural information about each learning activity.
   *
   * <p>Field resolution strategy:
   * - Primary values (adLibCod, adLibDes) are used when available
   * - Fallback values are retrieved from {@link CinecaChiaveAd}
   * - This ensures robustness against incomplete API payloads
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAttivitaPiano {

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
    private CinecaChiaveAd chiaveAd;
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

    public CinecaChiaveAd getChiaveAd() {
      return chiaveAd;
    }

    public Boolean isObbligatorio() {
      return sceltaFlg == null || sceltaFlg == 0;
    }

    /**
     * Returns the activity description, using a fallback strategy if the primary value is missing.
     */
    public String getAdDes() {
      if (adLibDes != null && !adLibDes.isBlank()) {
        return adLibDes;
      }
      return chiaveAd != null ? chiaveAd.getAdDes() : null;
    }

    /**
     * Returns the activity code, using a fallback strategy if the primary value is missing.
     */
    public String getAdCod() {
      if (adLibCod != null && !adLibCod.isBlank()) {
        return adLibCod;
      }
      return chiaveAd != null ? chiaveAd.getAdCod() : null;
    }
  }

  /**
   * Represents the contextual key of an academic activity in the Cineca system.
   *
   * <p>This DTO is used as a fallback structure when primary activity data is incomplete
   * or partially unavailable. It provides the essential identifying information for a teaching
   * activity.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaChiaveAd {

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

  /**
   * Represents an exam booking (prenotazione) in the Cineca system.
   *
   * <p>This DTO maps the calesa-service booking response and contains detailed information
   * about a student's exam registration, including identifiers, scheduling data, and outcome
   * status.
   *
   * <p>Notes:
   * - Some fields are wrapped using Cineca-specific value objects
   * - Dates are provided as raw strings as returned by the external API
   * - Exam outcome information is represented via {@link CinecaEsitoPrenotazione}
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPrenotazione {

    @JsonProperty("applistaId")
    private Long applistaId;
    @JsonProperty("adId")
    private Long adId;
    @JsonProperty("adStuCod")
    private String adStuCod;
    @JsonProperty("adStuDes")
    private String adStuDes;
    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("cdsId")
    private Long cdsId;
    @JsonProperty("appId")
    private Long appId;
    @JsonProperty("appLogId")
    private Long appLogId;
    @JsonProperty("stuId")
    private Long stuId;
    @JsonProperty("dataIns")
    private String dataIns;
    @JsonProperty("dataEsa")
    private String dataEsa;
    @JsonProperty("dataRifEsitoStu")
    private String dataRifEsitoStu;
    @JsonProperty("pesoAd")
    private Double pesoAd;
    @JsonProperty("posizApp")
    private Integer posizApp;
    @JsonProperty("esito")
    private CinecaEsitoPrenotazione esito;
    @JsonProperty("presaVisione")
    private CinecaValueWrapper presaVisione;
    @JsonProperty("statoAdsce")
    private CinecaValueWrapper statoAdsce;
    @JsonProperty("pubblId")
    private Long pubblId;
    @JsonProperty("domandeEsame")
    private String domandeEsame;

    public Long getApplistaId() {
      return applistaId;
    }

    public Long getAdId() {
      return adId;
    }

    public String getAdStuCod() {
      return adStuCod;
    }

    public String getAdStuDes() {
      return adStuDes;
    }

    public Long getAdsceId() {
      return adsceId;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public Long getAppId() {
      return appId;
    }

    public Long getAppLogId() {
      return appLogId;
    }

    public Long getStuId() {
      return stuId;
    }

    public String getDataIns() {
      return dataIns;
    }

    public String getDataEsa() {
      return dataEsa;
    }

    public String getDataRifEsitoStu() {
      return dataRifEsitoStu;
    }

    public Double getPesoAd() {
      return pesoAd;
    }

    public Integer getPosizApp() {
      return posizApp;
    }

    public CinecaEsitoPrenotazione getEsito() {
      return esito;
    }

    public String getPresaVisione() {
      return presaVisione != null ? presaVisione.getValue() : null;
    }

    public String getStatoAdsce() {
      return statoAdsce != null ? statoAdsce.getValue() : null;
    }

    public Long getPubblId() {
      return pubblId;
    }

    public String getDomandeEsame() {
      return domandeEsame;
    }
  }

  /**
   * Represents the exam outcome information associated with a booking in the Cineca system.
   *
   * <p>This DTO contains the evaluation result of an exam session, including grade,
   * status flags, and judgment metadata as returned by the calesa-service.
   *
   * <p>Notes:
   * - Status flags are represented as integer values (1 = true, otherwise false)
   * - The evaluation mode code is wrapped using {@link CinecaValueWrapper}
   * - Boolean helper methods provide a more convenient interpretation of raw flags
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaEsitoPrenotazione {

    @JsonProperty("votoEsa")
    private Integer votoEsa;
    @JsonProperty("superatoFlg")
    private Integer superatoFlg;
    @JsonProperty("assenteFlg")
    private Integer assenteFlg;
    @JsonProperty("ritiratoFlg")
    private Integer ritiratoFlg;
    @JsonProperty("tipoGiudCod")
    private String tipoGiudCod;
    @JsonProperty("tipoGiudizioDes")
    private String tipoGiudizioDes;
    @JsonProperty("modValCod")
    private CinecaValueWrapper modValCod;

    public Integer getVotoEsa() {
      return votoEsa;
    }

    public Boolean isSuperato() {
      return superatoFlg != null && superatoFlg == 1;
    }

    public Boolean isAssente() {
      return assenteFlg != null && assenteFlg == 1;
    }

    public Boolean isRitirato() {
      return ritiratoFlg != null && ritiratoFlg == 1;
    }

    public String getTipoGiudCod() {
      return tipoGiudCod;
    }

    public String getTipoGiudizioDes() {
      return tipoGiudizioDes;
    }

    public String getModValCod() {
      return modValCod != null ? modValCod.getValue() : null;
    }
  }

  /**
   * Represents an exam session (appello) retrieved from the Cineca calesa-service.
   *
   * <p>This DTO maps the exam session availability response and contains scheduling
   * information, course metadata, and booking state for a specific student.
   *
   * <p>Notes:
   * - Date fields are provided as raw strings in the format returned by the API
   * - Booking state fields indicate whether the session is available or already booked
   * - Optional identifiers are present only when a booking exists
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAppello {

    @JsonProperty("appId")
    private Long appId;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("dataInizioApp")
    private String dataInizio;
    @JsonProperty("dataFineApp")
    private String dataFine;
    @JsonProperty("dataScadPren")
    private String dataScadPrenotazione;
    @JsonProperty("tipoApp")
    private String tipo;
    @JsonProperty("tipoAppDes")
    private String tipoDes;
    @JsonProperty("docenteDes")
    private String docente;
    @JsonProperty("aula")
    private String aula;
    @JsonProperty("prenotabile")
    private Boolean prenotabile;
    @JsonProperty("prenotato")
    private Boolean prenotato;
    @JsonProperty("prenotazioneId")
    private Long prenotazioneId;

    public Long getAppId() {
      return appId;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public String getDataInizio() {
      return dataInizio;
    }

    public String getDataFine() {
      return dataFine;
    }

    public String getDataScadPrenotazione() {
      return dataScadPrenotazione;
    }

    public String getTipo() {
      return tipo;
    }

    public String getTipoDes() {
      return tipoDes;
    }

    public String getDocente() {
      return docente;
    }

    public String getAula() {
      return aula;
    }

    public Boolean getPrenotabile() {
      return prenotabile;
    }

    public Boolean getPrenotato() {
      return prenotato;
    }

    public Long getPrenotazioneId() {
      return prenotazioneId;
    }
  }

  /**
   * Represents a student badge record retrieved from the Cineca badge-service.
   *
   * <p>This DTO maps the badge-service response and contains personal, academic,
   * and identification data associated with a student's institutional badge.
   *
   * <p>Notes:
   * - Image availability fields are represented as integer flags (1 = present, otherwise absent)
   * - Date fields are provided as raw strings as returned by the external API
   * - This object primarily serves identification and access control purposes
   */
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

  /**
   * Represents the financial status summary (semaforo) of a student in the Cineca tasse-service.
   *
   * <p>This DTO provides a high-level overview of the student's administrative and financial
   * situation, including the overall status indicator, outstanding amounts, and lists of due and
   * overdue fees.
   *
   * <p>Notes:
   * - The {@code semaforo} field typically represents the student's status level (e.g. green,
   * yellow, red depending on Cineca configuration)
   * - Monetary values are provided as strings as returned by the API
   * - Fee details are split into due and overdue categories
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSemaforo {

    @JsonProperty("semaforo")
    private String semaforo;
    @JsonProperty("importoDovuto")
    private String importoDovuto;
    @JsonProperty("tasseScadute")
    private List<CinecaVoceTassa> tasseScadute;
    @JsonProperty("tasseDovute")
    private List<CinecaVoceTassa> tasseDovute;

    public String getSemaforo() {
      return semaforo;
    }

    public String getImportoDovuto() {
      return importoDovuto;
    }

    public List<CinecaVoceTassa> getTasseScadute() {
      return tasseScadute;
    }

    public List<CinecaVoceTassa> getTasseDovute() {
      return tasseDovute;
    }
  }

  /**
   * Represents a single tax item (voce tassa) within a student's financial record in the Cineca
   * system.
   *
   * <p>This DTO maps detailed fee information returned by the tasse-service, including
   * identifiers, descriptions, amounts, and due dates for each tax component.
   *
   * <p>Notes:
   * - Monetary values are represented as strings as provided by the API
   * - Date fields are returned in raw format from Cineca
   * - Each entry corresponds to a single tax line item within a broader fee structure
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaVoceTassa {

    @JsonProperty("fattId")
    private Long fattId;
    @JsonProperty("tassaId")
    private Long tassaId;
    @JsonProperty("tassaCod")
    private String tassaCod;
    @JsonProperty("tassaDes")
    private String tassaDes;
    @JsonProperty("voceId")
    private Long voceId;
    @JsonProperty("voceCod")
    private String voceCod;
    @JsonProperty("voceDes")
    private String voceDes;
    @JsonProperty("importoVoce")
    private String importoVoce;
    @JsonProperty("dataScadenza")
    private String dataScadenza;
    @JsonProperty("dataPagTollerataMax")
    private String dataPagTollerataMax;

    public Long getFattId() {
      return fattId;
    }

    public Long getTassaId() {
      return tassaId;
    }

    public String getTassaCod() {
      return tassaCod;
    }

    public String getTassaDes() {
      return tassaDes;
    }

    public Long getVoceId() {
      return voceId;
    }

    public String getVoceCod() {
      return voceCod;
    }

    public String getVoceDes() {
      return voceDes;
    }

    public String getImportoVoce() {
      return importoVoce;
    }

    public String getDataScadenza() {
      return dataScadenza;
    }

    public String getDataPagTollerataMax() {
      return dataPagTollerataMax;
    }
  }

  /**
   * Represents a financial charge (addebito) in the Cineca tasse-service.
   *
   * <p>This DTO maps detailed billing information related to student fees, including
   * tax details, invoice data, payment status, and administrative identifiers.
   *
   * <p>Notes:
   * - Monetary values may be represented as numeric or string types depending on the field
   * - Status flags are encoded as integers (1 = true, otherwise false)
   * - Dates are provided in raw format as returned by the external API
   * - This object aggregates both tax-level and invoice-level information
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAddebito {

    @JsonProperty("aaId")
    private Long aaId;
    @JsonProperty("tassaDes")
    private String tassaDes;
    @JsonProperty("tassaCod")
    private String tassaCod;
    @JsonProperty("tipoTaxCod")
    private String tipoTaxCod;
    @JsonProperty("voceDes")
    private String voceDes;
    @JsonProperty("importoVoce")
    private Double importoVoce;
    @JsonProperty("scadenzaAddebito")
    private String scadenzaAddebito;
    @JsonProperty("scadutoFlg")
    private Integer scadutoFlg;
    @JsonProperty("fattId")
    private Long fattId;
    @JsonProperty("scadFattura")
    private String scadFattura;
    @JsonProperty("fattScadutaFlg")
    private Integer fattScadutaFlg;
    @JsonProperty("importoFattura")
    private String importoFattura;
    @JsonProperty("dataEmissione")
    private String dataEmissione;
    @JsonProperty("pagatoFlg")
    private Integer pagatoFlg;
    @JsonProperty("dataPagamento")
    private String dataPagamento;
    @JsonProperty("importoPag")
    private Double importoPag;
    @JsonProperty("annullataFlg")
    private Integer annullataFlg;
    @JsonProperty("rataDes")
    private String rataDes;
    @JsonProperty("iuv")
    private String iuv;
    @JsonProperty("codiceAvviso")
    private String codiceAvviso;

    public Long getAaId() {
      return aaId;
    }

    public String getTassaDes() {
      return tassaDes;
    }

    public String getTassaCod() {
      return tassaCod;
    }

    public String getTipoTaxCod() {
      return tipoTaxCod;
    }

    public String getVoceDes() {
      return voceDes;
    }

    public Double getImportoVoce() {
      return importoVoce;
    }

    public String getScadenzaAddebito() {
      return scadenzaAddebito;
    }

    public Integer getScadutoFlg() {
      return scadutoFlg;
    }

    public Long getFattId() {
      return fattId;
    }

    public String getScadFattura() {
      return scadFattura;
    }

    public Integer getFattScadutaFlg() {
      return fattScadutaFlg;
    }

    public String getImportoFattura() {
      return importoFattura;
    }

    public String getDataEmissione() {
      return dataEmissione;
    }

    public Integer getPagatoFlg() {
      return pagatoFlg;
    }

    public String getDataPagamento() {
      return dataPagamento;
    }

    public Double getImportoPag() {
      return importoPag;
    }

    public Integer getAnnullataFlg() {
      return annullataFlg;
    }

    public String getRataDes() {
      return rataDes;
    }

    public String getIuv() {
      return iuv;
    }

    public String getCodiceAvviso() {
      return codiceAvviso;
    }
  }
}
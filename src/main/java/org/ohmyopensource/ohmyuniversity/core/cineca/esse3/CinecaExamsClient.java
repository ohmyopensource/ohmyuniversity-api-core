package org.ohmyopensource.ohmyuniversity.core.cineca.esse3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca ESSE3 exam-related services.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code calesa-service-v1} — exam sessions and Basic Auth bookings</li>
 *   <li>{@code libretto-service-v2} — bookable sessions and active bookings</li>
 *   <li>{@code questionari-service-v1} — teaching evaluation surveys</li>
 * </ul>
 */
@Component
public class CinecaExamsClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaExamsClient.class);

  // ============ Class Methods ============

  /**
   * Resolves tipoIscrCod which Cineca returns as either a plain String or a value-wrapper Map.
   */
  private static String resolveTipoIscrCod(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof String s) {
      return s;
    }
    if (raw instanceof Map<?, ?> map) {
      Object v = map.get("value");
      return v != null ? v.toString() : null;
    }
    return raw.toString();
  }

  /**
   * Retrieves exam sessions from {@code calesa-service-v1}.
   *
   * <p>Requires both a Bearer JWT and the Cineca auth token as a query parameter.
   * The {@code q=APPELLI_PRENOTABILI} filter restricts results to bookable sessions.
   *
   * @param baseUrl   Cineca ESSE3 base URL
   * @param jwt       Cineca JWT token
   * @param authToken Cineca session auth token
   * @param cdsId     course of study identifier
   * @param adId      teaching activity identifier
   * @param stuId     student identifier
   * @return list of exam sessions; empty if none available
   */
  public List<CinecaExamSession> getSessions(
      String baseUrl, String jwt, String authToken,
      Long cdsId, Long adId, Long stuId) {
    log.debug("CinecaExamsClient: GET sessions cdsId={} adId={} stuId={}", cdsId, adId, stuId);
    List<CinecaExamSession> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "calesa-service-v1", "appelli",
                cdsId.toString(), adId.toString())
            .queryParam("q", "APPELLI_PRENOTABILI")
            .queryParam("stuId", stuId)
            .queryParam("authToken", authToken)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: sessions 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for sessions: " + body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on sessions")))
        .bodyToFlux(CinecaExamSession.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves bookable exam sessions from {@code libretto-service-v2}.
   *
   * <p>Uses {@code q=APPELLI_PRENOTABILI_E_FUTURI}. Accessible with STUDENTE role
   * via {@code checkMatId}, bypassing the {@code checkAbildocStu} restriction that blocks
   * calesa-service on some universities (e.g. UNIMOL).
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param matId   student career segment identifier
   * @return list of bookable sessions; empty if none available
   */
  public List<CinecaBookableSession> getBookableSessions(
      String baseUrl, String jwt, Long matId) {
    log.debug("CinecaExamsClient: GET bookable sessions matId={}", matId);
    List<CinecaBookableSession> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "libretto-service-v2",
                "libretti", matId.toString(), "appelli")
            .queryParam("q", "APPELLI_PRENOTABILI_E_FUTURI")
            .queryParam("optionalFields",
                "prenotabile,prenotato,staSceCod,adStuCod,adStuDes")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: bookable sessions 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for bookable sessions matId=" + matId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on bookable sessions")))
        .bodyToFlux(CinecaBookableSession.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves active bookings from {@code libretto-service-v2}.
   *
   * <p>Returns all exam registrations for the student. Callers are responsible
   * for filtering by date or outcome as needed.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param matId   student career segment identifier
   * @return list of exam registrations; empty if none available
   */
  public List<CinecaBooking> getBookings(String baseUrl, String jwt, Long matId) {
    log.debug("CinecaExamsClient: GET bookings matId={}", matId);
    List<CinecaBooking> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "libretto-service-v2",
                "libretti", matId.toString(), "prenotazioni")
            .queryParam("optionalFields",
                "aulaDes,dataOraTurno,dataInizioIscr,dataFineIscr,adStuDes,tipoIscrCod")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: bookings 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for bookings matId=" + matId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on bookings")))
        .bodyToFlux(CinecaBooking.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  /**
   * Retrieves exam bookings from {@code calesa-service-v1} using Basic Auth.
   *
   * <p>This endpoint requires both Basic Authentication and a valid JSESSIONID cookie.
   * JWT alone is insufficient for this service.
   *
   * @param baseUrl   Cineca ESSE3 base URL
   * @param username  Cineca username
   * @param password  Cineca password (never persisted)
   * @param authToken Cineca session token (JSESSIONID)
   * @param matId     student career segment identifier
   * @return list of booking entries; empty if none available
   */
  public List<CinecaLegacyBooking> getLegacyBookings(
      String baseUrl, String username, String password,
      String authToken, Long matId) {
    log.debug("CinecaExamsClient: GET legacy bookings matId={}", matId);
    List<CinecaLegacyBooking> result = webClient.get()
        .uri(baseUrl + "/calesa-service-v1/prenotazioni/" + matId)
        .header(authHeader(), basicAuth(username, password))
        .header("Cookie", "JSESSIONID=" + authToken)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: legacy bookings 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for legacy bookings matId=" + matId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on legacy bookings")))
        .bodyToFlux(CinecaLegacyBooking.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

  // ============ DTOs ============

  /**
   * Retrieves teaching evaluation surveys from {@code questionari-service-v1}.
   *
   * @param baseUrl      Cineca ESSE3 base URL
   * @param jwt          Cineca JWT token
   * @param matId        student career segment identifier
   * @param surveyFilter {@code "C"} = pending compilation, {@code "P"} = all with surveys
   * @return list of survey entries; empty if none available
   */
  public List<CinecaSurveyRow> getSurveys(
      String baseUrl, String jwt, Long matId, String surveyFilter) {
    log.debug("CinecaExamsClient: GET surveys matId={} filter={}", matId, surveyFilter);
    List<CinecaSurveyRow> result = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "libretto", matId.toString())
            .queryParam("questFilter", surveyFilter)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: surveys 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for surveys matId=" + matId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on surveys")))
        .bodyToFlux(CinecaSurveyRow.class)
        .collectList()
        .block();
    return result != null ? result : List.of();
  }

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
  public static class CinecaExamSession {

    @JsonProperty("appId")
    private Long appId;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("dataInizioApp")
    private String startDate;
    @JsonProperty("dataFineApp")
    private String endDate;
    @JsonProperty("dataScadPren")
    private String bookingDeadline;
    @JsonProperty("tipoApp")
    private String type;
    @JsonProperty("tipoAppDes")
    private String typeDes;
    @JsonProperty("docenteDes")
    private String lecturer;
    @JsonProperty("aula")
    private String room;
    @JsonProperty("prenotabile")
    private Boolean bookable;
    @JsonProperty("prenotato")
    private Boolean booked;
    @JsonProperty("prenotazioneId")
    private Long bookingId;

    public Long getAppId() {
      return appId;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public String getStartDate() {
      return startDate;
    }

    public String getEndDate() {
      return endDate;
    }

    public String getBookingDeadline() {
      return bookingDeadline;
    }

    public String getType() {
      return type;
    }

    public String getTypeDes() {
      return typeDes;
    }

    public String getLecturer() {
      return lecturer;
    }

    public String getRoom() {
      return room;
    }

    public Boolean getBookable() {
      return bookable;
    }

    public Boolean getBooked() {
      return booked;
    }

    public Long getBookingId() {
      return bookingId;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaBookableSession {

    @JsonProperty("appId")
    private Long appId;
    @JsonProperty("appelloId")
    private Long appelloId;
    @JsonProperty("cdsId")
    private Long cdsId;
    @JsonProperty("adId")
    private Long adId;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("adStuCod")
    private String adStuCod;
    @JsonProperty("adStuDes")
    private String adStuDes;
    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("matId")
    private Long matId;
    @JsonProperty("dataInizioApp")
    private String startDate;
    @JsonProperty("dataInizioIscr")
    private String registrationStart;
    @JsonProperty("dataFineIscr")
    private String registrationEnd;
    @JsonProperty("oraEsa")
    private String examTime;
    @JsonProperty("stato")
    private String status;
    @JsonProperty("statoDes")
    private String statusDes;
    @JsonProperty("presidenteNome")
    private String presidentFirstName;
    @JsonProperty("presidenteCognome")
    private String presidentLastName;
    @JsonProperty("note")
    private String notes;
    @JsonProperty("numIscritti")
    private Integer registeredCount;
    @JsonProperty("tipoAppCod")
    private String sessionTypeCod;
    @JsonProperty("tipoIscrCod")
    private Object tipoIscrCod;
    @JsonProperty("desApp")
    private String sessionDes;

    public Long getAppId() {
      return appId;
    }

    public Long getAppelloId() {
      return appelloId;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public Long getAdId() {
      return adId;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
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

    public Long getMatId() {
      return matId;
    }

    public String getStartDate() {
      return startDate;
    }

    public String getRegistrationStart() {
      return registrationStart;
    }

    public String getRegistrationEnd() {
      return registrationEnd;
    }

    public String getExamTime() {
      return examTime;
    }

    public String getStatus() {
      return status;
    }

    public String getStatusDes() {
      return statusDes;
    }

    public String getLecturer() {
      return presidentFirstName + " " + presidentLastName;
    }

    public String getNotes() {
      return notes;
    }

    public Integer getRegisteredCount() {
      return registeredCount;
    }

    public String getSessionTypeCod() {
      return sessionTypeCod;
    }

    public String getTipoIscrCod() {
      return resolveTipoIscrCod(tipoIscrCod);
    }

    public String getSessionDes() {
      return sessionDes;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaBooking {

    @JsonProperty("applistaId")
    private Long applistaId;
    @JsonProperty("cdsId")
    private Long cdsId;
    @JsonProperty("adId")
    private Long adId;
    @JsonProperty("appId")
    private Long appId;
    @JsonProperty("adStuCod")
    private String adStuCod;
    @JsonProperty("adStuDes")
    private String adStuDes;
    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("dataOraTurno")
    private String examDateTime;
    @JsonProperty("dataInizioIscr")
    private String registrationStart;
    @JsonProperty("dataFineIscr")
    private String registrationEnd;
    @JsonProperty("aulaDes")
    private String roomDes;
    @JsonProperty("pesoAd")
    private Double credits;
    @JsonProperty("posizApp")
    private Integer position;
    @JsonProperty("domandeEsame")
    private String examQuestions;
    @JsonProperty("tipoIscrCod")
    private Object tipoIscrCod;
    @JsonProperty("esito")
    private CinecaBookingResult result;

    public Long getApplistaId() {
      return applistaId;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public Long getAdId() {
      return adId;
    }

    public Long getAppId() {
      return appId;
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

    public String getExamDateTime() {
      return examDateTime;
    }

    public String getRegistrationStart() {
      return registrationStart;
    }

    public String getRegistrationEnd() {
      return registrationEnd;
    }

    public String getRoomDes() {
      return roomDes;
    }

    public Double getCredits() {
      return credits;
    }

    public Integer getPosition() {
      return position;
    }

    public String getExamQuestions() {
      return examQuestions;
    }

    public String getTipoIscrCod() {
      return resolveTipoIscrCod(tipoIscrCod);
    }

    public CinecaBookingResult getResult() {
      return result;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaBookingResult {

    @JsonProperty("superatoFlg")
    private Integer passedFlg;
    @JsonProperty("ritiratoFlg")
    private Integer withdrawnFlg;
    @JsonProperty("assenteFlg")
    private Integer absentFlg;
    @JsonProperty("votoEsa")
    private Integer grade;
    @JsonProperty("tipoGiudCod")
    private String judgmentCod;
    @JsonProperty("tipoGiudizioDes")
    private String judgmentDes;

    public boolean isPassed() {
      return passedFlg != null && passedFlg == 1;
    }

    public boolean isWithdrawn() {
      return withdrawnFlg != null && withdrawnFlg == 1;
    }

    public boolean isAbsent() {
      return absentFlg != null && absentFlg == 1;
    }

    public Integer getGrade() {
      return grade;
    }

    public String getJudgmentCod() {
      return judgmentCod;
    }

    public String getJudgmentDes() {
      return judgmentDes;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaLegacyBooking {

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
    private String insertDate;
    @JsonProperty("dataEsa")
    private String examDate;
    @JsonProperty("dataRifEsitoStu")
    private String resultReferenceDate;
    @JsonProperty("pesoAd")
    private Double credits;
    @JsonProperty("posizApp")
    private Integer position;
    @JsonProperty("esito")
    private CinecaLegacyBookingResult result;
    @JsonProperty("presaVisione")
    private CinecaValueWrapper viewed;
    @JsonProperty("statoAdsce")
    private CinecaValueWrapper activityStatus;
    @JsonProperty("pubblId")
    private Long publicationId;
    @JsonProperty("domandeEsame")
    private String examQuestions;

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

    public String getInsertDate() {
      return insertDate;
    }

    public String getExamDate() {
      return examDate;
    }

    public String getResultReferenceDate() {
      return resultReferenceDate;
    }

    public Double getCredits() {
      return credits;
    }

    public Integer getPosition() {
      return position;
    }

    public CinecaLegacyBookingResult getResult() {
      return result;
    }

    public String getViewed() {
      return viewed != null ? viewed.getValue() : null;
    }

    public String getActivityStatus() {
      return activityStatus != null ? activityStatus.getValue() : null;
    }

    public Long getPublicationId() {
      return publicationId;
    }

    public String getExamQuestions() {
      return examQuestions;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaLegacyBookingResult {

    @JsonProperty("votoEsa")
    private Integer grade;
    @JsonProperty("superatoFlg")
    private Integer passedFlg;
    @JsonProperty("assenteFlg")
    private Integer absentFlg;
    @JsonProperty("ritiratoFlg")
    private Integer withdrawnFlg;
    @JsonProperty("tipoGiudCod")
    private String judgmentCod;
    @JsonProperty("tipoGiudizioDes")
    private String judgmentDes;
    @JsonProperty("modValCod")
    private CinecaValueWrapper evaluationMode;

    public Integer getGrade() {
      return grade;
    }

    public boolean isPassed() {
      return passedFlg != null && passedFlg == 1;
    }

    public boolean isAbsent() {
      return absentFlg != null && absentFlg == 1;
    }

    public boolean isWithdrawn() {
      return withdrawnFlg != null && withdrawnFlg == 1;
    }

    public String getJudgmentCod() {
      return judgmentCod;
    }

    public String getJudgmentDes() {
      return judgmentDes;
    }

    public String getEvaluationMode() {
      return evaluationMode != null ? evaluationMode.getValue() : null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyRow {

    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("annoCorso")
    private Integer yearOfCourse;
    @JsonProperty("peso")
    private Double credits;
    @JsonProperty("statoLink")
    private Integer surveyStatus;
    @JsonProperty("numPrenotazioni")
    private Integer bookingCount;
    @JsonProperty("matId")
    private Long matId;
    @JsonProperty("stuId")
    private Long stuId;

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public Long getAdsceId() {
      return adsceId;
    }

    public Integer getYearOfCourse() {
      return yearOfCourse;
    }

    public Double getCredits() {
      return credits;
    }

    public Integer getSurveyStatus() {
      return surveyStatus;
    }

    public Integer getBookingCount() {
      return bookingCount;
    }

    public Long getMatId() {
      return matId;
    }

    public Long getStuId() {
      return stuId;
    }
  }
}
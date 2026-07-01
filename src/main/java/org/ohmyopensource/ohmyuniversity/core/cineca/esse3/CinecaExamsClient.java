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
   * Books an exam session via {@code calesa-service-v1}.
   *
   * <p>Accessible to STUDENTE role via {@code checkAbildocStu}. This endpoint
   * requires HTTP Basic Authentication — the Bearer JWT is rejected with a 500. The Cineca password
   * is provided at request time and is never persisted.
   *
   * @param baseUrl  Cineca ESSE3 base URL
   * @param username Cineca username
   * @param password Cineca password (never persisted)
   * @param cdsId    course of study identifier
   * @param adId     teaching activity identifier
   * @param appId    exam session identifier
   * @param adsceId  libretto row identifier (activity context)
   * @throws CinecaClient.CinecaBookingException     if Cineca rejects the booking (e.g. survey not
   *                                                 filled)
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public void bookExam(
      String baseUrl, String username, String password,
      Long cdsId, Long adId, Long appId, Long adsceId) {
    log.debug("CinecaExamsClient: POST book exam cdsId={} adId={} appId={} adsceId={}",
        cdsId, adId, appId, adsceId);
    webClient.post()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "calesa-service-v1", "appelli",
                cdsId.toString(), adId.toString(), appId.toString(), "iscritti")
            .build())
        .header(authHeader(), basicAuth(username, password))
        .bodyValue(Map.of("adsceId", adsceId))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: bookExam 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on bookExam")))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Cancels an exam booking via {@code calesa-service-v1}.
   *
   * <p>Accessible to STUDENTE role via {@code applista-student-permission}.
   * Requires HTTP Basic Authentication; the Cineca password is provided at request time and is
   * never persisted.
   *
   * @param baseUrl  Cineca ESSE3 base URL
   * @param username Cineca username
   * @param password Cineca password (never persisted)
   * @param cdsId    course of study identifier
   * @param adId     teaching activity identifier
   * @param appId    exam session identifier
   * @param stuId    student career identifier
   * @throws CinecaClient.CinecaBookingException     if Cineca rejects the cancellation
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public void cancelBooking(
      String baseUrl, String username, String password,
      Long cdsId, Long adId, Long appId, Long stuId) {
    log.debug("CinecaExamsClient: DELETE cancel booking cdsId={} adId={} appId={} stuId={}",
        cdsId, adId, appId, stuId);
    webClient.delete()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "calesa-service-v1", "appelli",
                cdsId.toString(), adId.toString(), appId.toString(),
                "iscritti", stuId.toString())
            .build())
        .header(authHeader(), basicAuth(username, password))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: cancelBooking 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on cancelBooking")))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Retrieves the questionnaire metadata for a booklet activity from
   * {@code questionari-service-v1}.
   *
   * <p>Returns {@code questionarioId}, {@code questConfigId}, {@code anonimoFlg}
   * and the {@code tagsValdid} string required to start a compilation session. The
   * {@code eventCompId} is the fixed constant {@code EV_VAL_DID} for teaching evaluation
   * questionnaires.
   *
   * @param baseUrl Cineca ESSE3 base URL
   * @param jwt     Cineca JWT token
   * @param adsceId booklet activity identifier
   * @return questionnaire metadata, or {@code null} if none available
   */
  public CinecaSurveyUnit getSurveyUnit(String baseUrl, String jwt, Long adsceId) {
    log.debug("CinecaExamsClient: GET survey unit adsceId={}", adsceId);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "libretto", adsceId.toString(), "unitadidattiche")
            .queryParam("eventCompId", "EV_VAL_DID")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: survey unit 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for survey unit adsceId=" + adsceId));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on survey unit")))
        .bodyToMono(CinecaSurveyUnit.class)
        .block();
  }

  /**
   * Starts a new questionnaire compilation session via {@code questionari-service-v1} and returns
   * the first page.
   *
   * <p>Uses Bearer JWT. The {@code eventCompId} is the fixed constant
   * {@code EV_VAL_DID}. The {@code tags} string (from {@code tagsValdid}) is passed in the request
   * body. The returned page carries {@code questCompId} and {@code userCompId} that must be
   * preserved for all subsequent steps.
   *
   * @param baseUrl        Cineca ESSE3 base URL
   * @param jwt            Cineca JWT token
   * @param stuId          student career identifier
   * @param adsceId        booklet activity identifier
   * @param questionarioId questionnaire identifier
   * @param questConfigId  questionnaire configuration identifier
   * @param tags           the {@code tagsValdid} pipe-separated tag string
   * @return the first questionnaire page
   * @throws CinecaClient.CinecaBookingException     if Cineca rejects the request
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public CinecaSurveyPage startSurvey(
      String baseUrl, String jwt,
      Long stuId, Long adsceId, Long questionarioId, Long questConfigId, String tags) {
    log.debug(
        "CinecaExamsClient: PUT start survey stuId={} adsceId={} questionarioId={} questConfigId={}",
        stuId, adsceId, questionarioId, questConfigId);
    return webClient.put()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(), adsceId.toString(),
                "quest", questionarioId.toString(), "start")
            .queryParam("eventCompId", "EV_VAL_DID")
            .queryParam("questConfigId", questConfigId)
            .build())
        .header(authHeader(), bearer(jwt))
        .bodyValue(Map.of("tags", tags))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: startSurvey 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on startSurvey")))
        .bodyToMono(CinecaSurveyPage.class)
        .block();
  }

  /**
   * Saves the answers for a questionnaire page via {@code questionari-service-v1}.
   *
   * <p>Uses Bearer JWT. Each answer must carry {@code corpoRisposta} (empty string
   * for fixed-text answers); a null value triggers a NO_TEXT validation error.
   *
   * @param baseUrl        Cineca ESSE3 base URL
   * @param jwt            Cineca JWT token
   * @param stuId          student career identifier
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param pageId         page identifier
   * @param answers        the answers to save
   * @throws CinecaClient.CinecaBookingException     if Cineca rejects the answers
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public void saveSurveyPage(
      String baseUrl, String jwt, Long stuId, Long questionarioId,
      Long questCompId, Long pageId, List<?> answers) {
    log.debug("CinecaExamsClient: PUT save survey page stuId={} questCompId={} pageId={}",
        stuId, questCompId, pageId);
    webClient.put()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(),
                "quest", questionarioId.toString(), questCompId.toString(),
                "save", pageId.toString())
            .queryParam("eventCompId", "EV_VAL_DID")
            .build())
        .header(authHeader(), bearer(jwt))
        .bodyValue(answers)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: saveSurveyPage 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on saveSurveyPage")))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Retrieves an adjacent questionnaire page (next or prev) via {@code questionari-service-v1}.
   *
   * <p>Uses Bearer JWT. The page returned is determined dynamically by Cineca
   * based on the answers given so far (branching logic may apply).
   *
   * @param baseUrl        Cineca ESSE3 base URL
   * @param jwt            Cineca JWT token
   * @param stuId          student career identifier
   * @param adsceId        booklet activity identifier
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param pageId         current page identifier
   * @param userCompId     user session identifier
   * @param direction      {@code "next"} or {@code "prev"}
   * @return the adjacent page, or {@code null} if there is none
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public CinecaSurveyPage getAdjacentSurveyPage(
      String baseUrl, String jwt, Long stuId, Long adsceId, Long questionarioId,
      Long questCompId, Long pageId, Long userCompId, String direction) {
    log.debug("CinecaExamsClient: GET survey {} page stuId={} questCompId={} pageId={}",
        direction, stuId, questCompId, pageId);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(), adsceId.toString(),
                "quest", questionarioId.toString(), questCompId.toString(),
                "pagina", pageId.toString(), direction)
            .queryParam("userCompId", userCompId)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(status -> status.value() == 404, r -> {
          log.debug("CinecaExamsClient: getAdjacentSurveyPage 404 — no adjacent page");
          return Mono.empty();
        })
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: getAdjacentSurveyPage 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on getAdjacentSurveyPage")))
        .bodyToMono(CinecaSurveyPage.class)
        .block();
  }

  /**
   * Retrieves a specific questionnaire page by id via {@code questionari-service-v1}.
   *
   * @return the page, or {@code null} if not found
   */
  public CinecaSurveyPage getSurveyPage(
      String baseUrl, String jwt, Long stuId, Long adsceId, Long questionarioId,
      Long questCompId, Long pageId, Long userCompId) {
    log.debug("CinecaExamsClient: GET survey page stuId={} questCompId={} pageId={}",
        stuId, questCompId, pageId);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(), adsceId.toString(),
                "quest", questionarioId.toString(), questCompId.toString(),
                "getPagina", pageId.toString())
            .queryParam("userCompId", userCompId)
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(status -> status.value() == 404, r -> Mono.empty())
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: getSurveyPage 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on getSurveyPage")))
        .bodyToMono(CinecaSurveyPage.class)
        .block();
  }

  /**
   * Confirms and submits a completed questionnaire via {@code questionari-service-v1}.
   *
   * <p>Uses Bearer JWT. This is the final, irreversible step of the compilation flow.
   *
   * @param baseUrl        Cineca ESSE3 base URL
   * @param jwt            Cineca JWT token
   * @param stuId          student career identifier
   * @param adsceId        booklet activity identifier
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param questConfigId  questionnaire configuration identifier
   * @param userCompId     user session identifier
   * @throws CinecaClient.CinecaBookingException     if Cineca rejects the confirmation
   * @throws CinecaClient.CinecaUnavailableException if Cineca is unreachable
   */
  public void confirmSurvey(
      String baseUrl, String jwt, Long stuId, Long adsceId, Long questionarioId,
      Long questCompId, Long questConfigId, Long userCompId) {
    log.debug("CinecaExamsClient: PUT confirm survey stuId={} questCompId={}", stuId, questCompId);
    webClient.put()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(), adsceId.toString(),
                "quest", questionarioId.toString(), questCompId.toString(), "conferma")
            .queryParam("questConfigId", questConfigId)
            .queryParam("userCompId", userCompId)
            .queryParam("eventCompId", "EV_VAL_DID")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: confirmSurvey 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on confirmSurvey")))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Retrieves the compilation summary via {@code questionari-service-v1}.
   *
   * @return the summary, or {@code null} if not available
   */
  public CinecaSurveySummary getSurveySummary(
      String baseUrl, String jwt, Long stuId, Long adsceId, Long questionarioId,
      Long questCompId, Long questConfigId, Long userCompId) {
    log.debug("CinecaExamsClient: GET survey summary stuId={} questCompId={}", stuId, questCompId);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("e3rest", "api", "questionari-service-v1",
                "questionari", "compilazione", stuId.toString(), adsceId.toString(),
                "quest", questionarioId.toString(), questCompId.toString(), "summary")
            .queryParam("questConfigId", questConfigId)
            .queryParam("userCompId", userCompId)
            .queryParam("eventCompId", "EV_VAL_DID")
            .build())
        .header(authHeader(), bearer(jwt))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            r.bodyToMono(String.class).flatMap(body -> {
              log.error("CinecaExamsClient: getSurveySummary 4xx body: {}", body);
              return Mono.error(new CinecaClient.CinecaBookingException(body));
            }))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on getSurveySummary")))
        .bodyToMono(CinecaSurveySummary.class)
        .block();
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyUnit {

    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("questionarioId")
    private Long questionarioId;
    @JsonProperty("questConfigId")
    private Long questConfigId;
    @JsonProperty("questionarioDes")
    private String questionarioDes;
    @JsonProperty("des")
    private String des;
    @JsonProperty("anonimoFlg")
    private Integer anonimoFlg;
    @JsonProperty("stato")
    private Integer stato;
    @JsonProperty("udLogPdsListWeb")
    private List<CinecaSurveyUnitTag> udLogPdsListWeb;

    public Long getAdsceId() {
      return adsceId;
    }

    public Long getQuestionarioId() {
      return questionarioId;
    }

    public Long getQuestConfigId() {
      return questConfigId;
    }

    public String getQuestionarioDes() {
      return questionarioDes;
    }

    public String getDes() {
      return des;
    }

    public Integer getAnonimoFlg() {
      return anonimoFlg;
    }

    public Integer getStato() {
      return stato;
    }

    public List<CinecaSurveyUnitTag> getUdLogPdsListWeb() {
      return udLogPdsListWeb;
    }

    /**
     * Returns the {@code tagsValdid} string from the first teaching unit entry, required as the
     * body of the start request. Returns {@code null} if absent.
     */
    public String resolveTags() {
      if (udLogPdsListWeb == null || udLogPdsListWeb.isEmpty()) {
        return null;
      }
      return udLogPdsListWeb.get(0).getTagsValdid();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyUnitTag {

    @JsonProperty("adsceId")
    private Long adsceId;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("adDes")
    private String adDes;
    @JsonProperty("docentiNome")
    private String teacherFirstName;
    @JsonProperty("docentiCognome")
    private String teacherLastName;
    @JsonProperty("statoLink")
    private Integer surveyStatus;
    @JsonProperty("tagsValdid")
    private String tagsValdid;

    public Long getAdsceId() {
      return adsceId;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAdDes() {
      return adDes;
    }

    public String getTeacherFirstName() {
      return teacherFirstName;
    }

    public String getTeacherLastName() {
      return teacherLastName;
    }

    public Integer getSurveyStatus() {
      return surveyStatus;
    }

    public String getTagsValdid() {
      return tagsValdid;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyPage {

    @JsonProperty("paginaId")
    private Long paginaId;
    @JsonProperty("questCompId")
    private Long questCompId;
    @JsonProperty("userCompId")
    private Long userCompId;
    @JsonProperty("questionarioCod")
    private String questionarioCod;
    @JsonProperty("questionarioId")
    private Long questionarioId;
    @JsonProperty("pPaginaPrecId")
    private Long prevPageId;
    @JsonProperty("pPaginaSuccId")
    private Long nextPageId;
    @JsonProperty("pQuestionarioDes")
    private String questionarioDes;
    @JsonProperty("pQuestionarioNote")
    private String questionarioNote;
    @JsonProperty("des")
    private String des;
    @JsonProperty("numeroRisposte")
    private Integer answerCount;
    @JsonProperty("paragrafi")
    private List<CinecaSurveyParagraph> paragrafi;

    public Long getPaginaId() {
      return paginaId;
    }

    public Long getQuestCompId() {
      return questCompId;
    }

    public Long getUserCompId() {
      return userCompId;
    }

    public String getQuestionarioCod() {
      return questionarioCod;
    }

    public Long getQuestionarioId() {
      return questionarioId;
    }

    public Long getPrevPageId() {
      return prevPageId;
    }

    public Long getNextPageId() {
      return nextPageId;
    }

    public String getQuestionarioDes() {
      return questionarioDes;
    }

    public String getQuestionarioNote() {
      return questionarioNote;
    }

    public String getDes() {
      return des;
    }

    public Integer getAnswerCount() {
      return answerCount;
    }

    public List<CinecaSurveyParagraph> getParagrafi() {
      return paragrafi;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyParagraph {

    @JsonProperty("paragrafoId")
    private Long paragrafoId;
    @JsonProperty("paginaId")
    private Long paginaId;
    @JsonProperty("elemCod")
    private String elemCod;
    @JsonProperty("elementiDes")
    private String des;
    @JsonProperty("elementiNota")
    private String note;
    @JsonProperty("obbligatorioFlg")
    private Integer mandatoryFlg;
    @JsonProperty("domande")
    private List<CinecaSurveyQuestion> domande;

    public Long getParagrafoId() {
      return paragrafoId;
    }

    public Long getPaginaId() {
      return paginaId;
    }

    public String getElemCod() {
      return elemCod;
    }

    public String getDes() {
      return des;
    }

    public String getNote() {
      return note;
    }

    public Integer getMandatoryFlg() {
      return mandatoryFlg;
    }

    public List<CinecaSurveyQuestion> getDomande() {
      return domande;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyQuestion {

    @JsonProperty("domandaId")
    private Long domandaId;
    @JsonProperty("paragrafoId")
    private Long paragrafoId;
    @JsonProperty("elemCod")
    private String elemCod;
    @JsonProperty("elementiDes")
    private String des;
    @JsonProperty("elementiNota")
    private String note;
    @JsonProperty("obbligatorioFlg")
    private Integer mandatoryFlg;
    @JsonProperty("numMaxSce")
    private Integer maxChoices;
    @JsonProperty("tipoFormatoCod")
    private String formatCod;
    @JsonProperty("rispDisponibili")
    private List<CinecaSurveyAnswer> rispDisponibili;

    public Long getDomandaId() {
      return domandaId;
    }

    public Long getParagrafoId() {
      return paragrafoId;
    }

    public String getElemCod() {
      return elemCod;
    }

    public String getDes() {
      return des;
    }

    public String getNote() {
      return note;
    }

    public Integer getMandatoryFlg() {
      return mandatoryFlg;
    }

    public Integer getMaxChoices() {
      return maxChoices;
    }

    public String getFormatCod() {
      return formatCod;
    }

    public List<CinecaSurveyAnswer> getRispDisponibili() {
      return rispDisponibili;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveyAnswer {

    @JsonProperty("rispostaId")
    private Long rispostaId;
    @JsonProperty("domandaId")
    private Long domandaId;
    @JsonProperty("elemCod")
    private String elemCod;
    @JsonProperty("elementiDes")
    private String des;
    @JsonProperty("elementiNota")
    private String note;
    @JsonProperty("rispostaFormatoCod")
    private String answerFormatCod;
    @JsonProperty("domandaFormatoCod")
    private String questionFormatCod;
    @JsonProperty("obbligatorioFlg")
    private Integer mandatoryFlg;

    public Long getRispostaId() {
      return rispostaId;
    }

    public Long getDomandaId() {
      return domandaId;
    }

    public String getElemCod() {
      return elemCod;
    }

    public String getDes() {
      return des;
    }

    public String getNote() {
      return note;
    }

    public String getAnswerFormatCod() {
      return answerFormatCod;
    }

    public String getQuestionFormatCod() {
      return questionFormatCod;
    }

    public Integer getMandatoryFlg() {
      return mandatoryFlg;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSurveySummary {

    @JsonProperty("questCompId")
    private Long questCompId;
    @JsonProperty("questionarioId")
    private Long questionarioId;
    @JsonProperty("pQuestionarioDes")
    private String questionarioDes;
    @JsonProperty("stato")
    private String stato;
    @JsonProperty("pagine")
    private List<CinecaSummaryPage> pagine;

    public Long getQuestCompId() {
      return questCompId;
    }

    public Long getQuestionarioId() {
      return questionarioId;
    }

    public String getQuestionarioDes() {
      return questionarioDes;
    }

    public String getStato() {
      return stato;
    }

    public List<CinecaSummaryPage> getPagine() {
      return pagine;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSummaryPage {

    @JsonProperty("paginaId")
    private Long paginaId;
    @JsonProperty("paragrafiRiepilogo")
    private List<CinecaSummaryParagraph> paragrafiRiepilogo;

    public Long getPaginaId() {
      return paginaId;
    }

    public List<CinecaSummaryParagraph> getParagrafiRiepilogo() {
      return paragrafiRiepilogo;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSummaryParagraph {

    @JsonProperty("paragrafoId")
    private Long paragrafoId;
    @JsonProperty("elementiDes")
    private String des;
    @JsonProperty("domandeRiepilogo")
    private List<CinecaSummaryQuestion> domandeRiepilogo;
    @JsonProperty("risposteRiepilogo")
    private List<CinecaSummaryAnswer> risposteRiepilogo;

    public Long getParagrafoId() {
      return paragrafoId;
    }

    public String getDes() {
      return des;
    }

    public List<CinecaSummaryQuestion> getDomandeRiepilogo() {
      return domandeRiepilogo;
    }

    public List<CinecaSummaryAnswer> getRisposteRiepilogo() {
      return risposteRiepilogo;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSummaryQuestion {

    @JsonProperty("domandaId")
    private Long domandaId;
    @JsonProperty("elementiDes")
    private String des;

    public Long getDomandaId() {
      return domandaId;
    }

    public String getDes() {
      return des;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaSummaryAnswer {

    @JsonProperty("domandaId")
    private Long domandaId;
    @JsonProperty("elementiDes")
    private String des;
    @JsonProperty("testoLibero")
    private String testoLibero;

    public Long getDomandaId() {
      return domandaId;
    }

    public String getDes() {
      return des;
    }

    public String getTestoLibero() {
      return testoLibero;
    }
  }
}
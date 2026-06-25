package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookableSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookingResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaExamSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBookingResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyRow;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse.AppelloLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse.Appello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse.EsitoPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse.Prenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse.IscrizioneAppello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveysResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveysResponse.QuestionarioEsame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for exam-related operations against Cineca ESSE3.
 *
 * <p>Covers:
 * <ul>
 *   <li>exam sessions (appelli) from calesa-service-v1</li>
 *   <li>bookable sessions from libretto-service-v2</li>
 *   <li>active bookings (prenotazioni) from libretto-service-v2</li>
 *   <li>legacy booking history from calesa-service-v1 (Basic Auth)</li>
 *   <li>teaching evaluation surveys (questionari)</li>
 * </ul>
 *
 * <p>All data is fetched in real-time from Cineca; nothing is persisted locally.
 */
@Service
public class ExamsService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(ExamsService.class);

  private static final DateTimeFormatter CINECA_DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final CinecaExamsClient examsClient;

  // ============ Constructor ============

  public ExamsService(
      CinecaExamsClient examsClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.examsClient = examsClient;
  }

  // ============ Public Methods ============

  /**
   * Retrieves available exam sessions from calesa-service-v1 for a specific activity.
   *
   * @param principal authenticated OhMyU principal
   * @param cdsId     course of study identifier
   * @param adId      teaching activity identifier
   * @return available sessions response
   */
  public SessionsResponse getSessions(OmuPrincipal principal, Long cdsId, Long adId) {
    String jwt = resolveCinecaJwt(principal);
    String authToken = resolveCinecaAuthToken(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaExamSession> sessions =
        examsClient.getSessions(baseUrl, jwt, authToken, cdsId, adId, principal.stuId());

    log.debug("ExamsService: fetched {} sessions for cdsId={} adId={}", sessions.size(), cdsId,
        adId);

    SessionsResponse response = new SessionsResponse();
    response.setAppelli(sessions.stream().map(this::toAppello).toList());
    return response;
  }

  /**
   * Retrieves bookable exam sessions from libretto-service-v2.
   *
   * <p>Uses {@code APPELLI_PRENOTABILI_E_FUTURI}. Accessible with STUDENTE role,
   * bypassing the {@code checkAbildocStu} restriction on calesa-service.
   *
   * @param principal authenticated OhMyU principal
   * @return bookable sessions response
   */
  public BookableSessionsResponse getBookableSessions(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaBookableSession> sessions =
        examsClient.getBookableSessions(baseUrl, jwt, principal.matId());

    log.debug("ExamsService: fetched {} bookable sessions for matId={}", sessions.size(),
        principal.matId());

    BookableSessionsResponse response = new BookableSessionsResponse();
    response.setAppelli(sessions.stream().map(this::toAppelloLibretto).toList());
    return response;
  }

  /**
   * Retrieves active upcoming bookings from libretto-service-v2.
   *
   * <p>Filters out past exams and those already passed or withdrawn.
   *
   * @param principal authenticated OhMyU principal
   * @return active bookings response
   */
  public BookingsResponse getBookings(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaBooking> all = examsClient.getBookings(baseUrl, jwt, principal.matId());

    log.debug("ExamsService: fetched {} raw bookings for matId={}", all.size(), principal.matId());

    List<IscrizioneAppello> active = all.stream()
        .filter(b -> {
          CinecaBookingResult r = b.getResult();
          if (r != null && (r.isPassed() || r.isWithdrawn())) return false;
          String dt = b.getExamDateTime();
          if (dt == null || dt.isBlank()) return false;
          try {
            LocalDate date = LocalDate.parse(dt.split(" ")[0], CINECA_DATE_FMT);
            return !date.isBefore(LocalDate.now());
          } catch (Exception e) {
            return false;
          }
        })
        .map(this::toIscrizioneAppello)
        .toList();

    BookingsResponse response = new BookingsResponse();
    response.setPrenotazioni(active);
    return response;
  }

  /**
   * Retrieves legacy booking history from calesa-service-v1 using Basic Auth.
   *
   * <p>The Cineca password is provided at request time and is never persisted.
   *
   * @param principal authenticated OhMyU principal
   * @param password  Cineca password
   * @return full booking history response
   */
  public LegacyBookingsResponse getLegacyBookings(OmuPrincipal principal, String password) {
    String authToken = resolveCinecaAuthToken(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    String username = resolveUsername(principal);

    List<CinecaLegacyBooking> bookings =
        examsClient.getLegacyBookings(baseUrl, username, password, authToken, principal.matId());

    log.debug("ExamsService: fetched {} legacy bookings for matId={}", bookings.size(),
        principal.matId());

    LegacyBookingsResponse response = new LegacyBookingsResponse();
    response.setPrenotazioni(bookings.stream().map(this::toPrenotazione).toList());
    return response;
  }

  /**
   * Retrieves teaching evaluation surveys from questionari-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @return surveys split into pending and completed
   */
  public SurveysResponse getSurveys(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaSurveyRow> pending = examsClient.getSurveys(baseUrl, jwt, principal.matId(), "C");
    List<CinecaSurveyRow> completed = examsClient.getSurveys(baseUrl, jwt, principal.matId(), "P")
        .stream()
        .filter(r -> r.getSurveyStatus() != null && r.getSurveyStatus() == 1)
        .toList();

    log.debug("ExamsService: surveys pending={} completed={} for matId={}",
        pending.size(), completed.size(), principal.matId());

    SurveysResponse response = new SurveysResponse();
    response.setDaCompilare(pending.stream().map(this::toQuestionarioEsame).toList());
    response.setCompilati(completed.stream().map(this::toQuestionarioEsame).toList());
    return response;
  }

  // ============ Mappers ============

  private Appello toAppello(CinecaExamSession s) {
    Appello a = new Appello();
    a.setAppId(s.getAppId());
    a.setAdCod(s.getAdCod());
    a.setAdDes(s.getAdDes());
    a.setDataInizio(s.getStartDate());
    a.setDataFine(s.getEndDate());
    a.setDataScadPrenotazione(s.getBookingDeadline());
    a.setTipo(s.getType());
    a.setTipoDes(s.getTypeDes());
    a.setDocente(s.getLecturer());
    a.setAula(s.getRoom());
    a.setPrenotabile(s.getBookable());
    a.setPrenotato(s.getBooked());
    a.setPrenotazioneId(s.getBookingId());
    return a;
  }

  private AppelloLibretto toAppelloLibretto(CinecaBookableSession s) {
    AppelloLibretto a = new AppelloLibretto();
    a.setAppId(s.getAppId());
    a.setAppelloId(s.getAppelloId());
    a.setCdsId(s.getCdsId());
    a.setAdId(s.getAdId());
    a.setAdCod(s.getAdCod());
    a.setAdDes(s.getAdDes());
    a.setAdsceId(s.getAdsceId());
    a.setDataInizioApp(s.getStartDate());
    a.setDataInizioIscr(s.getRegistrationStart());
    a.setDataFineIscr(s.getRegistrationEnd());
    a.setOraEsa(s.getExamTime());
    a.setStato(s.getStatus());
    a.setStatoDes(s.getStatusDes());
    a.setDocente(s.getLecturer());
    a.setNote(s.getNotes());
    a.setNumIscritti(s.getRegisteredCount());
    a.setTipoIscrCod(s.getTipoIscrCod());
    a.setDesApp(s.getSessionDes());
    return a;
  }

  private IscrizioneAppello toIscrizioneAppello(CinecaBooking b) {
    IscrizioneAppello i = new IscrizioneAppello();
    i.setApplistaId(b.getApplistaId());
    i.setCdsId(b.getCdsId());
    i.setAdId(b.getAdId());
    i.setAppId(b.getAppId());
    i.setAdStuCod(b.getAdStuCod());
    i.setAdStuDes(b.getAdStuDes());
    i.setAdsceId(b.getAdsceId());
    i.setDataOraTurno(b.getExamDateTime());
    i.setDataInizioIscr(b.getRegistrationStart());
    i.setDataFineIscr(b.getRegistrationEnd());
    i.setAulaDes(b.getRoomDes());
    i.setTipoIscrCod(b.getTipoIscrCod());
    return i;
  }

  private Prenotazione toPrenotazione(CinecaLegacyBooking b) {
    Prenotazione p = new Prenotazione();
    p.setApplistaId(b.getApplistaId());
    p.setAdId(b.getAdId());
    p.setAdStuCod(b.getAdStuCod());
    p.setAdStuDes(b.getAdStuDes());
    p.setAdsceId(b.getAdsceId());
    p.setCdsId(b.getCdsId());
    p.setAppId(b.getAppId());
    p.setAppLogId(b.getAppLogId());
    p.setStuId(b.getStuId());
    p.setDataIns(b.getInsertDate());
    p.setDataEsa(b.getExamDate());
    p.setDataRifEsitoStu(b.getResultReferenceDate());
    p.setPesoAd(b.getCredits());
    p.setPosizApp(b.getPosition());
    p.setPresaVisione(b.getViewed());
    p.setStatoAdsce(b.getActivityStatus());
    p.setEsitoPubblicato(b.getPublicationId() != null);
    p.setDomandeEsame(b.getExamQuestions());

    CinecaLegacyBookingResult r = b.getResult();
    if (r != null) {
      EsitoPrenotazione esito = new EsitoPrenotazione();
      esito.setVotoEsa(r.getGrade());
      esito.setSuperato(r.isPassed());
      esito.setAssente(r.isAbsent());
      esito.setRitirato(r.isWithdrawn());
      esito.setTipoGiudCod(r.getJudgmentCod());
      esito.setTipoGiudizioDes(r.getJudgmentDes());
      p.setEsito(esito);
    }
    return p;
  }

  private QuestionarioEsame toQuestionarioEsame(CinecaSurveyRow r) {
    QuestionarioEsame q = new QuestionarioEsame();
    q.setAdCod(r.getAdCod());
    q.setAdDes(r.getAdDes());
    q.setAdsceId(r.getAdsceId());
    q.setAnnoCorso(r.getYearOfCourse());
    q.setCfu(r.getCredits());
    q.setStatoLink(r.getSurveyStatus());
    return q;
  }
}
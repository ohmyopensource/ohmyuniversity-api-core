package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookableSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookingResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaExamSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBookingResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSummaryParagraph;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyAnswer;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyPage;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyParagraph;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyQuestion;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyRow;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveySummary;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyUnit;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookableSessionsResponse.AppelloLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BookingsResponse.IscrizioneAppello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse.EsitoPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.LegacyBookingsResponse.Prenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SessionsResponse.Appello;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyAnswerRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyStartResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyStartResponse.SurveyAnswer;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyStartResponse.SurveyPage;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyStartResponse.SurveyParagraph;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveyStartResponse.SurveyQuestion;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveySummaryResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveySummaryResponse.SummaryItem;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SurveySummaryResponse.SummaryPage;
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
          if (r != null && (r.isPassed() || r.isWithdrawn())) {
            return false;
          }
          String dt = b.getExamDateTime();
          if (dt == null || dt.isBlank()) {
            return false;
          }
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

  /**
   * Starts a questionnaire compilation session for a booklet activity.
   *
   * <p>Two-step flow against questionari-service-v1:
   * <ol>
   *   <li>fetch questionnaire metadata (questionarioId, questConfigId, tags)
   *       via {@code unitadidattiche}</li>
   *   <li>start the compilation and return the first page</li>
   * </ol>
   *
   * @param principal authenticated OhMyU principal
   * @param adsceId   booklet activity identifier
   * @return the started session with its first page
   */
  public SurveyStartResponse startSurvey(OmuPrincipal principal, Long adsceId) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaSurveyUnit unit = examsClient.getSurveyUnit(baseUrl, jwt, adsceId);
    if (unit == null || unit.getQuestionarioId() == null || unit.getQuestConfigId() == null) {
      throw new CinecaClient.CinecaBookingException(
          "{\"retErrMsg\":\"Questionario non disponibile per questa attività\"}");
    }

    String tags = unit.resolveTags();

    CinecaSurveyPage page = examsClient.startSurvey(
        baseUrl, jwt, principal.stuId(), adsceId,
        unit.getQuestionarioId(), unit.getQuestConfigId(), tags);

    log.info("ExamsService: started survey adsceId={} questCompId={} for stuId={}",
        adsceId, page != null ? page.getQuestCompId() : null, principal.stuId());

    SurveyStartResponse response = new SurveyStartResponse();
    response.setQuestCompId(page.getQuestCompId());
    response.setUserCompId(page.getUserCompId());
    response.setQuestionarioId(unit.getQuestionarioId());
    response.setQuestConfigId(unit.getQuestConfigId());
    response.setAnonimoFlg(unit.getAnonimoFlg());
    response.setQuestionarioDes(unit.getQuestionarioDes());
    response.setPage(toSurveyPage(page));
    return response;
  }

  /**
   * Saves the answers for a questionnaire page.
   *
   * @param principal      authenticated OhMyU principal
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param pageId         page identifier
   * @param answers        answers to save
   */
  public void saveSurveyPage(OmuPrincipal principal, Long questionarioId,
      Long questCompId, Long pageId, List<SurveyAnswerRequest> answers) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    examsClient.saveSurveyPage(baseUrl, jwt, principal.stuId(),
        questionarioId, questCompId, pageId, answers);

    log.info("ExamsService: saved survey page questCompId={} pageId={} for stuId={}",
        questCompId, pageId, principal.stuId());
  }

  /**
   * Fetches an adjacent questionnaire page (next or prev).
   *
   * @param principal      authenticated OhMyU principal
   * @param adsceId        booklet activity identifier
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param pageId         current page identifier
   * @param userCompId     user session identifier
   * @param direction      {@code "next"} or {@code "prev"}
   * @return the adjacent page, or {@code null} if there is none
   */
  public SurveyPage navigateSurvey(OmuPrincipal principal, Long adsceId,
      Long questionarioId, Long questCompId, Long pageId, Long userCompId, String direction) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    String dir = "prev".equalsIgnoreCase(direction) ? "prev" : "next";

    CinecaSurveyPage page = examsClient.getAdjacentSurveyPage(
        baseUrl, jwt, principal.stuId(), adsceId, questionarioId,
        questCompId, pageId, userCompId, dir);

    log.debug("ExamsService: navigated {} to page questCompId={} for stuId={}",
        dir, questCompId, principal.stuId());

    return page == null ? null : toSurveyPage(page);
  }

  /**
   * Loads a specific questionnaire page by id (for editing from the summary).
   */
  public SurveyPage getSurveyPage(OmuPrincipal principal, Long adsceId, Long questionarioId,
      Long questCompId, Long pageId, Long userCompId) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaSurveyPage page = examsClient.getSurveyPage(baseUrl, jwt, principal.stuId(),
        adsceId, questionarioId, questCompId, pageId, userCompId);

    log.debug("ExamsService: loaded survey page {} questCompId={} for stuId={}",
        pageId, questCompId, principal.stuId());

    return page == null ? null : toSurveyPage(page);
  }

  /**
   * Confirms and submits a completed questionnaire. Irreversible.
   *
   * @param principal      authenticated OhMyU principal
   * @param adsceId        booklet activity identifier
   * @param questionarioId questionnaire identifier
   * @param questCompId    compilation session identifier
   * @param questConfigId  questionnaire configuration identifier
   * @param userCompId     user session identifier
   */
  public void confirmSurvey(OmuPrincipal principal, Long adsceId, Long questionarioId,
      Long questCompId, Long questConfigId, Long userCompId) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    examsClient.confirmSurvey(baseUrl, jwt, principal.stuId(), adsceId,
        questionarioId, questCompId, questConfigId, userCompId);

    log.info("ExamsService: confirmed survey questCompId={} for stuId={}",
        questCompId, principal.stuId());
  }

  /**
   * Retrieves the compilation summary, flattening question/answer pairs per page.
   */
  public SurveySummaryResponse getSurveySummary(OmuPrincipal principal, Long adsceId,
      Long questionarioId, Long questCompId, Long questConfigId, Long userCompId) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaSurveySummary summary = examsClient.getSurveySummary(baseUrl, jwt, principal.stuId(),
        adsceId, questionarioId, questCompId, questConfigId, userCompId);

    log.debug("ExamsService: fetched survey summary questCompId={} for stuId={}",
        questCompId, principal.stuId());

    SurveySummaryResponse response = new SurveySummaryResponse();
    if (summary == null) {
      response.setPagine(List.of());
      return response;
    }
    response.setQuestionarioDes(summary.getQuestionarioDes());
    response.setPagine(
        summary.getPagine() == null ? List.of()
            : summary.getPagine().stream().map(p -> {
              SummaryPage sp = new SummaryPage();
              sp.setPaginaId(p.getPaginaId());
              List<SummaryItem> items = new ArrayList<>();
              for (CinecaSummaryParagraph par :
                  (p.getParagrafiRiepilogo() == null ? List.<CinecaSummaryParagraph>of()
                      : p.getParagrafiRiepilogo())) {
                Map<Long, String> qDes = new HashMap<>();
                if (par.getDomandeRiepilogo() != null) {
                  par.getDomandeRiepilogo().forEach(q -> qDes.put(q.getDomandaId(), q.getDes()));
                }
                if (par.getRisposteRiepilogo() != null) {
                  par.getRisposteRiepilogo().forEach(a -> {
                    SummaryItem it = new SummaryItem();
                    it.setParagrafoDes(par.getDes());
                    it.setDomandaDes(qDes.get(a.getDomandaId()));
                    it.setRispostaDes(a.getDes());
                    it.setTestoLibero(
                        a.getTestoLibero() != null && !a.getTestoLibero().isBlank()
                            ? a.getTestoLibero() : null);
                    items.add(it);
                  });
                }
              }
              sp.setItems(items);
              return sp;
            }).toList());
    return response;
  }

  /**
   * Books an exam session via calesa-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @param cdsId     course of study identifier
   * @param adId      teaching activity identifier
   * @param appId     exam session identifier
   * @param adsceId   libretto row identifier
   */
  public void bookExam(OmuPrincipal principal, Long cdsId, Long adId, Long appId,
      Long adsceId, String password) {
    String baseUrl = resolveBaseUrl(principal.universityId());
    String username = resolveUsername(principal);

    examsClient.bookExam(baseUrl, username, password, cdsId, adId, appId, adsceId);

    log.info("ExamsService: booked exam cdsId={} adId={} appId={} stuId={}",
        cdsId, adId, appId, principal.stuId());
  }

  /**
   * Cancels an exam booking via calesa-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @param cdsId     course of study identifier
   * @param adId      teaching activity identifier
   * @param appId     exam session identifier
   */
  public void cancelBooking(OmuPrincipal principal, Long cdsId, Long adId, Long appId,
      String password) {
    String baseUrl = resolveBaseUrl(principal.universityId());
    String username = resolveUsername(principal);

    examsClient.cancelBooking(baseUrl, username, password, cdsId, adId, appId, principal.stuId());

    log.info("ExamsService: cancelled booking cdsId={} adId={} appId={} stuId={}",
        cdsId, adId, appId, principal.stuId());
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

  private SurveyPage toSurveyPage(CinecaSurveyPage p) {
    SurveyPage page = new SurveyPage();
    page.setPaginaId(p.getPaginaId());
    page.setPrevPageId(p.getPrevPageId());
    page.setNextPageId(p.getNextPageId());
    page.setDes(p.getDes());
    page.setParagrafi(
        p.getParagrafi() == null ? List.of()
            : p.getParagrafi().stream().map(this::toSurveyParagraph).toList());
    return page;
  }

  private SurveyParagraph toSurveyParagraph(CinecaSurveyParagraph p) {
    SurveyParagraph par = new SurveyParagraph();
    par.setParagrafoId(p.getParagrafoId());
    par.setDes(p.getDes());
    par.setNote(p.getNote());
    par.setDomande(
        p.getDomande() == null ? List.of()
            : p.getDomande().stream().map(this::toSurveyQuestion).toList());
    return par;
  }

  private SurveyQuestion toSurveyQuestion(CinecaSurveyQuestion q) {
    SurveyQuestion question = new SurveyQuestion();
    question.setDomandaId(q.getDomandaId());
    question.setDes(q.getDes());
    question.setMandatory(q.getMandatoryFlg() != null && q.getMandatoryFlg() == 1);
    question.setMaxChoices(q.getMaxChoices());
    question.setFormatCod(q.getFormatCod());
    question.setRisposte(
        q.getRispDisponibili() == null ? List.of()
            : q.getRispDisponibili().stream().map(this::toSurveyAnswer).toList());
    return question;
  }

  private SurveyAnswer toSurveyAnswer(CinecaSurveyAnswer a) {
    SurveyAnswer ans = new SurveyAnswer();
    ans.setRispostaId(a.getRispostaId());
    ans.setDes(a.getDes());
    ans.setFormatCod(a.getAnswerFormatCod());
    return ans;
  }
}
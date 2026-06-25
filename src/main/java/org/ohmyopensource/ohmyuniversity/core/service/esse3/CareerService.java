package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaExamResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanActivity;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanDetail;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanHeader;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookingResult;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.TranscriptResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.TranscriptResponse.RigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.GradesResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.StudyPlanResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.ExamHistoryResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.ExamHistoryResponse.EsameConStorico;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.ExamHistoryResponse.Tentativo;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.RecommendationsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for career-related academic data against Cineca ESSE3.
 *
 * <p>Covers:
 * <ul>
 *   <li>transcript (libretto) — exam records with grades and status</li>
 *   <li>grades (medie) — averages and CFU statistics</li>
 *   <li>study plan (piano) — planned academic activities</li>
 *   <li>exam history (storico) — all attempts grouped by activity</li>
 *   <li>recommendations (suggeriti) — prioritised pending exams</li>
 * </ul>
 *
 * <p>All data is fetched in real-time from Cineca; nothing is persisted locally.
 */
@Service
public class CareerService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(CareerService.class);

  private static final String TIPO_MEDIA_ARITMETICA = "A";
  private static final String TIPO_MEDIA_PESATA = "P";
  private static final DateTimeFormatter CINECA_DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final CinecaCareerClient careerClient;
  private final CinecaExamsClient examsClient;

  // ============ Constructor ============

  public CareerService(
      CinecaCareerClient careerClient,
      CinecaExamsClient examsClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.careerClient = careerClient;
    this.examsClient = examsClient;
  }

  // ============ Public Methods ============

  /**
   * Retrieves the student transcript from libretto-service-v2.
   *
   * @param principal authenticated OhMyU principal
   * @return transcript response with all exam rows
   */
  public TranscriptResponse getTranscript(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaTranscriptRow> rows = careerClient.getTranscript(baseUrl, jwt, principal.matId());
    log.debug("CareerService: fetched {} transcript rows for matId={}", rows.size(),
        principal.matId());

    TranscriptResponse response = new TranscriptResponse();
    response.setRighe(rows.stream().map(this::toRigaLibretto).toList());
    return response;
  }

  /**
   * Retrieves grade averages and CFU statistics.
   *
   * <p>Fetches both medie from Cineca and the full transcript to compute
   * CFU progress metrics.
   *
   * @param principal authenticated OhMyU principal
   * @return aggregated grade and CFU statistics
   */
  public GradesResponse getGrades(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    var medie = careerClient.getGrades(baseUrl, jwt, principal.matId());
    var rows = careerClient.getTranscript(baseUrl, jwt, principal.matId());

    log.debug("CareerService: fetched {} grade entries for matId={}", medie.size(),
        principal.matId());

    GradesResponse response = new GradesResponse();
    for (var m : medie) {
      String tipo = m.getTipoMediaCod();
      Integer base = m.getBase();
      if (tipo == null || base == null) continue;
      if (TIPO_MEDIA_ARITMETICA.equals(tipo) && base == 30) {
        response.setMediaAritmetica(m.getMedia());
      } else if (TIPO_MEDIA_PESATA.equals(tipo) && base == 30) {
        response.setMediaPesata(m.getMedia());
      } else if (TIPO_MEDIA_ARITMETICA.equals(tipo) && base == 110) {
        response.setBaseMax110(m.getMedia());
      }
    }

    long passed = rows.stream().filter(r -> "S".equals(r.getStato())).count();
    double cfuAcq = rows.stream()
        .filter(r -> "S".equals(r.getStato()) && r.getPeso() != null)
        .mapToDouble(CinecaTranscriptRow::getPeso).sum();
    double cfuTot = rows.stream()
        .filter(r -> r.getPeso() != null)
        .mapToDouble(CinecaTranscriptRow::getPeso).sum();

    response.setEsamiSuperati((int) passed);
    response.setEsamiTotali(rows.size());
    response.setCfu(cfuAcq);
    response.setCfuTotali(cfuTot);
    if (cfuTot > 0) {
      response.setPercentualeCfu(Math.round((cfuAcq / cfuTot) * 1000.0) / 10.0);
    }
    return response;
  }

  /**
   * Retrieves the student's study plan from piani-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @return study plan response; empty if no plan exists
   */
  public StudyPlanResponse getStudyPlan(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaStudyPlanHeader> headers =
        careerClient.getStudyPlanHeaders(baseUrl, jwt, principal.stuId());

    if (headers.isEmpty()) {
      log.warn("CareerService: no study plan found for stuId={}", principal.stuId());
      StudyPlanResponse empty = new StudyPlanResponse();
      empty.setRighe(List.of());
      return empty;
    }

    Long pianoId = headers.get(0).getPianoId();
    CinecaStudyPlanDetail detail =
        careerClient.getStudyPlanDetail(baseUrl, jwt, principal.stuId(), pianoId);

    log.debug("CareerService: fetched study plan pianoId={} for stuId={}", pianoId,
        principal.stuId());

    StudyPlanResponse response = new StudyPlanResponse();
    response.setRighe(detail == null ? List.of()
        : detail.getActivities().stream().map(this::toRigaPiano).toList());
    return response;
  }

  /**
   * Retrieves the complete exam attempt history grouped by course activity.
   *
   * <p>Uses libretto-service-v2 prenotazioni — same source as bookings,
   * but groups all attempts (past and future) per activity.
   *
   * @param principal authenticated OhMyU principal
   * @return exam history response grouped by adsceId
   */
  public ExamHistoryResponse getExamHistory(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaBooking> all = examsClient.getBookings(baseUrl, jwt, principal.matId());
    log.debug("CareerService: building exam history from {} entries for matId={}",
        all.size(), principal.matId());

    Map<Long, EsameConStorico> map = new LinkedHashMap<>();
    for (CinecaBooking b : all) {
      Long key = b.getAdsceId();
      if (key == null) continue;

      EsameConStorico esame = map.computeIfAbsent(key, k -> {
        EsameConStorico e = new EsameConStorico();
        e.setAdCod(b.getAdStuCod());
        e.setAdDes(b.getAdStuDes());
        e.setAdsceId(k);
        e.setCfu(b.getCredits());
        e.setTentativi(new ArrayList<>());
        return e;
      });

      Tentativo t = new Tentativo();
      t.setApplistaId(b.getApplistaId());
      t.setDataOraTurno(b.getExamDateTime());
      t.setDataInizioIscr(b.getRegistrationStart());
      t.setDataFineIscr(b.getRegistrationEnd());
      t.setTipoIscrCod(b.getTipoIscrCod());
      t.setDomandeEsame(b.getExamQuestions());
      t.setPosizApp(b.getPosition());

      String dataOra = b.getExamDateTime();
      if (dataOra != null && !dataOra.isBlank()) {
        try {
          LocalDate data = LocalDate.parse(dataOra.split(" ")[0], CINECA_DATE_FMT);
          t.setFuturo(!data.isBefore(LocalDate.now()));
        } catch (Exception ignored) { }
      }

      CinecaBookingResult result = b.getResult();
      if (result != null) {
        t.setSuperato(result.isPassed());
        t.setRitirato(result.isWithdrawn());
        t.setAssente(result.isAbsent());
        t.setVotoEsa(result.getGrade());
        t.setTipoGiudCod(result.getJudgmentCod());
        t.setTipoGiudizioDes(result.getJudgmentDes());
      }

      esame.getTentativi().add(t);
    }

    ExamHistoryResponse response = new ExamHistoryResponse();
    response.setEsami(new ArrayList<>(map.values()));
    return response;
  }

  /**
   * Returns an ordered list of recommended exams for the authenticated student.
   *
   * <p>Compares the study plan against the transcript, excludes passed activities,
   * and ranks remaining exams by {@code (annoCorso * 100) + cfu} — lower is better.
   *
   * @param principal authenticated OhMyU principal
   * @return ordered recommendations; empty if no valid study plan exists
   */
  public RecommendationsResponse getRecommendations(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaTranscriptRow> rows = careerClient.getTranscript(baseUrl, jwt, principal.matId());
    Set<String> passed = rows.stream()
        .filter(r -> "S".equals(r.getStato()) && r.getAdCod() != null)
        .map(CinecaTranscriptRow::getAdCod)
        .collect(Collectors.toSet());

    List<CinecaStudyPlanHeader> headers =
        careerClient.getStudyPlanHeaders(baseUrl, jwt, principal.stuId());
    if (headers.isEmpty()) {
      RecommendationsResponse empty = new RecommendationsResponse();
      empty.setEsami(List.of());
      return empty;
    }

    CinecaStudyPlanDetail detail =
        careerClient.getStudyPlanDetail(baseUrl, jwt, principal.stuId(),
            headers.get(0).getPianoId());

    List<RecommendationsResponse.EsameSuggerito> suggestions = detail == null ? List.of()
        : detail.getActivities().stream()
            .filter(a -> a.getAdCod() != null && !passed.contains(a.getAdCod()))
            .map(a -> {
              RecommendationsResponse.EsameSuggerito s = new RecommendationsResponse.EsameSuggerito();
              s.setAdCod(a.getAdCod());
              s.setAdDes(a.getAdDes());
              s.setCfu(a.getCfu());
              s.setAnnoCorso(a.getAnnoCorso());
              int anno = a.getAnnoCorso() != null ? a.getAnnoCorso() : 99;
              int cfu = a.getCfu() != null ? a.getCfu().intValue() : 99;
              s.setScore((anno * 100) + cfu);
              return s;
            })
            .sorted(java.util.Comparator.comparingInt(RecommendationsResponse.EsameSuggerito::getScore))
            .toList();

    log.debug("CareerService: {} recommendations for stuId={}", suggestions.size(),
        principal.stuId());

    RecommendationsResponse response = new RecommendationsResponse();
    response.setEsami(suggestions);
    return response;
  }

  // ============ Mappers ============

  private RigaLibretto toRigaLibretto(CinecaTranscriptRow r) {
    RigaLibretto riga = new RigaLibretto();
    riga.setAdsceId(r.getAdsceId());
    riga.setAdCod(r.getAdCod());
    riga.setAdDes(r.getAdDes());
    riga.setAnnoCorso(r.getAnnoCorso());
    riga.setStato(r.getStato());
    riga.setStatoDes(r.getStatoDes());
    riga.setPeso(r.getPeso());
    riga.setTipoInsCod(r.getTipoInsCod());
    riga.setTipoInsDes(r.getTipoInsDes());
    riga.setNumAppelliPrenotabili(r.getNumAppelliPrenotabili());
    riga.setSuperata("S".equals(r.getStato()));

    CinecaExamResult esito = r.getEsito();
    if (esito != null) {
      riga.setVoto(esito.getVoto() != null ? esito.getVoto().intValue() : null);
      riga.setLode(esito.getLodeFlg() != null && esito.getLodeFlg() == 1);
      riga.setDataEsame(esito.getDataEsa());
    }
    return riga;
  }

  private StudyPlanResponse.RigaPiano toRigaPiano(CinecaStudyPlanActivity a) {
    StudyPlanResponse.RigaPiano riga = new StudyPlanResponse.RigaPiano();
    riga.setAdsceId(a.getAdsceId());
    riga.setAdCod(a.getAdCod());
    riga.setAdDes(a.getAdDes());
    riga.setAnnoCorso(a.getAnnoCorso());
    riga.setCfu(a.getCfu());
    riga.setObbligatorio(a.isRequired());
    return riga;
  }
}
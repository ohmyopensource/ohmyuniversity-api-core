package org.ohmyopensource.ohmyuniversity.core.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAddebito;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAppello;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAppelloLibretto;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAttivitaPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaMedia;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaPianoDettaglio;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaRigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaSemaforo;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaTestataPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.AppelliLibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.AppelloResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.CarrieraInfoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse.RigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse.EsitoPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse.Prenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioniLibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.ProfiloResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.QuestionariResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.StoricoEsamiResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.SuggerimentiResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse.Addebito;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse.VoceTassa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for orchestrating career-related operations against Cineca ESSE3 APIs.
 *
 * <p>Every request retrieves live academic data using the authenticated Cineca session associated
 * with the current OhMyUniversity user.
 *
 * <p>No academic information is persisted locally inside the application.
 */
@Service
public class CarrieraService {

  private static final Logger log = LoggerFactory.getLogger(CarrieraService.class);

  private static final String TIPO_MEDIA_ARITMETICA = "A";
  private static final String TIPO_MEDIA_PESATA = "P";
  private static final DateTimeFormatter CINECA_DATE_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private final CinecaCarrieraClient cinecaClient;
  private final CinecaSessionStore sessionStore;
  private final UniversityRegistry universityRegistry;
  private final UniversityConnectionRepository connectionRepository;

  // ============ Constructor ============

  /**
   * Creates a new career service instance.
   *
   * @param cinecaClient         Cineca career API client
   * @param sessionStore         Cineca session storage service
   * @param universityRegistry   university configuration registry
   * @param connectionRepository repository used to resolve Cineca connections
   */
  public CarrieraService(
      CinecaCarrieraClient cinecaClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    this.cinecaClient = cinecaClient;
    this.sessionStore = sessionStore;
    this.universityRegistry = universityRegistry;
    this.connectionRepository = connectionRepository;
  }

  // ============ Class Methods ============

  /**
   * Retrieves the complete student transcript from Cineca.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return transcript response containing all exam entries
   */
  public LibrettoResponse getLibretto(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaRigaLibretto> righe = cinecaClient.getRigheLibretto(
        baseUrl, cinecaJwt, principal.matId());

    log.debug("CarrieraService: fetched {} righe libretto for matId={}",
        righe.size(), principal.matId());

    LibrettoResponse response = new LibrettoResponse();
    response.setRighe(righe.stream().map(this::toRigaLibretto).toList());
    return response;
  }

  /**
   * Retrieves academic statistics and average calculations for the student.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return aggregated academic statistics response
   */
  public MediaResponse getMedia(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaMedia> medie = cinecaClient.getMedia(baseUrl, cinecaJwt, principal.matId());

    log.debug("CarrieraService: fetched {} medie rows for matId={}", medie.size(),
        principal.matId());

    MediaResponse response = new MediaResponse();

    for (CinecaMedia m : medie) {
      String tipo = m.getTipoMediaCod();
      Integer base = m.getBase();
      if (tipo == null || base == null) {
        continue;
      }

      if (TIPO_MEDIA_ARITMETICA.equals(tipo) && base == 30) {
        response.setMediaAritmetica(m.getMedia());
      } else if (TIPO_MEDIA_PESATA.equals(tipo) && base == 30) {
        response.setMediaPesata(m.getMedia());
      } else if (TIPO_MEDIA_ARITMETICA.equals(tipo) && base == 110) {
        response.setBaseMax110(m.getMedia());
      }
    }

    List<CinecaRigaLibretto> righe = cinecaClient.getRigheLibretto(
        baseUrl, cinecaJwt, principal.matId());

    long superati = righe.stream()
        .filter(r -> "S".equals(r.getStato()))
        .count();
    double cfuAcq = righe.stream()
        .filter(r -> "S".equals(r.getStato()) && r.getPeso() != null)
        .mapToDouble(CinecaRigaLibretto::getPeso)
        .sum();
    double cfuTot = righe.stream()
        .filter(r -> r.getPeso() != null)
        .mapToDouble(CinecaRigaLibretto::getPeso)
        .sum();

    response.setEsamiSuperati((int) superati);
    response.setEsamiTotali(righe.size());
    response.setCfu(cfuAcq);
    response.setCfuTotali(cfuTot);

    if (cfuTot > 0) {
      response.setPercentualeCfu(Math.round((cfuAcq / cfuTot) * 1000.0) / 10.0);
    }

    return response;
  }

  /**
   * Retrieves the student's study plan from Cineca.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return study plan response
   */
  public PianoStudioResponse getPiano(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaTestataPiano> headers = cinecaClient.getPianoHeaders(
        baseUrl, cinecaJwt, principal.stuId());

    if (headers.isEmpty()) {
      log.warn("CarrieraService: no piano found for stuId={}", principal.stuId());
      PianoStudioResponse empty = new PianoStudioResponse();
      empty.setRighe(List.of());
      return empty;
    }

    Long pianoId = headers.get(0).getPianoId();
    CinecaPianoDettaglio dettaglio = cinecaClient.getPianoDettaglio(
        baseUrl, cinecaJwt, principal.stuId(), pianoId);

    log.debug("CarrieraService: fetched piano pianoId={} for stuId={}", pianoId,
        principal.stuId());

    PianoStudioResponse response = new PianoStudioResponse();
    if (dettaglio != null) {
      response.setRighe(dettaglio.getAttivita().stream()
          .map(this::toRigaPiano).toList());
    } else {
      response.setRighe(List.of());
    }
    return response;
  }

  /**
   * Retrieves all available exam sessions for the specified activity.
   *
   * @param principal authenticated OhMyUniversity principal
   * @param cdsId     course identifier
   * @param adId      teaching activity identifier
   * @return available exam sessions response
   */
  public AppelloResponse getAppelli(OmuPrincipal principal, Long cdsId, Long adId) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String cinecaAuthToken = resolveCinecaAuthToken(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaAppello> appelli = cinecaClient.getAppelli(
        baseUrl, cinecaJwt, cinecaAuthToken, cdsId, adId, principal.stuId());

    log.debug("CarrieraService: fetched {} appelli for cdsId={} adId={}",
        appelli.size(), cdsId, adId);

    AppelloResponse response = new AppelloResponse();
    response.setAppelli(appelli.stream().map(this::toAppello).toList());
    return response;
  }

  /**
   * Retrieves all bookable exam sessions for the student using the libretto-service endpoint.
   *
   * <p>Unlike {@link #getAppelli}, this method uses
   * /libretto-service-v2/libretti/{matId}/appelli which is accessible to STUDENTE role via
   * checkMatId, bypassing the checkAbildocStu restriction that blocks the calesa-service endpoint
   * on some universities (e.g. UNIMOL).
   *
   * @param principal authenticated OhMyUniversity principal
   * @return response containing all bookable and future exam sessions
   */
  public AppelliLibrettoResponse getAppelliPrenotabili(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaAppelloLibretto> appelli = cinecaClient.getAppelliLibretto(
        baseUrl, cinecaJwt, principal.matId());

    log.debug("CarrieraService: fetched {} appelli prenotabili for matId={}",
        appelli.size(), principal.matId());

    AppelliLibrettoResponse response = new AppelliLibrettoResponse();
    response.setAppelli(appelli.stream().map(this::toAppelloLibretto).toList());
    return response;
  }

  /**
   * Retrieves the student's exam booking history.
   *
   * <p>The Cineca password is provided at request time and is never persisted.
   *
   * @param principal      authenticated OhMyUniversity principal
   * @param cinecaPassword Cineca password provided by the client
   * @return booking history response
   */
  public PrenotazioneResponse getPrenotazioni(OmuPrincipal principal, String cinecaPassword) {
    String cinecaAuthToken = resolveCinecaAuthToken(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    String cinecaUsername = resolveUsername(principal);

    List<CinecaPrenotazione> prenotazioni = cinecaClient.getPrenotazioni(
        baseUrl, cinecaUsername, cinecaPassword, cinecaAuthToken, principal.matId());

    log.debug("CarrieraService: fetched {} prenotazioni for matId={}",
        prenotazioni.size(), principal.matId());

    PrenotazioneResponse response = new PrenotazioneResponse();
    response.setPrenotazioni(prenotazioni.stream().map(this::toPrenotazione).toList());
    return response;
  }

  /**
   * Retrieves the student's tuition fee situation from Cineca.
   *
   * <p>Resolves the Cineca JWT and university ESSE3 base URL from the
   * authenticated principal, then fetches: - the semaforo summary containing payment status and due
   * amounts - the full list of accounting charges (addebiti)
   *
   * <p>The Cineca response is mapped into {@link TasseResponse},
   * converting Cineca fee entries into internal DTOs.
   *
   * @param principal authenticated OhMyUniversity principal containing university and student
   *                  identifiers
   * @return aggregated tuition fee and accounting information
   */
  public TasseResponse getTasse(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaSemaforo semaforo = cinecaClient.getSemaforo(
        baseUrl, cinecaJwt, principal.stuId());

    List<CinecaAddebito> addebiti = cinecaClient.getAddebiti(
        baseUrl, cinecaJwt, principal.stuId());

    log.debug("CarrieraService: fetched tasse semaforo={} addebiti={} for stuId={}",
        semaforo != null ? semaforo.getSemaforo() : "null",
        addebiti.size(), principal.stuId());

    TasseResponse response = new TasseResponse();

    if (semaforo != null) {
      response.setSemaforo(semaforo.getSemaforo());
      response.setImportoDovuto(semaforo.getImportoDovuto());
      response.setTasseScadute(semaforo.getTasseScadute() == null ? List.of() :
          semaforo.getTasseScadute().stream().map(this::toVoceTassa).toList());
      response.setTasseDovute(semaforo.getTasseDovute() == null ? List.of() :
          semaforo.getTasseDovute().stream().map(this::toVoceTassa).toList());
    }

    response.setAddebiti(addebiti.stream().map(this::toAddebito).toList());
    return response;
  }

  /**
   * Retrieves the student's digital university badge.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return badge response or null if no badge exists
   */
  public BadgeResponse getBadge(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaBadge> badges = cinecaClient.getBadges(baseUrl, cinecaJwt, principal.stuId());

    if (badges.isEmpty()) {
      log.warn("CarrieraService: no badge found for stuId={}", principal.stuId());
      return null;
    }

    return toBadgeResponse(badges.get(0));
  }

  /**
   * Returns an ordered list of suggested exams for the authenticated student.
   *
   * <p>The suggestion process compares the student's study plan with the
   * academic record (libretto) and excludes all activities already passed. Remaining exams are
   * ranked using a convenience score that prioritizes lower-year activities and, within the same
   * year, lower-CFU exams.
   *
   * <p>Score formula:
   * {@code (annoCorso * 100) + cfu}. Lower scores are suggested first.
   *
   * @param principal authenticated OhMyU principal
   * @return ordered list of suggested exams, or an empty list if no valid study plan is available
   */
  public SuggerimentiResponse getEsamiSuggeriti(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaRigaLibretto> righe = cinecaClient.getRigheLibretto(
        baseUrl, cinecaJwt, principal.matId());

    Set<String> superati = righe.stream()
        .filter(r -> "S".equals(r.getStato()) && r.getAdCod() != null)
        .map(CinecaRigaLibretto::getAdCod)
        .collect(java.util.stream.Collectors.toSet());

    List<CinecaTestataPiano> headers = cinecaClient.getPianoHeaders(
        baseUrl, cinecaJwt, principal.stuId());

    if (headers.isEmpty()) {
      SuggerimentiResponse empty = new SuggerimentiResponse();
      empty.setEsami(List.of());
      return empty;
    }

    CinecaPianoDettaglio dettaglio = cinecaClient.getPianoDettaglio(
        baseUrl, cinecaJwt, principal.stuId(), headers.get(0).getPianoId());

    List<SuggerimentiResponse.EsameSuggerito> suggeriti = dettaglio == null
        ? List.of()
        : dettaglio.getAttivita().stream()
            .filter(a -> a.getAdCod() != null && !superati.contains(a.getAdCod()))
            .map(a -> {
              SuggerimentiResponse.EsameSuggerito s = new SuggerimentiResponse.EsameSuggerito();
              s.setAdCod(a.getAdCod());
              s.setAdDes(a.getAdDes());
              s.setCfu(a.getCfu());
              s.setAnnoCorso(a.getAnnoCorso());
              int anno = a.getAnnoCorso() != null ? a.getAnnoCorso() : 99;
              int cfu = a.getCfu() != null ? a.getCfu().intValue() : 99;
              s.setScore((anno * 100) + cfu);
              return s;
            })
            .sorted(
                java.util.Comparator.comparingInt(SuggerimentiResponse.EsameSuggerito::getScore))
            .toList();

    log.debug("CarrieraService: {} esami suggeriti for stuId={}", suggeriti.size(),
        principal.stuId());

    SuggerimentiResponse response = new SuggerimentiResponse();
    response.setEsami(suggeriti);
    return response;
  }

  /**
   * Resolves the Cineca JWT associated with the current user session.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return Cineca JWT token
   */
  private String resolveCinecaJwt(OmuPrincipal principal) {
    return sessionStore.getCinecaJwt(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "Cineca session expired — please log in again"));
  }

  /**
   * Resolves the Cineca authentication token associated with the current user.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return Cineca authentication token
   */
  private String resolveCinecaAuthToken(OmuPrincipal principal) {
    return sessionStore.getCinecaAuthToken(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "Cineca auth token expired — please log in again"));
  }

  /**
   * Resolves the configured Cineca base URL for the specified university.
   *
   * @param universityId university identifier
   * @return Cineca base URL
   */
  private String resolveBaseUrl(String universityId) {
    return universityRegistry.resolve(universityId)
        .map(UniversityRegistry.UniversityConfig::baseUrl)
        .orElseThrow(() -> new IllegalArgumentException("Unknown university: " + universityId));
  }

  /**
   * Resolves the Cineca username associated with the current university.
   *
   * @param principal authenticated OhMyUniversity principal
   * @return Cineca username
   */
  private String resolveUsername(OmuPrincipal principal) {
    return connectionRepository
        .findByUserId(java.util.UUID.fromString(principal.omuUserId()))
        .stream()
        .filter(c -> c.getUniversityId().equals(principal.universityId()))
        .map(UniversityConnection::getUsernameCineca)
        .findFirst()
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "No Cineca connection found for university: " + principal.universityId()));
  }

  /**
   * Maps a Cineca transcript row into the public API DTO.
   *
   * @param r source Cineca transcript row
   * @return mapped transcript DTO
   */
  private RigaLibretto toRigaLibretto(CinecaRigaLibretto r) {
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

    if (r.getEsito() != null) {
      Double votoDouble = r.getEsito().getVoto();
      riga.setVoto(votoDouble != null ? votoDouble.intValue() : null);
      riga.setLode(r.getEsito().getLodeFlg() != null && r.getEsito().getLodeFlg() == 1);
      riga.setDataEsame(r.getEsito().getDataEsa());
    }

    return riga;
  }

  /**
   * Maps a Cineca study plan activity into the public API DTO.
   *
   * @param r source Cineca study plan activity
   * @return mapped study plan row
   */
  private PianoStudioResponse.RigaPiano toRigaPiano(CinecaAttivitaPiano r) {
    PianoStudioResponse.RigaPiano riga = new PianoStudioResponse.RigaPiano();
    riga.setAdsceId(r.getAdsceId());
    riga.setAdCod(r.getAdCod());
    riga.setAdDes(r.getAdDes());
    riga.setAnnoCorso(r.getAnnoCorso());
    riga.setCfu(r.getCfu());
    riga.setObbligatorio(r.isObbligatorio());
    return riga;
  }

  /**
   * Maps a Cineca exam session into the public API DTO.
   *
   * @param a source Cineca exam session
   * @return mapped exam session DTO
   */
  private AppelloResponse.Appello toAppello(CinecaAppello a) {
    AppelloResponse.Appello app = new AppelloResponse.Appello();
    app.setAppId(a.getAppId());
    app.setAdCod(a.getAdCod());
    app.setAdDes(a.getAdDes());
    app.setDataInizio(a.getDataInizio());
    app.setDataFine(a.getDataFine());
    app.setDataScadPrenotazione(a.getDataScadPrenotazione());
    app.setTipo(a.getTipo());
    app.setTipoDes(a.getTipoDes());
    app.setDocente(a.getDocente());
    app.setAula(a.getAula());
    app.setPrenotabile(a.getPrenotabile());
    app.setPrenotato(a.getPrenotato());
    app.setPrenotazioneId(a.getPrenotazioneId());
    return app;
  }

  /**
   * Maps a Cineca libretto-based exam session into the public API DTO.
   *
   * @param a source Cineca exam session from libretto-service
   * @return mapped exam session DTO
   */
  private AppelliLibrettoResponse.AppelloLibretto toAppelloLibretto(CinecaAppelloLibretto a) {
    AppelliLibrettoResponse.AppelloLibretto app = new AppelliLibrettoResponse.AppelloLibretto();
    app.setAppId(a.getAppId());
    app.setAppelloId(a.getAppelloId());
    app.setCdsId(a.getCdsId());
    app.setAdId(a.getAdId());
    app.setAdCod(a.getAdCod());
    app.setAdDes(a.getAdDes());
    app.setAdsceId(a.getAdsceId());
    app.setDataInizioApp(a.getDataInizioApp());
    app.setDataInizioIscr(a.getDataInizioIscr());
    app.setDataFineIscr(a.getDataFineIscr());
    app.setOraEsa(a.getOraEsa());
    app.setStato(a.getStato());
    app.setStatoDes(a.getStatoDes());
    app.setDocente(a.getPresidenteNome() + " " + a.getPresidenteCognome());
    app.setNote(a.getNote());
    app.setNumIscritti(a.getNumIscritti());
    app.setTipoIscrCod(a.getTipoIscrCod());
    app.setDesApp(a.getDesApp());
    return app;
  }

  /**
   * Retrieves current exam bookings from libretto-service without requiring password.
   */
  public PrenotazioniLibrettoResponse getPrenotazioniLibretto(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaCarrieraClient.CinecaIscrizioneAppello> all =
        cinecaClient.getPrenotazioniLibretto(baseUrl, cinecaJwt, principal.matId());

    log.debug("CarrieraService: fetched {} raw prenotazioni for matId={}",
        all.size(), principal.matId());

    List<PrenotazioniLibrettoResponse.IscrizioneAppello> attive = all.stream()
        .filter(i -> {
          if (i.getEsito() != null &&
              (i.getEsito().isSuperato() || i.getEsito().isRitirato())) {
            return false;
          }
          String dataOra = i.getDataOraTurno();
          if (dataOra == null || dataOra.isBlank()) {
            return false;
          }
          try {
            LocalDate data = LocalDate.parse(dataOra.split(" ")[0], CINECA_DATE_FMT);
            return !data.isBefore(LocalDate.now());
          } catch (Exception e) {
            return false;
          }
        })
        .map(this::toIscrizioneAppello)
        .toList();

    PrenotazioniLibrettoResponse response = new PrenotazioniLibrettoResponse();
    response.setPrenotazioni(attive);
    return response;
  }

  /**
   * Returns full exam attempt history grouped by course activity.
   */
  public StoricoEsamiResponse getStoricoEsami(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaCarrieraClient.CinecaIscrizioneAppello> all =
        cinecaClient.getPrenotazioniLibretto(baseUrl, cinecaJwt, principal.matId());

    log.debug("CarrieraService: building storico from {} prenotazioni for matId={}",
        all.size(), principal.matId());

    Map<Long, StoricoEsamiResponse.EsameConStorico> map = new LinkedHashMap<>();

    for (CinecaCarrieraClient.CinecaIscrizioneAppello i : all) {
      Long key = i.getAdsceId();
      if (key == null) {
        continue;
      }

      StoricoEsamiResponse.EsameConStorico esame = map.computeIfAbsent(key, k -> {
        StoricoEsamiResponse.EsameConStorico e = new StoricoEsamiResponse.EsameConStorico();
        e.setAdCod(i.getAdStuCod());
        e.setAdDes(i.getAdStuDes());
        e.setAdsceId(k);
        e.setCfu(i.getPesoAd());
        e.setTentativi(new ArrayList<>());
        return e;
      });

      StoricoEsamiResponse.Tentativo t = new StoricoEsamiResponse.Tentativo();
      t.setApplistaId(i.getApplistaId());
      t.setDataOraTurno(i.getDataOraTurno());
      t.setDataInizioIscr(i.getDataInizioIscr());
      t.setDataFineIscr(i.getDataFineIscr());
      t.setTipoIscrCod(i.getTipoIscrCod());
      t.setDomandeEsame(i.getDomandeEsame());
      t.setPosizApp(i.getPosizApp());

      String dataOra = i.getDataOraTurno();
      if (dataOra != null && !dataOra.isBlank()) {
        try {
          LocalDate data = LocalDate.parse(dataOra.split(" ")[0], CINECA_DATE_FMT);
          t.setFuturo(!data.isBefore(LocalDate.now()));
        } catch (Exception ignored) {
        }
      }

      if (i.getEsito() != null) {
        t.setSuperato(i.getEsito().isSuperato());
        t.setRitirato(i.getEsito().isRitirato());
        t.setAssente(i.getEsito().isAssente());
        t.setVotoEsa(i.getEsito().getVotoEsa());
        t.setTipoGiudCod(i.getEsito().getTipoGiudCod());
        t.setTipoGiudizioDes(i.getEsito().getTipoGiudizioDes());
      }

      esame.getTentativi().add(t);
    }

    StoricoEsamiResponse response = new StoricoEsamiResponse();
    response.setEsami(new ArrayList<>(map.values()));
    return response;
  }

  private PrenotazioniLibrettoResponse.IscrizioneAppello toIscrizioneAppello(
      CinecaCarrieraClient.CinecaIscrizioneAppello i) {
    PrenotazioniLibrettoResponse.IscrizioneAppello p =
        new PrenotazioniLibrettoResponse.IscrizioneAppello();
    p.setApplistaId(i.getApplistaId());
    p.setCdsId(i.getCdsId());
    p.setAdId(i.getAdId());
    p.setAppId(i.getAppId());
    p.setAdStuCod(i.getAdStuCod());
    p.setAdStuDes(i.getAdStuDes());
    p.setAdsceId(i.getAdsceId());
    p.setDataOraTurno(i.getDataOraTurno());
    p.setDataInizioIscr(i.getDataInizioIscr());
    p.setDataFineIscr(i.getDataFineIscr());
    p.setAulaDes(i.getAulaDes());
    p.setTipoIscrCod(i.getTipoIscrCod());
    return p;
  }

  /**
   * Retrieves full personal profile for the authenticated student.
   */
  public ProfiloResponse getProfilo(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    Long persId = sessionStore.getCinecaPersId(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException(
            "PersId not found in session — please log in again"));

    CinecaCarrieraClient.CinecaPersona p = cinecaClient.getPersona(baseUrl, cinecaJwt, persId);
    if (p == null) {
      throw new CinecaClient.CinecaAuthException("Persona not found for persId=" + persId);
    }

    log.debug("CarrieraService: fetched profilo for persId={}", persId);

    ProfiloResponse r = new ProfiloResponse();
    r.setPersId(p.getPersId());
    r.setNome(p.getNome());
    r.setCognome(p.getCognome());
    r.setCodFis(p.getCodFis());
    r.setDataNascita(p.getDataNascita());
    r.setSesso(p.getSesso());
    r.setLuogoNascita(
        p.getComuNascDes() != null ? p.getComuNascDes() + " (" + p.getComuNascSigla() + ")" : null);
    r.setProvinciaNascita(p.getProvNascDes());
    r.setCittadinanza(p.getDesCittadinanza());
    r.setStatoCivile(p.getStatoCivileDes());
    r.setProfessione(p.getProfessione());
    r.setEmail(p.getEmail());
    r.setEmailAte(p.getEmailAte());
    r.setEmailCertificata(p.getEmailCertificata());
    r.setCellulare(p.getCellulare());
    r.setTelefono(p.getTelRes());
    r.setUserId(p.getUserId());

    // Residenza
    String indirizzoRes = Stream.of(p.getViaRes(), p.getNumCivRes())
        .filter(s -> s != null && !s.isBlank())
        .collect(java.util.stream.Collectors.joining(", "));
    r.setIndirizzoResidenza(indirizzoRes);
    r.setCapResidenza(p.getCapRes());
    r.setComuneResidenza(
        p.getComuResDes() != null ? p.getComuResDes() + " (" + p.getComuResSigla() + ")" : null);
    r.setProvinciaResidenza(p.getProvResDes());
    r.setNazioneResidenza(p.getNaziResDes());

    // Domicilio
    boolean domComeRes = p.getDomComeResFlg() != null && p.getDomComeResFlg() == 1;
    r.setDomicilioComeResidenza(domComeRes);
    if (!domComeRes) {
      String indirizzoDom = Stream.of(p.getViaDom(), p.getNumCivDom())
          .filter(s -> s != null && !s.isBlank())
          .collect(java.util.stream.Collectors.joining(" "));
      r.setIndirizzoDomicilio(indirizzoDom);
      r.setCapDomicilio(p.getCapDom());
      r.setComuneDomicilio(
          p.getComuDomDes() != null ? p.getComuDomDes() + " (" + p.getComuDomSigla() + ")" : null);
      r.setNazioneDomicilio(p.getNaziDomDes());
    }

    // Emergenza
    r.setEmergenzaNome(p.getEmergNome());
    r.setEmergenzaCognome(p.getEmergCognome());
    r.setEmergenzaTelefono(p.getEmergTel());
    r.setEmergenzaEmail(p.getEmergEmail());
    r.setEmergenzaRapporto(p.getEmergRapporto());

    return r;
  }

  /**
   * Retrieves career type and course information from carriere-service-v1.
   */
  public CarrieraInfoResponse getCarrieraInfo(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaCarrieraClient.CinecaCarriera c = cinecaClient.getCarriera(baseUrl, cinecaJwt);
    if (c == null) throw new CinecaClient.CinecaUnavailableException("No carriera found");

    log.debug("CarrieraService: fetched carriera info tipoCorsoCod={}", c.getTipoCorsoCod());

    CarrieraInfoResponse r = new CarrieraInfoResponse();
    r.setStuId(c.getStuId());
    r.setMatId(c.getMatId());
    r.setMatricola(c.getMatricola());
    r.setPersId(c.getPersId());
    r.setNome(c.getNome());
    r.setCognome(c.getCognome());
    r.setCodFis(c.getCodFis());
    r.setDataNascita(c.getDataNascita());
    r.setSesso(c.getSesso());
    r.setEmail(c.getEmail());
    r.setEmailAte(c.getEmailAte());
    r.setEmailCertificata(c.getEmailCertificata());
    r.setTipoCorsoCod(c.getTipoCorsoCod());
    r.setTipoCorsoDes(c.getTipoCorsoDes());
    r.setCdsCod(c.getCdsCod());
    r.setCdsDes(c.getCdsDes());
    r.setFacCod(c.getFacCod());
    r.setFacDes(c.getFacDes());
    r.setAnnoCorso(c.getAnnoCorso());
    r.setAaIscrId(c.getAaIscrId());
    r.setAaOrdId(c.getAaOrdId());
    r.setDataImm(c.getDataImm());
    r.setDataIscr(c.getDataIscr());
    r.setDataFineCarriera(c.getDataFineCarriera());
    r.setStaStuCod(c.getStaStuCod());
    r.setStatiStuDes(c.getStatiStuDes());
    r.setProfstuDes(c.getProfstuDes());
    r.setPtFlg(c.getPtFlg());
    r.setSospFlg(c.getSospFlg());
    r.setAttlauFlg(c.getAttlauFlg());
    r.setSediDes(c.getSediDes());
    r.setSedeId(c.getSedeId());
    r.setUserId(c.getUserId());
    return r;
  }


  /**
   * Retrieves the profile photo bytes for the authenticated student.
   */
  public byte[] getFotoPersona(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    Long persId = sessionStore.getCinecaPersId(principal.omuUserId(), principal.universityId())
        .orElseThrow(() -> new CinecaClient.CinecaAuthException("PersId not found"));

    return cinecaClient.getFotoPersona(baseUrl, cinecaJwt, persId);
  }

  public String getCinecaJwt(OmuPrincipal principal) {
    return resolveCinecaJwt(principal);
  }

  public String getBaseUrl(String universityId) {
    return resolveBaseUrl(universityId);
  }

  /**
   * Retrieves questionnaire status split into pending and completed.
   */
  public QuestionariResponse getQuestionari(OmuPrincipal principal) {
    String cinecaJwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaCarrieraClient.CinecaRigaConQuestionario> daCompilare =
        cinecaClient.getQuestionariLibretto(baseUrl, cinecaJwt, principal.matId(), "C");

    List<CinecaCarrieraClient.CinecaRigaConQuestionario> compilati =
        cinecaClient.getQuestionariLibretto(baseUrl, cinecaJwt, principal.matId(), "P")
            .stream()
            .filter(r -> r.getStatoLink() != null && r.getStatoLink() == 1)
            .toList();

    log.debug("CarrieraService: questionari daCompilare={} compilati={} for matId={}",
        daCompilare.size(), compilati.size(), principal.matId());

    QuestionariResponse response = new QuestionariResponse();
    response.setDaCompilare(daCompilare.stream().map(this::toQuestionarioEsame).toList());
    response.setCompilati(compilati.stream().map(this::toQuestionarioEsame).toList());
    return response;
  }

  private QuestionariResponse.QuestionarioEsame toQuestionarioEsame(
      CinecaCarrieraClient.CinecaRigaConQuestionario r) {
    QuestionariResponse.QuestionarioEsame q = new QuestionariResponse.QuestionarioEsame();
    q.setAdCod(r.getAdCod());
    q.setAdDes(r.getAdDes());
    q.setAdsceId(r.getAdsceId());
    q.setAnnoCorso(r.getAnnoCorso());
    q.setCfu(r.getPeso());
    q.setStatoLink(r.getStatoLink());
    return q;
  }

  /**
   * Maps a Cineca booking entity into the public API DTO.
   *
   * @param p source Cineca booking entity
   * @return mapped booking DTO
   */
  private Prenotazione toPrenotazione(CinecaPrenotazione p) {
    Prenotazione pre = new Prenotazione();
    pre.setApplistaId(p.getApplistaId());
    pre.setAdId(p.getAdId());
    pre.setAdStuCod(p.getAdStuCod());
    pre.setAdStuDes(p.getAdStuDes());
    pre.setAdsceId(p.getAdsceId());
    pre.setCdsId(p.getCdsId());
    pre.setAppId(p.getAppId());
    pre.setAppLogId(p.getAppLogId());
    pre.setStuId(p.getStuId());
    pre.setDataIns(p.getDataIns());
    pre.setDataEsa(p.getDataEsa());
    pre.setDataRifEsitoStu(p.getDataRifEsitoStu());
    pre.setPesoAd(p.getPesoAd());
    pre.setPosizApp(p.getPosizApp());
    pre.setPresaVisione(p.getPresaVisione());
    pre.setStatoAdsce(p.getStatoAdsce());
    pre.setEsitoPubblicato(p.getPubblId() != null);
    pre.setDomandeEsame(p.getDomandeEsame());

    if (p.getEsito() != null) {
      EsitoPrenotazione esito = new EsitoPrenotazione();
      esito.setVotoEsa(p.getEsito().getVotoEsa());
      esito.setSuperato(p.getEsito().isSuperato());
      esito.setAssente(p.getEsito().isAssente());
      esito.setRitirato(p.getEsito().isRitirato());
      esito.setTipoGiudCod(p.getEsito().getTipoGiudCod());
      esito.setTipoGiudizioDes(p.getEsito().getTipoGiudizioDes());
      pre.setEsito(esito);
    }

    return pre;
  }

  /**
   * Maps a Cineca fee entry (CinecaVoceTassa) into the internal application model (VoceTassa).
   *
   * <p>Performs a direct field-to-field mapping without transformations,
   * preserving all financial and descriptive attributes provided by Cineca.
   *
   * @param v Cineca fee entry returned by the ESSE3 carrier service
   * @return internal representation of a single fee item
   */
  private VoceTassa toVoceTassa(CinecaCarrieraClient.CinecaVoceTassa v) {
    VoceTassa vt = new VoceTassa();
    vt.setFattId(v.getFattId());
    vt.setTassaId(v.getTassaId());
    vt.setTassaCod(v.getTassaCod());
    vt.setTassaDes(v.getTassaDes());
    vt.setVoceId(v.getVoceId());
    vt.setVoceCod(v.getVoceCod());
    vt.setVoceDes(v.getVoceDes());
    vt.setImportoVoce(v.getImportoVoce());
    vt.setDataScadenza(v.getDataScadenza());
    vt.setDataPagTollerataMax(v.getDataPagTollerataMax());
    return vt;
  }

  /**
   * Maps a Cineca accounting charge (CinecaAddebito) into the internal Addebito model used by the
   * application.
   *
   * <p>This method performs a direct field-by-field mapping, preserving all
   * billing, payment, and status information provided by Cineca without applying transformations or
   * business logic.
   *
   * @param a Cineca accounting charge returned by the ESSE3 carrier service
   * @return internal representation of a single accounting charge
   */
  private Addebito toAddebito(CinecaAddebito a) {
    Addebito ad = new Addebito();
    ad.setAaId(a.getAaId());
    ad.setTassaDes(a.getTassaDes());
    ad.setTassaCod(a.getTassaCod());
    ad.setTipoTaxCod(a.getTipoTaxCod());
    ad.setVoceDes(a.getVoceDes());
    ad.setImportoVoce(a.getImportoVoce());
    ad.setScadenzaAddebito(a.getScadenzaAddebito());
    ad.setScadutoFlg(a.getScadutoFlg());
    ad.setFattId(a.getFattId());
    ad.setScadFattura(a.getScadFattura());
    ad.setFattScadutaFlg(a.getFattScadutaFlg());
    ad.setImportoFattura(a.getImportoFattura());
    ad.setDataEmissione(a.getDataEmissione());
    ad.setPagatoFlg(a.getPagatoFlg());
    ad.setDataPagamento(a.getDataPagamento());
    ad.setImportoPag(a.getImportoPag());
    ad.setAnnullataFlg(a.getAnnullataFlg());
    ad.setRataDes(a.getRataDes());
    ad.setIuv(a.getIuv());
    ad.setCodiceAvviso(a.getCodiceAvviso());
    return ad;
  }

  /**
   * Maps a Cineca badge entity into the public API DTO.
   *
   * @param b source Cineca badge entity
   * @return mapped badge response
   */
  private BadgeResponse toBadgeResponse(CinecaBadge b) {
    BadgeResponse r = new BadgeResponse();
    r.setBdgId(b.getBdgId());
    r.setMatricola(b.getMatricola());
    r.setCognome(b.getCognome());
    r.setNome(b.getNome());
    r.setCodFis(b.getCodFis());
    r.setCodCds(b.getCodCds());
    r.setDesCds(b.getDesCds());
    r.setCodFac(b.getCodFac());
    r.setDesFac(b.getDesFac());
    r.setAaIscrAnn(b.getAaIscrAnn());
    r.setRfid(b.getRfid());
    r.setUniversita(b.getUniversita());
    r.setStaStuCod(b.getStaStuCod());
    r.setDataIni(b.getDataIni());
    r.setDataFin(b.getDataFin());
    r.setFrontImagePresent(b.getFrontImagePresent() != null && b.getFrontImagePresent() == 1);
    r.setRearImagePresent(b.getRearImagePresent() != null && b.getRearImagePresent() == 1);
    r.setBadgeBlbId(b.getBadgeBlbId());
    return r;
  }
}
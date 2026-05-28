package org.ohmyopensource.ohmyuniversity.core.service;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAppello;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAttivitaPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaMedia;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaPianoDettaglio;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaRigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaTestataPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.AppelloResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse.RigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PianoStudioResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse.EsitoPrenotazione;
import org.ohmyopensource.ohmyuniversity.core.dto.PrenotazioneResponse.Prenotazione;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for orchestrating career-related operations against
 * Cineca ESSE3 APIs.
 *
 * Every request retrieves live academic data using the authenticated
 * Cineca session associated with the current OhMyUniversity user.
 *
 * No academic information is persisted locally inside the application.
 */
@Service
public class CarrieraService {

  private static final Logger log = LoggerFactory.getLogger(CarrieraService.class);

  private static final String TIPO_MEDIA_ARITMETICA = "A";
  private static final String TIPO_MEDIA_PESATA = "P";

  private final CinecaCarrieraClient cinecaClient;
  private final CinecaSessionStore sessionStore;
  private final UniversityRegistry universityRegistry;
  private final UniversityConnectionRepository connectionRepository;

  // ============ Constructor ============

  /**
   * Creates a new career service instance.
   *
   * @param cinecaClient Cineca career API client
   * @param sessionStore Cineca session storage service
   * @param universityRegistry university configuration registry
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
      if (tipo == null || base == null) continue;

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
   * @param cdsId course identifier
   * @param adId teaching activity identifier
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
   * Retrieves the student's exam booking history.
   *
   * The Cineca password is provided at request time and is never persisted.
   *
   * @param principal authenticated OhMyUniversity principal
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
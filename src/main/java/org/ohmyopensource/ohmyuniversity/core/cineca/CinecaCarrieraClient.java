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
 * HTTP client for Cineca ESSE3 career-related endpoints.
 *
 * All calls use the Cineca JWT stored in Redis (obtained at login).
 * The base URL varies per university — passed as parameter on each call.
 */
@Component
public class CinecaCarrieraClient {

  private static final Logger log = LoggerFactory.getLogger(CinecaCarrieraClient.class);

  private final WebClient webClient;

  public CinecaCarrieraClient() {
    this.webClient = WebClient.builder()
        .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();
  }

  // ================================
  // Libretto
  // ================================

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

  // ================================
  // Libretto
  // ================================

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

  // ================================
  // Piano di studi
  // ================================

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

  // ================================
  // Piano di studi
  // ================================

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

  // ================================
  // Appelli
  // ================================

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

  // ================================
  // Prenotazioni
  // ================================

  /**
   * Fetches the full booking history for a student from calesa-service-v1.
   *
   * <p>calesa-service-v1 requires Basic Auth + JSESSIONID cookie. JWT alone
   * is insufficient — Cineca creates an anonymous session and SecurityCalesa
   * rejects requests protected by checkAbildocStu. prenotazioni/{matId} uses
   * a different security check that works correctly with Basic Auth + JSESSIONID.
   *
   * @param cinecaBaseUrl   base URL of the university ESSE3 instance
   * @param cinecaUsername  Cineca username (from UniversityConnection)
   * @param cinecaPassword  Cineca password (provided by client at request time)
   * @param cinecaAuthToken Cineca session token / JSESSIONID (from Redis)
   * @param matId           student career track ID
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

  // ================================
  // Badge
  // ================================

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

  // ================================
  // Private helpers
  // ================================

  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  // ================================
  // Raw Cineca DTOs
  // ================================

  /** Cineca wraps enum-like fields as {value: "..."} objects. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaValueWrapper {
    @JsonProperty("value")
    private String value;
    public String getValue() { return value; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaRigaLibretto {
    @JsonProperty("adsceId") private Long adsceId;
    @JsonProperty("adCod") private String adCod;
    @JsonProperty("adDes") private String adDes;
    @JsonProperty("annoCorso") private Integer annoCorso;
    @JsonProperty("stato") private CinecaValueWrapper stato;
    @JsonProperty("statoDes") private String statoDes;
    @JsonProperty("peso") private Double peso;
    @JsonProperty("tipoInsCod") private String tipoInsCod;
    @JsonProperty("tipoInsDes") private String tipoInsDes;
    @JsonProperty("esito") private CinecaEsito esito;
    @JsonProperty("numAppelliPrenotabili") private Integer numAppelliPrenotabili;

    public Long getAdsceId() { return adsceId; }
    public String getAdCod() { return adCod; }
    public String getAdDes() { return adDes; }
    public Integer getAnnoCorso() { return annoCorso; }
    public String getStato() { return stato != null ? stato.getValue() : null; }
    public String getStatoDes() { return statoDes; }
    public Double getPeso() { return peso; }
    public String getTipoInsCod() { return tipoInsCod; }
    public String getTipoInsDes() { return tipoInsDes; }
    public CinecaEsito getEsito() { return esito; }
    public Integer getNumAppelliPrenotabili() { return numAppelliPrenotabili; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaEsito {
    @JsonProperty("voto") private Double voto;
    @JsonProperty("lodeFlg") private Integer lodeFlg;
    @JsonProperty("dataEsa") private String dataEsa;
    @JsonProperty("tipoGiudCod") private String tipoGiudCod;
    @JsonProperty("tipoGiudDes") private String tipoGiudDes;

    public Double getVoto() { return voto; }
    public Integer getLodeFlg() { return lodeFlg; }
    public String getDataEsa() { return dataEsa; }
    public String getTipoGiudCod() { return tipoGiudCod; }
    public String getTipoGiudDes() { return tipoGiudDes; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaMedia {
    @JsonProperty("media") private Double media;
    @JsonProperty("base") private Integer base;
    @JsonProperty("tipoMediaCod") private CinecaValueWrapper tipoMediaCod;
    @JsonProperty("definizioneBase") private CinecaValueWrapper definizioneBase;
    @JsonProperty("tipoOk") private Integer tipoOk;

    public Double getMedia() { return media; }
    public Integer getBase() { return base; }
    public String getTipoMediaCod() {
      return tipoMediaCod != null ? tipoMediaCod.getValue() : null;
    }
    public String getDefinizioneBase() {
      return definizioneBase != null ? definizioneBase.getValue() : null;
    }
    public Integer getTipoOk() { return tipoOk; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaTestataPiano {
    @JsonProperty("stuId") private Long stuId;
    @JsonProperty("pianoId") private Long pianoId;
    @JsonProperty("stato") private CinecaValueWrapper stato;
    @JsonProperty("statoDes") private String statoDes;

    public Long getStuId() { return stuId; }
    public Long getPianoId() { return pianoId; }
    public String getStato() { return stato != null ? stato.getValue() : null; }
    public String getStatoDes() { return statoDes; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPianoDettaglio {
    @JsonProperty("stuId") private Long stuId;
    @JsonProperty("pianoId") private Long pianoId;
    @JsonProperty("attivita") private List<CinecaAttivitaPiano> attivita;

    public Long getStuId() { return stuId; }
    public Long getPianoId() { return pianoId; }
    public List<CinecaAttivitaPiano> getAttivita() {
      return attivita != null ? attivita : List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAttivitaPiano {
    @JsonProperty("adsceId") private Long adsceId;
    @JsonProperty("adLibCod") private String adLibCod;
    @JsonProperty("adLibDes") private String adLibDes;
    @JsonProperty("peso") private Double cfu;
    @JsonProperty("annoCorso") private Integer annoCorso;
    @JsonProperty("chiaveADContestualizzata") private CinecaChiaveAd chiaveAd;
    @JsonProperty("sceltaFlg") private Integer sceltaFlg;

    public Long getAdsceId() { return adsceId; }
    public Double getCfu() { return cfu; }
    public Integer getAnnoCorso() { return annoCorso; }
    public CinecaChiaveAd getChiaveAd() { return chiaveAd; }
    public Boolean isObbligatorio() { return sceltaFlg == null || sceltaFlg == 0; }

    public String getAdDes() {
      if (adLibDes != null && !adLibDes.isBlank()) return adLibDes;
      return chiaveAd != null ? chiaveAd.getAdDes() : null;
    }

    public String getAdCod() {
      if (adLibCod != null && !adLibCod.isBlank()) return adLibCod;
      return chiaveAd != null ? chiaveAd.getAdCod() : null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaChiaveAd {
    @JsonProperty("adCod") private String adCod;
    @JsonProperty("adDes") private String adDes;

    public String getAdCod() { return adCod; }
    public String getAdDes() { return adDes; }
  }

  /**
   * Prenotazione from calesa-service-v1/prenotazioni/{matId}.
   * presaVisione, statoAdsce, modValCod are {value:"..."} wrappers.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPrenotazione {
    @JsonProperty("applistaId") private Long applistaId;
    @JsonProperty("adId") private Long adId;
    @JsonProperty("adStuCod") private String adStuCod;
    @JsonProperty("adStuDes") private String adStuDes;
    @JsonProperty("adsceId") private Long adsceId;
    @JsonProperty("cdsId") private Long cdsId;
    @JsonProperty("appId") private Long appId;
    @JsonProperty("appLogId") private Long appLogId;
    @JsonProperty("stuId") private Long stuId;
    @JsonProperty("dataIns") private String dataIns;
    @JsonProperty("dataEsa") private String dataEsa;
    @JsonProperty("dataRifEsitoStu") private String dataRifEsitoStu;
    @JsonProperty("pesoAd") private Double pesoAd;
    @JsonProperty("posizApp") private Integer posizApp;
    @JsonProperty("esito") private CinecaEsitoPrenotazione esito;
    @JsonProperty("presaVisione") private CinecaValueWrapper presaVisione;
    @JsonProperty("statoAdsce") private CinecaValueWrapper statoAdsce;
    @JsonProperty("pubblId") private Long pubblId;
    @JsonProperty("domandeEsame") private String domandeEsame;

    public Long getApplistaId() { return applistaId; }
    public Long getAdId() { return adId; }
    public String getAdStuCod() { return adStuCod; }
    public String getAdStuDes() { return adStuDes; }
    public Long getAdsceId() { return adsceId; }
    public Long getCdsId() { return cdsId; }
    public Long getAppId() { return appId; }
    public Long getAppLogId() { return appLogId; }
    public Long getStuId() { return stuId; }
    public String getDataIns() { return dataIns; }
    public String getDataEsa() { return dataEsa; }
    public String getDataRifEsitoStu() { return dataRifEsitoStu; }
    public Double getPesoAd() { return pesoAd; }
    public Integer getPosizApp() { return posizApp; }
    public CinecaEsitoPrenotazione getEsito() { return esito; }
    public String getPresaVisione() {
      return presaVisione != null ? presaVisione.getValue() : null;
    }
    public String getStatoAdsce() {
      return statoAdsce != null ? statoAdsce.getValue() : null;
    }
    public Long getPubblId() { return pubblId; }
    public String getDomandeEsame() { return domandeEsame; }
  }

  /** Esito inside a prenotazione — modValCod is a {value:"..."} wrapper. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaEsitoPrenotazione {
    @JsonProperty("votoEsa") private Integer votoEsa;
    @JsonProperty("superatoFlg") private Integer superatoFlg;
    @JsonProperty("assenteFlg") private Integer assenteFlg;
    @JsonProperty("ritiratoFlg") private Integer ritiratoFlg;
    @JsonProperty("tipoGiudCod") private String tipoGiudCod;
    @JsonProperty("tipoGiudizioDes") private String tipoGiudizioDes;
    @JsonProperty("modValCod") private CinecaValueWrapper modValCod;

    public Integer getVotoEsa() { return votoEsa; }
    public Boolean isSuperato() { return superatoFlg != null && superatoFlg == 1; }
    public Boolean isAssente() { return assenteFlg != null && assenteFlg == 1; }
    public Boolean isRitirato() { return ritiratoFlg != null && ritiratoFlg == 1; }
    public String getTipoGiudCod() { return tipoGiudCod; }
    public String getTipoGiudizioDes() { return tipoGiudizioDes; }
    public String getModValCod() {
      return modValCod != null ? modValCod.getValue() : null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAppello {
    @JsonProperty("appId") private Long appId;
    @JsonProperty("adCod") private String adCod;
    @JsonProperty("adDes") private String adDes;
    @JsonProperty("dataInizioApp") private String dataInizio;
    @JsonProperty("dataFineApp") private String dataFine;
    @JsonProperty("dataScadPren") private String dataScadPrenotazione;
    @JsonProperty("tipoApp") private String tipo;
    @JsonProperty("tipoAppDes") private String tipoDes;
    @JsonProperty("docenteDes") private String docente;
    @JsonProperty("aula") private String aula;
    @JsonProperty("prenotabile") private Boolean prenotabile;
    @JsonProperty("prenotato") private Boolean prenotato;
    @JsonProperty("prenotazioneId") private Long prenotazioneId;

    public Long getAppId() { return appId; }
    public String getAdCod() { return adCod; }
    public String getAdDes() { return adDes; }
    public String getDataInizio() { return dataInizio; }
    public String getDataFine() { return dataFine; }
    public String getDataScadPrenotazione() { return dataScadPrenotazione; }
    public String getTipo() { return tipo; }
    public String getTipoDes() { return tipoDes; }
    public String getDocente() { return docente; }
    public String getAula() { return aula; }
    public Boolean getPrenotabile() { return prenotabile; }
    public Boolean getPrenotato() { return prenotato; }
    public Long getPrenotazioneId() { return prenotazioneId; }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaBadge {
    @JsonProperty("bdgId") private Long bdgId;
    @JsonProperty("matricola") private String matricola;
    @JsonProperty("cognome") private String cognome;
    @JsonProperty("nome") private String nome;
    @JsonProperty("codFis") private String codFis;
    @JsonProperty("codCds") private String codCds;
    @JsonProperty("desCds") private String desCds;
    @JsonProperty("codFac") private String codFac;
    @JsonProperty("desFac") private String desFac;
    @JsonProperty("aaIscrAnn") private Integer aaIscrAnn;
    @JsonProperty("rfid") private String rfid;
    @JsonProperty("universita") private String universita;
    @JsonProperty("staStuCod") private String staStuCod;
    @JsonProperty("dataIni") private String dataIni;
    @JsonProperty("dataFin") private String dataFin;
    @JsonProperty("frontImagePresent") private Integer frontImagePresent;
    @JsonProperty("rearImagePresent") private Integer rearImagePresent;
    @JsonProperty("badgeBlbId") private Long badgeBlbId;

    public Long getBdgId() { return bdgId; }
    public String getMatricola() { return matricola; }
    public String getCognome() { return cognome; }
    public String getNome() { return nome; }
    public String getCodFis() { return codFis; }
    public String getCodCds() { return codCds; }
    public String getDesCds() { return desCds; }
    public String getCodFac() { return codFac; }
    public String getDesFac() { return desFac; }
    public Integer getAaIscrAnn() { return aaIscrAnn; }
    public String getRfid() { return rfid; }
    public String getUniversita() { return universita; }
    public String getStaStuCod() { return staStuCod; }
    public String getDataIni() { return dataIni; }
    public String getDataFin() { return dataFin; }
    public Integer getFrontImagePresent() { return frontImagePresent; }
    public Integer getRearImagePresent() { return rearImagePresent; }
    public Long getBadgeBlbId() { return badgeBlbId; }
  }
}
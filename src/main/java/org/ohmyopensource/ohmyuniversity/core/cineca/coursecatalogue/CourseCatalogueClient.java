package org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.AbstractCinecaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for Cineca Course Catalogue ({@code coursecatalogue.cineca.it}).
 *
 * <p>This is a separate Cineca product from ESSE3 REST (different domain,
 * data model and identifier scheme), used here purely to enrich exam
 * accordion details with data ESSE3 does not expose: prerequisites and the
 * per-module course syllabus ("programma per CFU"), plus the full
 * multi-year course plan. All endpoints are public — no authentication
 * required.
 *
 * <p>The unfiltered course listing endpoint ({@code /api/v1/corsi?anno=Y})
 * groups every course of the university by category, several levels deep
 * ({@code gruppo -> subgroups -> cds[] -> cdsSub[]}). Its {@code corso}
 * query parameter, despite appearances, does <b>not</b> filter this
 * response — verified empirically, the response is byte-for-byte the same
 * shape with or without it. {@link #resolveCourseDetail} therefore always
 * fetches the same full listing and recursively searches it for the
 * {@code cdsSub} entry matching the ESSE3 {@code cdsCod}; that single entry
 * already contains both the Course Catalogue's own course code and the full
 * multi-year {@code percorsi} plan, so one heavy cached call serves both
 * purposes.
 */
@Component
public class CourseCatalogueClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CourseCatalogueClient.class);

  /**
   * Buffer size (MB) for {@link #largePayloadWebClient}. The unfiltered
   * course listing can exceed several tens of MB for large universities,
   * well beyond the 10MB buffer {@link AbstractCinecaClient} configures for
   * regular ESSE3 payloads.
   */
  private static final int LARGE_BUFFER_SIZE_MB = 100;

  /**
   * Dedicated {@link WebClient} with an enlarged in-memory buffer, used only
   * for the one heavy, infrequent, cached call in {@link #resolveCourseDetail}.
   */
  private final WebClient largePayloadWebClient = WebClient.builder()
      .codecs(config -> config.defaultCodecs()
          .maxInMemorySize(LARGE_BUFFER_SIZE_MB * 1024 * 1024))
      .build();

  /**
   * Dedicated Jackson mapper used to parse the course listing response
   * manually as a raw string, bypassing WebClient's auto-configured codecs
   * — which, in this project's Jackson setup, cannot decode the generic
   * {@link JsonNode} type directly.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Caches the resolved course entry (Course Catalogue code + full
   * multi-year plan) per university, cohort and ESSE3 course code.
   * Identical for every student sharing those three, and barely changes
   * within an academic year, so a long TTL is safe.
   */
  private final TtlCache<Optional<CinecaCourseCatalogueDetail>> courseDetailCache =
      new TtlCache<>(Duration.ofHours(24));

  // ============ Class Methods ============

  /**
   * Resolves the Course Catalogue entry — its own course code and full
   * multi-year plan — for a given ESSE3 {@code cdsCod}, by fetching and
   * searching the full course listing for the given cohort year.
   *
   * @param baseUrl Course Catalogue base URL (e.g. {@code https://unich.coursecatalogue.cineca.it})
   * @param year    cohort (immatricolazione) year
   * @param cdsCod  ESSE3 course of study code (e.g. {@code "LM41"})
   * @return the resolved course entry, or empty if not found
   */
  public Optional<CinecaCourseCatalogueDetail> resolveCourseDetail(
      String baseUrl, int year, String cdsCod) {
    String cacheKey = baseUrl + ":" + year + ":" + cdsCod;
    return courseDetailCache.getOrCompute(cacheKey, () -> {
      log.debug("CourseCatalogueClient: resolving course detail for cdsCod={} year={}",
          cdsCod, year);
      String rawBody = largePayloadWebClient.get()
          .uri(uriBuilder -> uriBuilder
              .scheme("https")
              .host(extractHost(baseUrl))
              .pathSegment("api", "v1", "corsi")
              .queryParam("anno", year)
              .build())
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, r ->
              Mono.error(new CinecaClient.CinecaAuthException(
                  "Unauthorized for course catalogue listing year=" + year)))
          .onStatus(HttpStatusCode::is5xxServerError, r ->
              Mono.error(new CinecaClient.CinecaUnavailableException(
                  "Cineca error on course catalogue listing")))
          .bodyToMono(String.class)
          .block();

      Optional<CinecaCourseCatalogueDetail> result;
      try {
        JsonNode root = rawBody == null ? null : objectMapper.readTree(rawBody);
        JsonNode courseNode = root == null ? null : findCourseNode(root, cdsCod, year);
        result = courseNode == null
            ? Optional.empty()
            : Optional.of(objectMapper.treeToValue(courseNode, CinecaCourseCatalogueDetail.class));
      } catch (Exception e) {
        log.error("CourseCatalogueClient: failed to parse/convert course catalogue listing", e);
        result = Optional.empty();
      }
      log.debug("CourseCatalogueClient: resolved cdsCod={} -> found={}", cdsCod, result.isPresent());
      return result;
    });
  }

  /**
   * Recursively searches an arbitrarily-nested JSON tree for an object whose
   * {@code cdsCod} matches {@code targetCdsCod} and that carries a
   * {@code cdsSub} array, returning that array's first entry — which itself
   * contains both the Course Catalogue course code and the full multi-year
   * {@code percorsi} plan.
   */
  private JsonNode findCourseNode(JsonNode node, String targetCdsCod, int targetYear) {
    if (node.isObject()) {
      JsonNode cdsCodNode = node.get("cdsCod");
      JsonNode subNode = node.get("cdsSub");
      if (cdsCodNode != null && targetCdsCod.equals(cdsCodNode.asText())
          && subNode != null && subNode.isArray() && !subNode.isEmpty()) {
        for (JsonNode candidate : subNode) {
          JsonNode aaNode = candidate.get("aa");
          if (aaNode != null && String.valueOf(targetYear).equals(aaNode.asText())) {
            return candidate;
          }
        }
      }
      Iterator<String> fieldNames = node.fieldNames();
      while (fieldNames.hasNext()) {
        JsonNode result = findCourseNode(node.get(fieldNames.next()), targetCdsCod, targetYear);
        if (result != null) {
          return result;
        }
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        JsonNode result = findCourseNode(child, targetCdsCod, targetYear);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  /**
   * Retrieves the detailed syllabus for a single teaching activity: overall
   * and per-module prerequisites, contents and objectives. This endpoint
   * genuinely filters to the requested activity, unlike the course listing.
   *
   * @param baseUrl         Course Catalogue base URL
   * @param annoOfferta     offer year of the activity (the plan entry's {@code aa})
   * @param insegnamentoCod composite Course Catalogue activity code (the plan entry's {@code cod})
   * @param ordinamentoAa   ordinamento year (the plan entry's {@code ordinamento_aa})
   * @param afPercorsoId    percorso identifier (the plan entry's {@code af_percorso_id})
   * @param courseCode      Course Catalogue course code
   * @param cohortYear      cohort (immatricolazione) year
   * @return the activity detail, or {@code null} if not found
   */
  public CinecaInsegnamentoDetail getInsegnamentoDetail(
      String baseUrl, int annoOfferta, String insegnamentoCod, int ordinamentoAa,
      String afPercorsoId, String courseCode, int cohortYear) {
    log.debug("CourseCatalogueClient: GET insegnamento detail cod={}", insegnamentoCod);
    String rawBody = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("api", "v1", "insegnamento")
            .queryParam("anno", annoOfferta)
            .queryParam("insegnamento", insegnamentoCod)
            .queryParam("ordinamento_aa", ordinamentoAa)
            .queryParam("af_percorso", afPercorsoId)
            .queryParam("corso_cod", courseCode)
            .queryParam("corso_aa", cohortYear)
            .build())
        .header(HttpHeaders.USER_AGENT,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .onStatus(status -> status.value() == 404, r -> Mono.empty())
        .onStatus(HttpStatusCode::is4xxClientError, r ->
            Mono.error(new CinecaClient.CinecaAuthException(
                "Unauthorized for insegnamento detail cod=" + insegnamentoCod)))
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException(
                "Cineca error on insegnamento detail")))
        .bodyToMono(String.class)
        .block();

    if (rawBody == null || rawBody.isBlank() || !rawBody.trim().startsWith("{")) {
      log.warn("CourseCatalogueClient: non-JSON response for insegnamento detail cod={} "
          + "(likely no data for this parameter combination)", insegnamentoCod);
      return null;
    }

    try {
      return objectMapper.readValue(rawBody, CinecaInsegnamentoDetail.class);
    } catch (Exception e) {
      log.error("CourseCatalogueClient: failed to parse insegnamento detail cod={}",
          insegnamentoCod, e);
      return null;
    }
  }

  // ============ DTOs ============

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCourseCatalogueDetail {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("cdsCod")
    private String cdsCod;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("percorsi")
    private List<CinecaPercorso> percorsi;

    public String getCod() {
      return cod;
    }

    public String getCdsCod() {
      return cdsCod;
    }

    public String getDesIt() {
      return desIt;
    }

    public List<CinecaPercorso> getPercorsi() {
      return percorsi;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaPercorso {

    @JsonProperty("pdsId")
    private String pdsId;
    @JsonProperty("pdsCod")
    private String pdsCod;
    @JsonProperty("comune")
    private Boolean comune;
    @JsonProperty("anni")
    private List<CinecaAnnoOfferta> anni;

    public String getPdsId() {
      return pdsId;
    }

    public String getPdsCod() {
      return pdsCod;
    }

    /**
     * True for the shared/pooled percorso some universities expose (observed
     * as {@code "comune": true}), which is not a real curriculum but a
     * catch-all bucket — always excluded from selection.
     */
    public Boolean getComune() {
      return comune;
    }

    public List<CinecaAnnoOfferta> getAnni() {
      return anni;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaAnnoOfferta {

    @JsonProperty("anno")
    private String anno;
    @JsonProperty("annoOfferta")
    private String annoOfferta;
    @JsonProperty("insegnamenti")
    private List<CinecaInsegnamentoGroup> insegnamenti;

    public String getAnno() {
      return anno;
    }

    public String getAnnoOfferta() {
      return annoOfferta;
    }

    public List<CinecaInsegnamentoGroup> getInsegnamenti() {
      return insegnamenti;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaInsegnamentoGroup {

    @JsonProperty("label_it")
    private String labelIt;
    @JsonProperty("attivita")
    private List<CinecaCoursePlanEntry> attivita;

    public String getLabelIt() {
      return labelIt;
    }

    public List<CinecaCoursePlanEntry> getAttivita() {
      return attivita;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCoursePlanEntry {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("crediti")
    private Integer crediti;
    @JsonProperty("aa")
    private String aa;
    @JsonProperty("ordinamento_aa")
    private Integer ordinamentoAa;
    @JsonProperty("annoCorso")
    private Integer annoCorso;
    @JsonProperty("af_percorso_id")
    private String afPercorsoId;
    @JsonProperty("tafDes_it")
    private String tafDesIt;
    @JsonProperty("periodo_didattico_it")
    private String periodoDidatticoItDirect;
    @JsonProperty("moduli")
    private List<CinecaModuloPeriodo> moduli;

    public String getCod() {
      return cod;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getDesIt() {
      return desIt;
    }

    public Integer getCrediti() {
      return crediti;
    }

    public String getAa() {
      return aa;
    }

    public Integer getOrdinamentoAa() {
      return ordinamentoAa;
    }

    public Integer getAnnoCorso() {
      return annoCorso;
    }

    public String getAfPercorsoId() {
      return afPercorsoId;
    }

    public String getTafDesIt() {
      return tafDesIt;
    }

    /** Falls back to the activity-level field when there is no per-module split. */
    public String getPeriodDes() {
      if (moduli != null && !moduli.isEmpty() && moduli.get(0).getPeriodoDidatticoIt() != null) {
        return moduli.get(0).getPeriodoDidatticoIt();
      }
      return periodoDidatticoItDirect;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaModuloPeriodo {

    @JsonProperty("periodo_didattico_it")
    private String periodoDidatticoIt;

    public String getPeriodoDidatticoIt() {
      return periodoDidatticoIt;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaInsegnamentoDetail {

    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("moduli")
    private List<CinecaModuloInfo> moduli;
    @JsonProperty("testiTotali")
    private List<CinecaTestiEntry> testiTotali;

    public String getDesIt() {
      return desIt;
    }

    public String getAdCod() {
      return adCod;
    }

    public List<CinecaModuloInfo> getModuli() {
      return moduli;
    }

    public List<CinecaTestiEntry> getTestiTotali() {
      return testiTotali;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaModuloInfo {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("crediti")
    private Integer crediti;
    @JsonProperty("docenti")
    private List<CinecaDocenteInfo> docenti;

    public String getCod() {
      return cod;
    }

    public String getDesIt() {
      return desIt;
    }

    public Integer getCrediti() {
      return crediti;
    }

    /** Distinct teacher names for this module, in the order Cineca returns them. */
    public List<String> getProfessorNames() {
      if (docenti == null) {
        return List.of();
      }
      return docenti.stream()
          .map(CinecaDocenteInfo::getDes)
          .filter(des -> des != null && !des.isBlank())
          .distinct()
          .toList();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaDocenteInfo {

    @JsonProperty("des")
    private String des;

    public String getDes() {
      return des;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaTestiEntry {

    @JsonProperty("chiave_udCod")
    private String chiaveUdCod;
    @JsonProperty("prerequisiti_it")
    private String prerequisitiIt;
    @JsonProperty("mod_prerequisiti_it")
    private String modPrerequisitiIt;
    @JsonProperty("mod_contenuti_it")
    private String modContenutiIt;
    @JsonProperty("contenuti_it")
    private String contenutiIt;

    public String getChiaveUdCod() {
      return chiaveUdCod;
    }

    public String getPrerequisitiIt() {
      return prerequisitiIt;
    }

    public String getModPrerequisitiIt() {
      return modPrerequisitiIt;
    }

    public String getModContenutiIt() {
      return modContenutiIt;
    }

    /**
     * Course-level content ("contenuti_it"). Populated for single-subject
     * courses ("corso monodisciplinare") that have no per-module breakdown
     * at all — their whole program lives here instead of in {@code
     * mod_contenuti_it} entries.
     */
    public String getContenutiIt() {
      return contenutiIt;
    }

    public boolean isCourseLevel() {
      return chiaveUdCod == null;
    }
  }
}
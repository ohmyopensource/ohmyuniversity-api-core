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
 * data model and identifier scheme), used here purely to enrich exam accordion details with data
 * ESSE3 does not expose: prerequisites and the per-module course syllabus ("programma per CFU"),
 * plus the full multi-year course plan. All endpoints are public — no authentication required.
 *
 * <p>The unfiltered course listing endpoint ({@code /api/v1/corsi?anno=Y})
 * groups every course of the university by category, several levels deep
 * ({@code gruppo -> subgroups -> cds[] -> cdsSub[]}). Its {@code corso} query parameter, despite
 * appearances, does <b>not</b> filter this response — verified empirically, the response is
 * byte-for-byte the same shape with or without it. {@link #resolveCourseDetail} therefore always
 * fetches the same full listing and recursively searches it for the {@code cdsSub} entry matching
 * the ESSE3 {@code cdsCod}; that single entry already contains both the Course Catalogue's own
 * course code and the full multi-year {@code percorsi} plan, so one heavy cached call serves both
 * purposes.
 */
@Component
public class CourseCatalogueClient extends AbstractCinecaClient {

  private static final Logger log = LoggerFactory.getLogger(CourseCatalogueClient.class);

  /**
   * Buffer size (MB) for {@link #largePayloadWebClient}. The unfiltered course listing can exceed
   * several tens of MB for large universities, well beyond the 10MB buffer
   * {@link AbstractCinecaClient} configures for regular ESSE3 payloads.
   */
  private static final int LARGE_BUFFER_SIZE_MB = 100;

  /**
   * Dedicated {@link WebClient} with an enlarged in-memory buffer, used only for the one heavy,
   * infrequent, cached call in {@link #resolveCourseDetail}.
   */
  private final WebClient largePayloadWebClient = WebClient.builder()
      .codecs(config -> config.defaultCodecs()
          .maxInMemorySize(LARGE_BUFFER_SIZE_MB * 1024 * 1024))
      .build();

  /**
   * Dedicated Jackson mapper used to parse the course listing response manually as a raw string,
   * bypassing WebClient's auto-configured codecs — which, in this project's Jackson setup, cannot
   * decode the generic {@link JsonNode} type directly.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Caches the resolved course entry (Course Catalogue code + full multi-year plan) per university,
   * cohort and ESSE3 course code. Identical for every student sharing those three, and barely
   * changes within an academic year, so a long TTL is safe.
   */
  private final TtlCache<Optional<CinecaCourseCatalogueDetail>> courseDetailCache =
      new TtlCache<>(Duration.ofHours(24));

  /**
   * Caches the raw, unfiltered course listing itself (parsed once as a {@link JsonNode} tree) per
   * university and year — shared by {@link #resolveCourseDetail} and {@link #getAllCourseCodes} so
   * both features pay the cost of the large listing download only once per TTL window, regardless
   * of which one triggers it first.
   */
  private final TtlCache<JsonNode> listingCache = new TtlCache<>(Duration.ofHours(24));

  // ============ Class Methods ============

  /**
   * Resolves the Course Catalogue entry — its own course code and full multi-year plan — for a
   * given ESSE3 {@code cdsCod}, by fetching and searching the full course listing for the given
   * cohort year.
   *
   * @param baseUrl Course Catalogue base URL (e.g.
   *                {@code https://unich.coursecatalogue.cineca.it})
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
      JsonNode root = fetchListing(baseUrl, year);

      Optional<CinecaCourseCatalogueDetail> result;
      try {
        JsonNode courseNode = root == null ? null : findCourseNode(root, cdsCod, year);
        result = courseNode == null
            ? Optional.empty()
            : Optional.of(objectMapper.treeToValue(courseNode, CinecaCourseCatalogueDetail.class));
      } catch (Exception e) {
        log.error("CourseCatalogueClient: failed to parse/convert course catalogue listing", e);
        result = Optional.empty();
      }
      log.debug("CourseCatalogueClient: resolved cdsCod={} -> found={}", cdsCod,
          result.isPresent());
      return result;
    });
  }

  /**
   * Retrieves every course code known to the Course Catalogue for a given university and year, by
   * fetching (and caching) the same full course listing {@link #resolveCourseDetail} uses, then
   * walking it for every {@code cdsSub} entry instead of searching for one specific {@code cdsCod}.
   * Used to build an ateneo-wide professor directory (iterating {@link #getListaDocenti} per
   * course) without a second heavy network call.
   *
   * @param baseUrl Course Catalogue base URL for the university
   * @param year    offer year
   * @return every distinct course found in the listing; empty if unresolvable
   */
  public List<CinecaCourseCode> getAllCourseCodes(String baseUrl, int year) {
    JsonNode root = fetchListing(baseUrl, year);
    if (root == null) {
      return List.of();
    }
    List<CinecaCourseCode> result = new java.util.ArrayList<>();
    collectCourseCodes(root, year, result);
    return result;
  }

  /**
   * Fetches (and caches for 24h) the raw, unfiltered course listing for a university/year — the
   * same slow, large payload used by both {@link #resolveCourseDetail} and
   * {@link #getAllCourseCodes}.
   */
  private JsonNode fetchListing(String baseUrl, int year) {
    String cacheKey = baseUrl + ":" + year;
    return listingCache.getOrCompute(cacheKey, () -> {
      log.debug("CourseCatalogueClient: fetching full course listing baseUrl={} year={}",
          baseUrl, year);
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
      try {
        return rawBody == null ? null : objectMapper.readTree(rawBody);
      } catch (Exception e) {
        log.error("CourseCatalogueClient: failed to parse course catalogue listing", e);
        return null;
      }
    });
  }

  /**
   * Recursively collects every {@code cdsSub} entry (for the given year) anywhere in the listing
   * tree — same node shape {@link #findCourseNode} searches for, but gathering all of them instead
   * of stopping at the first match for one specific {@code cdsCod}.
   */
  private void collectCourseCodes(JsonNode node, int targetYear, List<CinecaCourseCode> out) {
    if (node.isObject()) {
      JsonNode subNode = node.get("cdsSub");
      if (subNode != null && subNode.isArray()) {
        for (JsonNode candidate : subNode) {
          JsonNode aaNode = candidate.get("aa");
          if (aaNode != null && String.valueOf(targetYear).equals(aaNode.asText())) {
            try {
              out.add(objectMapper.treeToValue(candidate, CinecaCourseCode.class));
            } catch (Exception e) {
              log.debug("CourseCatalogueClient: skipping unparsable cdsSub entry", e);
            }
          }
        }
      }
      Iterator<String> fieldNames = node.fieldNames();
      while (fieldNames.hasNext()) {
        collectCourseCodes(node.get(fieldNames.next()), targetYear, out);
      }
    } else if (node.isArray()) {
      for (JsonNode child : node) {
        collectCourseCodes(child, targetYear, out);
      }
    }
  }

  /**
   * Recursively searches an arbitrarily-nested JSON tree for an object whose {@code cdsCod} matches
   * {@code targetCdsCod} and that carries a {@code cdsSub} array, returning that array's first
   * entry — which itself contains both the Course Catalogue course code and the full multi-year
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
   * Retrieves the detailed syllabus for a single teaching activity: overall and per-module
   * prerequisites, contents and objectives. This endpoint genuinely filters to the requested
   * activity, unlike the course listing.
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

  /**
   * Retrieves the professors currently teaching a given degree course (public, no authentication —
   * same trust level as the rest of Course Catalogue). Each entry includes a ready-made link to the
   * university's own teacher profile page when Cineca has one on file.
   *
   * @param baseUrl Course Catalogue base URL for the university
   * @param aa      offering year (current calendar year, per convention used elsewhere for
   *                "current" Cineca lookups)
   * @param cdsCod  Course Catalogue course code
   * @param sedeId  location identifier
   * @return list of professors; empty if none resolvable
   */
  public List<CatalogueDocente> getListaDocenti(
      String baseUrl, String aa, String cdsCod, Long sedeId) {
    log.debug("CourseCatalogueClient: GET lista-docenti aa={} cdsCod={} sedeId={}",
        aa, cdsCod, sedeId);
    String raw = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("api", "v1", "lista-docenti", aa, cdsCod)
            .queryParam("sede", sedeId)
            .build())
        .retrieve()
        .bodyToMono(String.class)
        .block();

    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(raw);
      JsonNode docenti = root.get("docenti");
      if (docenti == null) {
        return List.of();
      }
      List<CatalogueDocente> result = new java.util.ArrayList<>();
      for (JsonNode d : docenti) {
        result.add(mapper.treeToValue(d, CatalogueDocente.class));
      }
      return result;
    } catch (Exception e) {
      log.error("CourseCatalogueClient: failed to parse lista-docenti aa={} cdsCod={}", aa, cdsCod,
          e);
      return List.of();
    }
  }

  /**
   * Retrieves full detail for a single professor: real email (when compiled by the university —
   * confirmed present for UNIMOL via this endpoint, unlike ESSE3's logistica-service teaching-load
   * data, which never has it), department, and full list of taught courses.
   *
   * @param baseUrl   Course Catalogue base URL for the university
   * @param docenteId Course Catalogue internal id (the {@code _id} field from
   *                  {@link #getListaDocenti}) — NOT ESSE3's numeric docenteId/matricola
   * @param anno      academic year
   * @return professor detail, or {@code null} if not found
   */
  public CatalogueDocenteDetail getDocenteDetail(String baseUrl, String docenteId, String anno) {
    log.debug("CourseCatalogueClient: GET docente detail id={} anno={}", docenteId, anno);
    String raw = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("api", "v1", "docente", docenteId)
            .queryParam("anno", anno)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(
                new CinecaClient.CinecaUnavailableException("Cineca error on docente detail")))
        .bodyToMono(String.class)
        .onErrorResume(e -> e instanceof CinecaClient.CinecaUnavailableException
            ? Mono.error(e) : Mono.empty())
        .block();

    if (raw == null || raw.isBlank()) {
      return null;
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(raw, CatalogueDocenteDetail.class);
    } catch (Exception e) {
      log.error("CourseCatalogueClient: failed to parse docente detail id={}", docenteId, e);
      return null;
    }
  }

  /**
   * Retrieves lightweight base data for a single course — notably {@code codicione} and
   * {@code ordinamento_aa}, both required as query parameters for {@link #getCorsoOfferta}. Much
   * cheaper than the full listing used by {@link #resolveCourseDetail}/{@link #getAllCourseCodes}
   * (single course, not the whole ateneo catalogue).
   *
   * @param baseUrl Course Catalogue base URL for the university
   * @param year    offer year
   * @param cod     Course Catalogue internal course code (the {@code cod} field from
   *                {@link CinecaCourseCatalogueDetail}, same identifier used elsewhere as
   *                "courseCode")
   * @return course base data, or {@code Optional.empty()} if not found
   */
  public Optional<CinecaCorsoBase> getCorsoBase(String baseUrl, int year, String cod) {
    String raw = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("api", "v1", "corso", String.valueOf(year), cod)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(new CinecaClient.CinecaUnavailableException("Cineca error on corso base")))
        .bodyToMono(String.class)
        .onErrorResume(e -> e instanceof CinecaClient.CinecaUnavailableException
            ? Mono.error(e) : Mono.empty())
        .block();

    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(raw);

      if (!root.isArray() || root.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(mapper.treeToValue(root.get(0), CinecaCorsoBase.class));
    } catch (Exception e) {
      log.error("CourseCatalogueClient: failed to parse corso base cod={} year={}", cod, year, e);
      return Optional.empty();
    }
  }

  /**
   * Retrieves the full multi-year teaching offer for a course, including, for every activity, the
   * professors actually assigned to teach it ({@code docenti[]}) — a richer, correctly-attributed
   * source than {@link #getListaDocenti}, which only returns "who teaches somewhere in this degree
   * course", not "who teaches this specific activity".
   *
   * <p>The response shape is irregular (a "SON" root object keyed by year,
   * then by an opaque composite period key), so it's parsed generically via
   * {@link com.fasterxml.jackson.databind.JsonNode} traversal rather than a fixed POJO — only the
   * inner {@code attivita[]} entries have a stable, mappable shape.
   *
   * @param baseUrl         Course Catalogue base URL for the university
   * @param cod             Course Catalogue internal course code
   * @param codicione       from {@link #getCorsoBase}
   * @param annoOrdinamento from {@link #getCorsoBase} ({@code ordinamento_aa})
   * @param sedeId          location identifier
   * @return every activity found across all years/periods; empty if unresolvable
   */
  public List<CorsoOffertaAttivita> getCorsoOfferta(
      String baseUrl, String cod, String codicione, String annoOrdinamento, Long sedeId) {
    String raw = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host(extractHost(baseUrl))
            .pathSegment("api", "v1", "corso-offerta", cod)
            .queryParam("codicione", codicione)
            .queryParam("annoOrdinamento", annoOrdinamento)
            .queryParam("sede", sedeId)
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.empty())
        .onStatus(HttpStatusCode::is5xxServerError, r ->
            Mono.error(
                new CinecaClient.CinecaUnavailableException("Cineca error on corso offerta")))
        .bodyToMono(String.class)
        .onErrorResume(e -> e instanceof CinecaClient.CinecaUnavailableException
            ? Mono.error(e) : Mono.empty())
        .block();

    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(raw);

      List<CorsoOffertaAttivita> result = new java.util.ArrayList<>();
      Iterator<String> years = root.fieldNames();
      while (years.hasNext()) {
        JsonNode yearNode = root.get(years.next());
        if (yearNode == null || !yearNode.isObject()) {
          continue;
        }
        Iterator<String> periods = yearNode.fieldNames();
        while (periods.hasNext()) {
          JsonNode periodNode = yearNode.get(periods.next());
          JsonNode attivitaNode = periodNode.get("attivita");
          if (attivitaNode == null || !attivitaNode.isArray()) {
            continue;
          }
          for (JsonNode a : attivitaNode) {
            result.add(mapper.treeToValue(a, CorsoOffertaAttivita.class));
          }
        }
      }
      return result;
    } catch (Exception e) {
      log.error("CourseCatalogueClient: failed to parse corso offerta cod={}", cod, e);
      return List.of();
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

  /**
   * A single course entry from the unfiltered listing, used by {@link #getAllCourseCodes}.
   * {@code sedeCod}, when present, lets the caller pass the exact campus for
   * {@link #getListaDocenti}; when absent (not confirmed present on every university/course), the
   * caller must fall back to something else — see {@code CourseCatalogueService}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCourseCode {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("cdsCod")
    private String cdsCod;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("sede_cod")
    private String sedeCod;

    public String getCod() {
      return cod;
    }

    public String getCdsCod() {
      return cdsCod;
    }

    public String getDesIt() {
      return desIt;
    }

    public String getSedeCod() {
      return sedeCod;
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
     * True for the shared/pooled percorso some universities expose (observed as
     * {@code "comune": true}), which is not a real curriculum but a catch-all bucket — always
     * excluded from selection.
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

    /**
     * Falls back to the activity-level field when there is no per-module split.
     */
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

    /**
     * Distinct teacher names for this module, in the order Cineca returns them.
     */
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
     * Course-level content ("contenuti_it"). Populated for single-subject courses ("corso
     * monodisciplinare") that have no per-module breakdown at all — their whole program lives here
     * instead of in {@code mod_contenuti_it} entries.
     */
    public String getContenutiIt() {
      return contenutiIt;
    }

    public boolean isCourseLevel() {
      return chiaveUdCod == null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CatalogueDocente {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("des")
    private String des;
    @JsonProperty("matricola")
    private String matricola;
    @JsonProperty("cod")
    private String cod;
    @JsonProperty("hyperlink")
    private String hyperlink;
    @JsonProperty("urlUnifind")
    private String urlUnifind;

    public String getId() {
      return id;
    }

    public String getDes() {
      return des;
    }

    public String getMatricola() {
      return matricola;
    }

    public String getCod() {
      return cod;
    }

    /**
     * Direct link to the university's own teacher profile page, when Cineca has one on file.
     */
    public String getHyperlink() {
      return (hyperlink == null || hyperlink.isBlank()) ? null : hyperlink.trim();
    }

    public String getUrlUnifind() {
      return (urlUnifind == null || urlUnifind.isBlank()) ? null : urlUnifind;
    }
  }

  /**
   * Full professor profile from Course Catalogue's own docente detail endpoint — a different,
   * richer source than ESSE3's logistica-service CaricoDocenti (which never has email populated).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CatalogueDocenteDetail {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("cod")
    private String cod;
    @JsonProperty("des")
    private String des;
    @JsonProperty("ruolo_it")
    private String ruoloIt;
    @JsonProperty("ruolo_en")
    private String ruoloEn;
    @JsonProperty("matricola")
    private String matricola;
    @JsonProperty("insegnamenti_ricerca")
    private String insegnamentiRicerca;
    @JsonProperty("insegnamenti")
    private List<CatalogueInsegnamento> insegnamenti;
    @JsonProperty("curriculum_it")
    private String curriculumIt;
    @JsonProperty("curriculum_en")
    private String curriculumEn;
    @JsonProperty("biografia_it")
    private String biografiaIt;
    @JsonProperty("biografia_en")
    private String biografiaEn;
    @JsonProperty("pubblicazioni_it")
    private String pubblicazioniIt;
    @JsonProperty("pubblicazioni_en")
    private String pubblicazioniEn;
    @JsonProperty("note_it")
    private String noteIt;
    @JsonProperty("note_en")
    private String noteEn;
    @JsonProperty("aa")
    private String aa;
    @JsonProperty("ateCod")
    private String ateCod;
    @JsonProperty("email")
    private String email;
    @JsonProperty("emailAteneo")
    private String emailAteneo;
    @JsonProperty("hyperlink")
    private String hyperlink;
    @JsonProperty("facCod")
    private String facCod;
    @JsonProperty("facDes")
    private String facDes;
    @JsonProperty("urlUnifind")
    private String urlUnifind;
    @JsonProperty("url")
    private String url;

    private static String blankToNull(String s) {
      return (s == null || s.isBlank()) ? null : s.trim();
    }

    public String getId() {
      return id;
    }

    public String getCod() {
      return cod;
    }

    public String getDes() {
      return des;
    }

    public String getRuoloIt() {
      return ruoloIt;
    }

    public String getRuoloEn() {
      return ruoloEn;
    }

    public String getMatricola() {
      return matricola;
    }

    public String getInsegnamentiRicerca() {
      return insegnamentiRicerca;
    }

    public List<CatalogueInsegnamento> getInsegnamenti() {
      return insegnamenti != null ? insegnamenti : List.of();
    }

    public String getCurriculumIt() {
      return blankToNull(curriculumIt);
    }

    public String getCurriculumEn() {
      return blankToNull(curriculumEn);
    }

    public String getBiografiaIt() {
      return blankToNull(biografiaIt);
    }

    public String getBiografiaEn() {
      return blankToNull(biografiaEn);
    }

    public String getPubblicazioniIt() {
      return blankToNull(pubblicazioniIt);
    }

    public String getPubblicazioniEn() {
      return blankToNull(pubblicazioniEn);
    }

    public String getNoteIt() {
      return blankToNull(noteIt);
    }

    public String getNoteEn() {
      return blankToNull(noteEn);
    }

    public String getAa() {
      return aa;
    }

    public String getEmail() {
      return blankToNull(email);
    }

    public String getEmailAteneo() {
      return blankToNull(emailAteneo);
    }

    public String getHyperlink() {
      return blankToNull(hyperlink);
    }

    public String getFacCod() {
      return facCod;
    }

    public String getFacDes() {
      return facDes;
    }

    public String getUrlUnifind() {
      return blankToNull(urlUnifind);
    }

    public String getUrl() {
      return url;
    }
  }

  /**
   * A single course taught by a professor, from CatalogueDocenteDetail.insegnamenti.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CatalogueInsegnamento {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("aa")
    private String aa;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("des_en")
    private String desEn;
    @JsonProperty("corso_cod")
    private String corsoCod;
    @JsonProperty("cdsCod")
    private String cdsCod;
    @JsonProperty("ordinamento_aa")
    private Integer ordinamentoAa;
    @JsonProperty("corso_des_it")
    private String corsoDesIt;
    @JsonProperty("corso_des_en")
    private String corsoDesEn;
    @JsonProperty("corso_anno")
    private String corsoAnno;
    @JsonProperty("corso_aa")
    private String corsoAa;
    @JsonProperty("corso_percorso_des_it")
    private String corsoPercorsoDesIt;
    @JsonProperty("corso_percorso_des_en")
    private String corsoPercorsoDesEn;
    @JsonProperty("corso_percorso_cod")
    private String corsoPercorsoCod;
    @JsonProperty("udCod")
    private String udCod;
    @JsonProperty("statoAttivita")
    private String statoAttivita;
    @JsonProperty("tipo_corso_cod")
    private String tipoCorsoCod;
    @JsonProperty("tipo_corso_des_it")
    private String tipoCorsoDesIt;
    @JsonProperty("tipo_corso_des_en")
    private String tipoCorsoDesEn;
    @JsonProperty("profili")
    private List<String> profili;

    public String getCod() {
      return cod;
    }

    public String getAdCod() {
      return adCod;
    }

    public String getAa() {
      return aa;
    }

    public String getDesIt() {
      return desIt;
    }

    public String getDesEn() {
      return desEn;
    }

    public String getCorsoCod() {
      return corsoCod;
    }

    public String getCdsCod() {
      return cdsCod;
    }

    public Integer getOrdinamentoAa() {
      return ordinamentoAa;
    }

    public String getCorsoDesIt() {
      return corsoDesIt;
    }

    public String getCorsoDesEn() {
      return corsoDesEn;
    }

    public String getCorsoAnno() {
      return corsoAnno;
    }

    public String getCorsoAa() {
      return corsoAa;
    }

    public String getCorsoPercorsoDesIt() {
      return corsoPercorsoDesIt;
    }

    public String getCorsoPercorsoDesEn() {
      return corsoPercorsoDesEn;
    }

    public String getCorsoPercorsoCod() {
      return corsoPercorsoCod;
    }

    public String getUdCod() {
      return udCod;
    }

    public String getStatoAttivita() {
      return statoAttivita;
    }

    public String getTipoCorsoCod() {
      return tipoCorsoCod;
    }

    public String getTipoCorsoDesIt() {
      return tipoCorsoDesIt;
    }

    public String getTipoCorsoDesEn() {
      return tipoCorsoDesEn;
    }

    public List<String> getProfili() {
      return profili != null ? profili : List.of();
    }

    /**
     * Whether this activity is a shared/cross-listed one outside the student's own year bucket.
     */
    public boolean isAltreAttivita() {
      return "altreAttivita".equals(corsoAnno);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CinecaCorsoBase {

    @JsonProperty("cod")
    private String cod;
    @JsonProperty("codicione")
    private String codicione;
    @JsonProperty("ordinamento_aa")
    private Integer ordinamentoAa;

    public String getCod() {
      return cod;
    }

    public String getCodicione() {
      return codicione;
    }

    public Integer getOrdinamentoAa() {
      return ordinamentoAa;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CorsoOffertaAttivita {

    @JsonProperty("adCod")
    private String adCod;
    @JsonProperty("des_it")
    private String desIt;
    @JsonProperty("docenti")
    private List<CorsoOffertaDocente> docenti;

    public String getAdCod() {
      return adCod;
    }

    public String getDesIt() {
      return desIt;
    }

    public List<CorsoOffertaDocente> getDocenti() {
      return docenti != null ? docenti : List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CorsoOffertaDocente {

    @JsonProperty("cod")
    private Long cod;
    @JsonProperty("matricola")
    private String matricola;
    @JsonProperty("hyperlink")
    private String hyperlink;
    @JsonProperty("des")
    private String des;

    public Long getCod() {
      return cod;
    }

    public String getMatricola() {
      return matricola;
    }

    public String getHyperlink() {
      return hyperlink;
    }

    public String getDes() {
      return des;
    }
  }
}
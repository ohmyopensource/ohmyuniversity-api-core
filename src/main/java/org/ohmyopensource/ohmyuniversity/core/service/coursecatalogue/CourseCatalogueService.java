package org.ohmyopensource.ohmyuniversity.core.service.coursecatalogue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaAnnoOfferta;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaCourseCatalogueDetail;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaCoursePlanEntry;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaInsegnamentoDetail;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaInsegnamentoGroup;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaModuloInfo;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaPercorso;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CinecaTestiEntry;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.CourseCatalogueClient.CorsoOffertaDocente;
import org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue.TtlCache;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaCarriera;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CoursePlanResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CoursePlanResponse.CoursePlanExam;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CourseSyllabusResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.CourseSyllabusResponse.CfuBreakdownItem;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.DocenteDetailResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.coursecatalogue.DocentiListResponse;
import org.ohmyopensource.ohmyuniversity.core.service.esse3.AbstractEsse3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for enriching career data with Cineca Course Catalogue data
 * (coursecatalogue.cineca.it) — a separate Cineca product from ESSE3, used only for information
 * ESSE3 does not expose: the full multi-year course plan (including years not yet in the student's
 * libretto) and, per exam, prerequisites and the per-module CFU breakdown ("programma per CFU").
 *
 * <p>Course Catalogue data is identified by its own codes and is identical
 * for every student of the same university, cohort (immatricolazione year) and degree course —
 * never student-specific — so results are cached upstream in {@link CourseCatalogueClient}. The one
 * student-specific piece is <em>which curriculum track</em> (percorso) applies, resolved in
 * {@link #resolveContext} by matching against the student's own transcript.
 */
@Service
public class CourseCatalogueService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(CourseCatalogueService.class);

  private final CourseCatalogueClient catalogueClient;
  private final CinecaProfileClient profileClient;
  private final CinecaCareerClient careerClient;
  private final UniversityRegistry universityRegistryRef;

  /**
   * Caches the ateneo-wide professor directory per university (not per student — the underlying
   * data is identical for everyone), since building it requires iterating every course of the
   * university.
   */
  private final TtlCache<DocentiListResponse> allDocentiCache = new TtlCache<>(
      Duration.ofHours(24));

  // ============ Constructor ============

  /**
   * Constructs the service with all required Cineca clients and shared ESSE3 session/registry
   * dependencies.
   *
   * @param catalogueClient      Course Catalogue HTTP client
   * @param profileClient        ESSE3 profile/carriera client, used to resolve cdsCod and cohort
   * @param careerClient         ESSE3 career client, used to match the student's transcript against
   *                             curriculum tracks
   * @param sessionStore         shared Cineca session store (see AbstractEsse3Service)
   * @param universityRegistry   shared university configuration registry
   * @param connectionRepository shared university connection repository
   */
  public CourseCatalogueService(
      CourseCatalogueClient catalogueClient,
      CinecaProfileClient profileClient,
      CinecaCareerClient careerClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.catalogueClient = catalogueClient;
    this.profileClient = profileClient;
    this.careerClient = careerClient;
    this.universityRegistryRef = universityRegistry;
  }

  // ============ Class Methods ============

  /**
   * Returns the full multi-year course plan for the authenticated student's degree course, cohort
   * and curriculum track, flattened across years.
   *
   * @param principal authenticated OhMyU principal
   * @return the course plan; empty if the course could not be resolved in the Course Catalogue
   */
  public CoursePlanResponse getCoursePlan(OmuPrincipal principal) {
    CourseCatalogueContext ctx = resolveContext(principal);
    List<FlatEntry> entries = ctx == null ? List.of() : flattenActivities(ctx.percorsi());

    CoursePlanResponse response = new CoursePlanResponse();
    response.setExams(entries.stream().map(this::toCoursePlanExam).toList());
    return response;
  }

  /**
   * Returns the syllabus (prerequisites and per-module CFU breakdown) for a single teaching
   * activity within the authenticated student's course plan.
   *
   * @param principal authenticated OhMyU principal
   * @param adCod     ESSE3 teaching activity code
   * @return the syllabus response; all fields {@code null}/empty if the Course Catalogue has no
   * data for this activity (common — not every university compiles this)
   */
  public CourseSyllabusResponse getCourseSyllabus(OmuPrincipal principal, String adCod) {
    CourseSyllabusResponse response = new CourseSyllabusResponse();

    CourseCatalogueContext ctx = resolveContext(principal);
    if (ctx == null) {
      return response;
    }

    List<FlatEntry> entries = flattenActivities(ctx.percorsi());
    CinecaCoursePlanEntry entry = entries.stream()
        .map(FlatEntry::entry)
        .filter(e -> adCod.equals(e.getAdCod()))
        .findFirst()
        .orElse(null);
    if (entry == null || entry.getCod() == null || entry.getAa() == null
        || entry.getOrdinamentoAa() == null || entry.getAfPercorsoId() == null) {
      log.debug("CourseCatalogueService: no plan entry / incomplete keys for adCod={}", adCod);
      return response;
    }

    CinecaInsegnamentoDetail detail;
    try {
      detail = catalogueClient.getInsegnamentoDetail(
          ctx.catalogueBaseUrl(),
          Integer.parseInt(entry.getAa()),
          entry.getCod(),
          entry.getOrdinamentoAa(),
          entry.getAfPercorsoId(),
          ctx.courseCode(),
          ctx.cohortYear());
    } catch (NumberFormatException e) {
      log.debug("CourseCatalogueService: unparsable aa={} for adCod={}", entry.getAa(), adCod);
      return response;
    }
    if (detail == null) {
      return response;
    }

    populateSyllabus(response, detail);
    return response;
  }

  /**
   * Returns the professors who actually teach the exams in the student's own multi-year study plan
   * (all years, past and future — see {@link #getCoursePlan}), not every professor loosely
   * affiliated with the degree course. Cross-references two Course Catalogue endpoints:
   * {@code corso-offerta} (which activity is taught by which professor — correctly attributed, but
   * its docente objects lack the internal _id needed to fetch full detail) and
   * {@link #getAllDocenti}'s underlying {@code lista-docenti} data (which has that _id, keyed by
   * matricola).
   *
   * @param principal authenticated OhMyU principal
   * @return professors teaching the student's real courses; empty if not resolvable
   */
  public DocentiListResponse getDocenti(OmuPrincipal principal) {
    CourseCatalogueContext ctx = resolveContext(principal);
    DocentiListResponse response = new DocentiListResponse();
    if (ctx == null || ctx.sedeId() == null) {
      response.setDocenti(List.of());
      return response;
    }

    Optional<CourseCatalogueClient.CinecaCorsoBase> corsoBase =
        catalogueClient.getCorsoBase(ctx.catalogueBaseUrl(), ctx.cohortYear(), ctx.courseCode());
    if (corsoBase.isEmpty()) {
      response.setDocenti(List.of());
      return response;
    }

    List<CourseCatalogueClient.CorsoOffertaAttivita> offerta = catalogueClient.getCorsoOfferta(
        ctx.catalogueBaseUrl(),
        ctx.courseCode(),
        corsoBase.get().getCodicione(),
        String.valueOf(corsoBase.get().getOrdinamentoAa()),
        ctx.sedeId());

    Set<String> planAdCods = flattenActivities(ctx.percorsi()).stream()
        .map(this::toCoursePlanExam)
        .map(CoursePlanResponse.CoursePlanExam::getAdCod)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<String, CorsoOffertaDocente> byKey =
        new LinkedHashMap<>();
    for (CourseCatalogueClient.CorsoOffertaAttivita attivita : offerta) {
      if (attivita.getAdCod() == null || !planAdCods.contains(attivita.getAdCod())) {
        continue;
      }
      for (CourseCatalogueClient.CorsoOffertaDocente d : attivita.getDocenti()) {
        if (isPlaceholderName(d.getDes())) {
          continue;
        }
        String key = (d.getMatricola() != null && !d.getMatricola().isBlank())
            ? d.getMatricola() : d.getDes();
        if (key == null) {
          continue;
        }
        byKey.putIfAbsent(key, d);
      }
    }

    if (byKey.isEmpty()) {
      response.setDocenti(List.of());
      return response;
    }

    String currentYear = String.valueOf(java.time.Year.now().getValue());
    List<CourseCatalogueClient.CatalogueDocente> listaDocenti = catalogueClient
        .getListaDocenti(ctx.catalogueBaseUrl(), currentYear, ctx.courseCode(), ctx.sedeId());

    Map<String, String> idByMatricola = listaDocenti.stream()
        .filter(d -> d.getMatricola() != null && !d.getMatricola().isBlank())
        .collect(Collectors.toMap(
            CourseCatalogueClient.CatalogueDocente::getMatricola,
            CourseCatalogueClient.CatalogueDocente::getId,
            (a, b) -> a));
    Map<String, String> idByName = listaDocenti.stream()
        .filter(d -> d.getDes() != null)
        .collect(Collectors.toMap(
            d -> d.getDes().trim().toUpperCase(),
            CourseCatalogueClient.CatalogueDocente::getId,
            (a, b) -> a));

    response.setDocenti(byKey.values().stream()
        .map(d -> {
          DocentiListResponse.DocenteSummary s = new DocentiListResponse.DocenteSummary();
          String resolvedId = null;
          if (d.getMatricola() != null && !d.getMatricola().isBlank()) {
            resolvedId = idByMatricola.get(d.getMatricola());
          }
          if (resolvedId == null && d.getDes() != null) {
            resolvedId = idByName.get(d.getDes().trim().toUpperCase());
          }
          s.setId(resolvedId);
          s.setHasDetail(resolvedId != null);
          s.setName(d.getDes());
          s.setProfilePageUrl((d.getHyperlink() == null || d.getHyperlink().isBlank())
              ? null : d.getHyperlink().trim());
          return s;
        })
        .sorted(java.util.Comparator.comparing(
            DocentiListResponse.DocenteSummary::getName, String.CASE_INSENSITIVE_ORDER))
        .toList());
    return response;
  }

  /**
   * Returns full detail for a single professor, cross-referenced against the student's own
   * transcript so the response can flag which of the professor's courses the student is actually
   * taking.
   *
   * @param principal authenticated OhMyU principal
   * @param docenteId Course Catalogue internal id (from {@link #getDocenti})
   * @return professor detail, or an empty response if not found
   */
  public DocenteDetailResponse getDocenteDetail(OmuPrincipal principal, String docenteId) {
    CourseCatalogueContext ctx = resolveContext(principal);
    DocenteDetailResponse response = new DocenteDetailResponse();
    if (ctx == null) {
      return response;
    }

    String currentYear = String.valueOf(java.time.Year.now().getValue());
    CourseCatalogueClient.CatalogueDocenteDetail detail =
        catalogueClient.getDocenteDetail(ctx.catalogueBaseUrl(), docenteId, currentYear);
    if (detail == null) {
      return response;
    }

    String jwt = resolveCinecaJwt(principal);
    String esse3BaseUrl = resolveBaseUrl(principal.universityId());
    Set<String> ownAdCods = fetchKnownAdCods(esse3BaseUrl, jwt, principal.matId());

    response.setName(detail.getDes());
    response.setRole(detail.getRuoloIt());
    response.setEmail(detail.getEmail());
    response.setDepartment(detail.getFacDes());
    response.setProfilePageUrl(detail.getHyperlink());
    response.setBiography(detail.getBiografiaIt());
    response.setPublications(detail.getPubblicazioniIt());
    response.setNotes(detail.getNoteIt());
    Map<String, CourseCatalogueClient.CatalogueInsegnamento> uniqueByCourse =
        new LinkedHashMap<>();
    for (CourseCatalogueClient.CatalogueInsegnamento i : detail.getInsegnamenti()) {
      String key = i.getAdCod() != null ? i.getAdCod() : i.getDesIt();
      if (key == null) {
        continue;
      }
      CourseCatalogueClient.CatalogueInsegnamento existing = uniqueByCourse.get(key);
      if (existing == null || compareYearDesc(i.getAa(), existing.getAa()) > 0) {
        uniqueByCourse.put(key, i);
      }
    }

    response.setCourses(uniqueByCourse.values().stream()
        .map(i -> {
          DocenteDetailResponse.TaughtCourse c = new DocenteDetailResponse.TaughtCourse();
          c.setName(i.getDesIt());
          c.setDegreeCourseName(i.getCorsoDesIt());
          c.setDegreeCourseType(i.getTipoCorsoDesIt());
          c.setInStudentCareer(i.getAdCod() != null && ownAdCods.contains(i.getAdCod()));
          return c;
        })
        .toList());
    return response;
  }

  /**
   * Returns the ateneo-wide professor directory — every professor teaching any course of the
   * university, not just the requesting student's own. Built by iterating
   * {@link CourseCatalogueClient#getAllCourseCodes} and calling
   * {@link CourseCatalogueClient#getListaDocenti} once per course, cached for 24h since this is
   * identical for every student of the same university.
   *
   * @param principal authenticated OhMyU principal
   * @return the full professor directory; empty if not resolvable
   */
  public DocentiListResponse getAllDocenti(OmuPrincipal principal) {
    String catalogueBaseUrl = universityRegistryRef.getCourseCatalogueUrl(principal.universityId());
    if (catalogueBaseUrl == null) {
      DocentiListResponse empty = new DocentiListResponse();
      empty.setDocenti(List.of());
      return empty;
    }

    CourseCatalogueContext ctx = resolveContext(principal);
    Long fallbackSedeId = ctx != null ? ctx.sedeId() : null;

    String cacheKey = principal.universityId() + ":" + java.time.Year.now().getValue();
    return allDocentiCache.getOrCompute(cacheKey,
        () -> computeAllDocenti(catalogueBaseUrl, fallbackSedeId));
  }

  /**
   * Fetches and assembles the ateneo-wide professor directory (uncached — caching happens in
   * {@link #getAllDocenti}).
   *
   * @param catalogueBaseUrl Course Catalogue base URL for the university
   * @param fallbackSedeId   location to use for courses whose own sede code can't be parsed
   * @return the assembled directory
   */
  private DocentiListResponse computeAllDocenti(String catalogueBaseUrl, Long fallbackSedeId) {
    int currentYear = java.time.Year.now().getValue();
    String currentYearStr = String.valueOf(currentYear);

    List<CourseCatalogueClient.CinecaCourseCode> courseCodes =
        catalogueClient.getAllCourseCodes(catalogueBaseUrl, currentYear);

    Map<String, CourseCatalogueClient.CatalogueDocente> uniqueByMatricola =
        new LinkedHashMap<>();
    for (CourseCatalogueClient.CinecaCourseCode course : courseCodes) {
      Long sedeId = parseSedeId(course.getSedeCod(), fallbackSedeId);
      if (sedeId == null) {
        continue;
      }
      List<CourseCatalogueClient.CatalogueDocente> docenti = catalogueClient.getListaDocenti(
          catalogueBaseUrl, currentYearStr, course.getCod(), sedeId);
      for (CourseCatalogueClient.CatalogueDocente d : docenti) {
        if (!isRealProfessor(d)) {
          continue;
        }
        String key = (d.getMatricola() != null && !d.getMatricola().isBlank())
            ? d.getMatricola() : d.getDes();
        uniqueByMatricola.putIfAbsent(key, d);
      }
    }

    DocentiListResponse response = new DocentiListResponse();
    response.setDocenti(uniqueByMatricola.values().stream()
        .map(this::toDocenteSummary)
        .sorted(Comparator.comparing(
            DocentiListResponse.DocenteSummary::getName, String.CASE_INSENSITIVE_ORDER))
        .toList());
    return response;
  }

  /**
   * Parses a location code, falling back to a default when absent or unparsable.
   *
   * @param sedeCod  raw location code string
   * @param fallback location to use if parsing fails or the code is absent
   * @return the parsed location id, or the fallback
   */
  private Long parseSedeId(String sedeCod, Long fallback) {
    if (sedeCod != null && !sedeCod.isBlank()) {
      try {
        return Long.parseLong(sedeCod.trim());
      } catch (NumberFormatException e) {
        // fall through to fallback
      }
    }
    return fallback;
  }

  /**
   * Maps a Course Catalogue professor entry to its API response shape.
   *
   * @param d professor entry to map
   * @return the mapped summary
   */
  private DocentiListResponse.DocenteSummary toDocenteSummary(
      CourseCatalogueClient.CatalogueDocente d) {
    DocentiListResponse.DocenteSummary s = new DocentiListResponse.DocenteSummary();
    s.setId(d.getId());
    s.setHasDetail(d.getId() != null && !d.getId().isBlank());
    s.setName(d.getDes());
    s.setProfilePageUrl(d.getHyperlink());
    return s;
  }

  /**
   * Filters out administrative/placeholder entries that Cineca includes in the same list as real
   * professors (e.g. a generic "SEGRETERIA STUDENTI" contact, or "DA DEFINIRE DA DEFINIRE" for an
   * unassigned teaching slot). Identified by known sentinel matricola values and name patterns —
   * matricola alone can't be used as "blank = fake" since some real professors also have an empty
   * matricola.
   *
   * @param d professor entry to check
   * @return {@code true} if this is a real professor, not a placeholder
   */
  private boolean isRealProfessor(CourseCatalogueClient.CatalogueDocente d) {
    String des = d.getDes();
    if (des == null || des.isBlank()) {
      return false;
    }
    String upper = des.toUpperCase();
    if (upper.contains("DA DEFINIRE")) {
      return false;
    }
    if (upper.contains("SEGRETERIA STUDENTI")) {
      return false;
    }
    String matricola = d.getMatricola();
    return !"0001".equals(matricola) && !"000000000".equals(matricola);
  }

  /**
   * Same placeholder-filtering logic as isRealProfessor, adapted for CorsoOffertaDocente.
   *
   * @param des professor display name to check
   * @return {@code true} if this name is a known administrative placeholder
   */
  private boolean isPlaceholderName(String des) {
    if (des == null || des.isBlank()) {
      return true;
    }
    String upper = des.toUpperCase();
    return upper.contains("DA DEFINIRE") || upper.contains("SEGRETERIA STUDENTI");
  }

  /**
   * Resolves the university/cohort/course/track context needed for every Course Catalogue call: the
   * catalogue base URL, the cohort year (parsed from the student's immatricolazione date), the
   * Course Catalogue course code, and the specific curriculum track(s) relevant to this student.
   *
   * @param principal authenticated OhMyU principal
   * @return the resolved context, or {@code null} if any piece could not be resolved
   */
  private CourseCatalogueContext resolveContext(OmuPrincipal principal) {
    String esse3BaseUrl = resolveBaseUrl(principal.universityId());
    String jwt = resolveCinecaJwt(principal);

    CinecaCarriera carriera = profileClient.getCarriera(esse3BaseUrl, jwt, principal.matId());
    if (carriera == null || carriera.getCdsCod() == null || carriera.getDataImm() == null) {
      log.warn("CourseCatalogueService: missing cdsCod/dataImm for matId={}", principal.matId());
      return null;
    }

    Integer cohortYear = parseCohortYear(carriera.getDataImm());
    if (cohortYear == null) {
      log.warn("CourseCatalogueService: could not parse cohort year from dataImm={}",
          carriera.getDataImm());
      return null;
    }

    String catalogueBaseUrl = universityRegistryRef.getCourseCatalogueUrl(principal.universityId());
    if (catalogueBaseUrl == null) {
      log.warn("CourseCatalogueService: no course catalogue URL configured for university={}",
          principal.universityId());
      return null;
    }

    Optional<CinecaCourseCatalogueDetail> detail =
        catalogueClient.resolveCourseDetail(catalogueBaseUrl, cohortYear, carriera.getCdsCod());
    if (detail.isEmpty()) {
      log.warn("CourseCatalogueService: could not resolve course detail for cdsCod={} year={}",
          carriera.getCdsCod(), cohortYear);
      return null;
    }

    Set<String> knownAdCods = fetchKnownAdCods(esse3BaseUrl, jwt, principal.matId());
    List<CinecaPercorso> relevantPercorsi =
        selectRelevantPercorsi(detail.get().getPercorsi(), knownAdCods);

    return new CourseCatalogueContext(
        catalogueBaseUrl, cohortYear, detail.get().getCod(), relevantPercorsi,
        carriera.getSedeId());
  }

  /**
   * Retrieves the set of ESSE3 activity codes ({@code adCod}) the student actually has in their
   * transcript — a reliable signal of which specific curriculum track they follow, independent of
   * any Course Catalogue identifier.
   *
   * @param esse3BaseUrl ESSE3 base URL for the university
   * @param jwt          Cineca JWT token
   * @param matId        student career segment identifier
   * @return set of known activity codes; empty if the transcript could not be fetched
   */
  private Set<String> fetchKnownAdCods(String esse3BaseUrl, String jwt, Long matId) {
    try {
      List<CinecaTranscriptRow> rows = careerClient.getTranscript(esse3BaseUrl, jwt, matId);
      Set<String> codes = new HashSet<>();
      for (CinecaTranscriptRow row : rows) {
        if (row.getAdCod() != null) {
          codes.add(row.getAdCod());
        }
      }
      return codes;
    } catch (Exception e) {
      log.warn("CourseCatalogueService: failed to fetch transcript for track matching, "
          + "matId={}", matId, e);
      return Set.of();
    }
  }

  /**
   * Selects the single percorso (curriculum track) matching the student's actual known exams,
   * discarding shared/pooled buckets (Course Catalogue's {@code comune} flag) and any other tracks
   * that do not match.
   *
   * <p>When only one non-shared track exists, or when the student has no
   * transcript yet to match against (e.g. first-year enrollment before any exam), falls back to the
   * first available track rather than failing — best effort over no data.
   *
   * @param percorsi    all curriculum tracks returned by the Course Catalogue
   * @param knownAdCods activity codes known from the student's transcript
   * @return the single best-matching track, or all tracks if none can be resolved
   */
  private List<CinecaPercorso> selectRelevantPercorsi(
      List<CinecaPercorso> percorsi, Set<String> knownAdCods) {
    if (percorsi == null || percorsi.isEmpty()) {
      return List.of();
    }
    List<CinecaPercorso> candidates = percorsi.stream()
        .filter(p -> !Boolean.TRUE.equals(p.getComune()))
        .toList();
    if (candidates.isEmpty()) {
      return percorsi;
    }
    if (candidates.size() == 1) {
      return candidates;
    }

    CinecaPercorso best = null;
    int bestScore = -1;
    for (CinecaPercorso candidate : candidates) {
      int score = countMatchingActivities(candidate, knownAdCods);
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    log.debug("CourseCatalogueService: selected percorso pdsCod={} (score={}) among {} tracks",
        best != null ? best.getPdsCod() : null, bestScore, candidates.size());
    return best != null ? List.of(best) : List.of(candidates.get(0));
  }

  /**
   * Counts how many of the percorso's activities match the student's known exam codes.
   *
   * @param percorso    the curriculum track to score
   * @param knownAdCods activity codes known from the student's transcript
   * @return number of matching activities
   */
  private int countMatchingActivities(CinecaPercorso percorso, Set<String> knownAdCods) {
    if (knownAdCods.isEmpty() || percorso.getAnni() == null) {
      return 0;
    }
    int count = 0;
    for (CinecaAnnoOfferta anno : percorso.getAnni()) {
      if (anno.getInsegnamenti() == null) {
        continue;
      }
      for (CinecaInsegnamentoGroup group : anno.getInsegnamenti()) {
        if (group.getAttivita() == null) {
          continue;
        }
        for (CinecaCoursePlanEntry entry : group.getAttivita()) {
          if (entry.getAdCod() != null && knownAdCods.contains(entry.getAdCod())) {
            count++;
          }
        }
      }
    }
    return count;
  }

  /**
   * Parses the cohort year out of a Cineca date string in {@code DD/MM/YYYY HH24:MI:SS} format
   * (e.g. {@code "06/09/2023 00:00:00"}).
   *
   * @param dataImm raw Cineca immatricolazione date string
   * @return the parsed cohort year, or {@code null} if unparsable
   */
  private Integer parseCohortYear(String dataImm) {
    try {
      String datePart = dataImm.split(" ")[0];
      String[] parts = datePart.split("/");
      return Integer.parseInt(parts[2]);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Compares two "aa" year strings, treating unparsable values (e.g. "altreAttivita") as oldest.
   *
   * @param a first year string
   * @param b second year string
   * @return standard {@link Integer#compare} result, or {@code 0} if either is unparsable
   */
  private int compareYearDesc(String a, String b) {
    try {
      return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Flattens the nested {@code percorsi -> anni -> insegnamenti -> attivita} structure, pairing
   * each activity with the academic year of its containing bucket. The bucket-level year is
   * authoritative because some universities (observed: Molise) leave the activity's own
   * {@code annoCorso} field entirely unpopulated, while others (observed: Chieti) populate it
   * redundantly and consistently with the bucket. Non-numeric buckets (e.g. Molise's
   * {@code "altreAttivita"} — shared electives cross-listed from other degree programs, not part of
   * this student's own curriculum) are skipped entirely.
   *
   * @param percorsi curriculum tracks to flatten
   * @return every activity paired with its bucket year, across all given tracks
   */
  private List<FlatEntry> flattenActivities(List<CinecaPercorso> percorsi) {
    List<FlatEntry> flat = new ArrayList<>();
    if (percorsi == null) {
      return flat;
    }
    for (CinecaPercorso percorso : percorsi) {
      if (percorso.getAnni() == null) {
        continue;
      }
      for (CinecaAnnoOfferta anno : percorso.getAnni()) {
        Integer bucketYear = parseCurriculumYear(anno.getAnno());
        if (bucketYear == null) {
          continue;
        }
        if (anno.getInsegnamenti() == null) {
          continue;
        }
        for (CinecaInsegnamentoGroup group : anno.getInsegnamenti()) {
          if (group.getAttivita() == null) {
            continue;
          }
          for (CinecaCoursePlanEntry entry : group.getAttivita()) {
            flat.add(new FlatEntry(entry, bucketYear));
          }
        }
      }
    }
    return flat;
  }

  /**
   * Parses {@code anno} into a positive curriculum year, or {@code null} for non-numeric buckets
   * such as Molise's {@code "altreAttivita"}.
   *
   * @param anno raw bucket year string
   * @return the parsed year, or {@code null} if not a positive integer
   */
  private Integer parseCurriculumYear(String anno) {
    if (anno == null) {
      return null;
    }
    try {
      int year = Integer.parseInt(anno.trim());
      return year > 0 ? year : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Maps a flattened plan entry to its API response shape.
   *
   * @param flatEntry the entry and its resolved bucket year
   * @return the mapped exam
   */
  private CoursePlanExam toCoursePlanExam(FlatEntry flatEntry) {
    CinecaCoursePlanEntry entry = flatEntry.entry();
    CoursePlanExam exam = new CoursePlanExam();
    exam.setAdCod(entry.getAdCod());
    exam.setName(entry.getDesIt());
    exam.setCfu(entry.getCrediti());
    exam.setAcademicYear(
        entry.getAnnoCorso() != null && entry.getAnnoCorso() > 0
            ? entry.getAnnoCorso()
            : flatEntry.bucketYear());
    exam.setPeriod(entry.getPeriodDes());
    String taf = entry.getTafDesIt();
    exam.setMandatory(taf == null || !taf.toLowerCase().contains("scelta"));
    return exam;
  }

  /**
   * Populates the syllabus response's prerequisites and per-module CFU breakdown from the
   * activity's raw Course Catalogue detail.
   *
   * @param response the response to populate
   * @param detail   raw activity detail from the Course Catalogue
   */
  private void populateSyllabus(CourseSyllabusResponse response, CinecaInsegnamentoDetail detail) {
    if (detail.getTestiTotali() == null) {
      return;
    }

    String prerequisites = detail.getTestiTotali().stream()
        .filter(CinecaTestiEntry::isCourseLevel)
        .map(CinecaTestiEntry::getPrerequisitiIt)
        .filter(this::isMeaningful)
        .findFirst()
        .or(() -> detail.getTestiTotali().stream()
            .map(CinecaTestiEntry::getModPrerequisitiIt)
            .filter(this::isMeaningful)
            .findFirst())
        .orElse(null);
    response.setPrerequisites(stripHtml(prerequisites));

    List<CfuBreakdownItem> breakdown = new ArrayList<>();
    for (CinecaTestiEntry testi : detail.getTestiTotali()) {
      if (testi.isCourseLevel() || !isMeaningful(testi.getModContenutiIt())) {
        continue;
      }
      CinecaModuloInfo modulo = detail.getModuli() == null ? null : detail.getModuli().stream()
          .filter(m -> testi.getChiaveUdCod().equals(m.getCod()))
          .findFirst()
          .orElse(null);

      CfuBreakdownItem item = new CfuBreakdownItem();
      item.setModuleName(modulo != null ? modulo.getDesIt() : testi.getChiaveUdCod());
      item.setCfu(modulo != null ? modulo.getCrediti() : null);
      item.setContent(stripHtml(testi.getModContenutiIt()));
      item.setProfessors(modulo != null ? modulo.getProfessorNames() : List.of());
      breakdown.add(item);
    }

    if (breakdown.isEmpty()) {
      String courseContent = detail.getTestiTotali().stream()
          .filter(CinecaTestiEntry::isCourseLevel)
          .map(CinecaTestiEntry::getContenutiIt)
          .filter(this::isMeaningful)
          .findFirst()
          .orElse(null);
      if (courseContent != null) {
        CfuBreakdownItem item = new CfuBreakdownItem();
        item.setModuleName(detail.getDesIt());
        item.setCfu(null);
        item.setContent(stripHtml(courseContent));
        item.setProfessors(List.of());
        breakdown.add(item);
      }
    }

    response.setCfuBreakdown(breakdown);
  }

  /**
   * Checks whether a raw text field carries real content, filtering out Cineca's own "see
   * integrated course" placeholder text.
   *
   * @param raw raw (possibly HTML) text field
   * @return {@code true} if the field has meaningful content
   */
  private boolean isMeaningful(String raw) {
    String stripped = stripHtml(raw);
    return stripped != null && !stripped.isBlank()
        && !stripped.toUpperCase()
        .contains("INFORMAZIONE INSERITA NEL PROGRAMMA DEL CORSO INTEGRATO");
  }

  /**
   * Strips the HTML markup ({@code <p>}, {@code <span>}, inline styles) that the Course Catalogue
   * wraps every text field in, leaving plain text.
   *
   * @param html raw HTML-wrapped text
   * @return plain text, or {@code null} if the input was {@code null} or blank after stripping
   */
  private String stripHtml(String html) {
    if (html == null) {
      return null;
    }
    String text = html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ")
        .replaceAll("\\s+", " ").trim();
    return text.isBlank() ? null : text;
  }

  /**
   * Pairs a raw plan entry with the academic year of its containing bucket.
   */
  private record FlatEntry(CinecaCoursePlanEntry entry, int bucketYear) {

  }

  /**
   * Bundle of everything needed to call the Course Catalogue for a specific student: which
   * university, which cohort, the Course Catalogue's own course code, and the curriculum track(s)
   * relevant to this student.
   */
  private record CourseCatalogueContext(
      String catalogueBaseUrl, int cohortYear, String courseCode,
      List<CinecaPercorso> percorsi, Long sedeId) {

  }
}
package org.ohmyopensource.ohmyuniversity.core.service.coursecatalogue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.ohmyopensource.ohmyuniversity.core.service.esse3.AbstractEsse3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for enriching career data with Cineca Course Catalogue
 * data (coursecatalogue.cineca.it) — a separate Cineca product from ESSE3,
 * used only for information ESSE3 does not expose: the full multi-year
 * course plan (including years not yet in the student's libretto) and, per
 * exam, prerequisites and the per-module CFU breakdown ("programma per
 * CFU").
 *
 * <p>Course Catalogue data is identified by its own codes and is identical
 * for every student of the same university, cohort (immatricolazione year)
 * and degree course — never student-specific — so results are cached
 * upstream in {@link CourseCatalogueClient}. The one student-specific piece
 * is <em>which curriculum track</em> (percorso) applies, resolved in
 * {@link #resolveContext} by matching against the student's own transcript.
 */
@Service
public class CourseCatalogueService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(CourseCatalogueService.class);

  private final CourseCatalogueClient catalogueClient;
  private final CinecaProfileClient profileClient;
  private final CinecaCareerClient careerClient;
  private final UniversityRegistry universityRegistryRef;

  // ============ Constructor ============

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
   * Returns the full multi-year course plan for the authenticated student's
   * degree course, cohort and curriculum track, flattened across years.
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
   * Returns the syllabus (prerequisites and per-module CFU breakdown) for a
   * single teaching activity within the authenticated student's course plan.
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
   * Resolves the university/cohort/course/track context needed for every
   * Course Catalogue call: the catalogue base URL, the cohort year (parsed
   * from the student's immatricolazione date), the Course Catalogue course
   * code, and the specific curriculum track(s) relevant to this student.
   *
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

    // ESSE3's own career metadata does not expose which specific
    // percorso/indirizzo (curriculum track) the student is enrolled in, and
    // the Course Catalogue's cdsSub[].percorsi[] can contain several distinct
    // tracks for a single course (observed: Molise's "Scienze Turistiche"
    // has separate "ET" and "Tsl" tracks under the same cdsCod). We resolve
    // the correct one by matching against the student's actual known exams
    // (from their ESSE3 transcript) rather than any Course-Catalogue-side
    // identifier, since no such identifier is available from ESSE3.
    Set<String> knownAdCods = fetchKnownAdCods(esse3BaseUrl, jwt, principal.matId());
    List<CinecaPercorso> relevantPercorsi =
        selectRelevantPercorsi(detail.get().getPercorsi(), knownAdCods);

    return new CourseCatalogueContext(
        catalogueBaseUrl, cohortYear, detail.get().getCod(), relevantPercorsi);
  }

  /**
   * Retrieves the set of ESSE3 activity codes ({@code adCod}) the student
   * actually has in their transcript — a reliable signal of which specific
   * curriculum track they follow, independent of any Course Catalogue
   * identifier.
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
   * Selects the single percorso (curriculum track) matching the student's
   * actual known exams, discarding shared/pooled buckets (Course Catalogue's
   * {@code comune} flag) and any other tracks that do not match.
   *
   * <p>When only one non-shared track exists, or when the student has no
   * transcript yet to match against (e.g. first-year enrollment before any
   * exam), falls back to the first available track rather than failing —
   * best effort over no data.
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

  /** Counts how many of the percorso's activities match the student's known exam codes. */
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
   * Parses the cohort year out of a Cineca date string in
   * {@code DD/MM/YYYY HH24:MI:SS} format (e.g. {@code "06/09/2023 00:00:00"}).
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
   * Flattens the nested {@code percorsi -> anni -> insegnamenti -> attivita}
   * structure, pairing each activity with the academic year of its
   * containing bucket. The bucket-level year is authoritative because some
   * universities (observed: Molise) leave the activity's own
   * {@code annoCorso} field entirely unpopulated, while others (observed:
   * Chieti) populate it redundantly and consistently with the bucket.
   * Non-numeric buckets (e.g. Molise's {@code "altreAttivita"} — shared
   * electives cross-listed from other degree programs, not part of this
   * student's own curriculum) are skipped entirely.
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
   * Parses {@code anno} into a positive curriculum year, or {@code null}
   * for non-numeric buckets such as Molise's {@code "altreAttivita"}.
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

  /** Pairs a raw plan entry with the academic year of its containing bucket. */
  private record FlatEntry(CinecaCoursePlanEntry entry, int bucketYear) {
  }

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

  private boolean isMeaningful(String raw) {
    String stripped = stripHtml(raw);
    return stripped != null && !stripped.isBlank()
        && !stripped.toUpperCase().contains("INFORMAZIONE INSERITA NEL PROGRAMMA DEL CORSO INTEGRATO");
  }

  /**
   * Strips the HTML markup ({@code <p>}, {@code <span>}, inline styles) that
   * the Course Catalogue wraps every text field in, leaving plain text.
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
   * Bundle of everything needed to call the Course Catalogue for a specific
   * student: which university, which cohort, the Course Catalogue's own
   * course code, and the curriculum track(s) relevant to this student.
   */
  private record CourseCatalogueContext(
      String catalogueBaseUrl, int cohortYear, String courseCode,
      List<CinecaPercorso> percorsi) {
  }
}
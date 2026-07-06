package org.ohmyopensource.ohmyuniversity.core.service.coursecatalogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * upstream in {@link CourseCatalogueClient}.
 */
@Service
public class CourseCatalogueService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(CourseCatalogueService.class);

  private final CourseCatalogueClient catalogueClient;
  private final CinecaProfileClient profileClient;
  private final UniversityRegistry universityRegistryRef;

  // ============ Constructor ============

  public CourseCatalogueService(
      CourseCatalogueClient catalogueClient,
      CinecaProfileClient profileClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.catalogueClient = catalogueClient;
    this.profileClient = profileClient;
    this.universityRegistryRef = universityRegistry;
  }

  // ============ Class Methods ============

  /**
   * Returns the full multi-year course plan for the authenticated student's
   * degree course and cohort, flattened across years.
   *
   * @param principal authenticated OhMyU principal
   * @return the course plan; empty if the course could not be resolved in the Course Catalogue
   */
  public CoursePlanResponse getCoursePlan(OmuPrincipal principal) {
    CourseCatalogueContext ctx = resolveContext(principal);
    List<CinecaCoursePlanEntry> entries =
        ctx == null ? List.of() : flattenActivities(ctx.detail().getPercorsi());

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

    List<CinecaCoursePlanEntry> entries = flattenActivities(ctx.detail().getPercorsi());
    CinecaCoursePlanEntry entry = entries.stream()
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
          ctx.detail().getCod(),
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
   * Resolves the university/cohort/course context needed for every Course
   * Catalogue call: the catalogue base URL, the cohort year (parsed from the
   * student's immatricolazione date) and the resolved Course Catalogue
   * course entry (own course code + full multi-year plan).
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

    return new CourseCatalogueContext(catalogueBaseUrl, cohortYear, detail.get());
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

  /** Flattens the nested {@code percorsi -> anni -> insegnamenti -> attivita} structure. */
  private List<CinecaCoursePlanEntry> flattenActivities(List<CinecaPercorso> percorsi) {
    List<CinecaCoursePlanEntry> flat = new ArrayList<>();
    if (percorsi == null) {
      return flat;
    }
    for (CinecaPercorso percorso : percorsi) {
      if (percorso.getAnni() == null) {
        continue;
      }
      for (CinecaAnnoOfferta anno : percorso.getAnni()) {
        if (anno.getInsegnamenti() == null) {
          continue;
        }
        for (CinecaInsegnamentoGroup group : anno.getInsegnamenti()) {
          if (group.getAttivita() != null) {
            flat.addAll(group.getAttivita());
          }
        }
      }
    }
    return flat;
  }

  private CoursePlanExam toCoursePlanExam(CinecaCoursePlanEntry entry) {
    CoursePlanExam exam = new CoursePlanExam();
    exam.setAdCod(entry.getAdCod());
    exam.setName(entry.getDesIt());
    exam.setCfu(entry.getCrediti());
    exam.setAcademicYear(entry.getAnnoCorso());
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
   * student: which university, which cohort, and the already-resolved
   * course entry (own course code + full plan).
   */
  private record CourseCatalogueContext(
      String catalogueBaseUrl, int cohortYear, CinecaCourseCatalogueDetail detail) {
  }
}
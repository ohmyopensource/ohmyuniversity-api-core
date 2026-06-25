package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaInternshipsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaInternshipsClient.CinecaInternshipApplication;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InternshipApplicationResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.InternshipApplicationResponse.InternshipApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for internship-related operations against Cineca ESSE3.
 *
 * <p>Covers {@code tirocini-service-v1}:
 * <ul>
 *   <li>internship applications (domande di tirocinio)</li>
 * </ul>
 *
 * <p>All data is fetched in real-time from Cineca; nothing is persisted locally.
 */
@Service
public class InternshipsService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(InternshipsService.class);

  private final CinecaInternshipsClient internshipsClient;

  // ============ Constructor ============

  public InternshipsService(
      CinecaInternshipsClient internshipsClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.internshipsClient = internshipsClient;
  }

  // ============ Public Methods ============

  /**
   * Retrieves all internship applications for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return internship applications response
   */
  public InternshipApplicationResponse getApplications(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaInternshipApplication> applications =
        internshipsClient.getApplications(baseUrl, jwt, principal.stuId());

    log.debug("InternshipsService: fetched {} applications for stuId={}",
        applications.size(), principal.stuId());

    InternshipApplicationResponse response = new InternshipApplicationResponse();
    response.setApplications(applications.stream().map(this::toApplication).toList());
    return response;
  }

  // ============ Mappers ============

  private InternshipApplication toApplication(CinecaInternshipApplication a) {
    InternshipApplication app = new InternshipApplication();
    app.setDomTiroId(a.getDomTiroId());
    app.setDomTiroPrg(a.getDomTiroPrg());
    app.setAcademicYear(a.getAaId());
    app.setStatusCode(a.getStatoDomTiroCod());
    app.setStatusDescription(a.getStatoDomTiroDes());
    app.setInternshipTypeCode(a.getTipoTirocCod());
    app.setInternshipTypeDescription(a.getTipoTirocDes());
    app.setOrganizationId(a.getEnteId());
    app.setOrganizationName(a.getEnteDes());
    app.setOpportunityTitle(a.getOppTitolo());
    app.setOpportunityDescription(a.getOppDes());
    app.setStartDate(a.getStartDate());
    app.setDurationMonths(a.getDurationMonths());
    app.setCfuRecognitionEnabled(a.getAbilRicCfu());
    return app;
  }
}
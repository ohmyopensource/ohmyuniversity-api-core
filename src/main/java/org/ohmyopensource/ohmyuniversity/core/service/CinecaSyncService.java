package org.ohmyopensource.ohmyuniversity.core.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CinecaSyncState;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CinecaSyncState.EventType;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.CinecaSyncStateRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.KafkaEventPublisher;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CampusAssignmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CourseEditionDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.EnrollmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.TeachingAssignmentDiscoveredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for syncing Cineca libretto data and publishing Kafka integration events when
 * new facts are discovered.
 *
 * <p>This service implements the per-user sync model: each time a student
 * logs in, their transcript is fetched from Cineca and compared against the local
 * {@code cineca_sync_state} table. If a fact (course edition, enrollment, teaching assignment,
 * campus assignment) has not been notified yet, the corresponding Kafka event is published and the
 * state is recorded.
 * <p>
 * /**
 * <p>Sync constraints:</p>
 * <ul>
 *   <li>Only works with per-user Cineca JWT (no UTENTE_TECNICO available).</li>
 *   <li> Course edition event must be published before enrollment and teaching assignment events
 *     for the same channel (ordering constraint required by the chat consumer). </li>
 *   <li> Campus assignment events use {@code universityId} as a proxy for {@code campusId}
 *     (applicable for single-campus universities such as UNIMOL). </li>
 * </ul>
 *
 * <p>The sync runs asynchronously to avoid blocking the login response.
 * Errors are logged but not rethrown.
 *
 * <p>Note: this service receives Cineca credentials as explicit parameters from
 * {@code AuthService} rather than resolving them from a principal, because it runs after login
 * before a full OhMyU session is established. It therefore does not extend
 * {@code AbstractEsse3Service}.
 */
@Service
public class CinecaSyncService {

  private static final Logger log = LoggerFactory.getLogger(CinecaSyncService.class);

  /**
   * Sentinel adsceId used for campus assignment events, which are not tied to a specific course
   * activity but to the user/university pair.
   */
  private static final long CAMPUS_ASSIGNMENT_ADSCE_SENTINEL = -1L;

  private final CinecaCareerClient careerClient;
  private final CinecaSyncStateRepository syncStateRepository;
  private final OmuUserRepository userRepository;
  private final KafkaEventPublisher kafkaEventPublisher;

  // ============ Constructor ============

  /**
   * Constructs the sync service with all required dependencies.
   *
   * @param careerClient        client for Cineca transcript REST calls
   * @param syncStateRepository repository for deduplication state
   * @param userRepository      repository for OmuUser lookup
   * @param kafkaEventPublisher publisher for Kafka integration events
   */
  public CinecaSyncService(
      CinecaCareerClient careerClient,
      CinecaSyncStateRepository syncStateRepository,
      OmuUserRepository userRepository,
      KafkaEventPublisher kafkaEventPublisher) {
    this.careerClient = careerClient;
    this.syncStateRepository = syncStateRepository;
    this.userRepository = userRepository;
    this.kafkaEventPublisher = kafkaEventPublisher;
  }

  // ============ Class Methods ============

  /**
   * Triggers an asynchronous Cineca sync for a student after login.
   *
   * <p>Fetches the student's transcript from Cineca and compares it against
   * the local sync state. New facts are published as Kafka events and recorded.
   *
   * <p>Publishing order per course edition:
   * <ol>
   *   <li>{@code course-edition.discovered} — must arrive first</li>
   *   <li>{@code enrollment.discovered} — student added to channel</li>
   *   <li>{@code teaching-assignment.discovered} — professor added to channel</li>
   * </ol>
   *
   * @param omuUserId     the OhMyU user UUID string
   * @param universityId  the university identifier (e.g. "UNIMOL")
   * @param cinecaJwt     the Cineca Bearer JWT for this user
   * @param matId         the student's Cineca matricola ID
   * @param cinecaBaseUrl base URL of the Cineca ESSE3 API
   * @param academicYear  the current academic year (e.g. "2026")
   */
  @Async
  @Transactional
  public void syncAfterLogin(
      String omuUserId,
      String universityId,
      String cinecaJwt,
      Long matId,
      String cinecaBaseUrl,
      String academicYear) {

    String safeUserId = omuUserId.replaceAll("[\r\n]", "");
    String safeUniId = universityId.replaceAll("[\r\n]", "");

    log.debug("CinecaSyncService: starting sync for user={} university={}", safeUserId, safeUniId);

    OmuUser user = userRepository.findById(UUID.fromString(omuUserId)).orElse(null);
    if (user == null) {
      log.warn("CinecaSyncService: user not found, aborting sync for omuUserId={}", safeUserId);
      return;
    }

    try {
      syncTranscript(user, universityId, cinecaJwt, matId, cinecaBaseUrl, academicYear);
      syncCampusAssignment(user, universityId);
    } catch (Exception e) {
      log.error("CinecaSyncService: sync failed for user={} university={}: {}",
          safeUserId, safeUniId, e.getMessage());
    }
  }

  /**
   * Fetches the student's transcript from Cineca and publishes events for newly discovered course
   * editions, enrollments, and teaching assignments.
   *
   * @param user          the OhMyU user entity
   * @param universityId  the university identifier
   * @param cinecaJwt     the Cineca Bearer JWT
   * @param matId         the student's Cineca matricola ID
   * @param cinecaBaseUrl base URL of the Cineca ESSE3 API
   * @param academicYear  the current academic year string (e.g. "2026")
   */
  private void syncTranscript(
      OmuUser user,
      String universityId,
      String cinecaJwt,
      Long matId,
      String cinecaBaseUrl,
      String academicYear) {

    List<CinecaTranscriptRow> rows = careerClient.getTranscript(cinecaBaseUrl, cinecaJwt, matId);

    if (rows.isEmpty()) {
      log.debug("CinecaSyncService: empty transcript for user={}", user.getId());
      return;
    }

    for (CinecaTranscriptRow row : rows) {
      if (row.getAdsceId() == null) {
        continue;
      }

      String semester = (row.getAnnoCorso() != null && row.getAnnoCorso() % 2 == 0) ? "2" : "1";

      String externalChannelId = buildExternalChannelId(
          row.getAdCod(), universityId, academicYear, semester);

      publishIfNew(user, universityId, row.getAdsceId(), EventType.COURSE_EDITION,
          externalChannelId, () ->
              kafkaEventPublisher.publishCourseEditionDiscovered(
                  new CourseEditionDiscoveredEvent(
                      externalChannelId,
                      buildChannelName(row.getAdDes(), universityId, academicYear, semester),
                      row.getAdsceId().toString(),
                      academicYear,
                      semester
                  )
              ));

      publishIfNew(user, universityId, row.getAdsceId(), EventType.ENROLLMENT,
          null, () ->
              kafkaEventPublisher.publishEnrollmentDiscovered(
                  new EnrollmentDiscoveredEvent(
                      user.getId().toString(),
                      externalChannelId
                  )
              ));

      if (row.getEsito() == null && isDocenteTitolare(row)) {
        publishIfNew(user, universityId, row.getAdsceId(), EventType.TEACHING_ASSIGNMENT,
            null, () ->
                kafkaEventPublisher.publishTeachingAssignmentDiscovered(
                    new TeachingAssignmentDiscoveredEvent(
                        user.getId().toString(),
                        externalChannelId
                    )
                ));
      }
    }

    log.info("CinecaSyncService: transcript sync completed for user={} university={} rows={}",
        user.getId(), universityId.replaceAll("[\r\n]", ""), rows.size());
  }

  /**
   * Publishes a campus assignment event if not already notified.
   *
   * <p>Uses {@code universityId} as a proxy for {@code campusId} since Cineca does not
   * expose explicit campus identifiers for single-campus universities (e.g. UNIMOL).
   *
   * @param user         the OhMyU user entity
   * @param universityId the university identifier (used as campusId proxy)
   */
  private void syncCampusAssignment(OmuUser user, String universityId) {
    publishIfNew(user, universityId, CAMPUS_ASSIGNMENT_ADSCE_SENTINEL,
        EventType.CAMPUS_ASSIGNMENT, null, () ->
            kafkaEventPublisher.publishCampusAssignmentDiscovered(
                new CampusAssignmentDiscoveredEvent(
                    user.getId().toString(),
                    universityId,
                    universityId
                )
            ));
  }

  /**
   * Publishes a Kafka event only if the corresponding sync state does not exist yet. On first
   * publish, saves the sync state to prevent future duplicates.
   *
   * @param user              the OhMyU user entity
   * @param universityId      the university identifier
   * @param adsceId           the Cineca activity identifier
   * @param eventType         the type of event to check/publish
   * @param externalChannelId the channel ID to store in state (nullable)
   * @param publishAction     the action to execute if this is a new event
   */
  private void publishIfNew(
      OmuUser user,
      String universityId,
      Long adsceId,
      EventType eventType,
      String externalChannelId,
      Runnable publishAction) {

    boolean alreadyNotified = syncStateRepository
        .existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
            user.getId(), universityId, adsceId, eventType);

    if (alreadyNotified) {
      log.debug("CinecaSyncService: skipping already notified event type={} adsceId={}",
          eventType, adsceId);
      return;
    }

    publishAction.run();

    CinecaSyncState state = new CinecaSyncState();
    state.setUser(user);
    state.setUniversityId(universityId);
    state.setAdsceId(adsceId);
    state.setEventType(eventType);
    state.setExternalChannelId(externalChannelId);
    syncStateRepository.save(state);

    log.debug("CinecaSyncService: published and recorded event type={} adsceId={}",
        eventType, adsceId);
  }

  /**
   * Checks if a transcript row indicates a titular professor assignment.
   *
   * <p>Note: best-effort check based on available transcript data.
   * A more accurate check would require the Cineca docenti endpoint (UTENTE_TECNICO only).
   *
   * @param row the transcript row to evaluate
   * @return {@code true} if the row indicates a titular professor assignment
   */
  private boolean isDocenteTitolare(CinecaTranscriptRow row) {
    // @TODO: to check titolareFlg when Cineca exposes it for STUDENTE role
    return false;
  }

  /**
   * Builds a deterministic external channel ID from course and context identifiers.
   *
   * <p>Format: {@code {course-slug}-{university-slug}-{year}-{semester}}
   * Example: {@code inf0001-unimol-2026-1}
   *
   * @param adCod        Cineca course code (used as slug base)
   * @param universityId university identifier
   * @param academicYear academic year string
   * @param semester     semester string
   * @return deterministic channel ID safe for use as a Kafka key
   */
  private String buildExternalChannelId(
      String adCod,
      String universityId,
      String academicYear,
      String semester) {
    String courseSlug = toSlug(adCod != null ? adCod : "unknown");
    String uniSlug = toSlug(universityId);
    return courseSlug + "-" + uniSlug + "-" + academicYear + "-" + semester;
  }

  /**
   * Builds a human-readable channel name for the chat service.
   *
   * <p>Format: {@code {course description} — {university} — {year}/{semester}}
   * Example: {@code Analisi I — UNIMOL — 2026/1}
   *
   * @param adDes        course description from Cineca
   * @param universityId university identifier
   * @param academicYear academic year string
   * @param semester     semester string
   * @return human-readable channel name
   */
  private String buildChannelName(
      String adDes,
      String universityId,
      String academicYear,
      String semester) {
    String desc = adDes != null ? adDes : "Corso";
    return desc + " — " + universityId.toUpperCase(Locale.ROOT)
        + " — " + academicYear + "/" + semester;
  }

  /**
   * Converts a string to a URL-safe slug (lowercase, ASCII, hyphens only).
   *
   * @param input the raw string to slugify
   * @return the slugified string
   */
  private String toSlug(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized
        .replaceAll("[^\\p{ASCII}]", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(?:^-)|(?:-$)", "");
  }
}
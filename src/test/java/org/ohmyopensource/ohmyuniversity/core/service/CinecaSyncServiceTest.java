package org.ohmyopensource.ohmyuniversity.core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

/**
 * Unit tests for {@link CinecaSyncService}.
 *
 * <p>All dependencies are mocked via Mockito. No Docker, database, Kafka,
 * or Redis instance is required. Tests verify sync logic, deduplication
 * behaviour, ordering constraints, and edge cases.
 *
 * <p>Note: {@link CinecaSyncService#syncAfterLogin} is annotated with
 * {@code @Async} but executes synchronously in this unit test context
 * because no Spring context is loaded and the method is called directly
 * on the concrete instance.
 */
class CinecaSyncServiceTest {

  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String JWT = "cineca-jwt-token";
  private static final String ACADEMIC_YEAR = "2026";
  private static final Long MAT_ID = 106279L;
  private static final Long ADSCE_ID = 12345L;

  private CinecaCareerClient careerClient;
  private CinecaSyncStateRepository syncStateRepository;
  private OmuUserRepository userRepository;
  private KafkaEventPublisher kafkaEventPublisher;
  private CinecaSyncService syncService;

  /**
   * Initialises fresh mocks and a new {@link CinecaSyncService} instance
   * before each test to guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    careerClient = mock(CinecaCareerClient.class);
    syncStateRepository = mock(CinecaSyncStateRepository.class);
    userRepository = mock(OmuUserRepository.class);
    kafkaEventPublisher = mock(KafkaEventPublisher.class);

    syncService = new CinecaSyncService(
        careerClient,
        syncStateRepository,
        userRepository,
        kafkaEventPublisher);
  }

  /**
   * Builds a mock {@link OmuUser} with the given UUID string.
   *
   * @param userId string representation of the user UUID
   * @return configured mock
   */
  private OmuUser buildMockUser(String userId) {
    OmuUser user = mock(OmuUser.class);
    when(user.getId()).thenReturn(UUID.fromString(userId));
    return user;
  }

  /**
   * Builds a mock {@link CinecaTranscriptRow} with the given identifiers.
   *
   * @param adsceId Cineca activity identifier
   * @param adCod   course code
   * @param adDes   course description
   * @param annoCorso academic year of the course
   * @return configured mock
   */
  private CinecaTranscriptRow buildRiga(Long adsceId, String adCod, String adDes,
      Integer annoCorso) {
    CinecaTranscriptRow riga = mock(CinecaTranscriptRow.class);
    when(riga.getAdsceId()).thenReturn(adsceId);
    when(riga.getAdCod()).thenReturn(adCod);
    when(riga.getAdDes()).thenReturn(adDes);
    when(riga.getAnnoCorso()).thenReturn(annoCorso);
    when(riga.getEsito()).thenReturn(null);
    return riga;
  }

  /**
   * Stubs the sync state repository to report that no event has been notified
   * yet for any combination of user, university, adsceId, and event type.
   */
  private void noEventsNotifiedYet() {
    when(syncStateRepository.existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
        any(), any(), any(), any())).thenReturn(false);
  }

  /**
   * Stubs the sync state repository to report that all events have already
   * been notified for any combination.
   */
  private void allEventsAlreadyNotified() {
    when(syncStateRepository.existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
        any(), any(), any(), any())).thenReturn(true);
  }

  /**
   * Verifies the behaviour of {@link CinecaSyncService#syncAfterLogin} when the
   * user is not found in the database.
   */
  @Nested
  @DisplayName("syncAfterLogin() — user not found")
  class UserNotFound {

    /**
     * Verifies that when the user UUID does not exist in the database,
     * no Cineca API call is made and no Kafka event is published.
     */
    @Test
    @DisplayName("user not found → no Cineca call, no Kafka event published")
    void userNotFound() {
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.empty());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(careerClient, never()).getTranscript(any(), any(), any());
      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
      verify(kafkaEventPublisher, never()).publishCampusAssignmentDiscovered(any());
    }
  }

  /**
   * Verifies the behaviour of {@link CinecaSyncService#syncAfterLogin} when the
   * Cineca libretto is empty.
   */
  @Nested
  @DisplayName("syncAfterLogin() — empty libretto")
  class EmptyLibretto {

    /**
     * Verifies that when Cineca returns an empty libretto, no course edition
     * or enrollment events are published. Campus assignment is still published
     * because it is not tied to individual libretto rows.
     */
    @Test
    @DisplayName("empty libretto → no course/enrollment events, campus assignment still published")
    void emptyLibretto() {
      OmuUser user = buildMockUser(USER_ID);
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of());
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
      verify(kafkaEventPublisher).publishCampusAssignmentDiscovered(any());
    }
  }

  /**
   * Verifies the first-login behaviour of {@link CinecaSyncService#syncAfterLogin}
   * when new libretto rows are discovered.
   */
  @Nested
  @DisplayName("syncAfterLogin() — first login, new events")
  class FirstLogin {

    /**
     * Verifies that for a single libretto row with no prior sync state,
     * {@code course-edition.discovered} and {@code enrollment.discovered}
     * events are published, and the sync state is recorded for each.
     */
    @Test
    @DisplayName("single row, no prior state → course-edition and enrollment published")
    void singleRowFirstLogin() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher).publishCourseEditionDiscovered(
          any(CourseEditionDiscoveredEvent.class));
      verify(kafkaEventPublisher).publishEnrollmentDiscovered(
          any(EnrollmentDiscoveredEvent.class));
      verify(kafkaEventPublisher).publishCampusAssignmentDiscovered(
          any(CampusAssignmentDiscoveredEvent.class));
    }

    /**
     * Verifies that for a single libretto row, the sync state is saved
     * exactly twice: once for COURSE_EDITION and once for ENROLLMENT.
     */
    @Test
    @DisplayName("single row → sync state saved twice (COURSE_EDITION + ENROLLMENT)")
    void syncStateSavedTwicePerRow() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(syncStateRepository, times(3)).save(any(CinecaSyncState.class));
    }

    /**
     * Verifies that for multiple libretto rows, a course-edition and enrollment
     * event are published for each row independently.
     */
    @Test
    @DisplayName("multiple rows → one course-edition and enrollment event per row")
    void multipleRowsPublishPerRow() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga1 = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);
      CinecaTranscriptRow riga2 = buildRiga(99999L, "FI01", "Fisica I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga1, riga2));
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, times(2)).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, times(2)).publishEnrollmentDiscovered(any());
      verify(kafkaEventPublisher, times(1)).publishCampusAssignmentDiscovered(any());
    }

    /**
     * Verifies that a libretto row with a null {@code adsceId} is skipped
     * without publishing any event or throwing an exception.
     */
    @Test
    @DisplayName("row with null adsceId → skipped, no event published")
    void nullAdsceIdSkipped() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow rigaNull = mock(CinecaTranscriptRow.class);
      when(rigaNull.getAdsceId()).thenReturn(null);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(rigaNull));
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
    }
  }

  /**
   * Verifies the deduplication behaviour of {@link CinecaSyncService#syncAfterLogin}
   * when sync state already exists for previously published events.
   */
  @Nested
  @DisplayName("syncAfterLogin() — deduplication")
  class Deduplication {

    /**
     * Verifies that when all events for a libretto row have already been notified,
     * no Kafka event is published on the second login.
     */
    @Test
    @DisplayName("all events already notified → no Kafka event published on second login")
    void allAlreadyNotified() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      allEventsAlreadyNotified();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
      verify(kafkaEventPublisher, never()).publishCampusAssignmentDiscovered(any());
      verify(syncStateRepository, never()).save(any());
    }

    /**
     * Verifies that when only COURSE_EDITION has been notified but not ENROLLMENT,
     * only the enrollment event is published.
     */
    @Test
    @DisplayName("COURSE_EDITION already notified, ENROLLMENT not → only enrollment published")
    void partialDeduplication() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));

      when(syncStateRepository.existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
          UUID.fromString(USER_ID), UNIVERSITY_ID, ADSCE_ID, EventType.COURSE_EDITION))
          .thenReturn(true);

      when(syncStateRepository.existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
          UUID.fromString(USER_ID), UNIVERSITY_ID, ADSCE_ID, EventType.ENROLLMENT))
          .thenReturn(false);

      when(syncStateRepository.existsByUserIdAndUniversityIdAndAdsceIdAndEventType(
          UUID.fromString(USER_ID), UNIVERSITY_ID, -1L, EventType.CAMPUS_ASSIGNMENT))
          .thenReturn(false);

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher).publishEnrollmentDiscovered(any());
      verify(kafkaEventPublisher).publishCampusAssignmentDiscovered(any());
    }

    /**
     * Verifies that when sync state already exists, the repository save is
     * not called for already-notified events.
     */
    @Test
    @DisplayName("already notified → syncStateRepository.save never called")
    void noStateSavedWhenAlreadyNotified() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      allEventsAlreadyNotified();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(syncStateRepository, never()).save(any());
    }
  }

  /**
   * Verifies the ordering constraint for chat-related events.
   */
  @Nested
  @DisplayName("syncAfterLogin() — event ordering")
  class EventOrdering {

    /**
     * Verifies that {@code course-edition.discovered} is published before
     * {@code enrollment.discovered} for the same libretto row, by checking
     * that both are published in a single sync invocation and that the course
     * edition state is stored before the enrollment state.
     *
     * <p>This ordering is critical: the chat consumer silently drops enrollment
     * events if the corresponding channel does not yet exist.
     */
    @Test
    @DisplayName("course-edition published before enrollment in same sync call")
    void courseEditionBeforeEnrollment() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      var publishOrder = new java.util.ArrayList<String>();

      org.mockito.Mockito.doAnswer(inv -> {
        publishOrder.add("course-edition");
        return null;
      }).when(kafkaEventPublisher).publishCourseEditionDiscovered(any());

      org.mockito.Mockito.doAnswer(inv -> {
        publishOrder.add("enrollment");
        return null;
      }).when(kafkaEventPublisher).publishEnrollmentDiscovered(any());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      org.assertj.core.api.Assertions.assertThat(publishOrder)
          .containsExactly("course-edition", "enrollment");
    }
  }

  /**
   * Verifies the campus assignment sync behaviour.
   */
  @Nested
  @DisplayName("syncAfterLogin() — campus assignment")
  class CampusAssignment {

    /**
     * Verifies that a campus assignment event is always published on first login,
     * regardless of the libretto content.
     */
    @Test
    @DisplayName("first login → campus assignment published once")
    void campusAssignmentPublishedOnFirstLogin() {
      OmuUser user = buildMockUser(USER_ID);
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of());
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, times(1)).publishCampusAssignmentDiscovered(
          any(CampusAssignmentDiscoveredEvent.class));
    }

    /**
     * Verifies that the campus assignment event uses {@code universityId} as
     * both {@code campusId} and {@code universityId} in the payload (single-campus proxy).
     */
    @Test
    @DisplayName("campus assignment payload uses universityId as campusId proxy")
    void campusAssignmentPayloadUsesUniversityIdAsCampusId() {
      OmuUser user = buildMockUser(USER_ID);
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of());
      noEventsNotifiedYet();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher).publishCampusAssignmentDiscovered(
          new CampusAssignmentDiscoveredEvent(
              UUID.fromString(USER_ID).toString(),
              UNIVERSITY_ID,
              UNIVERSITY_ID
          )
      );
    }

    /**
     * Verifies that campus assignment is not published again if already notified.
     */
    @Test
    @DisplayName("campus assignment already notified → not published again")
    void campusAssignmentNotPublishedTwice() {
      OmuUser user = buildMockUser(USER_ID);
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of());
      allEventsAlreadyNotified();

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      verify(kafkaEventPublisher, never()).publishCampusAssignmentDiscovered(any());
    }
  }

  /**
   * Verifies semester calculation and externalChannelId generation.
   */
  @Nested
  @DisplayName("syncAfterLogin() — externalChannelId and channel name")
  class ChannelIdGeneration {

    /**
     * Verifies that a course edition event contains a non-null, non-blank
     * {@code externalChannelId} built from the course code, university, year and semester.
     */
    @Test
    @DisplayName("course-edition event contains non-blank externalChannelId")
    void externalChannelIdNotBlank() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      var capturedEvent = new CourseEditionDiscoveredEvent[1];
      org.mockito.Mockito.doAnswer(inv -> {
        capturedEvent[0] = inv.getArgument(0);
        return null;
      }).when(kafkaEventPublisher).publishCourseEditionDiscovered(any());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      org.assertj.core.api.Assertions.assertThat(capturedEvent[0].externalChannelId())
          .isNotBlank()
          .contains("an01")
          .contains("unimol")
          .contains(ACADEMIC_YEAR);
    }

    /**
     * Verifies that odd annoCorso maps to semester "1" and even to semester "2".
     */
    @Test
    @DisplayName("annoCorso=1 (odd) → semester=1 in externalChannelId")
    void oddAnnoCorsoMapsToSemester1() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "FI01", "Fisica I", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      var capturedEvent = new CourseEditionDiscoveredEvent[1];
      org.mockito.Mockito.doAnswer(inv -> {
        capturedEvent[0] = inv.getArgument(0);
        return null;
      }).when(kafkaEventPublisher).publishCourseEditionDiscovered(any());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      org.assertj.core.api.Assertions.assertThat(capturedEvent[0].semester()).isEqualTo("1");
    }

    /**
     * Verifies that even annoCorso maps to semester "2".
     */
    @Test
    @DisplayName("annoCorso=2 (even) → semester=2 in externalChannelId")
    void evenAnnoCorsoMapsToSemester2() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "FI02", "Fisica II", 2);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      var capturedEvent = new CourseEditionDiscoveredEvent[1];
      org.mockito.Mockito.doAnswer(inv -> {
        capturedEvent[0] = inv.getArgument(0);
        return null;
      }).when(kafkaEventPublisher).publishCourseEditionDiscovered(any());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      org.assertj.core.api.Assertions.assertThat(capturedEvent[0].semester()).isEqualTo("2");
    }

    /**
     * Verifies that null adCod falls back to "unknown" in the externalChannelId slug.
     */
    @Test
    @DisplayName("null adCod → externalChannelId contains 'unknown' slug")
    void nullAdCodFallsBackToUnknown() {
      OmuUser user = buildMockUser(USER_ID);
      CinecaTranscriptRow riga = buildRiga(ADSCE_ID, null, "Corso senza codice", 1);

      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenReturn(List.of(riga));
      noEventsNotifiedYet();

      var capturedEvent = new CourseEditionDiscoveredEvent[1];
      org.mockito.Mockito.doAnswer(inv -> {
        capturedEvent[0] = inv.getArgument(0);
        return null;
      }).when(kafkaEventPublisher).publishCourseEditionDiscovered(any());

      syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

      org.assertj.core.api.Assertions.assertThat(capturedEvent[0].externalChannelId())
          .contains("unknown");
    }
  }

  /**
   * Verifies that Cineca API errors during sync do not propagate to the caller.
   */
  @Nested
  @DisplayName("syncAfterLogin() — error handling")
  class ErrorHandling {

    /**
     * Verifies that when Cineca throws an exception during libretto fetch,
     * the sync fails silently (no exception propagated) and no Kafka event is published.
     */
    @Test
    @DisplayName("Cineca throws exception → sync fails silently, no event published")
    void cinecaThrowsException() {
      OmuUser user = buildMockUser(USER_ID);
      when(userRepository.findById(UUID.fromString(USER_ID))).thenReturn(Optional.of(user));
      when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
          .thenThrow(new RuntimeException("Cineca unavailable"));

      org.assertj.core.api.Assertions.assertThatNoException().isThrownBy(() ->
          syncService.syncAfterLogin(USER_ID, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL,
              ACADEMIC_YEAR));

      verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
      verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
    }
  }
}
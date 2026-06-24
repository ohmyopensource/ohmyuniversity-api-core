package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.utility.TestcontainersConfiguration;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CinecaSyncState;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.CinecaSyncStateRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


/**
 * Integration tests for {@link CinecaSyncService} using real PostgreSQL and Kafka
 * containers via Testcontainers.
 *
 * <p>These tests verify the deduplication behaviour using the actual
 * {@code cineca_sync_state} table managed by Flyway. {@link KafkaEventPublisher}
 * and {@link CinecaCareerClient} are mocked to isolate the sync engine
 * from external dependencies.
 *
 * <p>These tests require Docker to be available and are conditionally
 * enabled via the {@code docker.available} system property.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@EnabledIfSystemProperty(named = "docker.available", matches = "true")
@DirtiesContext
class CinecaSyncServiceIntegrationTest {

  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String JWT = "cineca-jwt-token";
  private static final String ACADEMIC_YEAR = "2026";
  private static final Long MAT_ID = 106279L;
  private static final Long ADSCE_ID = 12345L;

  @Autowired
  private CinecaSyncService cinecaSyncService;

  @Autowired
  private OmuUserRepository userRepository;

  @Autowired
  private CinecaSyncStateRepository syncStateRepository;

  @MockitoBean
  private CinecaCareerClient careerClient;

  @MockitoBean
  private KafkaEventPublisher kafkaEventPublisher;

  private OmuUser savedUser;

  /**
   * Creates a fresh {@link OmuUser} in the real database before each test,
   * and clears the sync state table to ensure test isolation.
   */
  @BeforeEach
  void setUp() {
    syncStateRepository.deleteAll();

    OmuUser user = new OmuUser();
    user.setCodiceFiscale("TSTXXX00A00X00" + UUID.randomUUID().toString().substring(0, 2));
    savedUser = userRepository.save(user);
  }

  /**
   * Builds a mock {@link CinecaTranscriptRow} with the given identifiers.
   */
  private CinecaTranscriptRow buildRiga(Long adsceId, String adCod, String adDes,
      Integer annoCorso) {
    CinecaTranscriptRow riga = org.mockito.Mockito.mock(CinecaTranscriptRow.class);
    when(riga.getAdsceId()).thenReturn(adsceId);
    when(riga.getAdCod()).thenReturn(adCod);
    when(riga.getAdDes()).thenReturn(adDes);
    when(riga.getAnnoCorso()).thenReturn(annoCorso);
    when(riga.getEsito()).thenReturn(null);
    return riga;
  }

  /**
   * Verifies that on the first login, Kafka events are published and
   * sync state rows are persisted in the database.
   */
  @Test
  @DisplayName("first login → events published and sync state persisted in DB")
  void firstLoginPersistsSyncState() {
    CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);
    when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
        .thenReturn(List.of(riga));

    cinecaSyncService.syncAfterLogin(
        savedUser.getId().toString(), UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    List<CinecaSyncState> states = syncStateRepository.findAll();
    assertThat(states).hasSizeGreaterThanOrEqualTo(2);

    boolean hasCourseEdition = states.stream()
        .anyMatch(s -> s.getEventType() == CinecaSyncState.EventType.COURSE_EDITION
            && s.getAdsceId().equals(ADSCE_ID));
    boolean hasEnrollment = states.stream()
        .anyMatch(s -> s.getEventType() == CinecaSyncState.EventType.ENROLLMENT
            && s.getAdsceId().equals(ADSCE_ID));

    assertThat(hasCourseEdition).isTrue();
    assertThat(hasEnrollment).isTrue();

    verify(kafkaEventPublisher, atLeastOnce()).publishCourseEditionDiscovered(any());
    verify(kafkaEventPublisher, atLeastOnce()).publishEnrollmentDiscovered(any());
  }

  /**
   * Verifies that on the second login with the same libretto,
   * no new Kafka events are published (deduplication via DB).
   */
  @Test
  @DisplayName("second login with same libretto → no new events published (DB dedup)")
  void secondLoginDeduplication() {
    CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);
    when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
        .thenReturn(List.of(riga));

    String userId = savedUser.getId().toString();

    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    org.mockito.Mockito.clearInvocations(kafkaEventPublisher);

    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    verify(kafkaEventPublisher, never()).publishCourseEditionDiscovered(any());
    verify(kafkaEventPublisher, never()).publishEnrollmentDiscovered(any());
    verify(kafkaEventPublisher, never()).publishCampusAssignmentDiscovered(any());
  }

  /**
   * Verifies that a new libretto row added between logins triggers new events
   * while existing rows are deduplicated.
   */
  @Test
  @DisplayName("new row added between logins → only new row triggers events")
  void newRowBetweenLoginsTriggesEvents() {
    CinecaTranscriptRow riga1 = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);
    CinecaTranscriptRow riga2 = buildRiga(99999L, "FI01", "Fisica I", 1);

    String userId = savedUser.getId().toString();

    when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
        .thenReturn(List.of(riga1));
    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    org.mockito.Mockito.clearInvocations(kafkaEventPublisher);

    when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
        .thenReturn(List.of(riga1, riga2));
    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    verify(kafkaEventPublisher, org.mockito.Mockito.times(1))
        .publishCourseEditionDiscovered(any());
    verify(kafkaEventPublisher, org.mockito.Mockito.times(1))
        .publishEnrollmentDiscovered(any());
  }

  /**
   * Verifies that the unique constraint on {@code cineca_sync_state} prevents
   * duplicate rows even if sync is called concurrently or repeatedly.
   */
  @Test
  @DisplayName("sync state unique constraint enforced — no duplicate rows in DB")
  void uniqueConstraintEnforced() {
    CinecaTranscriptRow riga = buildRiga(ADSCE_ID, "AN01", "Analisi I", 1);
    when(careerClient.getTranscript(BASE_URL, JWT, MAT_ID))
        .thenReturn(List.of(riga));

    String userId = savedUser.getId().toString();

    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    long countBefore = syncStateRepository.count();

    cinecaSyncService.syncAfterLogin(userId, UNIVERSITY_ID, JWT, MAT_ID, BASE_URL, ACADEMIC_YEAR);

    long countAfter = syncStateRepository.count();
    assertThat(countAfter).isEqualTo(countBefore);
  }
}
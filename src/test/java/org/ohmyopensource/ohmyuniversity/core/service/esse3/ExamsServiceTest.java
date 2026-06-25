package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookableSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookingResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaExamSession;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaSurveyRow;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link ExamsService}.
 *
 * <p>All dependencies are replaced by Mockito mocks. Tests verify mapping logic,
 * active booking filtering, and session expiry propagation.
 */
class ExamsServiceTest {

  private CinecaExamsClient examsClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private ExamsService service;

  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final String CINECA_AUTH_TOKEN = "fakeAuthToken";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

  /**
   * Initialises fresh mocks and shared stubs before each test.
   */
  @BeforeEach
  void setUp() {
    examsClient = mock(CinecaExamsClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    service = new ExamsService(
        examsClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "TSTXXX00A00X000X", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));
    when(sessionStore.getCinecaAuthToken(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_AUTH_TOKEN));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies that service methods throw {@link CinecaAuthException} when JWT is absent.
   */
  @Nested
  @DisplayName("Session expiry")
  class SessionExpiry {

    /**
     * Verifies that {@link ExamsService#getBookableSessions} throws when JWT is absent.
     */
    @Test
    @DisplayName("getBookableSessions: throws CinecaAuthException when JWT expired")
    void getBookableSessions_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getBookableSessions(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that {@link ExamsService#getSurveys} throws when JWT is absent.
     */
    @Test
    @DisplayName("getSurveys: throws CinecaAuthException when JWT expired")
    void getSurveys_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getSurveys(principal))
          .isInstanceOf(CinecaAuthException.class);
    }
  }

  /**
   * Verifies {@link ExamsService#getSessions} field mapping.
   */
  @Nested
  @DisplayName("Sessions")
  class Sessions {

    /**
     * Verifies that exam session fields are mapped correctly.
     */
    @Test
    @DisplayName("maps session fields correctly")
    void mapsSessionFields() {
      CinecaExamSession session = mock(CinecaExamSession.class);
      when(session.getAppId()).thenReturn(1001L);
      when(session.getAdCod()).thenReturn("411114");
      when(session.getAdDes()).thenReturn("Architettura degli elaboratori");
      when(session.getStartDate()).thenReturn("15/07/2026 00:00:00");
      when(session.getBookable()).thenReturn(true);
      when(session.getBooked()).thenReturn(false);

      when(examsClient.getSessions(any(), any(), any(), any(), any(), any()))
          .thenReturn(List.of(session));

      var response = service.getSessions(principal, 10018L, 5001L);

      assertThat(response.getAppelli()).hasSize(1);
      assertThat(response.getAppelli().get(0).getAppId()).isEqualTo(1001L);
      assertThat(response.getAppelli().get(0).getAdCod()).isEqualTo("411114");
      assertThat(response.getAppelli().get(0).getPrenotabile()).isTrue();
    }

    /**
     * Verifies that an empty list from Cineca produces an empty response.
     */
    @Test
    @DisplayName("returns empty list when Cineca returns no sessions")
    void returnsEmptyList() {
      when(examsClient.getSessions(any(), any(), any(), any(), any(), any()))
          .thenReturn(List.of());

      var response = service.getSessions(principal, 10018L, 5001L);

      assertThat(response.getAppelli()).isEmpty();
    }
  }

  /**
   * Verifies {@link ExamsService#getBookableSessions} field mapping.
   */
  @Nested
  @DisplayName("Bookable sessions")
  class BookableSessions {

    /**
     * Verifies that bookable session fields are mapped correctly.
     */
    @Test
    @DisplayName("maps bookable session fields correctly")
    void mapsBookableSessionFields() {
      CinecaBookableSession session = mock(CinecaBookableSession.class);
      when(session.getAppId()).thenReturn(2001L);
      when(session.getAdCod()).thenReturn("411114");
      when(session.getAdDes()).thenReturn("Architettura degli elaboratori");
      when(session.getStartDate()).thenReturn("15/07/2026 00:00:00");
      when(session.getStatus()).thenReturn("I");
      when(session.getLecturer()).thenReturn("MARIO ROSSI");
      when(session.getRegisteredCount()).thenReturn(42);

      when(examsClient.getBookableSessions(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(session));

      var response = service.getBookableSessions(principal);

      assertThat(response.getAppelli()).hasSize(1);
      assertThat(response.getAppelli().get(0).getAppId()).isEqualTo(2001L);
      assertThat(response.getAppelli().get(0).getDocente()).isEqualTo("MARIO ROSSI");
      assertThat(response.getAppelli().get(0).getNumIscritti()).isEqualTo(42);
    }
  }

  /**
   * Verifies {@link ExamsService#getBookings} active booking filtering.
   */
  @Nested
  @DisplayName("Bookings filtering")
  class BookingsFiltering {

    /**
     * Verifies that past bookings are excluded from the active bookings response.
     */
    @Test
    @DisplayName("excludes past bookings")
    void excludesPastBookings() {
      CinecaBooking past = mock(CinecaBooking.class);
      when(past.getExamDateTime()).thenReturn("01/01/2020 09:00:00");
      when(past.getResult()).thenReturn(null);
      when(past.getAdsceId()).thenReturn(1L);

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(past));

      var response = service.getBookings(principal);

      assertThat(response.getPrenotazioni()).isEmpty();
    }

    /**
     * Verifies that passed bookings are excluded from the active bookings response.
     */
    @Test
    @DisplayName("excludes passed bookings")
    void excludesPassedBookings() {
      CinecaBookingResult result = mock(CinecaBookingResult.class);
      when(result.isPassed()).thenReturn(true);
      when(result.isWithdrawn()).thenReturn(false);

      CinecaBooking passed = mock(CinecaBooking.class);
      when(passed.getExamDateTime()).thenReturn("31/12/2099 09:00:00");
      when(passed.getResult()).thenReturn(result);
      when(passed.getAdsceId()).thenReturn(1L);

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(passed));

      var response = service.getBookings(principal);

      assertThat(response.getPrenotazioni()).isEmpty();
    }

    /**
     * Verifies that future bookings with no result are included.
     */
    @Test
    @DisplayName("includes future bookings with no result")
    void includesFutureBookings() {
      CinecaBooking future = mock(CinecaBooking.class);
      when(future.getExamDateTime()).thenReturn("31/12/2099 09:00:00");
      when(future.getResult()).thenReturn(null);
      when(future.getAdsceId()).thenReturn(1L);
      when(future.getApplistaId()).thenReturn(999L);
      when(future.getAdStuCod()).thenReturn("411114");
      when(future.getAdStuDes()).thenReturn("Architettura degli elaboratori");

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(future));

      var response = service.getBookings(principal);

      assertThat(response.getPrenotazioni()).hasSize(1);
      assertThat(response.getPrenotazioni().get(0).getApplistaId()).isEqualTo(999L);
    }
  }

  /**
   * Verifies {@link ExamsService#getSurveys} split into pending and completed.
   */
  @Nested
  @DisplayName("Surveys")
  class Surveys {

    /**
     * Verifies that pending and completed surveys are correctly split.
     */
    @Test
    @DisplayName("splits pending and completed surveys correctly")
    void splitsSurveysCorrectly() {
      CinecaSurveyRow pending = mock(CinecaSurveyRow.class);
      when(pending.getAdCod()).thenReturn("411114");
      when(pending.getAdDes()).thenReturn("Architettura");
      when(pending.getSurveyStatus()).thenReturn(3);

      CinecaSurveyRow completed = mock(CinecaSurveyRow.class);
      when(completed.getAdCod()).thenReturn("411115");
      when(completed.getAdDes()).thenReturn("Analisi");
      when(completed.getSurveyStatus()).thenReturn(1);

      when(examsClient.getSurveys(BASE_URL, CINECA_JWT, MAT_ID, "C"))
          .thenReturn(List.of(pending));
      when(examsClient.getSurveys(BASE_URL, CINECA_JWT, MAT_ID, "P"))
          .thenReturn(List.of(completed));

      var response = service.getSurveys(principal);

      assertThat(response.getDaCompilare()).hasSize(1);
      assertThat(response.getDaCompilare().get(0).getAdCod()).isEqualTo("411114");
      assertThat(response.getCompilati()).hasSize(1);
      assertThat(response.getCompilati().get(0).getAdCod()).isEqualTo("411115");
    }

    /**
     * Verifies that surveys with statoLink != 1 are excluded from completed.
     */
    @Test
    @DisplayName("excludes surveys with statoLink != 1 from completed")
    void excludesNonCompletedFromCompilati() {
      CinecaSurveyRow notFullyCompleted = mock(CinecaSurveyRow.class);
      when(notFullyCompleted.getSurveyStatus()).thenReturn(2);

      when(examsClient.getSurveys(BASE_URL, CINECA_JWT, MAT_ID, "C")).thenReturn(List.of());
      when(examsClient.getSurveys(BASE_URL, CINECA_JWT, MAT_ID, "P"))
          .thenReturn(List.of(notFullyCompleted));

      var response = service.getSurveys(principal);

      assertThat(response.getCompilati()).isEmpty();
    }
  }
}
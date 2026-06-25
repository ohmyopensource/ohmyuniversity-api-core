package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaLegacyBookingResult;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link ExamsService#getLegacyBookings}.
 */
class ExamsServiceExtraTest {

  private CinecaExamsClient examsClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private ExamsService service;

  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final String CINECA_AUTH = "fakeAuthToken";
  private static final String USERNAME = "a.delmuto";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

  @BeforeEach
  void setUp() {
    examsClient = mock(CinecaExamsClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    service = new ExamsService(
        examsClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "DLMLSS04E14L113Q", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));
    when(sessionStore.getCinecaAuthToken(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_AUTH));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));

    UniversityConnection conn = mock(UniversityConnection.class);
    when(conn.getUniversityId()).thenReturn(UNIVERSITY_ID);
    when(conn.getUsernameCineca()).thenReturn(USERNAME);
    when(connectionRepository.findByUserId(UUID.fromString(OMU_USER_ID)))
        .thenReturn(List.of(conn));
  }

  /**
   * Verifies {@link ExamsService#getLegacyBookings} field mapping.
   */
  @Nested
  @DisplayName("getLegacyBookings")
  class GetLegacyBookings {

    /**
     * Verifies that legacy booking fields are mapped correctly.
     */
    @Test
    @DisplayName("maps legacy booking fields correctly")
    void mapsLegacyBookingFields() {
      CinecaLegacyBooking booking = mock(CinecaLegacyBooking.class);
      when(booking.getApplistaId()).thenReturn(1228134L);
      when(booking.getAdStuCod()).thenReturn("429008");
      when(booking.getAdStuDes()).thenReturn("Basi di Dati");
      when(booking.getAdsceId()).thenReturn(2884996L);
      when(booking.getExamDate()).thenReturn("15/06/2025");
      when(booking.getInsertDate()).thenReturn("01/05/2025 10:30:00");
      when(booking.getResult()).thenReturn(null);
      when(booking.getPublicationId()).thenReturn(null);

      when(examsClient.getLegacyBookings(BASE_URL, USERNAME, "password", CINECA_AUTH, MAT_ID))
          .thenReturn(List.of(booking));

      var response = service.getLegacyBookings(principal, "password");

      assertThat(response.getPrenotazioni()).hasSize(1);
      assertThat(response.getPrenotazioni().get(0).getApplistaId()).isEqualTo(1228134L);
      assertThat(response.getPrenotazioni().get(0).getAdStuCod()).isEqualTo("429008");
      assertThat(response.getPrenotazioni().get(0).getEsito()).isNull();
      assertThat(response.getPrenotazioni().get(0).getEsitoPubblicato()).isFalse();
    }

    /**
     * Verifies that legacy booking result is mapped into esito correctly.
     */
    @Test
    @DisplayName("maps legacy booking result into esito")
    void mapsResultIntoEsito() {
      CinecaLegacyBookingResult result = mock(CinecaLegacyBookingResult.class);
      when(result.getGrade()).thenReturn(28);
      when(result.isPassed()).thenReturn(true);
      when(result.isAbsent()).thenReturn(false);
      when(result.isWithdrawn()).thenReturn(false);
      when(result.getJudgmentCod()).thenReturn("IDON");
      when(result.getJudgmentDes()).thenReturn("Idoneo");

      CinecaLegacyBooking booking = mock(CinecaLegacyBooking.class);
      when(booking.getApplistaId()).thenReturn(999L);
      when(booking.getAdStuCod()).thenReturn("411114");
      when(booking.getAdStuDes()).thenReturn("Architettura");
      when(booking.getResult()).thenReturn(result);
      when(booking.getPublicationId()).thenReturn(12345L);

      when(examsClient.getLegacyBookings(BASE_URL, USERNAME, "password", CINECA_AUTH, MAT_ID))
          .thenReturn(List.of(booking));

      var response = service.getLegacyBookings(principal, "password");
      var prenotazione = response.getPrenotazioni().get(0);

      assertThat(prenotazione.getEsito()).isNotNull();
      assertThat(prenotazione.getEsito().getVotoEsa()).isEqualTo(28);
      assertThat(prenotazione.getEsito().getSuperato()).isTrue();
      assertThat(prenotazione.getEsito().getTipoGiudCod()).isEqualTo("IDON");
      assertThat(prenotazione.getEsitoPubblicato()).isTrue();
    }

    /**
     * Verifies that an empty list is handled correctly.
     */
    @Test
    @DisplayName("returns empty list when no legacy bookings")
    void returnsEmptyList() {
      when(examsClient.getLegacyBookings(BASE_URL, USERNAME, "password", CINECA_AUTH, MAT_ID))
          .thenReturn(List.of());

      var response = service.getLegacyBookings(principal, "password");

      assertThat(response.getPrenotazioni()).isEmpty();
    }
  }
}
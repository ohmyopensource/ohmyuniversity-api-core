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
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaCharge;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaFeeStatus;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;

/**
 * Unit tests for {@link FeesService} and {@link ProfileService} (badge/fees mapping).
 *
 * <p>All dependencies are replaced by Mockito mocks.
 */
class FeesAndProfileServiceTest {

  private CinecaFeesClient feesClient;
  private CinecaProfileClient profileClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private FeesService feesService;
  private ProfileService profileService;

  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

  /**
   * Initialises fresh mocks and shared stubs before each test.
   */
  @BeforeEach
  void setUp() {
    feesClient = mock(CinecaFeesClient.class);
    profileClient = mock(CinecaProfileClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    feesService = new FeesService(
        feesClient, sessionStore, universityRegistry, connectionRepository);
    profileService = new ProfileService(
        profileClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "TSTXXX00A00X000X", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies that {@link FeesService#getStatus} correctly assembles the response
   * from the semaforo status and addebiti list.
   */
  @Nested
  @DisplayName("Fees status")
  class FeesStatus {

    /**
     * Verifies semaforo VERDE is mapped correctly with empty lists.
     */
    @Test
    @DisplayName("maps semaforo VERDE correctly")
    void mapsSemaforoVerde() {
      CinecaFeeStatus status = mock(CinecaFeeStatus.class);
      when(status.getStatus()).thenReturn("VERDE");
      when(status.getAmountDue()).thenReturn("0.0");
      when(status.getOverdueItems()).thenReturn(List.of());
      when(status.getDueItems()).thenReturn(List.of());

      when(feesClient.getFeeStatus(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(status);
      when(feesClient.getCharges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      TasseResponse response = feesService.getStatus(principal);

      assertThat(response.getSemaforo()).isEqualTo("VERDE");
      assertThat(response.getImportoDovuto()).isEqualTo("0.0");
      assertThat(response.getTasseScadute()).isEmpty();
      assertThat(response.getTasseDovute()).isEmpty();
      assertThat(response.getAddebiti()).isEmpty();
    }

    /**
     * Verifies addebiti are mapped correctly.
     */
    @Test
    @DisplayName("maps charges correctly")
    void mapsChargesCorrectly() {
      CinecaFeeStatus status = mock(CinecaFeeStatus.class);
      when(status.getStatus()).thenReturn("VERDE");
      when(status.getAmountDue()).thenReturn("0.0");
      when(status.getOverdueItems()).thenReturn(List.of());
      when(status.getDueItems()).thenReturn(List.of());

      CinecaCharge charge = mock(CinecaCharge.class);
      when(charge.getAcademicYear()).thenReturn(2025L);
      when(charge.getFeeDes()).thenReturn("Contributi universitari");
      when(charge.getPaidFlg()).thenReturn(1);
      when(charge.getAmount()).thenReturn(140.0);
      when(charge.getIuv()).thenReturn("000000041596920");

      when(feesClient.getFeeStatus(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(status);
      when(feesClient.getCharges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of(charge));

      TasseResponse response = feesService.getStatus(principal);

      assertThat(response.getAddebiti()).hasSize(1);
      assertThat(response.getAddebiti().get(0).getTassaDes())
          .isEqualTo("Contributi universitari");
      assertThat(response.getAddebiti().get(0).getPagatoFlg()).isEqualTo(1);
      assertThat(response.getAddebiti().get(0).getIuv()).isEqualTo("000000041596920");
    }

    /**
     * Verifies null semaforo is handled gracefully.
     */
    @Test
    @DisplayName("handles null fee status gracefully")
    void handlesNullFeeStatus() {
      when(feesClient.getFeeStatus(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(null);
      when(feesClient.getCharges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      TasseResponse response = feesService.getStatus(principal);

      assertThat(response.getSemaforo()).isNull();
      assertThat(response.getAddebiti()).isEmpty();
    }
  }

  /**
   * Verifies that {@link ProfileService#getBadge} correctly maps badge fields.
   */
  @Nested
  @DisplayName("Badge")
  class Badge {

    /**
     * Verifies null when no badge found.
     */
    @Test
    @DisplayName("returns null when no badges found")
    void returnsNullWhenNoBadges() {
      when(profileClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      var response = profileService.getBadge(principal);

      assertThat(response).isNull();
    }

    /**
     * Verifies badge fields are mapped correctly.
     */
    @Test
    @DisplayName("maps badge fields correctly")
    void mapsBadgeCorrectly() {
      CinecaBadge badge = mock(CinecaBadge.class);
      when(badge.getBdgId()).thenReturn(39498L);
      when(badge.getNome()).thenReturn("ALESSIO");
      when(badge.getCognome()).thenReturn("DEL MUTO");
      when(badge.getMatricola()).thenReturn("178026");
      when(badge.getFrontImagePresent()).thenReturn(0);
      when(badge.getRearImagePresent()).thenReturn(0);

      when(profileClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of(badge));

      var response = profileService.getBadge(principal);

      assertThat(response).isNotNull();
      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getCognome()).isEqualTo("DEL MUTO");
      assertThat(response.getMatricola()).isEqualTo("178026");
      assertThat(response.getFrontImagePresent()).isFalse();
    }
  }
}
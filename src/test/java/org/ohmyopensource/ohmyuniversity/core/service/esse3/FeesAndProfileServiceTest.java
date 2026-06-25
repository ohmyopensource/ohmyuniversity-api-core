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
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaInvoice;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaFeesClient.CinecaRefund;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link FeesService} and {@link ProfileService}.
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
  private static final Long PERS_ID = 92533L;

  private OmuPrincipal principal;

  @BeforeEach
  void setUp() {
    feesClient = mock(CinecaFeesClient.class);
    profileClient = mock(CinecaProfileClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    feesService = new FeesService(feesClient, sessionStore, universityRegistry, connectionRepository);
    profileService = new ProfileService(profileClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "TSTXXX00A00X000X", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID)).thenReturn(Optional.of(CINECA_JWT));
    when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID)).thenReturn(Optional.of(PERS_ID));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  @Nested
  @DisplayName("Fee status")
  class FeeStatus {

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

      var response = feesService.getStatus(principal);

      assertThat(response.getSemaforo()).isEqualTo("VERDE");
      assertThat(response.getImportoDovuto()).isEqualTo("0.0");
      assertThat(response.getAddebiti()).isEmpty();
    }

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

      var response = feesService.getStatus(principal);

      assertThat(response.getAddebiti()).hasSize(1);
      assertThat(response.getAddebiti().get(0).getTassaDes()).isEqualTo("Contributi universitari");
      assertThat(response.getAddebiti().get(0).getIuv()).isEqualTo("000000041596920");
    }

    @Test
    @DisplayName("handles null fee status gracefully")
    void handlesNullFeeStatus() {
      when(feesClient.getFeeStatus(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(null);
      when(feesClient.getCharges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      var response = feesService.getStatus(principal);

      assertThat(response.getSemaforo()).isNull();
      assertThat(response.getAddebiti()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Invoices")
  class Invoices {

    @Test
    @DisplayName("maps invoice fields correctly")
    void mapsInvoiceFieldsCorrectly() {
      CinecaInvoice invoice = mock(CinecaInvoice.class);
      when(invoice.getFattId()).thenReturn(1278017L);
      when(invoice.getAcademicYear()).thenReturn(2025);
      when(invoice.getDescription()).thenReturn("Contributo universitario");
      when(invoice.getAmount()).thenReturn(156.0);
      when(invoice.getPaidAmount()).thenReturn(156.0);
      when(invoice.getPaidFlg()).thenReturn(1);
      when(invoice.getIuv()).thenReturn("000000041596920");

      when(feesClient.getInvoices(BASE_URL, CINECA_JWT, STU_ID, PERS_ID))
          .thenReturn(List.of(invoice));

      var response = feesService.getInvoices(principal);

      assertThat(response.getInvoices()).hasSize(1);
      assertThat(response.getInvoices().get(0).getFattId()).isEqualTo(1278017L);
      assertThat(response.getInvoices().get(0).getDescription()).isEqualTo("Contributo universitario");
      assertThat(response.getInvoices().get(0).getPaidFlg()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns empty list when no invoices")
    void returnsEmptyList() {
      when(feesClient.getInvoices(BASE_URL, CINECA_JWT, STU_ID, PERS_ID)).thenReturn(List.of());

      var response = feesService.getInvoices(principal);

      assertThat(response.getInvoices()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Refunds")
  class Refunds {

    @Test
    @DisplayName("returns empty list when no refunds")
    void returnsEmptyList() {
      when(feesClient.getRefunds(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(List.of());

      var response = feesService.getRefunds(principal);

      assertThat(response.getRefunds()).isEmpty();
    }

    @Test
    @DisplayName("maps refund fields correctly")
    void mapsRefundFields() {
      CinecaRefund refund = mock(CinecaRefund.class);
      when(refund.getInvoiceId()).thenReturn(9999L);
      when(refund.getFeeDes()).thenReturn("Rimborso tassa");
      when(refund.getRefundAmount()).thenReturn("50.00");
      when(refund.getRefundDate()).thenReturn("01/01/2026");
      when(refund.getRefundStatus()).thenReturn("EROGATO");

      when(feesClient.getRefunds(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(List.of(refund));

      var response = feesService.getRefunds(principal);

      assertThat(response.getRefunds()).hasSize(1);
      assertThat(response.getRefunds().get(0).getFattId()).isEqualTo(9999L);
      assertThat(response.getRefunds().get(0).getFeeDes()).isEqualTo("Rimborso tassa");
    }
  }

  @Nested
  @DisplayName("Badge")
  class Badge {

    @Test
    @DisplayName("returns null when no badges found")
    void returnsNullWhenNoBadges() {
      when(profileClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());
      assertThat(profileService.getBadge(principal)).isNull();
    }

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
      assertThat(response.getFrontImagePresent()).isFalse();
    }
  }
}
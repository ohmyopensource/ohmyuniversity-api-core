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
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanActivity;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanDetail;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanHeader;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBooking;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient.CinecaBookingResult;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link CareerService#getExamHistory} and
 * {@link CareerService#getRecommendations}.
 */
class CareerServiceExtraTest {

  private CinecaCareerClient careerClient;
  private CinecaExamsClient examsClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private CareerService service;

  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

  @BeforeEach
  void setUp() {
    careerClient = mock(CinecaCareerClient.class);
    examsClient = mock(CinecaExamsClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    service = new CareerService(
        careerClient, examsClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "DLMLSS04E14L113Q", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies {@link CareerService#getExamHistory} grouping and mapping.
   */
  @Nested
  @DisplayName("getExamHistory")
  class GetExamHistory {

    /**
     * Verifies that bookings are grouped by adsceId correctly.
     */
    @Test
    @DisplayName("groups bookings by adsceId")
    void groupsByAdsceId() {
      CinecaBooking b1 = booking(1001L, "411114", "Architettura", "01/03/2025 09:00:00");
      CinecaBooking b2 = booking(1001L, "411114", "Architettura", "15/06/2025 09:00:00");
      CinecaBooking b3 = booking(1002L, "411115", "Analisi I", "10/07/2025 09:00:00");

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(b1, b2, b3));

      var response = service.getExamHistory(principal);

      assertThat(response.getEsami()).hasSize(2);
      assertThat(response.getEsami().get(0).getAdsceId()).isEqualTo(1001L);
      assertThat(response.getEsami().get(0).getTentativi()).hasSize(2);
      assertThat(response.getEsami().get(1).getAdsceId()).isEqualTo(1002L);
      assertThat(response.getEsami().get(1).getTentativi()).hasSize(1);
    }

    /**
     * Verifies that bookings with null adsceId are skipped.
     */
    @Test
    @DisplayName("skips bookings with null adsceId")
    void skipsNullAdsceId() {
      CinecaBooking b = mock(CinecaBooking.class);
      when(b.getAdsceId()).thenReturn(null);

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(b));

      var response = service.getExamHistory(principal);

      assertThat(response.getEsami()).isEmpty();
    }

    /**
     * Verifies that past exam attempt is marked as non-future.
     */
    @Test
    @DisplayName("marks past attempts as futuro=false")
    void marksPastAttemptAsFuturoFalse() {
      CinecaBooking b = booking(1001L, "411114", "Architettura", "01/01/2020 09:00:00");

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(b));

      var response = service.getExamHistory(principal);

      assertThat(response.getEsami().get(0).getTentativi().get(0).isFuturo()).isFalse();
    }

    /**
     * Verifies that future exam attempt is marked as future.
     */
    @Test
    @DisplayName("marks future attempts as futuro=true")
    void marksFutureAttemptAsFuturoTrue() {
      CinecaBooking b = booking(1001L, "411114", "Architettura", "31/12/2099 09:00:00");

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(b));

      var response = service.getExamHistory(principal);

      assertThat(response.getEsami().get(0).getTentativi().get(0).isFuturo()).isTrue();
    }

    /**
     * Verifies that exam result is mapped into the tentativo.
     */
    @Test
    @DisplayName("maps exam result into tentativo")
    void mapsExamResult() {
      CinecaBookingResult result = mock(CinecaBookingResult.class);
      when(result.isPassed()).thenReturn(true);
      when(result.isWithdrawn()).thenReturn(false);
      when(result.isAbsent()).thenReturn(false);
      when(result.getGrade()).thenReturn(29);
      when(result.getJudgmentCod()).thenReturn("IDON");

      CinecaBooking b = mock(CinecaBooking.class);
      when(b.getAdsceId()).thenReturn(1001L);
      when(b.getAdStuCod()).thenReturn("411114");
      when(b.getAdStuDes()).thenReturn("Architettura");
      when(b.getExamDateTime()).thenReturn("01/03/2025 09:00:00");
      when(b.getResult()).thenReturn(result);

      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(b));

      var response = service.getExamHistory(principal);
      var tentativo = response.getEsami().get(0).getTentativi().get(0);

      assertThat(tentativo.isSuperato()).isTrue();
      assertThat(tentativo.getVotoEsa()).isEqualTo(29);
      assertThat(tentativo.getTipoGiudCod()).isEqualTo("IDON");
    }

    /**
     * Verifies empty history when no bookings.
     */
    @Test
    @DisplayName("returns empty history when no bookings")
    void returnsEmptyWhenNoBookings() {
      when(examsClient.getBookings(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      var response = service.getExamHistory(principal);

      assertThat(response.getEsami()).isEmpty();
    }
  }

  /**
   * Verifies {@link CareerService#getRecommendations} filtering and ordering.
   */
  @Nested
  @DisplayName("getRecommendations")
  class GetRecommendations {

    /**
     * Verifies that passed exams are excluded from recommendations.
     */
    @Test
    @DisplayName("excludes passed exams from recommendations")
    void excludesPassedExams() {
      CinecaTranscriptRow passed = mock(CinecaTranscriptRow.class);
      when(passed.getStato()).thenReturn("S");
      when(passed.getAdCod()).thenReturn("411114");

      CinecaStudyPlanHeader header = mock(CinecaStudyPlanHeader.class);
      when(header.getPianoId()).thenReturn(1L);

      CinecaStudyPlanActivity activity = mock(CinecaStudyPlanActivity.class);
      when(activity.getAdCod()).thenReturn("411114");
      when(activity.getAdDes()).thenReturn("Architettura");
      when(activity.getCfu()).thenReturn(6.0);
      when(activity.getAnnoCorso()).thenReturn(1);

      CinecaStudyPlanDetail detail = mock(CinecaStudyPlanDetail.class);
      when(detail.getActivities()).thenReturn(List.of(activity));

      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(passed));
      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(header));
      when(careerClient.getStudyPlanDetail(BASE_URL, CINECA_JWT, STU_ID, 1L))
          .thenReturn(detail);

      var response = service.getRecommendations(principal);

      assertThat(response.getEsami()).isEmpty();
    }

    /**
     * Verifies that recommendations are ordered by score (anno * 100 + cfu).
     */
    @Test
    @DisplayName("orders recommendations by score ascending")
    void ordersRecommendationsByScore() {
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      CinecaStudyPlanHeader header = mock(CinecaStudyPlanHeader.class);
      when(header.getPianoId()).thenReturn(1L);

      CinecaStudyPlanActivity year1low = activity("AA001", "Analisi I", 1, 6.0);
      CinecaStudyPlanActivity year2 = activity("AA002", "Fisica II", 2, 9.0);
      CinecaStudyPlanActivity year1high = activity("AA003", "Programmazione", 1, 12.0);

      CinecaStudyPlanDetail detail = mock(CinecaStudyPlanDetail.class);
      when(detail.getActivities()).thenReturn(List.of(year2, year1high, year1low));

      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(header));
      when(careerClient.getStudyPlanDetail(BASE_URL, CINECA_JWT, STU_ID, 1L))
          .thenReturn(detail);

      var response = service.getRecommendations(principal);

      assertThat(response.getEsami()).hasSize(3);
      // score: year1low=106, year1high=112, year2=209
      assertThat(response.getEsami().get(0).getAdCod()).isEqualTo("AA001");
      assertThat(response.getEsami().get(1).getAdCod()).isEqualTo("AA003");
      assertThat(response.getEsami().get(2).getAdCod()).isEqualTo("AA002");
    }

    /**
     * Verifies empty recommendations when no plan headers found.
     */
    @Test
    @DisplayName("returns empty when no study plan headers")
    void returnsEmptyWhenNoHeaders() {
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());
      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of());

      var response = service.getRecommendations(principal);

      assertThat(response.getEsami()).isEmpty();
    }

    /**
     * Verifies empty recommendations when study plan detail is null.
     */
    @Test
    @DisplayName("returns empty when study plan detail is null")
    void returnsEmptyWhenDetailNull() {
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      CinecaStudyPlanHeader header = mock(CinecaStudyPlanHeader.class);
      when(header.getPianoId()).thenReturn(1L);
      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(header));
      when(careerClient.getStudyPlanDetail(BASE_URL, CINECA_JWT, STU_ID, 1L))
          .thenReturn(null);

      var response = service.getRecommendations(principal);

      assertThat(response.getEsami()).isEmpty();
    }
  }

  private CinecaBooking booking(Long adsceId, String adCod, String adDes, String dateTime) {
    CinecaBooking b = mock(CinecaBooking.class);
    when(b.getAdsceId()).thenReturn(adsceId);
    when(b.getAdStuCod()).thenReturn(adCod);
    when(b.getAdStuDes()).thenReturn(adDes);
    when(b.getExamDateTime()).thenReturn(dateTime);
    when(b.getResult()).thenReturn(null);
    return b;
  }

  private CinecaStudyPlanActivity activity(
      String adCod, String adDes, int anno, double cfu) {
    CinecaStudyPlanActivity a = mock(CinecaStudyPlanActivity.class);
    when(a.getAdCod()).thenReturn(adCod);
    when(a.getAdDes()).thenReturn(adDes);
    when(a.getAnnoCorso()).thenReturn(anno);
    when(a.getCfu()).thenReturn(cfu);
    return a;
  }
}
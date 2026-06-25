package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaExamResult;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaGrade;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanActivity;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanDetail;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaStudyPlanHeader;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaCareerClient.CinecaTranscriptRow;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaExamsClient;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.TranscriptResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.GradesResponse;

/**
 * Unit tests for {@link CareerService}.
 *
 * <p>All dependencies are replaced by Mockito mocks. Tests verify the mapping logic
 * that converts Cineca raw types into OhMyU DTOs and the aggregation calculations
 * performed by the service (averages, CFU counts, percentages).
 */
class CareerServiceTest {

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

  /**
   * Initialises fresh mocks and shared stubs before each test.
   */
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
        OMU_USER_ID, "TSTXXX00A00X000X", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies that service methods throw {@link CinecaAuthException} when the JWT is absent.
   */
  @Nested
  @DisplayName("Session expiry")
  class SessionExpiry {

    /**
     * Verifies that {@link CareerService#getTranscript} throws when JWT is absent.
     */
    @Test
    @DisplayName("getTranscript: throws CinecaAuthException when JWT expired")
    void getTranscript_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getTranscript(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that {@link CareerService#getGrades} throws when JWT is absent.
     */
    @Test
    @DisplayName("getGrades: throws CinecaAuthException when JWT expired")
    void getGrades_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString())).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.getGrades(principal))
          .isInstanceOf(CinecaAuthException.class);
    }
  }

  /**
   * Verifies {@link CareerService#getTranscript} field mapping.
   */
  @Nested
  @DisplayName("Transcript mapping")
  class TranscriptMapping {

    /**
     * Verifies that the grade is correctly extracted from the nested {@link CinecaExamResult}.
     */
    @Test
    @DisplayName("maps voto correctly from exam result")
    void mapsVotoFromResult() {
      CinecaTranscriptRow row = rowWith("S", 29.0, 0, "26/02/2024 00:00:00");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe()).hasSize(1);
      assertThat(response.getRighe().get(0).getVoto()).isEqualTo(29);
    }

    /**
     * Verifies that {@code lode=true} when lodeFlg=1.
     */
    @Test
    @DisplayName("maps lode=true when lodeFlg=1")
    void mapsLodeTrue() {
      CinecaTranscriptRow row = rowWith("S", 30.0, 1, "23/01/2026 00:00:00");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe().get(0).getLode()).isTrue();
    }

    /**
     * Verifies that {@code lode=false} when lodeFlg=0.
     */
    @Test
    @DisplayName("maps lode=false when lodeFlg=0")
    void mapsLodeFalse() {
      CinecaTranscriptRow row = rowWith("S", 28.0, 0, "16/02/2026 00:00:00");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe().get(0).getLode()).isFalse();
    }

    /**
     * Verifies that {@code superata=true} when stato=S.
     */
    @Test
    @DisplayName("sets superata=true when stato=S")
    void setsSuperataWhenStatoS() {
      CinecaTranscriptRow row = rowWith("S", 27.0, 0, "22/07/2025 00:00:00");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isTrue();
    }

    /**
     * Verifies that {@code superata=false} when stato=F.
     */
    @Test
    @DisplayName("sets superata=false when stato=F")
    void setsSuperataFalseWhenStatoF() {
      CinecaTranscriptRow row = rowWith("F", null, 0, "");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isFalse();
    }

    /**
     * Verifies that null voto is preserved (pass/fail exams).
     */
    @Test
    @DisplayName("handles null voto for pass/fail exams")
    void handlesNullVoto() {
      CinecaTranscriptRow row = rowWith("S", null, 0, "20/06/2024 00:00:00");
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of(row));

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe().get(0).getVoto()).isNull();
    }

    /**
     * Verifies that an empty Cineca response produces an empty list.
     */
    @Test
    @DisplayName("returns empty list when Cineca returns no rows")
    void returnsEmptyList() {
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      TranscriptResponse response = service.getTranscript(principal);

      assertThat(response.getRighe()).isEmpty();
    }
  }

  /**
   * Verifies the aggregation logic of {@link CareerService#getGrades}.
   */
  @Nested
  @DisplayName("Grades calculations")
  class GradesCalculations {

    /**
     * Verifies exam counts are correct.
     */
    @Test
    @DisplayName("calculates correct counts from rows")
    void calculatesCorrectCounts() {
      List<CinecaTranscriptRow> rows = List.of(
          rowWith("S", 29.0, 0, "26/02/2024 00:00:00"),
          rowWith("S", 23.0, 0, "15/02/2024 00:00:00"),
          rowWith("F", null, 0, "")
      );
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(rows);
      when(careerClient.getGrades(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      GradesResponse response = service.getGrades(principal);

      assertThat(response.getEsamiSuperati()).isEqualTo(2);
      assertThat(response.getEsamiTotali()).isEqualTo(3);
    }

    /**
     * Verifies CFU calculation excludes non-passed rows.
     */
    @Test
    @DisplayName("calculates CFU acquired correctly excluding frequentate")
    void calculatesCfuAcquiredCorrectly() {
      List<CinecaTranscriptRow> rows = List.of(
          rowWithPeso("S", 6.0),
          rowWithPeso("S", 9.0),
          rowWithPeso("F", 9.0)
      );
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(rows);
      when(careerClient.getGrades(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      GradesResponse response = service.getGrades(principal);

      assertThat(response.getCfu()).isEqualTo(15.0);
      assertThat(response.getCfuTotali()).isEqualTo(24.0);
    }

    /**
     * Verifies CFU percentage is rounded to one decimal.
     */
    @Test
    @DisplayName("calculates percentuale CFU correctly")
    void calculatesPercentualeCfu() {
      List<CinecaTranscriptRow> rows = List.of(
          rowWithPeso("S", 75.0),
          rowWithPeso("F", 25.0)
      );
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(rows);
      when(careerClient.getGrades(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      GradesResponse response = service.getGrades(principal);

      assertThat(response.getPercentualeCfu()).isEqualTo(75.0);
    }

    /**
     * Verifies correct grade selection by tipo and base.
     */
    @Test
    @DisplayName("picks mediaAritmetica from tipo=A base=30")
    void picksMediaAritmetica() {
      CinecaGrade gradeA30 = cinecaGrade("A", 30, 26.17);
      CinecaGrade gradeP30 = cinecaGrade("P", 30, 25.84);
      CinecaGrade gradeA110 = cinecaGrade("A", 110, 95.94);
      CinecaGrade gradeP110 = cinecaGrade("P", 110, 94.73);

      when(careerClient.getGrades(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(gradeA30, gradeP30, gradeA110, gradeP110));
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      GradesResponse response = service.getGrades(principal);

      assertThat(response.getMediaAritmetica()).isEqualTo(26.17);
      assertThat(response.getMediaPesata()).isEqualTo(25.84);
      assertThat(response.getBaseMax110()).isEqualTo(95.94);
    }

    /**
     * Verifies no division-by-zero when no rows exist.
     */
    @Test
    @DisplayName("returns null percentuale when no CFU totali")
    void returnsNullPercentualeWhenNoCfu() {
      when(careerClient.getTranscript(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());
      when(careerClient.getGrades(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      GradesResponse response = service.getGrades(principal);

      assertThat(response.getPercentualeCfu()).isNull();
    }
  }

  /**
   * Verifies {@link CareerService#getStudyPlan} resolution and mapping.
   */
  @Nested
  @DisplayName("Study plan")
  class StudyPlan {

    /**
     * Verifies empty response when no plan headers.
     */
    @Test
    @DisplayName("returns empty righe when no piano headers")
    void returnsEmptyWhenNoPianoHeaders() {
      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      var response = service.getStudyPlan(principal);

      assertThat(response.getRighe()).isEmpty();
    }

    /**
     * Verifies empty response when dettaglio is null.
     */
    @Test
    @DisplayName("returns empty righe when dettaglio is null")
    void returnsEmptyWhenDettaglioNull() {
      CinecaStudyPlanHeader header = mock(CinecaStudyPlanHeader.class);
      when(header.getPianoId()).thenReturn(1L);
      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(header));
      when(careerClient.getStudyPlanDetail(BASE_URL, CINECA_JWT, STU_ID, 1L))
          .thenReturn(null);

      var response = service.getStudyPlan(principal);

      assertThat(response.getRighe()).isEmpty();
    }

    /**
     * Verifies correct mapping of activities from the plan detail.
     */
    @Test
    @DisplayName("maps activities from plan detail correctly")
    void mapsActivitiesCorrectly() {
      CinecaStudyPlanHeader header = mock(CinecaStudyPlanHeader.class);
      when(header.getPianoId()).thenReturn(42L);

      CinecaStudyPlanActivity activity = mock(CinecaStudyPlanActivity.class);
      when(activity.getAdsceId()).thenReturn(2884996L);
      when(activity.getAdCod()).thenReturn("411114");
      when(activity.getAdDes()).thenReturn("Architettura degli elaboratori");
      when(activity.getAnnoCorso()).thenReturn(1);
      when(activity.getCfu()).thenReturn(6.0);
      when(activity.isRequired()).thenReturn(true);

      CinecaStudyPlanDetail detail = mock(CinecaStudyPlanDetail.class);
      when(detail.getActivities()).thenReturn(List.of(activity));

      when(careerClient.getStudyPlanHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(header));
      when(careerClient.getStudyPlanDetail(BASE_URL, CINECA_JWT, STU_ID, 42L))
          .thenReturn(detail);

      var response = service.getStudyPlan(principal);

      assertThat(response.getRighe()).hasSize(1);
      assertThat(response.getRighe().get(0).getAdCod()).isEqualTo("411114");
      assertThat(response.getRighe().get(0).getCfu()).isEqualTo(6.0);
      assertThat(response.getRighe().get(0).getObbligatorio()).isTrue();
    }
  }

  // ============ Helpers ============

  private CinecaTranscriptRow rowWith(String stato, Double voto, int lode, String dataEsa) {
    CinecaTranscriptRow row = mock(CinecaTranscriptRow.class);
    when(row.getStato()).thenReturn(stato);
    when(row.getPeso()).thenReturn(6.0);
    when(row.getAdsceId()).thenReturn(1L);
    when(row.getAdCod()).thenReturn("411000");
    when(row.getAdDes()).thenReturn("Test Exam");
    when(row.getAnnoCorso()).thenReturn(1);

    CinecaExamResult result = mock(CinecaExamResult.class);
    when(result.getVoto()).thenReturn(voto);
    when(result.getLodeFlg()).thenReturn(lode);
    when(result.getDataEsa()).thenReturn(dataEsa);
    when(row.getEsito()).thenReturn(result);

    return row;
  }

  private CinecaTranscriptRow rowWithPeso(String stato, double peso) {
    CinecaTranscriptRow row = mock(CinecaTranscriptRow.class);
    when(row.getStato()).thenReturn(stato);
    when(row.getPeso()).thenReturn(peso);
    when(row.getEsito()).thenReturn(null);
    return row;
  }

  private CinecaGrade cinecaGrade(String tipo, int base, double value) {
    CinecaGrade grade = mock(CinecaGrade.class);
    when(grade.getTipoMediaCod()).thenReturn(tipo);
    when(grade.getBase()).thenReturn(base);
    when(grade.getMedia()).thenReturn(value);
    return grade;
  }
}
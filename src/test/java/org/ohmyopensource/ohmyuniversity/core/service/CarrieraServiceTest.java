package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAddebito;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaAttivitaPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaEsito;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaMedia;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaPianoDettaglio;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaRigaLibretto;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaSemaforo;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaTestataPiano;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaCarrieraClient.CinecaValueWrapper;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.LibrettoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.MediaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.TasseResponse;

/**
 * Unit tests for {@link CarrieraService}.
 *
 * <p>All dependencies are replaced by Mockito mocks — no Spring context, no database,
 * no Redis instance, and no live Cineca ESSE3 connection are required. Tests focus on
 * the mapping logic that converts Cineca raw types into OMU DTOs, the aggregation
 * calculations performed by the service (averages, CFU counts, percentages), and the
 * propagation of {@link CinecaAuthException} when the stored Cineca JWT is absent.
 */
class CarrieraServiceTest {

  private CinecaCarrieraClient cinecaClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private CarrieraService service;

  /** Stable user identifier shared across all tests in this class. */
  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final String CINECA_AUTH_TOKEN = "fakeAuthToken";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

  /**
   * Initialises fresh Mockito mocks, constructs the {@link CarrieraService} under
   * test, and registers the happy-path stubs that are shared by all test methods:
   * a valid JWT and auth token in the session store, and a resolved
   * {@link UniversityConfig} with the expected base URL.
   */
  @BeforeEach
  void setUp() {
    cinecaClient = mock(CinecaCarrieraClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    service = new CarrieraService(
        cinecaClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "TSTXXX00A00X000X", UNIVERSITY_ID, STU_ID, MAT_ID, "178026");

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));
    when(sessionStore.getCinecaAuthToken(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_AUTH_TOKEN));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies that each public service method propagates a {@link CinecaAuthException}
   * when the Cineca JWT is absent from the session store, regardless of any other
   * downstream behaviour.
   */
  @Nested
  @DisplayName("Session expiry")
  class SessionExpiry {

    /**
     * Verifies that {@link CarrieraService#getLibretto} throws a
     * {@link CinecaAuthException} when {@link CinecaSessionStore#getCinecaJwt}
     * returns an empty {@link Optional}, indicating that the student's Cineca
     * session has expired.
     */
    @Test
    @DisplayName("getLibretto: throws CinecaAuthException when JWT expired")
    void getLibretto_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getLibretto(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that {@link CarrieraService#getMedia} throws a
     * {@link CinecaAuthException} when {@link CinecaSessionStore#getCinecaJwt}
     * returns an empty {@link Optional}, indicating that the student's Cineca
     * session has expired.
     */
    @Test
    @DisplayName("getMedia: throws CinecaAuthException when JWT expired")
    void getMedia_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMedia(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    /**
     * Verifies that {@link CarrieraService#getTasse} throws a
     * {@link CinecaAuthException} when {@link CinecaSessionStore#getCinecaJwt}
     * returns an empty {@link Optional}, indicating that the student's Cineca
     * session has expired.
     */
    @Test
    @DisplayName("getTasse: throws CinecaAuthException when JWT expired")
    void getTasse_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getTasse(principal))
          .isInstanceOf(CinecaAuthException.class);
    }
  }

  /**
   * Verifies that {@link CarrieraService#getLibretto} correctly maps each field
   * of the raw {@link CinecaRigaLibretto} — including nested {@link CinecaEsito}
   * values — into the corresponding {@link LibrettoResponse.RigaLibretto} fields.
   */
  @Nested
  @DisplayName("Libretto mapping")
  class LibrettoMapping {

    /**
     * Verifies that the integer vote is correctly extracted from the nested
     * {@link CinecaEsito#getVoto()} double value and set on the
     * {@link LibrettoResponse.RigaLibretto}.
     */
    @Test
    @DisplayName("maps voto correctly from esito nested object")
    void mapsVotoFromEsito() {
      CinecaRigaLibretto riga = rigaWith("S", 29.0, 0, "26/02/2024 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe()).hasSize(1);
      assertThat(response.getRighe().get(0).getVoto()).isEqualTo(29);
    }

    /**
     * Verifies that {@code lode} is set to {@code true} when
     * {@link CinecaEsito#getLodeFlg()} returns {@code 1}.
     */
    @Test
    @DisplayName("maps lode correctly when lodeFlg=1")
    void mapsLodeTrue() {
      CinecaRigaLibretto riga = rigaWith("S", 30.0, 1, "23/01/2026 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getLode()).isTrue();
    }

    /**
     * Verifies that {@code lode} is set to {@code false} when
     * {@link CinecaEsito#getLodeFlg()} returns {@code 0}.
     */
    @Test
    @DisplayName("maps lode correctly when lodeFlg=0")
    void mapsLodeFalse() {
      CinecaRigaLibretto riga = rigaWith("S", 28.0, 0, "16/02/2026 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getLode()).isFalse();
    }

    /**
     * Verifies that {@code superata} is set to {@code true} when
     * {@link CinecaRigaLibretto#getStato()} returns {@code "S"} (superata).
     */
    @Test
    @DisplayName("sets superata=true when stato=S")
    void setsSuperataWhenStatoS() {
      CinecaRigaLibretto riga = rigaWith("S", 27.0, 0, "22/07/2025 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isTrue();
    }

    /**
     * Verifies that {@code superata} is set to {@code false} when
     * {@link CinecaRigaLibretto#getStato()} returns {@code "F"} (frequentata,
     * not yet passed).
     */
    @Test
    @DisplayName("sets superata=false when stato=F")
    void setsSuperataFalseWhenStatoF() {
      CinecaRigaLibretto riga = rigaWith("F", null, 0, "");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isFalse();
    }

    /**
     * Verifies that when {@link CinecaEsito#getVoto()} is {@code null} — as is
     * the case for pass/fail exams such as Lingua inglese — the mapped
     * {@code voto} field is also {@code null} rather than zero or any other
     * default value.
     */
    @Test
    @DisplayName("handles null voto (pass/fail exams like Lingua inglese)")
    void handlesNullVoto() {
      CinecaRigaLibretto riga = rigaWith("S", null, 0, "20/06/2024 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getVoto()).isNull();
    }

    /**
     * Verifies that when {@link CinecaCarrieraClient#getRigheLibretto} returns
     * an empty list, {@link CarrieraService#getLibretto} produces a
     * {@link LibrettoResponse} with an empty {@code righe} list rather than
     * throwing or returning {@code null}.
     */
    @Test
    @DisplayName("returns empty list when Cineca returns no rows")
    void returnsEmptyList() {
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of());

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe()).isEmpty();
    }
  }

  /**
   * Verifies the aggregation logic performed by {@link CarrieraService#getMedia},
   * including exam counts, CFU totals and percentage, and the selection of the
   * correct {@link CinecaMedia} entry for each average field.
   */
  @Nested
  @DisplayName("Medie calculations")
  class MediaCalculations {

    /**
     * Verifies that the number of passed exams ({@code esamiSuperati}) and the
     * total number of exam rows ({@code esamiTotali}) are counted correctly,
     * distinguishing rows with stato {@code "S"} from those with stato {@code "F"}.
     */
    @Test
    @DisplayName("calculates correct counts from righe")
    void calculatesCorrectCounts() {
      List<CinecaRigaLibretto> righe = List.of(
          rigaWith("S", 29.0, 0, "26/02/2024 00:00:00"),
          rigaWith("S", 23.0, 0, "15/02/2024 00:00:00"),
          rigaWith("F", null, 0, "")
      );
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(righe);
      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getEsamiSuperati()).isEqualTo(2);
      assertThat(response.getEsamiTotali()).isEqualTo(3);
    }

    /**
     * Verifies that {@code cfu} (credits acquired) is the sum of the
     * {@link CinecaRigaLibretto#getPeso()} values of passed rows only, while
     * {@code cfuTotali} is the sum across all rows regardless of stato.
     */
    @Test
    @DisplayName("calculates CFU acquired correctly excluding frequentate")
    void calculatesCfuAcquiredCorrectly() {
      List<CinecaRigaLibretto> righe = List.of(
          rigaWithPeso("S", 6.0),
          rigaWithPeso("S", 9.0),
          rigaWithPeso("F", 9.0)
      );
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(righe);
      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getCfu()).isEqualTo(15.0);
      assertThat(response.getCfuTotali()).isEqualTo(24.0);
    }

    /**
     * Verifies that {@code percentualeCfu} is calculated as the ratio of acquired
     * CFU to total CFU, expressed as a percentage rounded to one decimal place.
     */
    @Test
    @DisplayName("calculates percentuale CFU correctly")
    void calculatesPercentualeCfu() {
      List<CinecaRigaLibretto> righe = List.of(
          rigaWithPeso("S", 75.0),
          rigaWithPeso("F", 25.0)
      );
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(righe);
      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getPercentualeCfu()).isEqualTo(75.0);
    }

    /**
     * Verifies that the service correctly selects the {@link CinecaMedia} entry
     * with {@code tipoMediaCod="A"} and {@code base=30} for {@code mediaAritmetica},
     * {@code tipoMediaCod="P"} and {@code base=30} for {@code mediaPesata}, and
     * {@code tipoMediaCod="A"} and {@code base=110} for {@code baseMax110}, when
     * all four variants are returned by {@link CinecaCarrieraClient#getMedia}.
     */
    @Test
    @DisplayName("picks mediaAritmetica from tipo=A base=30")
    void picksMediaAritmetica() {
      CinecaMedia mediaA30 = cinecaMedia("A", 30, 26.17);
      CinecaMedia mediaP30 = cinecaMedia("P", 30, 25.84);
      CinecaMedia mediaA110 = cinecaMedia("A", 110, 95.94);
      CinecaMedia mediaP110 = cinecaMedia("P", 110, 94.73);

      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(mediaA30, mediaP30, mediaA110, mediaP110));
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getMediaAritmetica()).isEqualTo(26.17);
      assertThat(response.getMediaPesata()).isEqualTo(25.84);
      assertThat(response.getBaseMax110()).isEqualTo(95.94);
    }

    /**
     * Verifies that {@code percentualeCfu} is {@code null} — rather than
     * producing a division-by-zero error — when no exam rows are present and
     * the total CFU is therefore zero.
     */
    @Test
    @DisplayName("returns zero percentuale when no CFU totali")
    void returnsZeroPercentualeWhenNoCfu() {
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());
      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getPercentualeCfu()).isNull();
    }
  }

  /**
   * Verifies that {@link CarrieraService#getPiano} correctly resolves the study
   * plan by fetching the list of {@link CinecaTestataPiano} headers, retrieving
   * the matching {@link CinecaPianoDettaglio}, and mapping each
   * {@link CinecaAttivitaPiano} into the response DTO.
   */
  @Nested
  @DisplayName("Piano di studi")
  class PianoStudi {

    /**
     * Verifies that when {@link CinecaCarrieraClient#getPianoHeaders} returns an
     * empty list — indicating the student has no active study plan — the service
     * returns a response with an empty {@code righe} list.
     */
    @Test
    @DisplayName("returns empty righe when Cineca returns no piano headers")
    void returnsEmptyWhenNoPianoHeaders() {
      when(cinecaClient.getPianoHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of());

      var response = service.getPiano(principal);

      assertThat(response.getRighe()).isEmpty();
    }

    /**
     * Verifies that when {@link CinecaCarrieraClient#getPianoDettaglio} returns
     * {@code null} for the plan header fetched from Cineca, the service returns
     * a response with an empty {@code righe} list rather than throwing a
     * {@link NullPointerException}.
     */
    @Test
    @DisplayName("returns empty righe when dettaglio is null")
    void returnsEmptyWhenDettaglioNull() {
      CinecaTestataPiano testata = mock(CinecaTestataPiano.class);
      when(testata.getPianoId()).thenReturn(1L);
      when(cinecaClient.getPianoHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(testata));
      when(cinecaClient.getPianoDettaglio(BASE_URL, CINECA_JWT, STU_ID, 1L))
          .thenReturn(null);

      var response = service.getPiano(principal);

      assertThat(response.getRighe()).isEmpty();
    }

    /**
     * Verifies that each {@link CinecaAttivitaPiano} within a
     * {@link CinecaPianoDettaglio} is correctly mapped to a
     * {@code PianoStudioResponse.RigaPiano}, preserving the activity code,
     * CFU credits, and the mandatory flag.
     */
    @Test
    @DisplayName("maps attivita from piano dettaglio correctly")
    void mapsAttivitaCorrectly() {
      CinecaTestataPiano testata = mock(CinecaTestataPiano.class);
      when(testata.getPianoId()).thenReturn(42L);

      CinecaAttivitaPiano attivita = mock(CinecaAttivitaPiano.class);
      when(attivita.getAdsceId()).thenReturn(2884996L);
      when(attivita.getAdCod()).thenReturn("411114");
      when(attivita.getAdDes()).thenReturn("Architettura degli elaboratori");
      when(attivita.getAnnoCorso()).thenReturn(1);
      when(attivita.getCfu()).thenReturn(6.0);
      when(attivita.isObbligatorio()).thenReturn(true);

      CinecaPianoDettaglio dettaglio = mock(CinecaPianoDettaglio.class);
      when(dettaglio.getAttivita()).thenReturn(List.of(attivita));

      when(cinecaClient.getPianoHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(testata));
      when(cinecaClient.getPianoDettaglio(BASE_URL, CINECA_JWT, STU_ID, 42L))
          .thenReturn(dettaglio);

      var response = service.getPiano(principal);

      assertThat(response.getRighe()).hasSize(1);
      assertThat(response.getRighe().get(0).getAdCod()).isEqualTo("411114");
      assertThat(response.getRighe().get(0).getCfu()).isEqualTo(6.0);
      assertThat(response.getRighe().get(0).getObbligatorio()).isTrue();
    }
  }

  /**
   * Verifies that {@link CarrieraService#getTasse} correctly assembles the
   * {@link TasseResponse} from the {@link CinecaSemaforo} traffic-light status
   * and the list of {@link CinecaAddebito} charge records returned by Cineca.
   */
  @Nested
  @DisplayName("Tasse")
  class Tasse {

    /**
     * Verifies that when Cineca returns a {@link CinecaSemaforo} with status
     * {@code "VERDE"} and no outstanding charges, all fields of the
     * {@link TasseResponse} are mapped correctly, including empty lists for
     * overdue fees, outstanding fees, and addebiti.
     */
    @Test
    @DisplayName("maps semaforo VERDE correctly")
    void mapsSemaforoVerde() {
      CinecaSemaforo semaforo = mock(CinecaSemaforo.class);
      when(semaforo.getSemaforo()).thenReturn("VERDE");
      when(semaforo.getImportoDovuto()).thenReturn("0.0");
      when(semaforo.getTasseScadute()).thenReturn(List.of());
      when(semaforo.getTasseDovute()).thenReturn(List.of());

      when(cinecaClient.getSemaforo(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(semaforo);
      when(cinecaClient.getAddebiti(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      TasseResponse response = service.getTasse(principal);

      assertThat(response.getSemaforo()).isEqualTo("VERDE");
      assertThat(response.getImportoDovuto()).isEqualTo("0.0");
      assertThat(response.getTasseScadute()).isEmpty();
      assertThat(response.getTasseDovute()).isEmpty();
      assertThat(response.getAddebiti()).isEmpty();
    }

    /**
     * Verifies that each {@link CinecaAddebito} returned by
     * {@link CinecaCarrieraClient#getAddebiti} is correctly mapped to the
     * corresponding DTO in {@link TasseResponse#getAddebiti()}, preserving the
     * fee description, payment flag, amount, and IUV payment identifier.
     */
    @Test
    @DisplayName("maps addebiti correctly")
    void mapsAddebitiCorrectly() {
      CinecaSemaforo semaforo = mock(CinecaSemaforo.class);
      when(semaforo.getSemaforo()).thenReturn("VERDE");
      when(semaforo.getImportoDovuto()).thenReturn("0.0");
      when(semaforo.getTasseScadute()).thenReturn(List.of());
      when(semaforo.getTasseDovute()).thenReturn(List.of());

      CinecaAddebito addebito = mock(CinecaAddebito.class);
      when(addebito.getAaId()).thenReturn(2025L);
      when(addebito.getTassaDes()).thenReturn("Contributi universitari");
      when(addebito.getPagatoFlg()).thenReturn(1);
      when(addebito.getImportoVoce()).thenReturn(140.0);
      when(addebito.getIuv()).thenReturn("000000041596920");

      when(cinecaClient.getSemaforo(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(semaforo);
      when(cinecaClient.getAddebiti(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of(addebito));

      TasseResponse response = service.getTasse(principal);

      assertThat(response.getAddebiti()).hasSize(1);
      assertThat(response.getAddebiti().get(0).getTassaDes())
          .isEqualTo("Contributi universitari");
      assertThat(response.getAddebiti().get(0).getPagatoFlg()).isEqualTo(1);
      assertThat(response.getAddebiti().get(0).getIuv()).isEqualTo("000000041596920");
    }

    /**
     * Verifies that when {@link CinecaCarrieraClient#getSemaforo} returns
     * {@code null} — which may occur if the student's tax position has not yet
     * been computed by Cineca — the service returns a {@link TasseResponse}
     * with a {@code null} semaforo field rather than throwing a
     * {@link NullPointerException}.
     */
    @Test
    @DisplayName("handles null semaforo gracefully")
    void handlesNullSemaforo() {
      when(cinecaClient.getSemaforo(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(null);
      when(cinecaClient.getAddebiti(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      TasseResponse response = service.getTasse(principal);

      assertThat(response.getSemaforo()).isNull();
      assertThat(response.getAddebiti()).isEmpty();
    }
  }

  /**
   * Verifies that {@link CarrieraService#getBadge} correctly maps the first
   * {@link CinecaBadge} returned by Cineca into a {@code BadgeResponse}, and
   * handles the case where no badge record exists for the student.
   */
  @Nested
  @DisplayName("Badge")
  class Badge {

    /**
     * Verifies that when {@link CinecaCarrieraClient#getBadges} returns an empty
     * list — indicating that no badge has been issued to the student — the service
     * returns {@code null} rather than throwing or returning an empty wrapper.
     */
    @Test
    @DisplayName("returns null when no badges found")
    void returnsNullWhenNoBadges() {
      when(cinecaClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      var response = service.getBadge(principal);

      assertThat(response).isNull();
    }

    /**
     * Verifies that the fields of the first {@link CinecaBadge} returned by
     * {@link CinecaCarrieraClient#getBadges} are correctly mapped to the
     * {@code BadgeResponse}, including name, surname, matricola, and the boolean
     * conversion of the {@code frontImagePresent} integer flag.
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

      when(cinecaClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of(badge));

      var response = service.getBadge(principal);

      assertThat(response).isNotNull();
      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getCognome()).isEqualTo("DEL MUTO");
      assertThat(response.getMatricola()).isEqualTo("178026");
      assertThat(response.getFrontImagePresent()).isFalse();
    }
  }

  /**
   * Constructs a mocked {@link CinecaRigaLibretto} with the given stato, voto,
   * lode flag, and exam date. The peso is fixed at {@code 6.0} and the activity
   * code at {@code "411000"} for all invocations of this helper.
   *
   * @param stato    the exam state, either {@code "S"} (passed) or {@code "F"} (attended)
   * @param voto     the numeric grade, or {@code null} for pass/fail exams
   * @param lode     {@code 1} if the exam was passed with honours, {@code 0} otherwise
   * @param dataEsa  the exam date string in {@code dd/MM/yyyy HH:mm:ss} format, or empty
   * @return a configured mock of {@link CinecaRigaLibretto}
   */
  private CinecaRigaLibretto rigaWith(String stato, Double voto, int lode, String dataEsa) {
    CinecaRigaLibretto riga = mock(CinecaRigaLibretto.class);
    when(riga.getStato()).thenReturn(stato);
    when(riga.getPeso()).thenReturn(6.0);
    when(riga.getAdsceId()).thenReturn(1L);
    when(riga.getAdCod()).thenReturn("411000");
    when(riga.getAdDes()).thenReturn("Test Exam");
    when(riga.getAnnoCorso()).thenReturn(1);

    CinecaEsito esito = mock(CinecaEsito.class);
    when(esito.getVoto()).thenReturn(voto);
    when(esito.getLodeFlg()).thenReturn(lode);
    when(esito.getDataEsa()).thenReturn(dataEsa);
    when(riga.getEsito()).thenReturn(esito);

    return riga;
  }

  /**
   * Constructs a mocked {@link CinecaRigaLibretto} with the given stato and peso,
   * suitable for CFU aggregation tests where grade and date fields are irrelevant.
   * The {@link CinecaEsito} is stubbed as {@code null}.
   *
   * @param stato the exam state, either {@code "S"} (passed) or {@code "F"} (attended)
   * @param peso  the number of CFU credits associated with the exam
   * @return a configured mock of {@link CinecaRigaLibretto}
   */
  private CinecaRigaLibretto rigaWithPeso(String stato, double peso) {
    CinecaRigaLibretto riga = mock(CinecaRigaLibretto.class);
    when(riga.getStato()).thenReturn(stato);
    when(riga.getPeso()).thenReturn(peso);
    when(riga.getEsito()).thenReturn(null);
    return riga;
  }

  /**
   * Constructs a mocked {@link CinecaMedia} with the given type code, base, and
   * average value, as returned by {@link CinecaCarrieraClient#getMedia}.
   *
   * @param tipo   the average type code: {@code "A"} for arithmetic, {@code "P"} for weighted
   * @param base   the grading base, either {@code 30} or {@code 110}
   * @param valore the computed average value
   * @return a configured mock of {@link CinecaMedia}
   */
  private CinecaMedia cinecaMedia(String tipo, int base, double valore) {
    CinecaMedia media = mock(CinecaMedia.class);
    when(media.getTipoMediaCod()).thenReturn(tipo);
    when(media.getBase()).thenReturn(base);
    when(media.getMedia()).thenReturn(valore);
    return media;
  }
}
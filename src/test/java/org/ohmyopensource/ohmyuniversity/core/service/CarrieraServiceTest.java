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
 * All dependencies are mocked — no Spring context, no DB, no Redis, no Cineca.
 * Tests focus on mapping logic, aggregation calculations, and session expiry handling.
 */
class CarrieraServiceTest {

  private CinecaCarrieraClient cinecaClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private CarrieraService service;

  // Fixed test principal
  private static final String OMU_USER_ID = UUID.randomUUID().toString();
  private static final String UNIVERSITY_ID = "UNIMOL";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CINECA_JWT = "fake.cineca.jwt";
  private static final String CINECA_AUTH_TOKEN = "fakeAuthToken";
  private static final Long STU_ID = 89486L;
  private static final Long MAT_ID = 106279L;

  private OmuPrincipal principal;

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

    // Default happy-path stubs
    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));
    when(sessionStore.getCinecaAuthToken(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_AUTH_TOKEN));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  // ================================
  // Session expiry
  // ================================

  @Nested
  @DisplayName("Session expiry")
  class SessionExpiry {

    @Test
    @DisplayName("getLibretto: throws CinecaAuthException when JWT expired")
    void getLibretto_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getLibretto(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("getMedia: throws CinecaAuthException when JWT expired")
    void getMedia_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMedia(principal))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("getTasse: throws CinecaAuthException when JWT expired")
    void getTasse_throwsWhenJwtExpired() {
      when(sessionStore.getCinecaJwt(anyString(), anyString()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getTasse(principal))
          .isInstanceOf(CinecaAuthException.class);
    }
  }

  // ================================
  // Libretto mapping
  // ================================

  @Nested
  @DisplayName("Libretto mapping")
  class LibrettoMapping {

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

    @Test
    @DisplayName("maps lode correctly when lodeFlg=1")
    void mapsLodeTrue() {
      CinecaRigaLibretto riga = rigaWith("S", 30.0, 1, "23/01/2026 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getLode()).isTrue();
    }

    @Test
    @DisplayName("maps lode correctly when lodeFlg=0")
    void mapsLodeFalse() {
      CinecaRigaLibretto riga = rigaWith("S", 28.0, 0, "16/02/2026 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getLode()).isFalse();
    }

    @Test
    @DisplayName("sets superata=true when stato=S")
    void setsSuperataWhenStatoS() {
      CinecaRigaLibretto riga = rigaWith("S", 27.0, 0, "22/07/2025 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isTrue();
    }

    @Test
    @DisplayName("sets superata=false when stato=F")
    void setsSuperataFalseWhenStatoF() {
      CinecaRigaLibretto riga = rigaWith("F", null, 0, "");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getSuperata()).isFalse();
    }

    @Test
    @DisplayName("handles null voto (pass/fail exams like Lingua inglese)")
    void handlesNullVoto() {
      CinecaRigaLibretto riga = rigaWith("S", null, 0, "20/06/2024 00:00:00");
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of(riga));

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe().get(0).getVoto()).isNull();
    }

    @Test
    @DisplayName("returns empty list when Cineca returns no rows")
    void returnsEmptyList() {
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID))
          .thenReturn(List.of());

      LibrettoResponse response = service.getLibretto(principal);

      assertThat(response.getRighe()).isEmpty();
    }
  }

  // ================================
  // Medie calculations
  // ================================

  @Nested
  @DisplayName("Medie calculations")
  class MediaCalculations {

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

    @Test
    @DisplayName("calculates CFU acquired correctly excluding frequentate")
    void calculatesCfuAcquiredCorrectly() {
      // S: 6 + 9 = 15 CFU acquired, F: 9 CFU not acquired, total = 24
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

    @Test
    @DisplayName("returns zero percentuale when no CFU totali")
    void returnsZeroPercentualeWhenNoCfu() {
      when(cinecaClient.getRigheLibretto(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());
      when(cinecaClient.getMedia(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(List.of());

      MediaResponse response = service.getMedia(principal);

      assertThat(response.getPercentualeCfu()).isNull();
    }
  }

  // ================================
  // Piano
  // ================================

  @Nested
  @DisplayName("Piano di studi")
  class PianoStudi {

    @Test
    @DisplayName("returns empty righe when Cineca returns no piano headers")
    void returnsEmptyWhenNoPianoHeaders() {
      when(cinecaClient.getPianoHeaders(BASE_URL, CINECA_JWT, STU_ID))
          .thenReturn(List.of());

      var response = service.getPiano(principal);

      assertThat(response.getRighe()).isEmpty();
    }

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

  // ================================
  // Tasse
  // ================================

  @Nested
  @DisplayName("Tasse")
  class Tasse {

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

  // ================================
  // Badge
  // ================================

  @Nested
  @DisplayName("Badge")
  class Badge {

    @Test
    @DisplayName("returns null when no badges found")
    void returnsNullWhenNoBadges() {
      when(cinecaClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of());

      var response = service.getBadge(principal);

      assertThat(response).isNull();
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

      when(cinecaClient.getBadges(BASE_URL, CINECA_JWT, STU_ID)).thenReturn(List.of(badge));

      var response = service.getBadge(principal);

      assertThat(response).isNotNull();
      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getCognome()).isEqualTo("DEL MUTO");
      assertThat(response.getMatricola()).isEqualTo("178026");
      assertThat(response.getFrontImagePresent()).isFalse();
    }
  }

  // ================================
  // Test helpers
  // ================================

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

  private CinecaRigaLibretto rigaWithPeso(String stato, double peso) {
    CinecaRigaLibretto riga = mock(CinecaRigaLibretto.class);
    when(riga.getStato()).thenReturn(stato);
    when(riga.getPeso()).thenReturn(peso);
    when(riga.getEsito()).thenReturn(null);
    return riga;
  }

  private CinecaMedia cinecaMedia(String tipo, int base, double valore) {
    CinecaMedia media = mock(CinecaMedia.class);
    when(media.getTipoMediaCod()).thenReturn(tipo);
    when(media.getBase()).thenReturn(base);
    when(media.getMedia()).thenReturn(valore);
    return media;
  }
}
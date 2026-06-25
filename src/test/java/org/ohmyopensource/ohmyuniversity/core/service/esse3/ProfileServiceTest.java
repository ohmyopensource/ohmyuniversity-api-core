package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaCarriera;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaPersona;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link ProfileService}.
 *
 * <p>Covers {@code getPersona} (full and fallback paths) and {@code getInfo}.
 */
class ProfileServiceTest {

  private CinecaProfileClient profileClient;
  private CinecaSessionStore sessionStore;
  private UniversityRegistry universityRegistry;
  private UniversityConnectionRepository connectionRepository;

  private ProfileService service;

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
    profileClient = mock(CinecaProfileClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    universityRegistry = mock(UniversityRegistry.class);
    connectionRepository = mock(UniversityConnectionRepository.class);

    service = new ProfileService(
        profileClient, sessionStore, universityRegistry, connectionRepository);

    principal = new OmuPrincipal(
        OMU_USER_ID, "DLMLSS04E14L113Q", UNIVERSITY_ID, STU_ID, MAT_ID, "178026", true);

    when(sessionStore.getCinecaJwt(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(CINECA_JWT));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies {@link ProfileService#getPersona} — full path with persId in session.
   */
  @Nested
  @DisplayName("getPersona — full path")
  class GetPersonaFull {

    /**
     * Verifies that all personal data fields are mapped correctly from Cineca.
     */
    @Test
    @DisplayName("maps persona fields correctly when persId is in session")
    void mapsPersonaFields() {
      when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
          .thenReturn(Optional.of(PERS_ID));

      CinecaPersona persona = mock(CinecaPersona.class);
      when(persona.getPersId()).thenReturn(PERS_ID);
      when(persona.getNome()).thenReturn("ALESSIO");
      when(persona.getCognome()).thenReturn("DEL MUTO");
      when(persona.getCodFis()).thenReturn("DLMLSS04E14L113Q");
      when(persona.getEmail()).thenReturn("a.delmuto@studenti.unimol.it");
      when(persona.getEmailAte()).thenReturn("a.delmuto@unimol.it");
      when(persona.getDomComeResFlg()).thenReturn(1);
      when(persona.getComuNascDes()).thenReturn("Termoli");
      when(persona.getComuNascSigla()).thenReturn("CB");
      when(persona.getViaRes()).thenReturn("Via Roma");
      when(persona.getNumCivRes()).thenReturn("1");
      when(persona.getComuResDes()).thenReturn("Termoli");
      when(persona.getComuResSigla()).thenReturn("CB");

      when(profileClient.getPersona(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(persona);

      var response = service.getPersona(principal);

      assertThat(response.getPersId()).isEqualTo(PERS_ID);
      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getCognome()).isEqualTo("DEL MUTO");
      assertThat(response.getEmail()).isEqualTo("a.delmuto@studenti.unimol.it");
      assertThat(response.getLuogoNascita()).isEqualTo("Termoli (CB)");
      assertThat(response.getIndirizzoResidenza()).isEqualTo("Via Roma, 1");
      assertThat(response.getComuneResidenza()).isEqualTo("Termoli (CB)");
      assertThat(response.isDomicilioComeResidenza()).isTrue();
    }

    /**
     * Verifies domicilio fields are populated when domComeRes=0.
     */
    @Test
    @DisplayName("populates domicilio fields when domComeResFlg=0")
    void populatesDomicilioFields() {
      when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
          .thenReturn(Optional.of(PERS_ID));

      CinecaPersona persona = mock(CinecaPersona.class);
      when(persona.getPersId()).thenReturn(PERS_ID);
      when(persona.getDomComeResFlg()).thenReturn(0);
      when(persona.getViaDom()).thenReturn("Via Milano");
      when(persona.getNumCivDom()).thenReturn("5");
      when(persona.getComuDomDes()).thenReturn("Campobasso");
      when(persona.getComuDomSigla()).thenReturn("CB");
      when(persona.getNaziDomDes()).thenReturn("Italia");

      when(profileClient.getPersona(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(persona);

      var response = service.getPersona(principal);

      assertThat(response.isDomicilioComeResidenza()).isFalse();
      assertThat(response.getIndirizzoDomicilio()).isEqualTo("Via Milano 5");
      assertThat(response.getComuneDomicilio()).isEqualTo("Campobasso (CB)");
      assertThat(response.getNazioneDomicilio()).isEqualTo("Italia");
    }

    /**
     * Verifies that CinecaAuthException is thrown when persona is null.
     */
    @Test
    @DisplayName("throws CinecaAuthException when persona not found")
    void throwsWhenPersonaNull() {
      when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
          .thenReturn(Optional.of(PERS_ID));
      when(profileClient.getPersona(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(null);

      assertThatThrownBy(() -> service.getPersona(principal))
          .isInstanceOf(CinecaAuthException.class);
    }
  }

  /**
   * Verifies {@link ProfileService#getPersona} — fallback path when persId is absent.
   */
  @Nested
  @DisplayName("getPersona — fallback path")
  class GetPersonaFallback {

    /**
     * Verifies that a minimal profile is returned from Redis when persId is not cached.
     */
    @Test
    @DisplayName("returns minimal profile from Redis when persId absent")
    void returnsMinimalProfileFromRedis() {
      when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
          .thenReturn(Optional.empty());
      when(sessionStore.getUserNome(OMU_USER_ID)).thenReturn(Optional.of("ALESSIO"));
      when(sessionStore.getUserCognome(OMU_USER_ID)).thenReturn(Optional.of("DEL MUTO"));
      when(sessionStore.getUserId(OMU_USER_ID)).thenReturn(Optional.of("a.delmuto"));

      var response = service.getPersona(principal);

      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getCognome()).isEqualTo("DEL MUTO");
      assertThat(response.getUserId()).isEqualTo("a.delmuto");
      assertThat(response.getCodFis()).isEqualTo("DLMLSS04E14L113Q");
      assertThat(response.isDomicilioComeResidenza()).isTrue();
    }

    /**
     * Verifies that empty strings are used when Redis has no name/surname.
     */
    @Test
    @DisplayName("uses empty strings when Redis has no nome/cognome")
    void usesEmptyStringsWhenRedisEmpty() {
      when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
          .thenReturn(Optional.empty());
      when(sessionStore.getUserNome(OMU_USER_ID)).thenReturn(Optional.empty());
      when(sessionStore.getUserCognome(OMU_USER_ID)).thenReturn(Optional.empty());
      when(sessionStore.getUserId(OMU_USER_ID)).thenReturn(Optional.empty());

      var response = service.getPersona(principal);

      assertThat(response.getNome()).isEmpty();
      assertThat(response.getCognome()).isEmpty();
    }
  }

  /**
   * Verifies {@link ProfileService#getInfo} field mapping.
   */
  @Nested
  @DisplayName("getInfo")
  class GetInfo {

    /**
     * Verifies that career info fields are mapped correctly from Cineca.
     */
    @Test
    @DisplayName("maps career info fields correctly")
    void mapsCareerInfoFields() {
      CinecaCarriera carriera = mock(CinecaCarriera.class);
      when(carriera.getStuId()).thenReturn(STU_ID);
      when(carriera.getMatId()).thenReturn(MAT_ID);
      when(carriera.getMatricola()).thenReturn("178026");
      when(carriera.getNome()).thenReturn("ALESSIO");
      when(carriera.getCognome()).thenReturn("DEL MUTO");
      when(carriera.getTipoCorsoCod()).thenReturn("L2");
      when(carriera.getCdsDes()).thenReturn("INFORMATICA");
      when(carriera.getAnnoCorso()).thenReturn(3);
      when(carriera.getStaStuCod()).thenReturn("A");

      when(profileClient.getCarriera(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(carriera);

      var response = service.getInfo(principal);

      assertThat(response.getStuId()).isEqualTo(STU_ID);
      assertThat(response.getMatId()).isEqualTo(MAT_ID);
      assertThat(response.getMatricola()).isEqualTo("178026");
      assertThat(response.getNome()).isEqualTo("ALESSIO");
      assertThat(response.getTipoCorsoCod()).isEqualTo("L2");
      assertThat(response.getCdsDes()).isEqualTo("INFORMATICA");
      assertThat(response.getAnnoCorso()).isEqualTo(3);
    }

    /**
     * Verifies that CinecaUnavailableException is thrown when carriera is null.
     */
    @Test
    @DisplayName("throws CinecaUnavailableException when carriera not found")
    void throwsWhenCarrieraNull() {
      when(profileClient.getCarriera(BASE_URL, CINECA_JWT, MAT_ID)).thenReturn(null);

      assertThatThrownBy(() -> service.getInfo(principal))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }
}
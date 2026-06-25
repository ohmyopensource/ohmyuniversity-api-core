package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry.UniversityConfig;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;

/**
 * Unit tests for {@link ProfileService#getAvatar}.
 */
class ProfileServiceExtraTest {

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
    when(sessionStore.getCinecaPersId(OMU_USER_ID, UNIVERSITY_ID))
        .thenReturn(Optional.of(PERS_ID));

    UniversityConfig config = mock(UniversityConfig.class);
    when(config.baseUrl()).thenReturn(BASE_URL);
    when(universityRegistry.resolve(UNIVERSITY_ID)).thenReturn(Optional.of(config));
  }

  /**
   * Verifies {@link ProfileService#getAvatar} delegation and null handling.
   */
  @Nested
  @DisplayName("getAvatar")
  class GetAvatar {

    /**
     * Verifies that avatar bytes are returned correctly.
     */
    @Test
    @DisplayName("returns JPEG bytes when avatar available")
    void returnsJpegBytes() {
      byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
      when(profileClient.getAvatar(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(jpeg);

      byte[] result = service.getAvatar(principal);

      assertThat(result).isEqualTo(jpeg);
    }

    /**
     * Verifies that null is returned when Cineca has no avatar.
     */
    @Test
    @DisplayName("returns null when no avatar available")
    void returnsNullWhenNoAvatar() {
      when(profileClient.getAvatar(BASE_URL, CINECA_JWT, PERS_ID)).thenReturn(null);

      byte[] result = service.getAvatar(principal);

      assertThat(result).isNull();
    }
  }
}
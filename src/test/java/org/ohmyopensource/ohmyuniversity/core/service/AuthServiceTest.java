package org.ohmyopensource.ohmyuniversity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaLoginResponse;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse;

/**
 * Unit tests for {@link AuthService}.
 *
 * All dependencies are mocked. No Spring context, no DB, no Redis.
 * Helper methods pre-build mock objects BEFORE any outer when() call
 * to avoid Mockito's UnfinishedStubbing.
 */
class AuthServiceTest {

  private CinecaClient cinecaClient;
  private CinecaSessionStore sessionStore;
  private OmuJwtService jwtService;
  private OmuUserRepository userRepository;
  private UniversityConnectionRepository connectionRepository;
  private UniversityRegistry universityRegistry;

  private AuthService authService;

  private static final String UNI_ID   = "UNIMOL";
  private static final String UNI_NAME = "Università degli Studi del Molise";
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  private static final String CF       = "TSTXXX00A00X000X";
  private static final UUID   USER_UUID = UUID.randomUUID();
  private static final String USER_ID  = USER_UUID.toString();

  @BeforeEach
  void setUp() {
    cinecaClient         = mock(CinecaClient.class);
    sessionStore         = mock(CinecaSessionStore.class);
    jwtService           = mock(OmuJwtService.class);
    userRepository       = mock(OmuUserRepository.class);
    connectionRepository = mock(UniversityConnectionRepository.class);
    universityRegistry   = mock(UniversityRegistry.class);

    authService = new AuthService(
        cinecaClient, sessionStore, jwtService,
        userRepository, connectionRepository, universityRegistry);
  }

  // ============================================================
  // Helpers — pre-built BEFORE any outer when() call
  // ============================================================

  private UniversityRegistry.UniversityConfig uniConfig() {
    return new UniversityRegistry.UniversityConfig(UNI_NAME, BASE_URL);
  }

  private LoginRequest buildRequest() {
    LoginRequest req = mock(LoginRequest.class);
    when(req.getUniversityId()).thenReturn(UNI_ID);
    when(req.getUsername()).thenReturn("adelmuto");
    when(req.getPassword()).thenReturn("secret");
    return req;
  }

  private OmuUser buildMockUser() {
    OmuUser user = mock(OmuUser.class);
    when(user.getId()).thenReturn(USER_UUID);
    when(user.getCodiceFiscale()).thenReturn(CF);
    return user;
  }

  private CinecaLoginResponse.CinecaUser buildCinecaUser(Long persId,
      List<CinecaLoginResponse.TrattoCarriera> tratti) {
    CinecaLoginResponse.CinecaUser u = mock(CinecaLoginResponse.CinecaUser.class);
    when(u.getCodiceFiscale()).thenReturn(CF);
    when(u.getUserId()).thenReturn("adelmuto");
    when(u.getPersId()).thenReturn(persId);
    when(u.getTrattiCarriera()).thenReturn(tratti);
    return u;
  }

  private CinecaLoginResponse buildCinecaResponse(
      CinecaLoginResponse.CinecaUser cinecaUser,
      String jwt,
      String authToken) {
    CinecaLoginResponse resp = mock(CinecaLoginResponse.class);
    when(resp.getUser()).thenReturn(cinecaUser);
    when(resp.getJwt()).thenReturn(jwt);
    when(resp.getAuthToken()).thenReturn(authToken);
    return resp;
  }

  private CinecaLoginResponse.TrattoCarriera buildTratto(
      Long stuId, Long matId, String matricola,
      String staStuCod, String staStuDes,
      CinecaLoginResponse.DettaglioTratto dettaglio) {
    CinecaLoginResponse.TrattoCarriera t = mock(CinecaLoginResponse.TrattoCarriera.class);
    when(t.getStuId()).thenReturn(stuId);
    when(t.getMatId()).thenReturn(matId);
    when(t.getMatricola()).thenReturn(matricola);
    when(t.getCdsDes()).thenReturn("Informatica");
    when(t.getStaStuCod()).thenReturn(staStuCod);
    when(t.getStaStuDes()).thenReturn(staStuDes);
    when(t.getDettaglioTratto()).thenReturn(dettaglio);
    return t;
  }

  // ============================================================
  // login()
  // ============================================================

  @Nested
  @DisplayName("login()")
  class Login {

    @Test
    @DisplayName("new user → user created, connection saved, tokens stored, response populated")
    void newUser() {
      // Pre-build all mocks before any outer when()
      LoginRequest req           = buildRequest();
      OmuUser savedUser          = buildMockUser();
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(12345L, List.of());
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, "cineca-jwt", "cineca-auth");

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.empty());
      when(userRepository.save(any())).thenReturn(savedUser);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), eq(UNI_ID), anyString())).thenReturn(Optional.empty());
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("access-token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

      LoginResponse response = authService.login(req);

      assertThat(response.getAccessToken()).isEqualTo("access-token");
      assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
      verify(connectionRepository).save(any(UniversityConnection.class));
      verify(sessionStore).storeCinecaJwt(USER_ID, UNI_ID, "cineca-jwt");
      verify(sessionStore).storeCinecaAuthToken(USER_ID, UNI_ID, "cineca-auth");
      verify(sessionStore).storeCinecaPersId(USER_ID, UNI_ID, 12345L);
      verify(sessionStore).storeRefreshToken("refresh-token", USER_ID);
    }

    @Test
    @DisplayName("existing user → user found, lastLoginAt updated, no new connection")
    void existingUser() {
      LoginRequest req = buildRequest();
      OmuUser existingUser = buildMockUser();
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of());
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(any())).thenReturn(existingUser);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), eq(UNI_ID), anyString())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("access-token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

      authService.login(req);

      verify(existingUser).setLastLoginAt(any());
      verify(connectionRepository, never()).save(any(UniversityConnection.class));
    }

    @Test
    @DisplayName("unknown universityId → throws IllegalArgumentException")
    void unknownUniversity() {
      LoginRequest req = buildRequest();
      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.login(req))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown university");
    }

    @Test
    @DisplayName("null JWT from Cineca → storeCinecaJwt never called")
    void nullJwtNotStored() {
      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of());
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, "auth-token");

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      authService.login(req);

      verify(sessionStore, never()).storeCinecaJwt(any(), any(), any());
    }

    @Test
    @DisplayName("null authToken from Cineca → storeCinecaAuthToken never called")
    void nullAuthTokenNotStored() {
      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of());
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, "jwt", null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      authService.login(req);

      verify(sessionStore, never()).storeCinecaAuthToken(any(), any(), any());
    }

    @Test
    @DisplayName("null trattiCarriera → profili empty, access token issued with null ids")
    void nullTrattiCarriera() {
      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, null);
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      LoginResponse response = authService.login(req);

      assertThat(response.getProfili()).isEmpty();
      verify(jwtService).issue(any(), any(), any(),
          eq(null), eq(null), eq(null));
    }

    @Test
    @DisplayName("tratto con staStuCod=A → ProfiloCarriera.attivo = true")
    void trattoAttivoTrue() {
      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.TrattoCarriera tratto =
          buildTratto(89486L, 106279L, "178026", "A", "Attivo", null);
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of(tratto));
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      LoginResponse response = authService.login(req);

      assertThat(response.getProfili()).hasSize(1);
      assertThat(response.getProfili().getFirst().isAttivo()).isTrue();
    }

    @Test
    @DisplayName("tratto con staStuCod=C → ProfiloCarriera.attivo = false")
    void trattoAttivoFalse() {
      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.TrattoCarriera tratto =
          buildTratto(89486L, 106279L, "178026", "C", "Cessato", null);
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of(tratto));
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      LoginResponse response = authService.login(req);

      assertThat(response.getProfili().getFirst().isAttivo()).isFalse();
    }

    @Test
    @DisplayName("tratto con dettaglioTratto → ProfiloCarriera popolato correttamente")
    void trattoConDettaglio() {
      CinecaLoginResponse.DettaglioTratto det = mock(CinecaLoginResponse.DettaglioTratto.class);
      when(det.getCdsCod()).thenReturn("IN01");
      when(det.getTipoCorsoCod()).thenReturn("L");
      when(det.getAnnoCorso()).thenReturn(3);
      when(det.getDurataAnni()).thenReturn(3);
      when(det.getAaIscrId()).thenReturn(2022);

      LoginRequest req = buildRequest();
      OmuUser user = buildMockUser();
      CinecaLoginResponse.TrattoCarriera tratto =
          buildTratto(89486L, 106279L, "178026", "A", "Attivo", det);
      CinecaLoginResponse.CinecaUser cinecaUser = buildCinecaUser(null, List.of(tratto));
      CinecaLoginResponse cinecaResp = buildCinecaResponse(cinecaUser, null, null);

      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(eq(BASE_URL), anyString(), anyString())).thenReturn(cinecaResp);
      when(userRepository.findByCodiceFiscale(CF)).thenReturn(Optional.of(user));
      when(userRepository.save(any())).thenReturn(user);
      when(connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
          any(), any(), any())).thenReturn(Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh");

      LoginResponse response = authService.login(req);

      LoginResponse.ProfiloCarriera profilo = response.getProfili().getFirst();
      assertThat(profilo.getCorsoCodice()).isEqualTo("IN01");
      assertThat(profilo.getTipoCorsoCod()).isEqualTo("L");
      assertThat(profilo.getAnnoCorso()).isEqualTo(3);
      assertThat(profilo.getAnnoAccademico()).isEqualTo(2022);
    }

    @Test
    @DisplayName("CinecaAuthException → propagates unchanged")
    void cinecaAuthException() {
      LoginRequest req = buildRequest();
      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(any(), any(), any()))
          .thenThrow(new CinecaAuthException("invalid credentials"));

      assertThatThrownBy(() -> authService.login(req))
          .isInstanceOf(CinecaAuthException.class);
    }

    @Test
    @DisplayName("CinecaUnavailableException → propagates unchanged")
    void cinecaUnavailableException() {
      LoginRequest req = buildRequest();
      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.of(uniConfig()));
      when(cinecaClient.login(any(), any(), any()))
          .thenThrow(new CinecaUnavailableException("cineca down"));

      assertThatThrownBy(() -> authService.login(req))
          .isInstanceOf(CinecaUnavailableException.class);
    }
  }

  // ============================================================
  // logout()
  // ============================================================

  @Nested
  @DisplayName("logout()")
  class Logout {

    @Test
    @DisplayName("valid refresh token → deleteRefreshToken and clearSession called")
    void validToken() {
      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));

      authService.logout("refresh-abc", UNI_ID);

      verify(sessionStore).deleteRefreshToken("refresh-abc");
      verify(sessionStore).clearSession(USER_ID, UNI_ID);
    }

    @Test
    @DisplayName("unknown refresh token → nothing called")
    void unknownToken() {
      when(sessionStore.getUserIdByRefreshToken("bad-token"))
          .thenReturn(Optional.empty());

      authService.logout("bad-token", UNI_ID);

      verify(sessionStore, never()).deleteRefreshToken(any());
      verify(sessionStore, never()).clearSession(any(), any());
    }
  }

  // ============================================================
  // refresh()
  // ============================================================

  @Nested
  @DisplayName("refresh()")
  class Refresh {

    @Test
    @DisplayName("valid flow → returns new access token")
    void validRefresh() {
      OmuUser user = buildMockUser();
      UniversityConnection conn = mock(UniversityConnection.class);
      when(conn.getUniversityId()).thenReturn(UNI_ID);

      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));
      when(userRepository.findById(USER_UUID)).thenReturn(Optional.of(user));
      when(sessionStore.getCinecaJwt(USER_ID, UNI_ID)).thenReturn(Optional.of("cineca-jwt"));
      when(connectionRepository.findByUserId(USER_UUID)).thenReturn(List.of(conn));
      when(jwtService.issue(any(), any(), any(), any(), any(), any()))
          .thenReturn("new-access-token");

      String result = authService.refresh("refresh-abc", UNI_ID);

      assertThat(result).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("invalid refresh token → throws IllegalArgumentException")
    void invalidRefreshToken() {
      when(sessionStore.getUserIdByRefreshToken("bad-token"))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh("bad-token", UNI_ID))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("user not found in DB → throws IllegalArgumentException")
    void userNotFound() {
      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));
      when(userRepository.findById(USER_UUID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh("refresh-abc", UNI_ID))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Cineca JWT expired in Redis → throws IllegalArgumentException")
    void cinecaJwtExpired() {
      OmuUser user = buildMockUser();

      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));
      when(userRepository.findById(USER_UUID)).thenReturn(Optional.of(user));
      when(sessionStore.getCinecaJwt(USER_ID, UNI_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh("refresh-abc", UNI_ID))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cineca session expired");
    }

    @Test
    @DisplayName("no university connection → throws IllegalArgumentException")
    void noConnection() {
      OmuUser user = buildMockUser();

      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));
      when(userRepository.findById(USER_UUID)).thenReturn(Optional.of(user));
      when(sessionStore.getCinecaJwt(USER_ID, UNI_ID)).thenReturn(Optional.of("cineca-jwt"));
      when(connectionRepository.findByUserId(USER_UUID)).thenReturn(List.of());

      assertThatThrownBy(() -> authService.refresh("refresh-abc", UNI_ID))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("No connection found");
    }
  }
}
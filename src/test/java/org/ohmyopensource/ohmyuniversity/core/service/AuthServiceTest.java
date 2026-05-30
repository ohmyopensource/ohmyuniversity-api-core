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
 * <p>All dependencies are mocked via Mockito. No Spring context, database,
 * or Redis instance is required. Helper factory methods build mock objects before any enclosing
 * {@code when()} call to avoid Mockito's {@code UnfinishedStubbingException}.
 */
class AuthServiceTest {

  /**
   * University identifier used as a stable fixture across all tests.
   */
  private static final String UNI_ID = "UNIMOL";
  /**
   * Human-readable university name used when constructing registry configs.
   */
  private static final String UNI_NAME = "Università degli Studi del Molise";
  /**
   * Cineca ESSE3 base URL associated with {@link #UNI_ID}.
   */
  private static final String BASE_URL = "https://unimol.esse3.cineca.it/e3rest/api";
  /**
   * Italian tax code used as a stable user identity fixture.
   */
  private static final String CF = "TSTXXX00A00X000X";
  /**
   * Fixed UUID representing the OhMyUniversity user across all tests.
   */
  private static final UUID USER_UUID = UUID.randomUUID();
  /**
   * String form of {@link #USER_UUID}, used wherever a user ID string is expected.
   */
  private static final String USER_ID = USER_UUID.toString();
  private CinecaClient cinecaClient;
  private CinecaSessionStore sessionStore;
  private OmuJwtService jwtService;
  private OmuUserRepository userRepository;
  private UniversityConnectionRepository connectionRepository;
  private UniversityRegistry universityRegistry;
  private AuthService authService;

  /**
   * Initialises fresh mocks and constructs a new {@link AuthService} instance before each test to
   * guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    cinecaClient = mock(CinecaClient.class);
    sessionStore = mock(CinecaSessionStore.class);
    jwtService = mock(OmuJwtService.class);
    userRepository = mock(OmuUserRepository.class);
    connectionRepository = mock(UniversityConnectionRepository.class);
    universityRegistry = mock(UniversityRegistry.class);

    authService = new AuthService(
        cinecaClient, sessionStore, jwtService,
        userRepository, connectionRepository, universityRegistry);
  }

  /**
   * Returns a {@link UniversityRegistry.UniversityConfig} for the standard test university.
   */
  private UniversityRegistry.UniversityConfig uniConfig() {
    return new UniversityRegistry.UniversityConfig(UNI_NAME, BASE_URL);
  }

  /**
   * Builds a mocked {@link LoginRequest} configured with the standard test university, username,
   * and password fixtures.
   */
  private LoginRequest buildRequest() {
    LoginRequest req = mock(LoginRequest.class);
    when(req.getUniversityId()).thenReturn(UNI_ID);
    when(req.getUsername()).thenReturn("adelmuto");
    when(req.getPassword()).thenReturn("secret");
    return req;
  }

  /**
   * Builds a mocked {@link OmuUser} identified by {@link #USER_UUID} and {@link #CF}.
   */
  private OmuUser buildMockUser() {
    OmuUser user = mock(OmuUser.class);
    when(user.getId()).thenReturn(USER_UUID);
    when(user.getCodiceFiscale()).thenReturn(CF);
    return user;
  }

  /**
   * Builds a mocked {@link CinecaLoginResponse.CinecaUser} with the provided {@code persId} and
   * {@code tratti} list.
   *
   * @param persId Cineca person identifier; may be {@code null}
   * @param tratti list of academic career segments; may be {@code null}
   * @return configured mock
   */
  private CinecaLoginResponse.CinecaUser buildCinecaUser(Long persId,
      List<CinecaLoginResponse.TrattoCarriera> tratti) {
    CinecaLoginResponse.CinecaUser u = mock(CinecaLoginResponse.CinecaUser.class);
    when(u.getCodiceFiscale()).thenReturn(CF);
    when(u.getUserId()).thenReturn("adelmuto");
    when(u.getPersId()).thenReturn(persId);
    when(u.getTrattiCarriera()).thenReturn(tratti);
    return u;
  }

  /**
   * Builds a mocked {@link CinecaLoginResponse} wrapping the provided user, JWT, and auth token
   * values.
   *
   * @param cinecaUser authenticated Cineca user
   * @param jwt        Cineca JWT; may be {@code null}
   * @param authToken  Cineca auth token; may be {@code null}
   * @return configured mock
   */
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

  /**
   * Builds a mocked {@link CinecaLoginResponse.TrattoCarriera} with the provided identifiers,
   * status codes, and optional detail block.
   *
   * @param stuId     Cineca student identifier
   * @param matId     Cineca career segment identifier
   * @param matricola student registration number
   * @param staStuCod career status code (e.g. {@code "A"} for active)
   * @param staStuDes human-readable career status description
   * @param dettaglio optional detail record; may be {@code null}
   * @return configured mock
   */
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

  /**
   * Verifies the end-to-end login flow of {@link AuthService#login(LoginRequest)}, covering user
   * creation, session storage, profilo mapping, and error propagation.
   */
  @Nested
  @DisplayName("login()")
  class Login {

    /**
     * Verifies that when the authenticating student has no existing OhMyUniversity account a new
     * {@link OmuUser} and {@link UniversityConnection} are persisted, all Cineca session tokens are
     * stored in Redis, and the response contains valid access and refresh tokens.
     */
    @Test
    @DisplayName("new user → user created, connection saved, tokens stored, response populated")
    void newUser() {
      LoginRequest req = buildRequest();
      OmuUser savedUser = buildMockUser();
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

    /**
     * Verifies that when the student already has an OhMyUniversity account {@code lastLoginAt} is
     * updated, no duplicate {@link UniversityConnection} is created, and the login flow completes
     * successfully.
     */
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
          any(), eq(UNI_ID), anyString())).thenReturn(
          Optional.of(mock(UniversityConnection.class)));
      when(jwtService.issue(any(), any(), any(), any(), any(), any())).thenReturn("access-token");
      when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

      authService.login(req);

      verify(existingUser).setLastLoginAt(any());
      verify(connectionRepository, never()).save(any(UniversityConnection.class));
    }

    /**
     * Verifies that an unregistered university identifier causes {@link AuthService#login} to throw
     * {@link IllegalArgumentException} with a message containing {@code "Unknown university"}.
     */
    @Test
    @DisplayName("unknown universityId → throws IllegalArgumentException")
    void unknownUniversity() {
      LoginRequest req = buildRequest();
      when(universityRegistry.resolve(UNI_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.login(req))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown university");
    }

    /**
     * Verifies that when Cineca returns a {@code null} JWT,
     * {@link CinecaSessionStore#storeCinecaJwt} is never invoked.
     */
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

    /**
     * Verifies that when Cineca returns a {@code null} auth token,
     * {@link CinecaSessionStore#storeCinecaAuthToken} is never invoked.
     */
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

    /**
     * Verifies that when the Cineca response contains a {@code null} {@code trattiCarriera} list
     * the resulting {@link LoginResponse} has an empty {@code profili} collection and
     * {@link OmuJwtService#issue} is called with {@code null} for all career identifiers.
     */
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

    /**
     * Verifies that a career segment with status code {@code "A"} (active) is mapped to a
     * {@link LoginResponse.ProfiloCarriera} where {@code attivo} is {@code true}.
     */
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

    /**
     * Verifies that a career segment with status code {@code "C"} (ceased) is mapped to a
     * {@link LoginResponse.ProfiloCarriera} where {@code attivo} is {@code false}.
     */
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

    /**
     * Verifies that when a career segment carries a {@link CinecaLoginResponse.DettaglioTratto} the
     * resulting {@link LoginResponse.ProfiloCarriera} is fully populated with the course code,
     * type, academic year, and enrolment year.
     */
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

    /**
     * Verifies that a {@link CinecaAuthException} thrown by {@link CinecaClient#login} propagates
     * unchanged from {@link AuthService#login}.
     */
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

    /**
     * Verifies that a {@link CinecaUnavailableException} thrown by {@link CinecaClient#login}
     * propagates unchanged from {@link AuthService#login}.
     */
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

  /**
   * Verifies the session invalidation behaviour of {@link AuthService#logout(String, String)}.
   */
  @Nested
  @DisplayName("logout()")
  class Logout {

    /**
     * Verifies that when a valid refresh token is provided both
     * {@link CinecaSessionStore#deleteRefreshToken} and {@link CinecaSessionStore#clearSession} are
     * invoked with the correct arguments.
     */
    @Test
    @DisplayName("valid refresh token → deleteRefreshToken and clearSession called")
    void validToken() {
      when(sessionStore.getUserIdByRefreshToken("refresh-abc"))
          .thenReturn(Optional.of(USER_ID));

      authService.logout("refresh-abc", UNI_ID);

      verify(sessionStore).deleteRefreshToken("refresh-abc");
      verify(sessionStore).clearSession(USER_ID, UNI_ID);
    }

    /**
     * Verifies that when the refresh token is not recognised no session data is touched — neither
     * deletion nor session clearing is attempted.
     */
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

  /**
   * Verifies the token rotation behaviour of {@link AuthService#refresh(String, String)}, covering
   * the happy path and all early-exit failure conditions.
   */
  @Nested
  @DisplayName("refresh()")
  class Refresh {

    /**
     * Verifies that when all session data is present and valid {@link AuthService#refresh} returns
     * a new access token issued by {@link OmuJwtService}.
     */
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

    /**
     * Verifies that an unrecognised refresh token causes {@link AuthService#refresh} to throw
     * {@link IllegalArgumentException} with a message containing
     * {@code "Invalid or expired refresh token"}.
     */
    @Test
    @DisplayName("invalid refresh token → throws IllegalArgumentException")
    void invalidRefreshToken() {
      when(sessionStore.getUserIdByRefreshToken("bad-token"))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> authService.refresh("bad-token", UNI_ID))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid or expired refresh token");
    }

    /**
     * Verifies that when the user identified by the refresh token no longer exists in the database
     * {@link AuthService#refresh} throws {@link IllegalArgumentException} with a message containing
     * {@code "User not found"}.
     */
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

    /**
     * Verifies that when the Cineca JWT has expired or been evicted from Redis
     * {@link AuthService#refresh} throws {@link IllegalArgumentException} with a message containing
     * {@code "Cineca session expired"}.
     */
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

    /**
     * Verifies that when no {@link UniversityConnection} exists for the user
     * {@link AuthService#refresh} throws {@link IllegalArgumentException} with a message containing
     * {@code "No connection found"}.
     */
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
package org.ohmyopensource.ohmyuniversity.core.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaLoginResponse;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaLoginResponse.TrattoCarriera;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginRequest;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse.ProfiloCarriera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing the authentication flow of OhMyUniversity.
 *
 * <p>It handles:
 * - Cineca authentication against ESSE3
 * - User provisioning and persistence
 * - University connection management
 * - Cineca session storage (JWT + auth token)
 * - OhMyU JWT access/refresh token issuance
 *
 * <p>This service is a critical entry point for the system authentication layer and coordinates
 * both external (Cineca) and internal (OhMyU) identity contexts.
 */
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final CinecaClient cinecaClient;
  private final CinecaSessionStore sessionStore;
  private final OmuJwtService jwtService;
  private final OmuUserRepository userRepository;
  private final UniversityConnectionRepository connectionRepository;
  private final UniversityRegistry universityRegistry;

  // ============ Constructor ============

  /**
   * Constructs the authentication service and injects all required dependencies.
   *
   * <p>This service depends on:
   * - CinecaClient for external ESSE3 authentication
   * - CinecaSessionStore for Redis-based session persistence
   * - OmuJwtService for JWT creation and validation
   * - OmuUserRepository for user persistence
   * - UniversityConnectionRepository for Cineca account mapping
   * - UniversityRegistry for resolving university configurations
   *
   * <p>All dependencies are required and must be provided by the Spring context.
   */
  public AuthService(
      CinecaClient cinecaClient,
      CinecaSessionStore sessionStore,
      OmuJwtService jwtService,
      OmuUserRepository userRepository,
      UniversityConnectionRepository connectionRepository,
      UniversityRegistry universityRegistry) {
    this.cinecaClient = cinecaClient;
    this.sessionStore = sessionStore;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
    this.connectionRepository = connectionRepository;
    this.universityRegistry = universityRegistry;
  }

  // ============ Class Methods ============

  /**
   * Authenticates a user against Cineca ESSE3 and initializes an OhMyUniversity session.
   *
   * @param request login credentials and university identifier
   * @return login response containing access token, refresh token and career profiles
   * @throws CinecaClient.CinecaAuthException        if credentials are invalid
   * @throws CinecaClient.CinecaUnavailableException if Cineca ESSE3 is unreachable
   * @throws IllegalArgumentException                if the universityId is not registered in the
   *                                                 system
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    UniversityRegistry.UniversityConfig uniConfig =
        universityRegistry.resolve(request.getUniversityId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown university: " + request.getUniversityId()));

    CinecaLoginResponse cinecaResponse = cinecaClient.login(
        uniConfig.baseUrl(),
        request.getUsername(),
        request.getPassword());

    CinecaLoginResponse.CinecaUser cinecaUser = cinecaResponse.getUser();
    String codiceFiscale = cinecaUser.getCodiceFiscale();

    log.info("AuthService: login successful for user='{}' at university='{}'",
        cinecaUser.getUserId(), request.getUniversityId());

    OmuUser omuUser = userRepository.findByCodiceFiscale(codiceFiscale)
        .orElseGet(() -> {
          OmuUser newUser = new OmuUser();
          newUser.setCodiceFiscale(codiceFiscale);
          return userRepository.save(newUser);
        });
    omuUser.setLastLoginAt(Instant.now());
    userRepository.save(omuUser);

    connectionRepository.findByUserIdAndUniversityIdAndUsernameCineca(
            omuUser.getId(), request.getUniversityId(), request.getUsername())
        .ifPresentOrElse(
            existing -> {
            },
            () -> {
              UniversityConnection conn = new UniversityConnection();
              conn.setUser(omuUser);
              conn.setUniversityId(request.getUniversityId());
              conn.setUniversityName(uniConfig.name());
              conn.setCinecaBaseUrl(uniConfig.baseUrl());
              conn.setUsernameCineca(request.getUsername());
              connectionRepository.save(conn);
            });

    String omuUserId = omuUser.getId().toString();
    if (cinecaResponse.getJwt() != null) {
      sessionStore.storeCinecaJwt(omuUserId, request.getUniversityId(),
          cinecaResponse.getJwt());
    }
    if (cinecaResponse.getAuthToken() != null) {
      sessionStore.storeCinecaAuthToken(omuUserId, request.getUniversityId(),
          cinecaResponse.getAuthToken());
    }
    if (cinecaUser.getPersId() != null) {
      sessionStore.storeCinecaPersId(omuUserId, request.getUniversityId(),
          cinecaUser.getPersId());
    }

    List<TrattoCarriera> tratti = cinecaUser.getTrattiCarriera();
    List<ProfiloCarriera> profili = tratti == null ? List.of() : tratti.stream()
        .map(t -> toProfiloCarriera(t, request.getUniversityId(), uniConfig.name()))
        .toList();

    TrattoCarriera defaultTratte = tratti != null && !tratti.isEmpty() ? tratti.get(0) : null;

    String accessToken = jwtService.issue(
        omuUserId,
        codiceFiscale,
        request.getUniversityId(),
        defaultTratte != null ? defaultTratte.getStuId() : null,
        defaultTratte != null ? defaultTratte.getMatId() : null,
        defaultTratte != null ? defaultTratte.getMatricola() : null);

    String refreshToken = jwtService.generateRefreshToken();
    sessionStore.storeRefreshToken(refreshToken, omuUserId);

    LoginResponse response = new LoginResponse();
    response.setAccessToken(accessToken);
    response.setRefreshToken(refreshToken);
    response.setProfili(profili);

    return response;
  }

  /**
   * Logs out the user by invalidating refresh token and clearing Cineca session data.
   *
   * <p>This method ensures:
   * - refresh token invalidation
   * - removal of Cineca session data from Redis
   *
   * @param refreshToken refresh token to invalidate
   * @param universityId university context to clear session for
   */
  public void logout(String refreshToken, String universityId) {
    sessionStore.getUserIdByRefreshToken(refreshToken).ifPresent(omuUserId -> {
      sessionStore.deleteRefreshToken(refreshToken);
      sessionStore.clearSession(omuUserId, universityId);
      log.info("AuthService: logout for user={} university={}", omuUserId, universityId);
    });
  }

  /**
   * Issues a new access token using a valid refresh token. If the Cineca JWT is still valid in
   * Redis it is reused. If it is expired, the user must re-login to get a fresh Cineca session.
   *
   * <p>The method:
   * - validates refresh token
   * - verifies user existence
   * - checks Cineca session validity in Redis
   * - issues a new JWT access token
   *
   * @param refreshToken refresh token used for session renewal
   * @param universityId university context
   * @return new OhMyU access token
   * @throws IllegalArgumentException if refresh token is invalid or expired
   * @throws IllegalArgumentException if user is not found
   * @throws IllegalArgumentException if Cineca session has expired
   * @throws IllegalArgumentException if no university connection exists
   */
  @Transactional(readOnly = true)
  public String refresh(String refreshToken, String universityId) {
    String omuUserId = sessionStore.getUserIdByRefreshToken(refreshToken)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

    OmuUser omuUser = userRepository.findById(java.util.UUID.fromString(omuUserId))
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + omuUserId));

    Optional<String> cinecaJwt = sessionStore.getCinecaJwt(omuUserId, universityId);
    if (cinecaJwt.isEmpty()) {
      log.warn("AuthService: Cineca JWT expired for user={} — re-login required", omuUserId);
      throw new IllegalArgumentException(
          "Cineca session expired — please log in again");
    }

    boolean connectionExists = connectionRepository
        .findByUserId(omuUser.getId())
        .stream()
        .anyMatch(c -> c.getUniversityId().equals(universityId));

    if (!connectionExists) {
      throw new IllegalArgumentException(
          "No connection found for university: " + universityId);
    }

    return jwtService.issue(
        omuUserId,
        omuUser.getCodiceFiscale(),
        universityId,
        null, null, null);
  }

  /**
   * Maps a Cineca career profile (TrattoCarriera) into the internal API DTO.
   *
   * <p>This method normalizes raw ESSE3 data into a stable representation used by the frontend,
   * enriching it with:
   * - university context
   * - activation state
   * - optional course metadata (when available)
   *
   * @param t              Cineca career segment from ESSE3
   * @param universityId   university identifier
   * @param universityName human-readable university name
   * @return normalized career profile DTO
   */
  private ProfiloCarriera toProfiloCarriera(
      TrattoCarriera t,
      String universityId,
      String universityName) {

    ProfiloCarriera p = new ProfiloCarriera();
    p.setUniversityId(universityId);
    p.setUniversityName(universityName);
    p.setStuId(t.getStuId());
    p.setMatId(t.getMatId());
    p.setMatricola(t.getMatricola());
    p.setCorsoNome(t.getCdsDes());
    p.setStatusStudente(t.getStaStuCod());
    p.setStatusDescrizione(t.getStaStuDes());
    p.setAttivo("A".equals(t.getStaStuCod()));

    if (t.getDettaglioTratto() != null) {
      p.setCorsoCodice(t.getDettaglioTratto().getCdsCod());
      p.setTipoCorsoCod(t.getDettaglioTratto().getTipoCorsoCod());
      p.setAnnoCorso(t.getDettaglioTratto().getAnnoCorso());
      p.setDurataAnni(t.getDettaglioTratto().getDurataAnni());
      p.setAnnoAccademico(t.getDettaglioTratto().getAaIscrId());
      p.setCdsId(t.getCdsId());
    }

    return p;
  }
}
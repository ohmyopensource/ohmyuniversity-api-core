package org.ohmyopensource.ohmyuniversity.core.service.kafka;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaCarriera;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CachedProfiloCarriera;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.CachedProfiloCarrieraRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.OmuUserRepository;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.service.CinecaSyncService;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.UserAuthenticatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Consumes {@code user.authenticated} events published by the auth service, and
 * performs everything that used to happen inline inside the core service's own login
 * flow before authentication was extracted into its own microservice: keeping a local
 * shadow of the user identity (needed for foreign keys on
 * {@code cached_profilo_carriera}, {@code cineca_sync_state}, and the calendar tables,
 * all of which live in this service's own database — each service owns its own schema
 * and cannot reference a table in another service's database), refreshing the cached
 * career profile, and triggering the Cineca transcript sync.
 *
 * <p>Failures here are logged and swallowed, never rethrown: this listener runs after
 * the user's login has already succeeded and the auth service has already issued a
 * valid session. A failure to enrich career data or sync the transcript must not
 * surface as an authentication problem — at worst, cached data stays stale until the
 * next login.
 */
@Service
public class UserAuthenticatedEventListener {

  private static final Logger log = LoggerFactory.getLogger(UserAuthenticatedEventListener.class);

  private final OmuUserRepository userRepository;
  private final CachedProfiloCarrieraRepository cachedProfiloRepository;
  private final CinecaProfileClient cinecaProfileClient;
  private final CinecaSyncService cinecaSyncService;
  private final UniversityConnectionRepository connectionRepository;

  public UserAuthenticatedEventListener(
      OmuUserRepository userRepository,
      CachedProfiloCarrieraRepository cachedProfiloRepository,
      CinecaProfileClient cinecaProfileClient,
      CinecaSyncService cinecaSyncService,
      UniversityConnectionRepository connectionRepository) {
    this.userRepository = userRepository;
    this.cachedProfiloRepository = cachedProfiloRepository;
    this.cinecaProfileClient = cinecaProfileClient;
    this.cinecaSyncService = cinecaSyncService;
    this.connectionRepository = connectionRepository;
  }

  /**
   * Handles a single {@code user.authenticated} event.
   *
   * @param event the login notification published by the auth service
   */
  @KafkaListener(topics = "user.authenticated", groupId = "core-service")
  @Transactional
  public void onUserAuthenticated(UserAuthenticatedEvent event) {
    String safeUserId = event.omuUserId().replaceAll("[\r\n]", "");
    String safeUniId = event.universityId().replaceAll("[\r\n]", "");

    log.debug("UserAuthenticatedEventListener: handling login for user={} university={}",
        safeUserId, safeUniId);

    try {
      OmuUser user = upsertUserShadow(event);
      upsertConnectionShadow(user, event);
      refreshCareerCache(user, event);
      triggerTranscriptSync(user, event);
    } catch (Exception e) {
      log.error("UserAuthenticatedEventListener: failed to process login event for user={} "
          + "university={}: {}", safeUserId, safeUniId, e.getMessage());
    }
  }

  /**
   * Ensures a local shadow row exists for this user, matching the same UUID the auth
   * service already assigned. The core service does not create or own user identity —
   * it only needs a local row to satisfy its own foreign key constraints.
   *
   * @param event the login notification, providing the user id and fiscal code
   * @return the existing or newly-created local shadow row
   */
  private OmuUser upsertUserShadow(UserAuthenticatedEvent event) {
    UUID userId = UUID.fromString(event.omuUserId());
    return userRepository.findById(userId).orElseGet(() -> {
      OmuUser user = new OmuUser();
      user.setId(userId);
      user.setCodiceFiscale(event.codiceFiscale());
      userRepository.save(user);
      log.info("UserAuthenticatedEventListener: created local user shadow for id={}", userId);
      return user;
    });
  }

  /**
   * Refreshes the cached career profile for this user/university, calling
   * carriere-service for fields not present in the login event (e.g. {@code attlauFlg}).
   * Falls back to a single-row cache entry built from the event's own default career
   * fields if the vendor token is absent or the enrichment call fails — degraded but
   * still usable, rather than leaving the user with no cached profile at all.
   *
   * @param user  the local user shadow row
   * @param event the login notification
   */
  private void refreshCareerCache(OmuUser user, UserAuthenticatedEvent event) {
    List<CinecaCarriera> carriere = List.of();
    if (event.vendorToken() != null) {
      try {
        carriere = cinecaProfileClient.getAllCarriere(event.vendorBaseUrl(), event.vendorToken());
      } catch (Exception e) {
        log.warn("UserAuthenticatedEventListener: could not fetch carriere-service for user={}, "
            + "falling back to the event's default career only", user.getId());
      }
    }

    if (!carriere.isEmpty()) {
      for (CinecaCarriera c : carriere) {
        upsertCachedProfilo(user, event, c);
      }
    } else if (event.defaultStuId() != null) {
      upsertCachedProfiloFromEventDefaults(user, event);
    }
  }

  /**
   * Upserts a single cached career profile row from a Cineca carriera entry.
   *
   * @param user  the local user shadow row
   * @param event the login notification, providing university context
   * @param c     the Cineca career entry to cache
   */
  private void upsertCachedProfilo(OmuUser user, UserAuthenticatedEvent event, CinecaCarriera c) {
    CachedProfiloCarriera cached = cachedProfiloRepository
        .findByUserIdAndStuId(user.getId(), c.getStuId())
        .orElseGet(CachedProfiloCarriera::new);
    cached.setUser(user);
    cached.setUniversityId(event.universityId());
    cached.setUniversityName(event.universityName());
    cached.setStuId(c.getStuId());
    cached.setMatId(c.getMatId());
    cached.setMatricola(c.getMatricola());
    cached.setCorsoNome(c.getCdsDes());
    cached.setCorsoCodice(c.getCdsCod());
    cached.setCdsId(c.getCdsId());
    cached.setTipoCorsoCod(c.getTipoCorsoCod());
    cached.setStatusStudente(c.getStaStuCod());
    cached.setStatusDescr(c.getStatiStuDes());
    cached.setAnnoCorso(c.getAnnoCorso());
    cached.setDurataAnni(c.getDurataCorso());
    cached.setAnnoAccademico(c.getAaIscrId());
    cached.setAttivo("A".equals(c.getStaStuCod()));
    cached.setLaureato(c.getAttlauFlg() != null && c.getAttlauFlg() == 1);
    cachedProfiloRepository.save(cached);
  }

  /**
   * Upserts a single cached career profile row using only the login event's own
   * default career fields, as a degraded fallback when carriere-service could not
   * be reached or the vendor token is absent.
   *
   * @param user  the local user shadow row
   * @param event the login notification, providing the default career fields
   */
  private void upsertCachedProfiloFromEventDefaults(OmuUser user, UserAuthenticatedEvent event) {
    CachedProfiloCarriera cached = cachedProfiloRepository
        .findByUserIdAndStuId(user.getId(), event.defaultStuId())
        .orElseGet(CachedProfiloCarriera::new);
    cached.setUser(user);
    cached.setUniversityId(event.universityId());
    cached.setUniversityName(event.universityName());
    cached.setStuId(event.defaultStuId());
    cached.setMatId(event.defaultMatId());
    cached.setMatricola(event.defaultMatricola());
    cached.setAnnoAccademico(event.defaultAaIscrId());
    cached.setAttivo(true);
    cachedProfiloRepository.save(cached);
  }

  /**
   * Triggers the async Cineca transcript sync, same guard condition as the old inline
   * login flow — requires both a vendor token and a default career segment.
   *
   * <p>The actual {@code syncAfterLogin} call is deferred until this listener's own
   * transaction has committed, via {@link TransactionSynchronizationManager}. Without
   * this, the {@code @Async} sync thread starts immediately and races the commit —
   * its own transaction opens before the user/connection/cache rows written here are
   * visible outside this transaction, so it looks up the user and finds nothing.
   *
   * @param user  the local user shadow row
   * @param event the login notification
   */
  private void triggerTranscriptSync(OmuUser user, UserAuthenticatedEvent event) {
    if (event.vendorToken() == null || event.defaultMatId() == null) {
      return;
    }
    String academicYear = event.defaultAaIscrId() != null
        ? String.valueOf(event.defaultAaIscrId())
        : String.valueOf(Year.now().getValue());

    String omuUserId = user.getId().toString();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              cinecaSyncService.syncAfterLogin(
                  omuUserId,
                  event.universityId(),
                  event.vendorToken(),
                  event.defaultMatId(),
                  event.vendorBaseUrl(),
                  academicYear);
            }
          });
    } else {
      cinecaSyncService.syncAfterLogin(
          omuUserId, event.universityId(), event.vendorToken(), event.defaultMatId(),
          event.vendorBaseUrl(), academicYear);
    }
  }

  /**
   * Ensures a local shadow row exists in university_connection, matching what the
   * old inline login flow used to create here directly. Needed because
   * AbstractEsse3Service.resolveUsername() reads this table locally for Basic Auth
   * calls (e.g. calesa-service bookings) — without this shadow, that table would stay
   * permanently empty now that the auth service owns the real write, in its own
   * database.
   *
   * @param user  the local user shadow row
   * @param event the login notification, providing university and username context
   */
  private void upsertConnectionShadow(OmuUser user, UserAuthenticatedEvent event) {
    connectionRepository
        .findByUserIdAndUniversityIdAndUsernameVendor(
            user.getId(), event.universityId(), event.username())
        .ifPresentOrElse(
            existing -> {
            },
            () -> {
              UniversityConnection connection = new UniversityConnection();
              connection.setUser(user);
              connection.setUniversityId(event.universityId());
              connection.setUniversityName(event.universityName());
              connection.setVendorBaseUrl(event.vendorBaseUrl());
              connection.setUsernameVendor(event.username());
              connectionRepository.save(connection);
            });
  }
}
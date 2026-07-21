package org.ohmyopensource.ohmyuniversity.core.service.kafka.event;

import java.time.Instant;

/**
 * Consumer-side payload of the {@code user.authenticated} Kafka event, published by the auth
 * service after a successful login.
 *
 * <p>Shape must match the producer record in {@code ohmyuniversity-auth}
 * ({@code UserAuthenticatedEvent}) exactly, as Jackson deserializes by field name without type
 * headers — same convention as {@code EnrollmentDiscoveredEvent}.
 *
 * <p>This is a fire-and-forget notification, not a command: if it is lost or arrives
 * late, the user's login already succeeded independently in the auth service — this only means the
 * local career cache and Cineca sync state go stale until the next successful delivery, not that
 * anything is broken.
 *
 * @param omuUserId        internal user identifier, shared identity key across services
 * @param codiceFiscale    the user's Italian fiscal code, used to provision the local omu_user
 *                         shadow row (its NOT NULL constraint requires this)
 * @param universityId     university tenant identifier
 * @param universityName   human-readable university name
 * @param vendorBaseUrl    platform vendor API base URL for this university
 * @param vendorToken      vendor-issued token (e.g. Cineca JWT), reused to fetch academic data
 * @param username         vendor username, used to provision the local university_connection shadow
 *                         row
 * @param defaultStuId     the career selected as active at login time, if any
 * @param defaultMatId     the career segment selected as active at login time, if any
 * @param defaultMatricola the student registration number of the default career, if any
 * @param defaultAaIscrId  enrollment academic year of the default career, if known
 * @param occurredAt       timestamp of the login event
 */
public record UserAuthenticatedEvent(
    String omuUserId,
    String codiceFiscale,
    String universityId,
    String universityName,
    String vendorBaseUrl,
    String vendorToken,
    String username,
    Long defaultStuId,
    Long defaultMatId,
    String defaultMatricola,
    Integer defaultAaIscrId,
    Instant occurredAt) {

}
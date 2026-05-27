package org.ohmyopensource.ohmyuniversity.core.config;

/**
 * Typed representation of the OhMyUniversity JWT claims,
 * stored as the authentication principal in the Spring Security context.
 */
public record OmuPrincipal(
    String omuUserId,
    String codiceFiscale,
    String universityId,
    Long stuId,
    Long matId,
    String matricola) {
}
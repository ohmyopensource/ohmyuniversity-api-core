package org.ohmyopensource.ohmyuniversity.core.config;

/**
 * Immutable representation of the authenticated OhMyUniversity user.
 *
 * This record is used as the Spring Security principal and is populated
 * from validated JWT claims.
 *
 * It provides a typed, structured view of the authentication context,
 * including both global identity and university-specific academic data.
 *
 * This principal is intentionally lightweight and contains only the
 * minimum information required to:
 * - identify the user
 * - resolve the active university context
 * - access the correct Cineca academic profile
 *
 * No sensitive credentials or session tokens are stored here.
 */
public record OmuPrincipal(
    String omuUserId,
    String codiceFiscale,
    String universityId,
    Long stuId,
    Long matId,
    String matricola) {
}
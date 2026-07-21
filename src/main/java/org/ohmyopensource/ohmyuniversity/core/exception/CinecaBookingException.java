package org.ohmyopensource.ohmyuniversity.core.exception;

/**
 * Thrown when Cineca rejects an exam booking with a business-rule error (4xx).
 *
 * <p>Carries the raw Cineca error message (e.g. "Non risulta compilato il
 * questionario di valutazione della didattica") so it can be surfaced to the user unchanged.
 */
public class CinecaBookingException extends RuntimeException {

  public CinecaBookingException(String message) {
    super(message);
  }
}

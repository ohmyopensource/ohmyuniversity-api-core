package org.ohmyopensource.ohmyuniversity.core.exception;

/**
 * Thrown when Cineca ESSE3 is unreachable or returns 5xx errors.
 */
public class CinecaUnavailableException extends RuntimeException {

  public CinecaUnavailableException(String message) {
    super(message);
  }
}
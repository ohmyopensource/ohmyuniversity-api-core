package org.ohmyopensource.ohmyuniversity.core.exception;

/**
 * Thrown when Cineca rejects authentication or a session token is invalid/expired.
 */
public class CinecaAuthException extends RuntimeException {

  public CinecaAuthException(String message) {
    super(message);
  }
}
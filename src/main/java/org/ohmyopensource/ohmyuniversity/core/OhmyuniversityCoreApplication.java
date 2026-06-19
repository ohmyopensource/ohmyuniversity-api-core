package org.ohmyopensource.ohmyuniversity.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the OhMyUniversity core microservice.
 *
 * <p>Bootstraps the Spring Boot application and initializes the full application context, including
 * security, persistence, and external integrations (Cineca clients, Redis, Kafka, etc.).
 */
@SpringBootApplication
@EnableAsync
public class OhmyuniversityCoreApplication {

  /**
   * Main method that starts the Spring Boot application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(OhmyuniversityCoreApplication.class, args);
  }

}

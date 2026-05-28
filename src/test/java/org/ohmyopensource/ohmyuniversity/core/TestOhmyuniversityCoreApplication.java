package org.ohmyopensource.ohmyuniversity.core;

import org.springframework.boot.SpringApplication;

/**
 * Test entry point for running the Spring Boot application with
 * Testcontainers support enabled.
 *
 * <p>This class bootstraps the main application context using the
 * Testcontainers configuration instead of production infrastructure.
 *
 * <p>It is used for local integration testing and debugging scenarios
 * where full application startup is required with real containerized
 * dependencies (PostgreSQL, Kafka, Redis).
 */
public class TestOhmyuniversityCoreApplication {

  /**
   * Starts the application using Spring Boot's test runner
   * with Testcontainers enabled.
   *
   * @param args command-line arguments passed to the test runtime
   */
  public static void main(String[] args) {
    SpringApplication.from(OhmyuniversityCoreApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }

}

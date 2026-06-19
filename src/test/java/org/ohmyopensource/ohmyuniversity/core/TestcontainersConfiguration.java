package org.ohmyopensource.ohmyuniversity.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration used for integration tests.
 *
 * <p>Spins up required infrastructure dependencies (Kafka, PostgreSQL, Redis)
 * inside Docker containers during test execution.
 *
 * <p>This ensures tests run in an isolated and reproducible environment,
 * independent of local or CI infrastructure setup.
 *
 * <p>Note: {@code apache/kafka:4.0.0} is used instead of {@code kafka-native}
 * because the native image causes segfaults on GitHub Actions CI runners.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  /**
   * Kafka container used for integration tests requiring event streaming.
   *
   * @return running KafkaContainer instance managed by Testcontainers
   */
  @Bean
  @ServiceConnection
  KafkaContainer kafkaContainer() {
    return new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));
  }

  /**
   * PostgreSQL container used as the primary relational database
   * during integration tests.
   *
   * @return running PostgreSQLContainer instance managed by Testcontainers
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
  }

  /**
   * Redis container used for caching and session storage in tests.
   *
   * <p>Exposes default Redis port (6379) for Spring Boot autoconfiguration.
   *
   * @return running Redis GenericContainer instance
   */
  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);
  }
}
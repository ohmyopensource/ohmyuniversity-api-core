package org.ohmyopensource.ohmyuniversity.core.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Spring EnvironmentPostProcessor responsible for loading environment variables from a local .env
 * file into the Spring Environment.
 *
 * <p>This component runs before the application context is initialized, ensuring that placeholders
 * defined in application.yaml (e.g. ${JWT_SECRET}) can be resolved correctly at startup time.
 *
 * <p>The .env file is considered optional:
 * - In local development, it provides convenience for configuration.
 * - In production/container environments (Docker, ECS, EKS), values are expected to be injected via
 * the runtime environment instead.
 *
 * <p>Any missing or malformed .env file is ignored silently to avoid blocking application startup.
 */
public class DotenvLoader implements EnvironmentPostProcessor, Ordered {

  private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

  /**
   * List of environment keys explicitly loaded from the .env file.
   *
   * <p>These keys define the core runtime configuration required by the application, including
   * database, cache, messaging, and security settings.
   */
  private static final String[] ENV_KEYS = {
      "SPRING_PROFILES_ACTIVE",
      "POSTGRES_URL",
      "POSTGRES_USERNAME",
      "POSTGRES_PASSWORD",
      "REDIS_CACHE_HOST",
      "REDIS_CACHE_PORT",
      "REDIS_CACHE_PASSWORD",
      "KAFKA_BOOTSTRAP_SERVERS",
      "JWT_SECRET",
      "JWT_EXPIRATION_MS",
      "JWT_REFRESH_EXPIRATION_MS"
  };

  // ============ Override Methods ============

  /**
   * Defines loading priority of this environment processor.
   *
   * <p>A high precedence ensures .env values are available early in the Spring bootstrap lifecycle.
   */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  /**
   * Loads .env variables and injects them into the Spring Environment.
   *
   * <p>The process:
   * - loads .env file from working directory
   * - extracts predefined keys
   * - merges them into Spring PropertySources with highest priority
   *
   * <p>Failures are intentionally ignored to keep the application resilient in environments
   * where .env is not present.
   */
  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment,
      SpringApplication application) {

    try {
      Dotenv dotenv = Dotenv.configure()
          .directory(System.getProperty("user.dir"))
          .ignoreIfMissing()
          .ignoreIfMalformed()
          .load();

      Properties props = new Properties();

      for (String key : ENV_KEYS) {
        try {
          String value = dotenv.get(key);
          props.put(key, value);
        } catch (Exception ignored) {
          // Missing key is intentionally ignored
        }
      }

      if (!props.isEmpty()) {
        environment.getPropertySources()
            .addFirst(new PropertiesPropertySource(DOTENV_PROPERTY_SOURCE_NAME, props));
      }

    } catch (Exception e) {
      // .env loading failure is non-blocking by design
    }
  }
}
package org.ohmyopensource.ohmyuniversity.core.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Loads environment variables from .env file into Spring's property sources.
 * Runs before the application context is initialized so that ${VAR} placeholders
 * in application.yaml are resolved correctly.
 *
 *The .env file is optional — in containerized environments (ECS, EKS)
 * variables are injected directly by the runtime. This loader silently ignores
 * a missing or malformed .env file.
 */
public class DotenvLoader implements EnvironmentPostProcessor, Ordered {

  private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

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

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

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
          // Key not present in .env — will fall back to system env or yaml default
        }
      }

      if (!props.isEmpty()) {
        environment.getPropertySources()
            .addFirst(new PropertiesPropertySource(DOTENV_PROPERTY_SOURCE_NAME, props));
      }

    } catch (Exception e) {
      // Silently ignore — .env is optional in containerized environments
    }
  }
}
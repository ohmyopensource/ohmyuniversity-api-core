package org.ohmyopensource.ohmyuniversity.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for the Spring Boot application context.
 *
 * <p>Verifies that the entire application context loads correctly
 * without throwing configuration or dependency injection errors.
 *
 * <p>This test is typically used as a baseline check to ensure that
 * the Spring configuration is valid.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OhmyuniversityCoreApplicationTests {

  /**
   * Ensures that the Spring application context loads successfully.
   *
   * <p>This is a minimal sanity check test that will fail if the
   * application context cannot be started due to misconfiguration,
   * missing beans, or broken dependency injection.
   */
  @Test
  void contextLoads() {
  }

}

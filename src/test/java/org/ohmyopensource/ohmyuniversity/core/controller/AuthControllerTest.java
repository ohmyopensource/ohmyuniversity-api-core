package org.ohmyopensource.ohmyuniversity.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaAuthException;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient.CinecaUnavailableException;
import org.ohmyopensource.ohmyuniversity.core.config.JwtAuthenticationFilter;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.LoginResponse.ProfiloCarriera;
import org.ohmyopensource.ohmyuniversity.core.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc slice tests for {@link AuthController}.
 *
 * <p>The Spring Security filter chain is disabled via
 * {@link AutoConfigureMockMvc#addFilters()} so that only the controller layer is exercised.
 * {@link AuthService} is mocked to isolate HTTP behaviour from business logic.
 *
 * <p>{@link JwtAuthenticationFilter} is declared as a {@link MockitoBean}
 * to prevent Spring from attempting to wire its {@code OmuJwtService} dependency within the MVC
 * slice context.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  /**
   * Mocked to isolate the controller layer from authentication business logic.
   */
  @MockitoBean
  private AuthService authService;

  /**
   * Mocked to satisfy the component scan within the MVC slice context, preventing Spring from
   * attempting to resolve its transitive dependencies.
   */
  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  /**
   * Verifies the HTTP contract of {@code POST /api/auth/login} across successful authentication,
   * credential failures, and validation errors.
   */
  @Nested
  @DisplayName("POST /api/auth/login")
  class Login {

    /**
     * Verifies that a successful login returns {@code 200 OK} with a populated {@code accessToken},
     * {@code refreshToken}, and {@code profili} array.
     */
    @Test
    @DisplayName("returns 200 with tokens and profili on successful login")
    void login_returns200() throws Exception {
      ProfiloCarriera profilo = new ProfiloCarriera();
      profilo.setUniversityId("UNIMOL");
      profilo.setStuId(89486L);
      profilo.setMatId(106279L);
      profilo.setMatricola("178026");
      profilo.setCorsoNome("INFORMATICA");
      profilo.setCdsId(10018L);

      LoginResponse response = new LoginResponse();
      response.setAccessToken("fake.access.token");
      response.setRefreshToken("fakerefreshtoken");
      response.setProfili(List.of(profilo));

      when(authService.login(any())).thenReturn(response);

      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "universityId": "UNIMOL",
                    "username": "a.delmuto",
                    "password": "secret"
                  }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("fake.access.token"))
          .andExpect(jsonPath("$.refreshToken").value("fakerefreshtoken"))
          .andExpect(jsonPath("$.profili[0].universityId").value("UNIMOL"))
          .andExpect(jsonPath("$.profili[0].stuId").value(89486))
          .andExpect(jsonPath("$.profili[0].cdsId").value(10018));
    }

    /**
     * Verifies that {@code POST /api/auth/login} returns {@code 401 Unauthorized} when
     * {@link AuthService#login} throws {@link CinecaAuthException}, indicating invalid Cineca
     * credentials.
     */
    @Test
    @DisplayName("returns 401 when Cineca credentials are invalid")
    void login_returns401OnInvalidCredentials() throws Exception {
      when(authService.login(any()))
          .thenThrow(new CinecaAuthException("Invalid credentials"));

      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "universityId": "UNIMOL",
                    "username": "a.delmuto",
                    "password": "wrong"
                  }
                  """))
          .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that {@code POST /api/auth/login} returns {@code 503 Service Unavailable} when
     * {@link AuthService#login} throws {@link CinecaUnavailableException}, indicating that the
     * Cineca ESSE3 service cannot be reached.
     */
    @Test
    @DisplayName("returns 503 when Cineca is unavailable")
    void login_returns503WhenCinecaDown() throws Exception {
      when(authService.login(any()))
          .thenThrow(new CinecaUnavailableException("Cineca down"));

      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "universityId": "UNIMOL",
                    "username": "a.delmuto",
                    "password": "secret"
                  }
                  """))
          .andExpect(status().isServiceUnavailable());
    }

    /**
     * Verifies that {@code POST /api/auth/login} returns {@code 404 Not Found} when
     * {@link AuthService#login} throws {@link IllegalArgumentException}, indicating that the
     * requested university identifier is not registered.
     */
    @Test
    @DisplayName("returns 404 when universityId is not registered")
    void login_returns404OnUnknownUniversity() throws Exception {
      when(authService.login(any()))
          .thenThrow(new IllegalArgumentException("Unknown university: UNKNOWN"));

      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "universityId": "UNKNOWN",
                    "username": "a.delmuto",
                    "password": "secret"
                  }
                  """))
          .andExpect(status().isNotFound());
    }

    /**
     * Verifies that {@code POST /api/auth/login} returns {@code 400 Bad Request} when the request
     * body is missing one or more fields required by bean validation.
     */
    @Test
    @DisplayName("returns 400 when request body is missing required fields")
    void login_returns400OnMissingFields() throws Exception {
      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/auth/refresh} for both valid and invalid or
   * expired refresh tokens.
   */
  @Nested
  @DisplayName("POST /api/auth/refresh")
  class Refresh {

    /**
     * Verifies that {@code POST /api/auth/refresh} returns {@code 200 OK} with the new access token
     * in the response body when a valid refresh token is provided.
     */
    @Test
    @DisplayName("returns 200 with new access token on valid refresh token")
    void refresh_returns200() throws Exception {
      when(authService.refresh("validtoken", "UNIMOL"))
          .thenReturn("new.access.token");

      mockMvc.perform(post("/api/auth/refresh")
              .param("refreshToken", "validtoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isOk())
          .andExpect(content().string("new.access.token"));
    }

    /**
     * Verifies that {@code POST /api/auth/refresh} returns {@code 401 Unauthorized} when
     * {@link AuthService#refresh} throws {@link IllegalArgumentException}, indicating that the
     * refresh token is invalid or has expired.
     */
    @Test
    @DisplayName("returns 401 when refresh token is invalid or expired")
    void refresh_returns401OnInvalidToken() throws Exception {
      when(authService.refresh("expiredtoken", "UNIMOL"))
          .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

      mockMvc.perform(post("/api/auth/refresh")
              .param("refreshToken", "expiredtoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isUnauthorized());
    }
  }

  /**
   * Verifies the HTTP contract of {@code POST /api/auth/logout}, including its idempotent behaviour
   * when the token does not exist.
   */
  @Nested
  @DisplayName("POST /api/auth/logout")
  class Logout {

    /**
     * Verifies that {@code POST /api/auth/logout} returns {@code 204 No Content} when the refresh
     * token is successfully invalidated.
     */
    @Test
    @DisplayName("returns 204 on successful logout")
    void logout_returns204() throws Exception {
      doNothing().when(authService).logout("sometoken", "UNIMOL");

      mockMvc.perform(post("/api/auth/logout")
              .param("refreshToken", "sometoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isNoContent());
    }

    /**
     * Verifies that {@code POST /api/auth/logout} returns {@code 204 No Content} even when the
     * provided token does not exist, confirming that logout is a fire-and-forget, idempotent
     * operation.
     */
    @Test
    @DisplayName("returns 204 even when token does not exist (idempotent)")
    void logout_isIdempotent() throws Exception {
      doNothing().when(authService).logout(any(), any());

      mockMvc.perform(post("/api/auth/logout")
              .param("refreshToken", "nonexistenttoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isNoContent());
    }
  }
}
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link AuthController} using MockMvc.
 *
 * Spring Security filter chain is disabled — only the controller layer is tested.
 * AuthService is mocked.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  // JwtAuthenticationFilter is a @Component — must be mocked to avoid
  // Spring trying to wire its OmuJwtService dependency in slice context
  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  // ================================
  // POST /api/auth/login
  // ================================

  @Nested
  @DisplayName("POST /api/auth/login")
  class Login {

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

    @Test
    @DisplayName("returns 400 when request body is missing required fields")
    void login_returns400OnMissingFields() throws Exception {
      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  // ================================
  // POST /api/auth/refresh
  // ================================

  @Nested
  @DisplayName("POST /api/auth/refresh")
  class Refresh {

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

  // ================================
  // POST /api/auth/logout
  // ================================

  @Nested
  @DisplayName("POST /api/auth/logout")
  class Logout {

    @Test
    @DisplayName("returns 204 on successful logout")
    void logout_returns204() throws Exception {
      doNothing().when(authService).logout("sometoken", "UNIMOL");

      mockMvc.perform(post("/api/auth/logout")
              .param("refreshToken", "sometoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("returns 204 even when token does not exist (idempotent)")
    void logout_isIdempotent() throws Exception {
      // logout never throws — it's a fire-and-forget operation
      doNothing().when(authService).logout(any(), any());

      mockMvc.perform(post("/api/auth/logout")
              .param("refreshToken", "nonexistenttoken")
              .param("universityId", "UNIMOL"))
          .andExpect(status().isNoContent());
    }
  }
}
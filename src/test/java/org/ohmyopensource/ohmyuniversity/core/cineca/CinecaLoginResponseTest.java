package org.ohmyopensource.ohmyuniversity.core.cineca;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CinecaLoginResponse} Jackson deserialization.
 *
 * Verifies correct mapping of Cineca ESSE3 /login JSON payload into the DTO hierarchy.
 * No Spring context required — plain ObjectMapper.
 */
class CinecaLoginResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ============================================================
  // Root fields
  // ============================================================

  @Nested
  @DisplayName("Root fields")
  class RootFields {

    @Test
    @DisplayName("deserializes authToken and jwt correctly")
    void rootTokens() throws Exception {
      String json = """
          {
            "authToken": "auth-abc",
            "jwt": "jwt-xyz"
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getAuthToken()).isEqualTo("auth-abc");
      assertThat(response.getJwt()).isEqualTo("jwt-xyz");
    }

    @Test
    @DisplayName("unknown root fields are ignored without exception")
    void ignoresUnknownRootFields() throws Exception {
      String json = """
          {
            "authToken": "auth-abc",
            "jwt": "jwt-xyz",
            "unknownField": "should be ignored",
            "anotherUnknown": 999
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getAuthToken()).isEqualTo("auth-abc");
    }

    @Test
    @DisplayName("missing authToken and jwt → both null, no exception")
    void missingTokens() throws Exception {
      CinecaLoginResponse response = objectMapper.readValue("{}", CinecaLoginResponse.class);

      assertThat(response.getAuthToken()).isNull();
      assertThat(response.getJwt()).isNull();
      assertThat(response.getUser()).isNull();
    }
  }

  // ============================================================
  // CinecaUser
  // ============================================================

  @Nested
  @DisplayName("CinecaUser")
  class CinecaUserTests {

    @Test
    @DisplayName("deserializes user fields via @JsonProperty names")
    void userFields() throws Exception {
      String json = """
          {
            "user": {
              "codFis": "TSTXXX00A00X000X",
              "firstName": "Alessio",
              "lastName": "Del Muto",
              "persId": 12345,
              "userId": "adelmuto"
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);
      CinecaLoginResponse.CinecaUser user = response.getUser();

      assertThat(user.getCodiceFiscale()).isEqualTo("TSTXXX00A00X000X");
      assertThat(user.getFirstName()).isEqualTo("Alessio");
      assertThat(user.getLastName()).isEqualTo("Del Muto");
      assertThat(user.getPersId()).isEqualTo(12345L);
      assertThat(user.getUserId()).isEqualTo("adelmuto");
    }

    @Test
    @DisplayName("unknown user fields are ignored")
    void ignoresUnknownUserFields() throws Exception {
      String json = """
          {
            "user": {
              "codFis": "TSTXXX00A00X000X",
              "unexpectedCinecaField": "ignore me"
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getUser().getCodiceFiscale()).isEqualTo("TSTXXX00A00X000X");
    }

    @Test
    @DisplayName("empty trattiCarriera list → empty list, not null")
    void emptyTrattiCarriera() throws Exception {
      String json = """
          {
            "user": {
              "codFis": "TSTXXX00A00X000X",
              "trattiCarriera": []
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getUser().getTrattiCarriera()).isNotNull().isEmpty();
    }
  }

  // ============================================================
  // TrattoCarriera
  // ============================================================

  @Nested
  @DisplayName("TrattoCarriera")
  class TrattoCarrieraTests {

    @Test
    @DisplayName("deserializes single tratto with all fields")
    void singleTratto() throws Exception {
      String json = """
          {
            "user": {
              "trattiCarriera": [
                {
                  "stuId": 89486,
                  "matId": 106279,
                  "matricola": "178026",
                  "cdsId": 1001,
                  "cdsDes": "Informatica",
                  "staStuCod": "A",
                  "staStuDes": "Attivo",
                  "staMatCod": "I",
                  "staMatDes": "Iscritto"
                }
              ]
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);
      CinecaLoginResponse.TrattoCarriera tratto =
          response.getUser().getTrattiCarriera().getFirst();

      assertThat(tratto.getStuId()).isEqualTo(89486L);
      assertThat(tratto.getMatId()).isEqualTo(106279L);
      assertThat(tratto.getMatricola()).isEqualTo("178026");
      assertThat(tratto.getCdsId()).isEqualTo(1001L);
      assertThat(tratto.getCdsDes()).isEqualTo("Informatica");
      assertThat(tratto.getStaStuCod()).isEqualTo("A");
      assertThat(tratto.getStaMatCod()).isEqualTo("I");
    }

    @Test
    @DisplayName("deserializes multiple trattiCarriera correctly")
    void multipleTratti() throws Exception {
      String json = """
          {
            "user": {
              "trattiCarriera": [
                { "stuId": 111, "matricola": "000001" },
                { "stuId": 222, "matricola": "000002" }
              ]
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getUser().getTrattiCarriera()).hasSize(2);
      assertThat(response.getUser().getTrattiCarriera())
          .extracting(CinecaLoginResponse.TrattoCarriera::getMatricola)
          .containsExactly("000001", "000002");
    }
  }

  // ============================================================
  // DettaglioTratto
  // ============================================================

  @Nested
  @DisplayName("DettaglioTratto")
  class DettaglioTrattoTests {

    @Test
    @DisplayName("deserializes nested dettaglioTratto with all fields")
    void dettaglioFields() throws Exception {
      String json = """
          {
            "user": {
              "trattiCarriera": [
                {
                  "stuId": 89486,
                  "dettaglioTratto": {
                    "cdsCod": "IN01",
                    "tipoCorsoCod": "L",
                    "annoCorso": 3,
                    "durataAnni": 3,
                    "aaIscrId": 2022,
                    "ultimoAnnoFlg": 1,
                    "facCod": "SC"
                  }
                }
              ]
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);
      CinecaLoginResponse.DettaglioTratto det =
          response.getUser().getTrattiCarriera().getFirst().getDettaglioTratto();

      assertThat(det.getCdsCod()).isEqualTo("IN01");
      assertThat(det.getTipoCorsoCod()).isEqualTo("L");
      assertThat(det.getAnnoCorso()).isEqualTo(3);
      assertThat(det.getDurataAnni()).isEqualTo(3);
      assertThat(det.getAaIscrId()).isEqualTo(2022);
      assertThat(det.getUltimoAnnoFlg()).isEqualTo(1);
      assertThat(det.getFacCod()).isEqualTo("SC");
    }

    @Test
    @DisplayName("absent dettaglioTratto → null, no exception")
    void absentDettaglio() throws Exception {
      String json = """
          {
            "user": {
              "trattiCarriera": [
                { "stuId": 89486 }
              ]
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getUser().getTrattiCarriera().getFirst().getDettaglioTratto()).isNull();
    }
  }

  // ============================================================
  // Full payload
  // ============================================================

  @Nested
  @DisplayName("Full payload")
  class FullPayload {

    @Test
    @DisplayName("complete realistic Cineca response deserializes without errors")
    void fullPayload() throws Exception {
      String json = """
          {
            "authToken": "auth-abc",
            "jwt": "jwt-xyz",
            "user": {
              "codFis": "TSTXXX00A00X000X",
              "firstName": "Alessio",
              "lastName": "Del Muto",
              "persId": 12345,
              "userId": "adelmuto",
              "trattiCarriera": [
                {
                  "stuId": 89486,
                  "matId": 106279,
                  "matricola": "178026",
                  "cdsId": 1001,
                  "cdsDes": "Informatica",
                  "staStuCod": "A",
                  "staStuDes": "Attivo",
                  "staMatCod": "I",
                  "staMatDes": "Iscritto",
                  "dettaglioTratto": {
                    "cdsCod": "IN01",
                    "tipoCorsoCod": "L",
                    "annoCorso": 3,
                    "durataAnni": 3,
                    "aaIscrId": 2022,
                    "ultimoAnnoFlg": 1,
                    "facCod": "SC"
                  }
                }
              ]
            }
          }
          """;

      CinecaLoginResponse response = objectMapper.readValue(json, CinecaLoginResponse.class);

      assertThat(response.getAuthToken()).isEqualTo("auth-abc");
      assertThat(response.getJwt()).isEqualTo("jwt-xyz");

      CinecaLoginResponse.CinecaUser user = response.getUser();
      assertThat(user.getCodiceFiscale()).isEqualTo("TSTXXX00A00X000X");
      assertThat(user.getTrattiCarriera()).hasSize(1);

      CinecaLoginResponse.TrattoCarriera tratto = user.getTrattiCarriera().getFirst();
      assertThat(tratto.getStuId()).isEqualTo(89486L);
      assertThat(tratto.getDettaglioTratto().getCdsCod()).isEqualTo("IN01");
    }
  }
}
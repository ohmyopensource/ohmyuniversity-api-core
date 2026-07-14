package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.List;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaStrutturaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaStrutturaClient.CinecaSede;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaStrutturaClient.CinecaStruttura;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.SedeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.StrutturaResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.TipoCorsoStrutturaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for faculty/department and location data from Cineca ESSE3 struttura-service-v1. Every
 * endpoint in this domain is publicly accessible (no student-specific data), but still goes through
 * the standard JWT resolution for consistency with the rest of the service layer.
 */
@Service
public class StrutturaService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(StrutturaService.class);

  private final CinecaStrutturaClient strutturaClient;

  public StrutturaService(
      CinecaStrutturaClient strutturaClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.strutturaClient = strutturaClient;
  }

  /**
   * Retrieves all faculties/departments for the student's university.
   *
   * @param principal authenticated OhMyU principal
   * @return list of faculties/departments with nested locations
   */
  public List<StrutturaResponse> getFacolta(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    List<CinecaStruttura> strutture = strutturaClient.getStrutture(baseUrl, jwt);
    log.debug("StrutturaService: fetched {} strutture", strutture.size());

    return strutture.stream().map(this::toStrutturaResponse).toList();
  }

  /**
   * Retrieves a single location (sede) by ID.
   *
   * @param principal authenticated OhMyU principal
   * @param sedeId    location identifier
   * @return location detail, or {@code null} if not found
   */
  public SedeResponse getSede(OmuPrincipal principal, Long sedeId) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaSede sede = strutturaClient.getSede(baseUrl, jwt, sedeId);
    return sede != null ? toSedeResponse(sede) : null;
  }

  // ============ Mappers ============

  private StrutturaResponse toStrutturaResponse(CinecaStruttura s) {
    StrutturaResponse r = new StrutturaResponse();
    r.setFacId(s.getFacId());
    r.setIstatCod(s.getIstatCod());
    r.setFacCod(s.getFacCod());
    r.setFacDes(s.getFacDes());
    r.setFacDesEng(s.getFacDesEng());
    r.setAteneoId(s.getAteneoId());
    r.setCitta(s.getCitta());
    r.setVia(s.getVia());
    r.setProv(s.getProv());
    r.setCap(s.getCap());
    r.setCodFis(s.getCodFis());
    r.setAaAttId(s.getAaAttId());
    r.setAaDisId(s.getAaDisId());
    r.setUrlSitoWeb(s.getUrlSitoWeb());
    r.setWebViewFlg(s.getWebViewFlg());
    r.setTel(s.getTel());
    r.setFax(s.getFax());
    r.setEmail(s.getEmail());
    r.setCodStatMiur(s.getCodStatMiur());
    r.setCsaCod(s.getCsaCod());
    r.setSdrTip(s.getSdrTip());
    r.setAreaDiscCod(s.getAreaDiscCod());
    r.setAreaDiscDesEng(s.getAreaDiscDesEng());
    r.setSedi(s.getSediStruttura().stream().map(this::toSedeResponse).toList());
    r.setTipiCorso(s.getTipiCorsoStruttura().stream().map(this::toTipoCorsoResponse).toList());
    return r;
  }

  private TipoCorsoStrutturaResponse toTipoCorsoResponse(
      CinecaStrutturaClient.CinecaTipoCorsoStruttura t) {
    TipoCorsoStrutturaResponse r = new TipoCorsoStrutturaResponse();
    r.setCdsId(t.getCdsId());
    r.setFacId(t.getFacId());
    r.setTipoCorsoCod(t.getTipoCorsoCod());
    r.setTipoCorsoDes(t.getTipoCorsoDes());
    return r;
  }

  private SedeResponse toSedeResponse(CinecaSede s) {
    SedeResponse r = new SedeResponse();
    r.setSedeId(s.getSedeId());
    r.setFacId(s.getFacId());
    r.setDefAmmFlg(s.getDefAmmFlg());
    r.setAteneoId(s.getAteneoId());
    r.setSedeDes(s.getSedeDes());
    r.setSedeDesEng(s.getSedeDesEng());
    r.setCap(s.getCap());
    r.setVia(s.getVia());
    r.setDirettore(s.getDirettore());
    r.setCitta(s.getCitta());
    r.setCistra(s.getCistra());
    r.setIstatCod(s.getIstatCod());
    r.setTel(s.getTel());
    r.setFax(s.getFax());
    r.setEmail(s.getEmail());
    r.setUrlSitoWeb(s.getUrlSitoWeb());
    return r;
  }
}
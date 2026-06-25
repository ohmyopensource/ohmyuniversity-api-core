package org.ohmyopensource.ohmyuniversity.core.service.esse3;

import java.util.stream.Stream;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.CinecaSessionStore;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaBadge;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaCarriera;
import org.ohmyopensource.ohmyuniversity.core.cineca.esse3.CinecaProfileClient.CinecaPersona;
import org.ohmyopensource.ohmyuniversity.core.config.OmuPrincipal;
import org.ohmyopensource.ohmyuniversity.core.config.UniversityRegistry;
import org.ohmyopensource.ohmyuniversity.core.domain.repository.UniversityConnectionRepository;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.BadgeResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.CareerInfoResponse;
import org.ohmyopensource.ohmyuniversity.core.dto.esse3.PersonaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for profile-related operations against Cineca ESSE3.
 *
 * <p>Covers personal data (persona), career metadata (carriera),
 * profile avatar and university badge.
 *
 * <p>All data is fetched in real-time from Cineca; nothing is persisted locally.
 */
@Service
public class ProfileService extends AbstractEsse3Service {

  private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

  private final CinecaProfileClient profileClient;

  // ============ Constructor ============

  public ProfileService(
      CinecaProfileClient profileClient,
      CinecaSessionStore sessionStore,
      UniversityRegistry universityRegistry,
      UniversityConnectionRepository connectionRepository) {
    super(sessionStore, universityRegistry, connectionRepository);
    this.profileClient = profileClient;
  }

  // ============ Public Methods ============

  /**
   * Retrieves full personal profile for the authenticated user.
   *
   * <p>Falls back to JWT/Redis claims if persId is not cached,
   * returning a minimal profile with identity data only.
   *
   * @param principal authenticated OhMyU principal
   * @return full or partial profile response
   */
  public PersonaResponse getPersona(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    Long persId = resolvePersIdOptional(principal).orElse(null);

    if (persId == null) {
      log.warn("ProfileService: persId not in session, returning minimal profile for user={}",
          principal.omuUserId());
      PersonaResponse r = new PersonaResponse();
      r.setCodFis(principal.codiceFiscale());
      r.setNome(sessionStore.getUserNome(principal.omuUserId()).orElse(""));
      r.setCognome(sessionStore.getUserCognome(principal.omuUserId()).orElse(""));
      r.setUserId(sessionStore.getUserId(principal.omuUserId()).orElse(""));
      r.setDomicilioComeResidenza(true);
      return r;
    }

    CinecaPersona p = profileClient.getPersona(baseUrl, jwt, persId);
    if (p == null) {
      throw new CinecaClient.CinecaAuthException("Persona not found for persId=" + persId);
    }

    log.debug("ProfileService: fetched persona persId={}", p.getPersId());
    return toProfiloResponse(p);
  }

  /**
   * Retrieves career metadata (tipoCorsoCod, cdsDes, facDes, etc.) from carriere-service-v1.
   *
   * @param principal authenticated OhMyU principal
   * @return career info response
   */
  public CareerInfoResponse getInfo(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    CinecaCarriera c = profileClient.getCarriera(baseUrl, jwt, principal.matId());
    if (c == null) {
      throw new CinecaClient.CinecaUnavailableException("No carriera found");
    }

    log.debug("ProfileService: fetched career info tipoCorsoCod={}", c.getTipoCorsoCod());
    return toCarrieraInfoResponse(c);
  }

  /**
   * Retrieves the profile avatar as raw JPEG bytes.
   *
   * @param principal authenticated OhMyU principal
   * @return JPEG image bytes, or {@code null} if not available
   */
  public byte[] getAvatar(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());
    Long persId = resolvePersId(principal);

    log.debug("ProfileService: fetching avatar persId={}", persId);
    return profileClient.getAvatar(baseUrl, jwt, persId);
  }

  /**
   * Retrieves the university badge for the authenticated student.
   *
   * @param principal authenticated OhMyU principal
   * @return badge response, or {@code null} if no badge exists
   */
  public BadgeResponse getBadge(OmuPrincipal principal) {
    String jwt = resolveCinecaJwt(principal);
    String baseUrl = resolveBaseUrl(principal.universityId());

    var badges = profileClient.getBadges(baseUrl, jwt, principal.stuId());
    if (badges.isEmpty()) {
      log.warn("ProfileService: no badge found for stuId={}", principal.stuId());
      return null;
    }

    log.debug("ProfileService: fetched badge stuId={}", principal.stuId());
    return toBadgeResponse(badges.get(0));
  }

  // ============ Mappers ============

  private PersonaResponse toProfiloResponse(CinecaPersona p) {
    PersonaResponse r = new PersonaResponse();
    r.setPersId(p.getPersId());
    r.setNome(p.getNome());
    r.setCognome(p.getCognome());
    r.setCodFis(p.getCodFis());
    r.setDataNascita(p.getDataNascita());
    r.setSesso(p.getSesso());
    r.setLuogoNascita(
        p.getComuNascDes() != null
            ? p.getComuNascDes() + " (" + p.getComuNascSigla() + ")"
            : null);
    r.setProvinciaNascita(p.getProvNascDes());
    r.setCittadinanza(p.getDesCittadinanza());
    r.setStatoCivile(p.getStatoCivileDes());
    r.setProfessione(p.getProfessione());
    r.setEmail(p.getEmail());
    r.setEmailAte(p.getEmailAte());
    r.setEmailCertificata(p.getEmailCertificata());
    r.setCellulare(p.getCellulare());
    r.setTelefono(p.getTelRes());
    r.setUserId(p.getUserId());

    String indirizzoRes = Stream.of(p.getViaRes(), p.getNumCivRes())
        .filter(s -> s != null && !s.isBlank())
        .collect(java.util.stream.Collectors.joining(", "));
    r.setIndirizzoResidenza(indirizzoRes);
    r.setCapResidenza(p.getCapRes());
    r.setComuneResidenza(
        p.getComuResDes() != null
            ? p.getComuResDes() + " (" + p.getComuResSigla() + ")"
            : null);
    r.setProvinciaResidenza(p.getProvResDes());
    r.setNazioneResidenza(p.getNaziResDes());

    boolean domComeRes = p.getDomComeResFlg() != null && p.getDomComeResFlg() == 1;
    r.setDomicilioComeResidenza(domComeRes);
    if (!domComeRes) {
      String indirizzoDom = Stream.of(p.getViaDom(), p.getNumCivDom())
          .filter(s -> s != null && !s.isBlank())
          .collect(java.util.stream.Collectors.joining(" "));
      r.setIndirizzoDomicilio(indirizzoDom);
      r.setCapDomicilio(p.getCapDom());
      r.setComuneDomicilio(
          p.getComuDomDes() != null
              ? p.getComuDomDes() + " (" + p.getComuDomSigla() + ")"
              : null);
      r.setNazioneDomicilio(p.getNaziDomDes());
    }

    r.setEmergenzaNome(p.getEmergNome());
    r.setEmergenzaCognome(p.getEmergCognome());
    r.setEmergenzaTelefono(p.getEmergTel());
    r.setEmergenzaEmail(p.getEmergEmail());
    r.setEmergenzaRapporto(p.getEmergRapporto());

    return r;
  }

  private CareerInfoResponse toCarrieraInfoResponse(CinecaCarriera c) {
    CareerInfoResponse r = new CareerInfoResponse();
    r.setStuId(c.getStuId());
    r.setMatId(c.getMatId());
    r.setMatricola(c.getMatricola());
    r.setPersId(c.getPersId());
    r.setNome(c.getNome());
    r.setCognome(c.getCognome());
    r.setCodFis(c.getCodFis());
    r.setDataNascita(c.getDataNascita());
    r.setSesso(c.getSesso());
    r.setEmail(c.getEmail());
    r.setEmailAte(c.getEmailAte());
    r.setEmailCertificata(c.getEmailCertificata());
    r.setTipoCorsoCod(c.getTipoCorsoCod());
    r.setTipoCorsoDes(c.getTipoCorsoDes());
    r.setCdsCod(c.getCdsCod());
    r.setCdsDes(c.getCdsDes());
    r.setFacCod(c.getFacCod());
    r.setFacDes(c.getFacDes());
    r.setAnnoCorso(c.getAnnoCorso());
    r.setAaIscrId(c.getAaIscrId());
    r.setAaOrdId(c.getAaOrdId());
    r.setDataImm(c.getDataImm());
    r.setDataIscr(c.getDataIscr());
    r.setDataFineCarriera(c.getDataFineCarriera());
    r.setStaStuCod(c.getStaStuCod());
    r.setStatiStuDes(c.getStatiStuDes());
    r.setProfstuDes(c.getProfstuDes());
    r.setPtFlg(c.getPtFlg());
    r.setSospFlg(c.getSospFlg());
    r.setAttlauFlg(c.getAttlauFlg());
    r.setSediDes(c.getSediDes());
    r.setSedeId(c.getSedeId());
    r.setUserId(c.getUserId());
    return r;
  }

  private BadgeResponse toBadgeResponse(CinecaBadge b) {
    BadgeResponse r = new BadgeResponse();
    r.setBdgId(b.getBdgId());
    r.setMatricola(b.getMatricola());
    r.setCognome(b.getCognome());
    r.setNome(b.getNome());
    r.setCodFis(b.getCodFis());
    r.setCodCds(b.getCodCds());
    r.setDesCds(b.getDesCds());
    r.setCodFac(b.getCodFac());
    r.setDesFac(b.getDesFac());
    r.setAaIscrAnn(b.getAaIscrAnn());
    r.setRfid(b.getRfid());
    r.setUniversita(b.getUniversita());
    r.setStaStuCod(b.getStaStuCod());
    r.setDataIni(b.getDataIni());
    r.setDataFin(b.getDataFin());
    r.setFrontImagePresent(b.getFrontImagePresent() != null && b.getFrontImagePresent() == 1);
    r.setRearImagePresent(b.getRearImagePresent() != null && b.getRearImagePresent() == 1);
    r.setBadgeBlbId(b.getBadgeBlbId());
    return r;
  }
}
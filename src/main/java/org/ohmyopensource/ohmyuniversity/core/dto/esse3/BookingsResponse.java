package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

import java.util.List;

/**
 * Response DTO for GET /api/v1/carriera/prenotazioni-libretto. Contains current exam bookings
 * without requiring Cineca password.
 */
public class BookingsResponse {

  private List<IscrizioneAppello> prenotazioni;

  public List<IscrizioneAppello> getPrenotazioni() {
    return prenotazioni;
  }

  public void setPrenotazioni(List<IscrizioneAppello> prenotazioni) {
    this.prenotazioni = prenotazioni;
  }

  public static class IscrizioneAppello {

    private Long applistaId;
    private Long cdsId;
    private Long adId;
    private Long appId;
    private String adStuCod;
    private String adStuDes;
    private Long adsceId;
    private String dataOraTurno;
    private String dataInizioIscr;
    private String dataFineIscr;
    private String aulaDes;
    private String tipoIscrCod;

    public Long getApplistaId() {
      return applistaId;
    }

    public void setApplistaId(Long v) {
      this.applistaId = v;
    }

    public Long getCdsId() {
      return cdsId;
    }

    public void setCdsId(Long v) {
      this.cdsId = v;
    }

    public Long getAdId() {
      return adId;
    }

    public void setAdId(Long v) {
      this.adId = v;
    }

    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long v) {
      this.appId = v;
    }

    public String getAdStuCod() {
      return adStuCod;
    }

    public void setAdStuCod(String v) {
      this.adStuCod = v;
    }

    public String getAdStuDes() {
      return adStuDes;
    }

    public void setAdStuDes(String v) {
      this.adStuDes = v;
    }

    public Long getAdsceId() {
      return adsceId;
    }

    public void setAdsceId(Long v) {
      this.adsceId = v;
    }

    public String getDataOraTurno() {
      return dataOraTurno;
    }

    public void setDataOraTurno(String v) {
      this.dataOraTurno = v;
    }

    public String getDataInizioIscr() {
      return dataInizioIscr;
    }

    public void setDataInizioIscr(String v) {
      this.dataInizioIscr = v;
    }

    public String getDataFineIscr() {
      return dataFineIscr;
    }

    public void setDataFineIscr(String v) {
      this.dataFineIscr = v;
    }

    public String getAulaDes() {
      return aulaDes;
    }

    public void setAulaDes(String v) {
      this.aulaDes = v;
    }

    public String getTipoIscrCod() {
      return tipoIscrCod;
    }

    public void setTipoIscrCod(String v) {
      this.tipoIscrCod = v;
    }
  }
}
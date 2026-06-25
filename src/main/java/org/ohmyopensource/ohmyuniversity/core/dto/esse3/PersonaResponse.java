package org.ohmyopensource.ohmyuniversity.core.dto.esse3;

/**
 * Response DTO for GET /api/v1/carriera/profilo.
 * Contains full personal data for the authenticated student.
 */
public class PersonaResponse {

  // Dati personali
  private Long persId;
  private String nome;
  private String cognome;
  private String codFis;
  private String dataNascita;
  private String sesso;
  private String luogoNascita;
  private String provinciaNascita;
  private String cittadinanza;
  private String statoCivile;
  private String professione;

  // Contatti
  private String email;
  private String emailAte;
  private String emailCertificata;
  private String cellulare;
  private String telefono;
  private String userId;

  // Residenza
  private String indirizzoResidenza;
  private String capResidenza;
  private String comuneResidenza;
  private String provinciaResidenza;
  private String nazioneResidenza;

  // Domicilio
  private boolean domicilioComeResidenza;
  private String indirizzoDomicilio;
  private String capDomicilio;
  private String comuneDomicilio;
  private String nazioneDomicilio;

  // Contatto emergenza
  private String emergenzaNome;
  private String emergenzaCognome;
  private String emergenzaTelefono;
  private String emergenzaEmail;
  private String emergenzaRapporto;

  // Getters e setters
  public Long getPersId() { return persId; }
  public void setPersId(Long v) { this.persId = v; }
  public String getNome() { return nome; }
  public void setNome(String v) { this.nome = v; }
  public String getCognome() { return cognome; }
  public void setCognome(String v) { this.cognome = v; }
  public String getCodFis() { return codFis; }
  public void setCodFis(String v) { this.codFis = v; }
  public String getDataNascita() { return dataNascita; }
  public void setDataNascita(String v) { this.dataNascita = v; }
  public String getSesso() { return sesso; }
  public void setSesso(String v) { this.sesso = v; }
  public String getLuogoNascita() { return luogoNascita; }
  public void setLuogoNascita(String v) { this.luogoNascita = v; }
  public String getProvinciaNascita() { return provinciaNascita; }
  public void setProvinciaNascita(String v) { this.provinciaNascita = v; }
  public String getCittadinanza() { return cittadinanza; }
  public void setCittadinanza(String v) { this.cittadinanza = v; }
  public String getStatoCivile() { return statoCivile; }
  public void setStatoCivile(String v) { this.statoCivile = v; }
  public String getProfessione() { return professione; }
  public void setProfessione(String v) { this.professione = v; }
  public String getEmail() { return email; }
  public void setEmail(String v) { this.email = v; }
  public String getEmailAte() { return emailAte; }
  public void setEmailAte(String v) { this.emailAte = v; }
  public String getEmailCertificata() { return emailCertificata; }
  public void setEmailCertificata(String v) { this.emailCertificata = v; }
  public String getCellulare() { return cellulare; }
  public void setCellulare(String v) { this.cellulare = v; }
  public String getTelefono() { return telefono; }
  public void setTelefono(String v) { this.telefono = v; }
  public String getUserId() { return userId; }
  public void setUserId(String v) { this.userId = v; }
  public String getIndirizzoResidenza() { return indirizzoResidenza; }
  public void setIndirizzoResidenza(String v) { this.indirizzoResidenza = v; }
  public String getCapResidenza() { return capResidenza; }
  public void setCapResidenza(String v) { this.capResidenza = v; }
  public String getComuneResidenza() { return comuneResidenza; }
  public void setComuneResidenza(String v) { this.comuneResidenza = v; }
  public String getProvinciaResidenza() { return provinciaResidenza; }
  public void setProvinciaResidenza(String v) { this.provinciaResidenza = v; }
  public String getNazioneResidenza() { return nazioneResidenza; }
  public void setNazioneResidenza(String v) { this.nazioneResidenza = v; }
  public boolean isDomicilioComeResidenza() { return domicilioComeResidenza; }
  public void setDomicilioComeResidenza(boolean v) { this.domicilioComeResidenza = v; }
  public String getIndirizzoDomicilio() { return indirizzoDomicilio; }
  public void setIndirizzoDomicilio(String v) { this.indirizzoDomicilio = v; }
  public String getCapDomicilio() { return capDomicilio; }
  public void setCapDomicilio(String v) { this.capDomicilio = v; }
  public String getComuneDomicilio() { return comuneDomicilio; }
  public void setComuneDomicilio(String v) { this.comuneDomicilio = v; }
  public String getNazioneDomicilio() { return nazioneDomicilio; }
  public void setNazioneDomicilio(String v) { this.nazioneDomicilio = v; }
  public String getEmergenzaNome() { return emergenzaNome; }
  public void setEmergenzaNome(String v) { this.emergenzaNome = v; }
  public String getEmergenzaCognome() { return emergenzaCognome; }
  public void setEmergenzaCognome(String v) { this.emergenzaCognome = v; }
  public String getEmergenzaTelefono() { return emergenzaTelefono; }
  public void setEmergenzaTelefono(String v) { this.emergenzaTelefono = v; }
  public String getEmergenzaEmail() { return emergenzaEmail; }
  public void setEmergenzaEmail(String v) { this.emergenzaEmail = v; }
  public String getEmergenzaRapporto() { return emergenzaRapporto; }
  public void setEmergenzaRapporto(String v) { this.emergenzaRapporto = v; }
}
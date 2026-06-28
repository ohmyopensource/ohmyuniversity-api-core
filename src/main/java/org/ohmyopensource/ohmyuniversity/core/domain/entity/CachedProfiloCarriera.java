package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Read cache for Cineca career profiles.
 *
 * <p>Populated at login time and used to aggregate multi-university profiles
 * in the {@code LoginResponse} without requiring re-authentication.
 *
 * <p>Data is upserted at every login for the corresponding university.
 * This table is NOT a source of truth for academic data — it is a UI cache only.
 */
@Entity
@Table(
    name = "cached_profilo_carriera",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cached_profilo_carriera_user_stu",
        columnNames = {"omu_user_id", "stu_id"}
    )
)
public class CachedProfiloCarriera {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "omu_user_id", nullable = false)
  private OmuUser user;

  @Column(name = "university_id", nullable = false, length = 20)
  private String universityId;

  @Column(name = "university_name", nullable = false)
  private String universityName;

  @Column(name = "stu_id", nullable = false)
  private Long stuId;

  @Column(name = "mat_id", nullable = false)
  private Long matId;

  @Column(name = "matricola", length = 50)
  private String matricola;

  @Column(name = "corso_nome")
  private String corsoNome;

  @Column(name = "corso_codice", length = 50)
  private String corsoCodice;

  @Column(name = "cds_id")
  private Long cdsId;

  @Column(name = "tipo_corso_cod", length = 20)
  private String tipoCorsoCod;

  @Column(name = "status_studente", length = 10)
  private String statusStudente;

  @Column(name = "status_descr", length = 100)
  private String statusDescr;

  @Column(name = "anno_corso")
  private Integer annoCorso;

  @Column(name = "durata_anni")
  private Integer durataAnni;

  @Column(name = "anno_accademico")
  private Integer annoAccademico;

  @Column(name = "attivo", nullable = false)
  private boolean attivo;

  @Column(name = "laureato", nullable = false)
  private boolean laureato;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // ============ Lifecycle ============

  @PrePersist
  @PreUpdate
  void onUpsert() {
    updatedAt = Instant.now();
  }

  // ============ Getters | Setters ============

  public UUID getId() { return id; }

  public OmuUser getUser() { return user; }
  public void setUser(OmuUser user) { this.user = user; }

  public String getUniversityId() { return universityId; }
  public void setUniversityId(String universityId) { this.universityId = universityId; }

  public String getUniversityName() { return universityName; }
  public void setUniversityName(String universityName) { this.universityName = universityName; }

  public Long getStuId() { return stuId; }
  public void setStuId(Long stuId) { this.stuId = stuId; }

  public Long getMatId() { return matId; }
  public void setMatId(Long matId) { this.matId = matId; }

  public String getMatricola() { return matricola; }
  public void setMatricola(String matricola) { this.matricola = matricola; }

  public String getCorsoNome() { return corsoNome; }
  public void setCorsoNome(String corsoNome) { this.corsoNome = corsoNome; }

  public String getCorsoCodice() { return corsoCodice; }
  public void setCorsoCodice(String corsoCodice) { this.corsoCodice = corsoCodice; }

  public Long getCdsId() { return cdsId; }
  public void setCdsId(Long cdsId) { this.cdsId = cdsId; }

  public String getTipoCorsoCod() { return tipoCorsoCod; }
  public void setTipoCorsoCod(String tipoCorsoCod) { this.tipoCorsoCod = tipoCorsoCod; }

  public String getStatusStudente() { return statusStudente; }
  public void setStatusStudente(String statusStudente) { this.statusStudente = statusStudente; }

  public String getStatusDescr() { return statusDescr; }
  public void setStatusDescr(String statusDescr) { this.statusDescr = statusDescr; }

  public Integer getAnnoCorso() { return annoCorso; }
  public void setAnnoCorso(Integer annoCorso) { this.annoCorso = annoCorso; }

  public Integer getDurataAnni() { return durataAnni; }
  public void setDurataAnni(Integer durataAnni) { this.durataAnni = durataAnni; }

  public Integer getAnnoAccademico() { return annoAccademico; }
  public void setAnnoAccademico(Integer annoAccademico) { this.annoAccademico = annoAccademico; }

  public boolean isAttivo() { return attivo; }
  public void setAttivo(boolean attivo) { this.attivo = attivo; }

  public boolean isLaureato() { return laureato; }
  public void setLaureato(boolean laureato) { this.laureato = laureato; }

  public Instant getUpdatedAt() { return updatedAt; }
}
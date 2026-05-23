package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.OmuUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link OmuUser}.
 */
@Repository
public interface OmuUserRepository extends JpaRepository<OmuUser, UUID> {

  Optional<OmuUser> findByCodiceFiscale(String codiceFiscale);
}
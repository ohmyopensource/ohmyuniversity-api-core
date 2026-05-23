package org.ohmyopensource.ohmyuniversity.core.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.UniversityConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link UniversityConnection}.
 */
@Repository
public interface UniversityConnectionRepository
    extends JpaRepository<UniversityConnection, UUID> {

  List<UniversityConnection> findByUserId(UUID userId);

  Optional<UniversityConnection> findByUserIdAndUniversityIdAndUsernameCineca(
      UUID userId,
      String universityId,
      String usernameCineca);
}
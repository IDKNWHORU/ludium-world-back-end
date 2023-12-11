package world.ludium.education.auth.ludium;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

public interface LudiumUserRepository extends JpaRepository<LudiumUser, UUID> {
    Optional<LudiumUser> findByGglId(BigInteger gglId);
}

package commodity.trading.repository;

import commodity.trading.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);
}

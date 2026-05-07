package commodity.trading.repository;

import commodity.trading.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByToken(String token);
    void deleteByToken(String token);

    @Modifying
    @Query("UPDATE JwtToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE JwtToken t SET t.revoked = true WHERE t.token = :token")
    void revokeByToken(@Param("token") String token);
}

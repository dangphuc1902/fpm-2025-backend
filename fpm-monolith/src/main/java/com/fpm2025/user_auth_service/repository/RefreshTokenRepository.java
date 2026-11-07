package com.fpm2025.user_auth_service.repository;

import com.fpm2025.user_auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    // Tra ve so dong da xoa: 0 nghia la request khac da dung token nay truoc (token chi dung 1 lan)
    @Modifying
    @Query("delete from RefreshToken r where r.token = :token")
    int consumeToken(@Param("token") String token);

    // Xoa bang query: repository.deleteAll(user.getRefreshTokens()) bi cascade cua collection luu lai khi flush
    @Modifying
    @Query("delete from RefreshToken r where r.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}

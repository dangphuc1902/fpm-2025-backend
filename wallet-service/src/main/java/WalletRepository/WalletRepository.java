package WalletRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpm_2025.wallet_service.entity.WalletEntity;
import com.google.common.base.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long>{
	Optional<WalletEntity> findByUserIdAndId(Long userId, Long id);
    List<WalletEntity> findByUserId(Long userId);
}

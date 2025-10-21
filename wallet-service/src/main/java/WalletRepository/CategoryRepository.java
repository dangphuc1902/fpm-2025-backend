package WalletRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpm_2025.wallet_service.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>{
	List<CategoryEntity> findByType(String type);
}

package com.fpm_2025.wallet_service.repository;

import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByName(String name);

    List<CategoryEntity> findByType(CategoryType type);

    List<CategoryEntity> findByParentIdIsNull();

    List<CategoryEntity> findByParentId(Long parentId);

    @Query("SELECT c FROM CategoryEntity c WHERE c.parentId IS NULL AND c.type = :type")
    List<CategoryEntity> findRootCategoriesByType(@Param("type") CategoryType type);

    @Query("SELECT c FROM CategoryEntity c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<CategoryEntity> findByIdWithChildren(@Param("id") Long id);

    boolean existsByName(String name);
}

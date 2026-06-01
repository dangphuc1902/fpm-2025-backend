//package com.fpm2025.user_auth_service.repository;
//
//import java.util.List;
//
//import com.fpm2025.user_auth_service.entity.UserEntity;
//import com.fpm2025.user_auth_service.entity.UserRoleEntity;
//import com.fpm2025.user_auth_service.entity.UserRoleEntityId;
//
//import io.lettuce.core.dynamic.annotation.Param;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//
//public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntityId> {	// Thêm các phương thức truy vấn tùy chỉnh nếu cần
//	List<UserRoleEntity> findByUser(UserEntity user);
//
//	// Hoặc dùng JPQL
//	@Query("SELECT ur.role.name FROM UserRoleEntity ur WHERE ur.user = :user")
//	List<String> findRoleNamesByUser(@Param("user") UserEntity user);
//}

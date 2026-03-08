package com.fpm_2025.wallet_service.dto.mapper;

import com.fpm_2025.wallet_service.dto.payload.request.CreateCategoryRequest;
import com.fpm_2025.wallet_service.dto.payload.response.CategoryResponse;
import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CategoryMapper - Chuyển đổi Category Entity ↔ DTO
 * 
 * ĐẶC BIỆT: Category có parent-child relationship (hierarchical)
 */
@Component
public class CategoryMapper {

    /**
     * Chuyển CreateCategoryRequest → CategoryEntity
     * 
     * DÙNG KHI: Admin tạo category mới
     */
    public CategoryEntity toEntity(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return CategoryEntity.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .iconPath(request.getIconPath())
                .type(request.getType())
                .build();
    }

    /**
     * Chuyển CreateCategoryRequest → CategoryEntity (with userId)
     * 
     * DÙNG KHI: User tạo custom category
     */
    public CategoryEntity toEntity(CreateCategoryRequest request, Long userId) {
        if (request == null) {
            return null;
        }

        return CategoryEntity.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .userId(userId)
                .iconPath(request.getIconPath())
                .type(request.getType())
                .build();
    }

    /**
     * Chuyển CategoryEntity → CategoryResponse (KHÔNG bao gồm children)
     * 
     * DÙNG KHI: Trả về single category hoặc flat list
     */
    public CategoryResponse toResponse(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .userId(entity.getUserId())
                .iconPath(entity.getIconPath())
                .color(entity.getColor())
                .type(entity.getType())
                .depth(entity.getDepth())
                .sortOrder(entity.getSortOrder())
                .children(null)  // Không load children (lazy)
                .build();
    }

    /**
     * Chuyển CategoryEntity → CategoryResponse (BAO GỒM children)
     * 
     * DÙNG KHI: Cần hierarchical tree structure
     */
    public CategoryResponse toResponseWithChildren(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        // Recursively map children
        List<CategoryResponse> childrenResponses = new ArrayList<>();
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            childrenResponses = entity.getChildren().stream()
                    .map(this::toResponseWithChildren)  // Recursive call
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .userId(entity.getUserId())
                .iconPath(entity.getIconPath())
                .color(entity.getColor())
                .type(entity.getType())
                .depth(entity.getDepth())
                .sortOrder(entity.getSortOrder())
                .children(childrenResponses)
                .build();
    }

    /**
     * Chuyển List<CategoryEntity> → List<CategoryResponse> (flat list)
     */
    public List<CategoryResponse> toResponseList(List<CategoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển List<CategoryEntity> → Tree structure
     * 
     * DÙNG KHI: Cần build category tree từ flat list
     */
    public List<CategoryResponse> toTreeStructure(List<CategoryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        // Filter root categories (no parent)
        return entities.stream()
                .filter(entity -> entity.getParentId() == null)
                .map(root -> {
                    // Find children for this root
                    List<CategoryEntity> children = entities.stream()
                            .filter(e -> root.getId().equals(e.getParentId()))
                            .collect(Collectors.toList());

                    root.setChildren(children);
                    return toResponseWithChildren(root);
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter categories by type
     * 
     * DÙNG KHI: Cần lấy chỉ expense hoặc income categories
     */
    public List<CategoryResponse> filterByType(List<CategoryEntity> entities, CategoryType type) {
        return entities.stream()
                .filter(entity -> entity.getType() == type)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
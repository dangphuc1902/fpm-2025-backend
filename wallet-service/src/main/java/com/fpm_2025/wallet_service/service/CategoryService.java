package com.fpm_2025.wallet_service.service;

import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.exception.DuplicateResourceException;
import com.fpm_2025.wallet_service.payload.request.CreateCategoryRequest;
import com.fpm_2025.wallet_service.payload.response.CategoryResponse;
import com.fpm_2025.wallet_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        // Validate parent category if provided
        if (request.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));

            // Ensure parent and child have same type
            if (!parent.getType().equals(request.getType())) {
                throw new IllegalArgumentException("Parent and child categories must have the same type");
            }
        }

        CategoryEntity category = CategoryEntity.builder()
            .name(request.getName())
            .parentId(request.getParentId())
            .iconPath(request.getIconPath())
            .type(request.getType())
            .build();

        CategoryEntity savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());

        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");
        List<CategoryEntity> categories = categoryRepository.findAll();
        return categories.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        log.info("Fetching categories by type: {}", type);
        List<CategoryEntity> categories = categoryRepository.findByType(type);
        return categories.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<CategoryResponse> getRootCategories() {
        log.info("Fetching root categories");
        List<CategoryEntity> categories = categoryRepository.findByParentIdIsNull();
        return categories.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public List<CategoryResponse> getRootCategoriesByType(CategoryType type) {
        log.info("Fetching root categories by type: {}", type);
        List<CategoryEntity> categories = categoryRepository.findRootCategoriesByType(type);
        return categories.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with id: {}", id);
        CategoryEntity category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    public CategoryResponse getCategoryWithChildren(Long id) {
        log.info("Fetching category with children, id: {}", id);
        CategoryEntity category = categoryRepository.findByIdWithChildren(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponseWithChildren(category);
    }

    public List<CategoryResponse> getSubCategories(Long parentId) {
        log.info("Fetching sub-categories for parent id: {}", parentId);

        // Verify parent exists
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent category not found with id: " + parentId);
        }

        List<CategoryEntity> subCategories = categoryRepository.findByParentId(parentId);
        return subCategories.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);

        CategoryEntity category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if category has children
        List<CategoryEntity> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete category with sub-categories. Delete sub-categories first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }

    // Mapping methods
    private CategoryResponse mapToResponse(CategoryEntity entity) {
        return CategoryResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .parentId(entity.getParentId())
            .iconPath(entity.getIconPath())
            .type(entity.getType())
            .children(children)
            .build();
    }
}
            .iconPath(entity.getIconPath())
            .type(entity.getType())
            .build();
    }

    private CategoryResponse mapToResponseWithChildren(CategoryEntity entity) {
        List<CategoryResponse> children = entity.getChildren().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return CategoryResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .parentId(entity.getParentId())
            
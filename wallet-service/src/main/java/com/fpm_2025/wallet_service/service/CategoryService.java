package com.fpm_2025.wallet_service.service;

import com.fpm_2025.wallet_service.entity.CategoryEntity;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import com.fpm_2025.wallet_service.exception.ResourceNotFoundException;
import com.fpm_2025.wallet_service.exception.DuplicateResourceException;
import com.fpm_2025.wallet_service.dto.payload.request.CreateCategoryRequest;
import com.fpm_2025.wallet_service.dto.payload.response.CategoryResponse;
import com.fpm_2025.wallet_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        logger.info("Creating new category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        if (request.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));

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
        logger.info("Category created successfully with id: {}", savedCategory.getId());

        return mapToResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        logger.info("Fetching all categories");
        List<CategoryEntity> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByType(CategoryType type) {
        logger.info("Fetching categories by type: {}", type);
        List<CategoryEntity> categories = categoryRepository.findByType(type);
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getRootCategories() {
        logger.info("Fetching root categories");
        List<CategoryEntity> categories = categoryRepository.findByParentIdIsNull();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getRootCategoriesByType(CategoryType type) {
        logger.info("Fetching root categories by type: {}", type);
        List<CategoryEntity> categories = categoryRepository.findRootCategoriesByType(type);
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        logger.info("Fetching category with id: {}", id);
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    public CategoryResponse getCategoryWithChildren(Long id) {
        logger.info("Fetching category with children, id: {}", id);
        CategoryEntity category = categoryRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponseWithChildren(category);
    }

    public List<CategoryResponse> getSubCategories(Long parentId) {
        logger.info("Fetching sub-categories for parent id: {}", parentId);

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
        logger.info("Deleting category with id: {}", id);

        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        List<CategoryEntity> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete category with sub-categories. Delete sub-categories first.");
        }

        categoryRepository.delete(category);
        logger.info("Category deleted successfully with id: {}", id);
    }

    private CategoryResponse mapToResponse(CategoryEntity entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .iconPath(entity.getIconPath())
                .type(entity.getType())
                .children(Collections.emptyList())
                .build();
    }

    private CategoryResponse mapToResponseWithChildren(CategoryEntity entity) {
        List<CategoryResponse> children = (entity.getChildren() == null) ? Collections.emptyList() :
                entity.getChildren().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());

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

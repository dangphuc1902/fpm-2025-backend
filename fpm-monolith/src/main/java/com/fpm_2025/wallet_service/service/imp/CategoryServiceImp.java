package com.fpm_2025.wallet_service.service.imp;

import com.fpm2025.domain.dto.response.CategoryResponse;
import com.fpm2025.domain.enums.CategoryType;
import com.fpm_2025.wallet_service.dto.payload.request.CreateCategoryRequest;

import java.util.List;

public interface CategoryServiceImp {
    CategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getCategoriesByType(CategoryType type);
    List<CategoryResponse> getRootCategories();
    List<CategoryResponse> getRootCategoriesByType(CategoryType type);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryWithChildren(Long id);
    List<CategoryResponse> getSubCategories(Long parentId);
    void deleteCategory(Long id);
}

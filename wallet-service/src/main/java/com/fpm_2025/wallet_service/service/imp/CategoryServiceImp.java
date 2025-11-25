package com.fpm_2025.wallet_service.service.imp;

import com.fpm_2025.wallet_service.dto.payload.request.CreateCategoryRequest;
import com.fpm_2025.wallet_service.dto.payload.response.CategoryResponse;
import com.fpm_2025.wallet_service.entity.enums.CategoryType;

import java.util.List;
// Quản lý danh mục (category) với cấu trúc cây (parent/child), icon, type (EXPENSE/INCOME). Hỗ trợ phân loại giao dịch chi tiết, cây danh mục (Ăn uống -> Cafe,....)
public interface CategoryServiceImp {
	// Tạo danh mục mới với parentId
    CategoryResponse createCategory(CreateCategoryRequest request);
    // Lấy tất cả danh mục
    List<CategoryResponse> getAllCategories();
    // Lấy danh mục theo loại (thu nhập/chi tiêu)
    List<CategoryResponse> getCategoriesByType(CategoryType type);
    // Lấy danh mục gốc (không có parent)
    List<CategoryResponse> getRootCategories();
    // Lấy danh mục gốc theo loại (thu nhập/chi tiêu)
    List<CategoryResponse> getRootCategoriesByType(CategoryType type);
    // Lấy danh mục theo id
    CategoryResponse getCategoryById(Long id);
    // Lấy danh mục cùng các danh mục con
    CategoryResponse getCategoryWithChildren(Long id);
    // Lấy danh mục con theo parentId
    List<CategoryResponse> getSubCategories(Long parentId);
    // Xoá danh mục theo id
    void deleteCategory(Long id);
}

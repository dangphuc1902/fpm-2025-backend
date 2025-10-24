package com.fpm_2025.wallet_service.controller;

import com.fpm_2025.wallet_service.payload.request.CreateCategoryRequest;
import com.fpm_2025.wallet_service.payload.response.BaseResponse;
import com.fpm_2025.wallet_service.payload.response.CategoryResponse;
import com.fpm_2025.wallet_service.entity.enums.*;
import com.fpm_2025.wallet_service.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create new category")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success(category, "Category created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(BaseResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get categories by type")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategoriesByType(
            @PathVariable CategoryType type) {
        List<CategoryResponse> categories = categoryService.getCategoriesByType(type);
        return ResponseEntity.ok(BaseResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(BaseResponse.success(categories, "Root categories retrieved successfully"));
    }

    @GetMapping("/root/type/{type}")
    @Operation(summary = "Get root categories by type")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getRootCategoriesByType(
            @PathVariable CategoryType type) {
        List<CategoryResponse> categories = categoryService.getRootCategoriesByType(type);
        return ResponseEntity.ok(BaseResponse.success(categories, "Root categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(BaseResponse.success(category, "Category retrieved successfully"));
    }

    @GetMapping("/{id}/with-children")
    @Operation(summary = "Get category with children")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryWithChildren(
            @PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryWithChildren(id);
        return ResponseEntity.ok(BaseResponse.success(category, "Category retrieved successfully"));
    }

    @GetMapping("/{parentId}/sub-categories")
    @Operation(summary = "Get sub-categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getSubCategories(
            @PathVariable Long parentId) {
        List<CategoryResponse> categories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(BaseResponse.success(categories, "Sub-categories retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Category deleted successfully"));
    }
}
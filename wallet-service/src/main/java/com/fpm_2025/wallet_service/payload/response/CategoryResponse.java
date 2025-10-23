package com.fpm_2025.wallet_service.payload.response;

import com.fpm_2025.wallet_service.entity.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private String iconPath;
    private CategoryType type;
    private List<CategoryResponse> children;
}

package com.fpm_2025.wallet_service.dto.payload.response;

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
    private Long userId;
    private String iconPath;
    private String color;
    private CategoryType type;
    private Integer depth;
    private Integer sortOrder;
    private List<CategoryResponse> children;
}

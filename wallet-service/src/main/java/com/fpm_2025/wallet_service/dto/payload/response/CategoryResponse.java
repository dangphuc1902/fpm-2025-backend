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
    private String iconPath;
    private CategoryType type;
    private List<CategoryResponse> children;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	public String getIconPath() {
		return iconPath;
	}
	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}
	public CategoryType getType() {
		return type;
	}
	public void setType(CategoryType type) {
		this.type = type;
	}
	public List<CategoryResponse> getChildren() {
		return children;
	}
	public void setChildren(List<CategoryResponse> children) {
		this.children = children;
	}
    
    
}

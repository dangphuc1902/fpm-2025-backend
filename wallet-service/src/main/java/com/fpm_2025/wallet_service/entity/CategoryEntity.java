package com.fpm_2025.wallet_service.entity;
import com.fpm_2025.wallet_service.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_name", columnList = "name"),
    @Index(name = "idx_categories_type", columnList = "type"),
    @Index(name = "idx_categories_parent_id", columnList = "parent_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

	public CategoryEntity getParent() {
		return parent;
	}

	public void setParent(CategoryEntity parent) {
		this.parent = parent;
	}

	public List<CategoryEntity> getChildren() {
		return children;
	}

	public void setChildren(List<CategoryEntity> children) {
		this.children = children;
	}

	public List<TransactionEntity> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<TransactionEntity> transactions) {
		this.transactions = transactions;
	}

	@Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "icon_path", length = 255)
    private String iconPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "category_type default 'expense'")
    private CategoryType type;

    // Self-referencing relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private CategoryEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CategoryEntity> children = new ArrayList<>();

    // Relationship with transactions
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();
}
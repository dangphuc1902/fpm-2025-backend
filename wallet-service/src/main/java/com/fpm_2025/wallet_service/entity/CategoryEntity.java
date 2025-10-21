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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
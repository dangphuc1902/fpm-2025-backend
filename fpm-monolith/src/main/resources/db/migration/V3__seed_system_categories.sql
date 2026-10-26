-- =============================================================================
-- V3: Seed 8 category he thong (user_id = NULL) khop voi danh sach cung tren app Android
--     (AddTransactionScreen: Food, Transport, Shopping, Bills, Entertainment, Health, Education, Other)
-- Dung NOT EXISTS vi UNIQUE(name, user_id, type) khong chan trung khi user_id = NULL
-- =============================================================================
INSERT INTO wallet.categories (name, user_id, type, color, depth, sort_order)
SELECT v.name, NULL, 'EXPENSE', v.color, 1, v.sort_order
FROM (
    SELECT 'Food' AS name, '#FF7043' AS color, 1 AS sort_order
    UNION ALL SELECT 'Transport',     '#42A5F5', 2
    UNION ALL SELECT 'Shopping',      '#AB47BC', 3
    UNION ALL SELECT 'Bills',         '#FFA726', 4
    UNION ALL SELECT 'Entertainment', '#EC407A', 5
    UNION ALL SELECT 'Health',        '#66BB6A', 6
    UNION ALL SELECT 'Education',     '#26C6DA', 7
    UNION ALL SELECT 'Other',         '#6C757D', 8
) v
WHERE NOT EXISTS (
    SELECT 1 FROM wallet.categories c WHERE c.name = v.name AND c.user_id IS NULL
);

-- =============================================================================
-- V2: Seed mock data cho môi trường test
-- Password của tất cả users: Password123!
-- BCrypt hash: $2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.
-- =============================================================================

-- -------------------------------------------------------
-- MOCK USERS
-- -------------------------------------------------------
INSERT IGNORE INTO auth.users (id, email, username, hashed_password, role, is_active)
VALUES
    (1, 'admin@fpm.com',  'admin_user',  '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'ADMIN', TRUE),
    (2, 'user1@fpm.com',  'test_user1',  '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'USER',  TRUE),
    (3, 'user2@fpm.com',  'test_user2',  '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'USER',  TRUE);

-- -------------------------------------------------------
-- MOCK WALLETS
-- -------------------------------------------------------
INSERT IGNORE INTO wallet.wallets (id, user_id, name, type, currency, balance, is_active)
VALUES
    (1, 2, 'Ví Tiền Mặt (Cá Nhân)', 'CASH',   'VND', 5000000.00,  TRUE),
    (2, 2, 'Tài Khoản VCB',          'BANK',   'VND', 15000000.00, TRUE),
    (3, 3, 'Ví Cá Nhân User2',       'CASH',   'VND', 2000000.00,  TRUE),
    (4, 2, 'Quỹ Gia Đình',           'SHARED', 'VND', 10000000.00, TRUE);

-- -------------------------------------------------------
-- MOCK TRANSACTIONS (user1)
-- -------------------------------------------------------
INSERT IGNORE INTO transaction.transactions
    (id, user_id, wallet_id, category_name, type, amount, currency, note, transaction_date)
VALUES
    (1, 2, 1, 'Ăn uống',      'EXPENSE', 150000,   'VND', 'Ăn sáng + trưa',    '2026-10-01 08:00:00'),
    (2, 2, 1, 'Di chuyển',    'EXPENSE', 50000,    'VND', 'Grab đi làm',        '2026-10-02 07:30:00'),
    (3, 2, 2, 'Lương',        'INCOME',  15000000, 'VND', 'Lương tháng 10',     '2026-10-05 09:00:00'),
    (4, 2, 1, 'Giải trí',     'EXPENSE', 200000,   'VND', 'Xem phim cuối tuần','2026-10-08 20:00:00'),
    (5, 2, 1, 'Mua sắm',      'EXPENSE', 500000,   'VND', 'Quần áo',            '2026-10-10 14:00:00'),
    (6, 2, 2, 'Thưởng',       'INCOME',  2000000,  'VND', 'Thưởng quý 3',       '2026-10-12 10:00:00');

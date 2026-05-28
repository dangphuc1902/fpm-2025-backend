-- Create database schemas
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS transaction;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS notification;

-- Schema auth
CREATE TABLE IF NOT EXISTS auth.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) UNIQUE,
    google_id VARCHAR(255) UNIQUE,
    username VARCHAR(100) UNIQUE,
    avatar_url VARCHAR(500),
    hashed_password VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS auth.families (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth.family_members (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL REFERENCES auth.families(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_family_user UNIQUE (family_id, user_id)
);

CREATE TABLE IF NOT EXISTS auth.family_invitations (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL REFERENCES auth.families(id) ON DELETE CASCADE,
    inviter_id BIGINT NOT NULL,
    invitee_email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth.user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth.login_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Schema wallet
CREATE TABLE IF NOT EXISTS wallet.wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    family_id BIGINT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'CASH',
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    icon VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wallet.wallet_permissions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallet.wallets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    permission_level VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_perm_user UNIQUE (wallet_id, user_id)
);

CREATE TABLE IF NOT EXISTS wallet.categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES wallet.categories(id) ON DELETE SET NULL,
    user_id BIGINT,
    icon_path VARCHAR(255),
    color VARCHAR(7) DEFAULT '#6C757D',
    type VARCHAR(20) NOT NULL DEFAULT 'EXPENSE',
    depth INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categories_name_user UNIQUE (name, user_id, type)
);

CREATE TABLE IF NOT EXISTS wallet.transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL REFERENCES wallet.wallets(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES wallet.categories(id) ON DELETE SET NULL,
    amount DECIMAL(15,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location_json JSONB,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Schema transaction
CREATE TABLE IF NOT EXISTS transaction.transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    note TEXT,
    location VARCHAR(255),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_transaction_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    source VARCHAR(50) DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction.transaction_attachments (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transaction.transactions(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction.recurring_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    category_id BIGINT,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    type VARCHAR(20) NOT NULL,
    description TEXT,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    start_date DATE NOT NULL,
    end_date DATE,
    next_run_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Schema reporting
CREATE TABLE IF NOT EXISTS reporting.transaction_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,
    total_income DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_expense DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_txn_summary UNIQUE (user_id, period)
);

CREATE TABLE IF NOT EXISTS reporting.monthly_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    total_income DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_expense DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    net_income DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    transaction_count INT NOT NULL DEFAULT 0,
    avg_daily_expense DECIMAL(15,2),
    top_expense_category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_monthly_user_month UNIQUE (user_id, year_month)
);

CREATE TABLE IF NOT EXISTS reporting.category_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    transaction_count INT NOT NULL DEFAULT 0,
    percentage DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cat_summary UNIQUE (user_id, year_month, category_id)
);

CREATE TABLE IF NOT EXISTS reporting.budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    amount_limit DECIMAL(15,2) NOT NULL,
    amount_used DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    year_month VARCHAR(7) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_budget_user_cat_month UNIQUE (user_id, category_id, year_month)
);

CREATE TABLE IF NOT EXISTS reporting.budget_alerts (
    id BIGSERIAL PRIMARY KEY,
    budget_id BIGINT NOT NULL REFERENCES reporting.budgets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    threshold_percent INT NOT NULL,
    amount_limit DECIMAL(15,2) NOT NULL,
    amount_used DECIMAL(15,2) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reporting.export_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    format VARCHAR(20) NOT NULL,
    period VARCHAR(7) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    file_size BIGINT,
    error_msg VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reporting.reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    data_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Schema notification
CREATE TABLE IF NOT EXISTS notification.fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_fcm_device UNIQUE (user_id, device_id)
);

CREATE TABLE IF NOT EXISTS notification.bank_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    raw_content TEXT NOT NULL,
    parsed_amount DECIMAL(15,2),
    parsed_type VARCHAR(20),
    parsed_account VARCHAR(100),
    parsed_note TEXT,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    checksum VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS notification.notification_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    payload_json TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_via VARCHAR(20) NOT NULL DEFAULT 'FCM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

-- Default Seed: System Categories
INSERT INTO wallet.categories (name, parent_id, user_id, icon_path, color, type, depth, sort_order) VALUES
    ('Ăn uống',     NULL, NULL, 'icon_food',         '#FF6B6B', 'EXPENSE', 1, 1),
    ('Di chuyển',   NULL, NULL, 'icon_transport',    '#4ECDC4', 'EXPENSE', 1, 2),
    ('Mua sắm',     NULL, NULL, 'icon_shopping',     '#45B7D1', 'EXPENSE', 1, 3),
    ('Giải trí',    NULL, NULL, 'icon_entertainment','#96CEB4', 'EXPENSE', 1, 4),
    ('Sức khỏe',    NULL, NULL, 'icon_health',       '#FFEAA7', 'EXPENSE', 1, 5),
    ('Giáo dục',    NULL, NULL, 'icon_education',    '#DDA0DD', 'EXPENSE', 1, 6),
    ('Hóa đơn',     NULL, NULL, 'icon_bills',        '#FF9F43', 'EXPENSE', 1, 7),
    ('Nhà cửa',     NULL, NULL, 'icon_housing',      '#A29BFE', 'EXPENSE', 1, 8),
    ('Khác',         NULL, NULL, 'icon_other',        '#6C757D', 'EXPENSE', 1, 9),
    ('Lương',        NULL, NULL, 'icon_salary',       '#28A745', 'INCOME',  1, 1),
    ('Thưởng',       NULL, NULL, 'icon_bonus',        '#20C997', 'INCOME',  1, 2),
    ('Đầu tư',      NULL, NULL, 'icon_investment',   '#17A2B8', 'INCOME',  1, 3),
    ('Freelance',    NULL, NULL, 'icon_freelance',    '#6F42C1', 'INCOME',  1, 4),
    ('Thu nhập khác',NULL, NULL, 'icon_other_income', '#6C757D', 'INCOME',  1, 5)
ON CONFLICT DO NOTHING;

-- Default Seed: Mock users (password: 'secret')
INSERT INTO auth.users (id, email, username, hashed_password, role, is_active) VALUES
(1, 'admin@fpm.com', 'admin_user', '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'ADMIN', true),
(2, 'user1@fpm.com', 'test_user1', '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'USER', true),
(3, 'user2@fpm.com', 'test_user2', '$2a$10$X/Vl.K/JmI21t0T3YQ/wV.Q4V/24I0y70y54P5gO.xG0M7B/0K1Q.', 'USER', true)
ON CONFLICT (id) DO NOTHING;
SELECT setval('auth.users_id_seq', 3);

-- Default Seed: Mock wallets
INSERT INTO wallet.wallets (id, user_id, family_id, name, type, currency, balance, is_active) VALUES
(1, 2, NULL, 'Ví Tiền Mặt (Cá Nhân)', 'CASH', 'VND', 5000000.00, true),
(2, 2, NULL, 'Tài Khoản VCB', 'BANK', 'VND', 15000000.00, true),
(3, 3, NULL, 'Ví Cá Nhân User2', 'CASH', 'VND', 2000000.00, true)
ON CONFLICT (id) DO NOTHING;
SELECT setval('wallet.wallets_id_seq', 3);

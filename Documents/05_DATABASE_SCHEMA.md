# 05 — Database Schema

> **Version:** 1.0 | **Updated:** 2026-05-15  
> **Database:** MySQL 8.0 | **Migration Tool:** Flyway

---

## Tổng quan

FPM dùng mô hình **Database per Service** — mỗi service có schema riêng biệt, không service nào được JOIN trực tiếp vào DB của service khác.

| Database | Service | Tables |
|----------|---------|--------|
| `user_auth_db` | `user-auth-service` | users, refresh_tokens, families, family_members, family_invitations, users_preferences |
| `wallet_db` | `wallet-service` | wallets, categories, wallet_permissions |
| `transaction_db` | `transaction-service` | transactions, transaction_attachments, recurring_transactions |
| `reporting_db` | `reporting-service` | monthly_summaries, category_summaries, budgets, budget_alerts, export_jobs, reports |
| `notification_db` | `notification-service` | notification_history, fcm_tokens, bank_notifications |

---

## 1. `user_auth_db` — Identity & Access

### Table: `users`

> 💡 **`hashed_password` nullable** — User đăng nhập bằng Google OAuth2 sẽ không có password.

```sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(100) UNIQUE,
    phone_number    VARCHAR(20)  UNIQUE,
    google_id       VARCHAR(255) UNIQUE,
    avatar_url      VARCHAR(500),
    hashed_password VARCHAR(255),           -- NULL nếu login bằng Google
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',   -- USER | ADMIN
    is_active       TINYINT(1)  NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login      TIMESTAMP NULL,

    INDEX idx_users_email       (email),
    INDEX idx_users_google_id   (google_id),
    INDEX idx_users_username    (username),
    INDEX idx_users_last_login  (last_login),
    INDEX idx_users_role        (role)
);
```

**Quan hệ:**
```
users (1) ──────── (*) refresh_tokens
users (1) ──────── (1) users_preferences
users (*) ────── (*) families  [qua family_members]
```

---

### Table: `refresh_tokens`

> 💡 **Multi-device support** — Một user có thể có nhiều refresh token (đăng nhập trên nhiều thiết bị). Mỗi record có `device_info` để phân biệt.

```sql
CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),            -- "Android 13, Samsung Galaxy S24"
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

### Table: `users_preferences`

```sql
CREATE TABLE users_preferences (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    language        VARCHAR(10) DEFAULT 'vi',       -- vi | en
    currency        VARCHAR(10) DEFAULT 'VND',      -- VND | USD | EUR
    theme           VARCHAR(20) DEFAULT 'light',    -- light | dark
    notifications_enabled TINYINT(1) DEFAULT 1,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

### Table: `families`

```sql
CREATE TABLE families (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    owner_id    BIGINT NOT NULL,               -- User tạo family
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (owner_id) REFERENCES users(id)
);
```

---

### Table: `family_members`

```sql
CREATE TABLE family_members (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(20) NOT NULL,     -- OWNER | ADMIN | MEMBER
    joined_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_family_user (family_id, user_id),
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
);
```

---

### Table: `family_invitations`

```sql
CREATE TABLE family_invitations (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id    BIGINT NOT NULL,
    inviter_id   BIGINT NOT NULL,          -- User gửi lời mời
    invitee_email VARCHAR(255) NOT NULL,   -- Email người được mời
    role         VARCHAR(20) NOT NULL,     -- Vai trò khi chấp nhận
    status       VARCHAR(20) NOT NULL,     -- PENDING | ACCEPTED | REJECTED
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,

    FOREIGN KEY (family_id)  REFERENCES families(id) ON DELETE CASCADE,
    FOREIGN KEY (inviter_id) REFERENCES users(id)
);
```

---

### ERD: user_auth_db

```
users
├── id (PK)
├── email (UNIQUE)
├── username (UNIQUE)
├── google_id (UNIQUE)
├── hashed_password
├── role
└── is_active
        │ 1
        │
        ├─── * refresh_tokens (user_id FK)
        │         ├── token (UNIQUE)
        │         ├── device_info
        │         └── expires_at
        │
        ├─── 1 users_preferences (user_id FK UNIQUE)
        │         ├── language
        │         └── currency
        │
        └─── * family_members (user_id FK)
                  ├── family_id FK ──► families
                  └── role
```

---

## 2. `wallet_db` — Wallet Management

### Table: `wallets`

> 💡 **`@Where(clause = "is_deleted = false")`** — Hibernate tự động thêm điều kiện này vào mọi query. Record bị "xóa" vẫn tồn tại trong DB nhưng bị ẩn hoàn toàn.

```sql
CREATE TABLE wallets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    family_id       BIGINT,                              -- NULL nếu ví cá nhân
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20)  NOT NULL DEFAULT 'CASH', -- CASH|CARD|BANK|INVESTMENT
    currency        VARCHAR(3)   NOT NULL DEFAULT 'VND',
    currency_symbol VARCHAR(5)   DEFAULT '₫',
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    icon            VARCHAR(50),
    is_active       TINYINT(1)  NOT NULL DEFAULT 1,
    is_deleted      TINYINT(1)  NOT NULL DEFAULT 0,      -- Soft delete flag
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_wallets_user_id       (user_id),
    INDEX idx_wallets_type          (type),
    INDEX idx_wallets_active        (is_active),
    INDEX idx_wallets_deleted       (is_deleted),
    INDEX idx_wallets_user_active   (user_id, is_active, is_deleted)  -- Composite
);
```

**Ràng buộc nghiệp vụ (BR-WALLET-05):** `is_active = false` → không được tạo transaction mới.

---

### Table: `categories`

```sql
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,                   -- NULL = system category (mặc định)
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20) NOT NULL,              -- INCOME | EXPENSE
    icon        VARCHAR(50),
    color       VARCHAR(7),                        -- Hex color: #FF5733
    is_default  TINYINT(1) DEFAULT 0,              -- 1 = category mặc định hệ thống
    is_active   TINYINT(1) DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_categories_user_id (user_id),
    INDEX idx_categories_type    (type)
);
```

---

### Table: `wallet_permissions`

> 💡 **Shared Wallet** — Cho phép user chia sẻ ví với người khác với các cấp quyền khác nhau.

```sql
CREATE TABLE wallet_permissions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,              -- User được cấp quyền
    permission_level VARCHAR(20) NOT NULL,        -- OWNER | WRITE | READ
    granted_by      BIGINT NOT NULL,              -- User cấp quyền
    granted_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_wallet_user (wallet_id, user_id),
    INDEX idx_wallet_permissions_wallet_id (wallet_id),
    INDEX idx_wallet_permissions_user_id   (user_id)
);
```

---

### ERD: wallet_db

```
wallets
├── id (PK)
├── user_id (no FK — references user_auth_db.users.id via gRPC)
├── family_id
├── balance DECIMAL(15,2)
├── type ENUM
└── is_deleted (soft delete)
        │ 1
        │
        └─── * wallet_permissions (wallet_id)
                  ├── user_id
                  └── permission_level (OWNER|WRITE|READ)

categories
├── id (PK)
├── user_id
├── type (INCOME|EXPENSE)
└── is_default
```

---

## 3. `transaction_db` — Transaction Management

### Table: `transactions`

```sql
CREATE TABLE transactions (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    wallet_id               BIGINT NOT NULL,
    category_id             BIGINT,                     -- NULL = uncategorized
    amount                  DECIMAL(15,2) NOT NULL,
    currency                VARCHAR(3) DEFAULT 'VND',
    type                    VARCHAR(20) NOT NULL,        -- INCOME | EXPENSE
    transaction_date        DATETIME NOT NULL,
    description             TEXT,
    note                    TEXT,
    location                VARCHAR(255),
    is_recurring            TINYINT(1) DEFAULT 0,
    recurring_transaction_id BIGINT,                    -- FK → recurring_transactions
    status                  VARCHAR(20) DEFAULT 'COMPLETED', -- COMPLETED|PENDING|CANCELLED
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_txn_user_id       (user_id),
    INDEX idx_txn_wallet_id     (wallet_id),
    INDEX idx_txn_category_id   (category_id),
    INDEX idx_txn_date          (transaction_date),
    INDEX idx_txn_type          (type)
);
```

**Ràng buộc nghiệp vụ (BR-TXN-02):** Trước khi INSERT, gọi gRPC `CheckSufficientBalance` — nếu `balance < amount` thì reject ngay.

---

### Table: `transaction_attachments`

```sql
CREATE TABLE transaction_attachments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_type       VARCHAR(50),          -- image/jpeg, application/pdf
    file_name       VARCHAR(255),
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);
```

---

### Table: `recurring_transactions`

> 💡 **Recurring Transaction** — Giao dịch lặp lại định kỳ (ví dụ: tiền thuê nhà mỗi tháng). Hệ thống tự động tạo `transactions` từ template này.

```sql
CREATE TABLE recurring_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    wallet_id       BIGINT NOT NULL,
    category_id     BIGINT,
    amount          DECIMAL(15,2) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    description     TEXT,
    frequency       VARCHAR(20) NOT NULL,   -- DAILY|WEEKLY|MONTHLY|YEARLY
    start_date      DATE NOT NULL,
    end_date        DATE,                   -- NULL = vô thời hạn
    next_run_date   DATE,
    is_active       TINYINT(1) DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### ERD: transaction_db

```
transactions
├── id (PK)
├── user_id     (no FK — resolved via JWT, verified via gRPC)
├── wallet_id   (no FK — verified via gRPC wallet-service)
├── category_id (no FK — resolved via gRPC wallet-service)
├── amount DECIMAL(15,2)
├── type (INCOME|EXPENSE)
├── status (COMPLETED|PENDING|CANCELLED)
└── is_recurring

    │ 1
    └── * transaction_attachments (transaction_id FK)
              └── file_url

recurring_transactions → generates → transactions (recurring_transaction_id)
```

---

## 4. `reporting_db` — Reporting & Analytics

### Table: `monthly_summaries`

> 💡 **Pre-aggregated data** — Thay vì query toàn bộ `transactions` mỗi lần load dashboard, reporting-service duy trì bảng tổng hợp sẵn. Update real-time khi nhận Kafka events.

```sql
CREATE TABLE monthly_summaries (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    year_month          VARCHAR(7) NOT NULL,        -- Format: "2026-05"
    total_income        DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_expense       DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_income          DECIMAL(15,2) NOT NULL DEFAULT 0, -- Computed: income - expense
    transaction_count   INT NOT NULL DEFAULT 0,
    avg_daily_expense   DECIMAL(15,2),
    top_expense_category VARCHAR(100),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,

    UNIQUE KEY uk_monthly_user_month (user_id, year_month),
    INDEX idx_monthly_user_id    (user_id),
    INDEX idx_monthly_year_month (year_month)
);
```

---

### Table: `category_summaries`

```sql
CREATE TABLE category_summaries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    year_month      VARCHAR(7) NOT NULL,
    category_id     BIGINT NOT NULL,
    category_name   VARCHAR(100),
    category_type   VARCHAR(20),            -- INCOME | EXPENSE
    total_amount    DECIMAL(15,2) NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    percentage      DECIMAL(5,2),           -- % của total_income hoặc total_expense

    INDEX idx_cat_summary_user_month (user_id, year_month)
);
```

---

### Table: `budgets`

```sql
CREATE TABLE budgets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    category_id     BIGINT NOT NULL,
    year_month      VARCHAR(7) NOT NULL,      -- Budget cho tháng nào
    budget_amount   DECIMAL(15,2) NOT NULL,
    spent_amount    DECIMAL(15,2) DEFAULT 0,  -- Cập nhật từ Kafka events
    alert_threshold DECIMAL(5,2) DEFAULT 80,  -- % để gửi cảnh báo
    is_alerted      TINYINT(1) DEFAULT 0,     -- Đã gửi cảnh báo chưa
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_budgets_user_id   (user_id),
    INDEX idx_budgets_user_month (user_id, year_month)
);
```

---

### Table: `budget_alerts`

```sql
CREATE TABLE budget_alerts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    budget_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    alert_type  VARCHAR(50),         -- WARNING (80%) | EXCEEDED (100%)
    message     TEXT,
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_sent     TINYINT(1) DEFAULT 0,

    INDEX idx_budget_alerts_user_id (user_id)
);
```

---

### Table: `export_jobs`

> 💡 **Async Export Pattern** — Export PDF/Excel có thể mất vài giây. Thay vì block request, hệ thống tạo `export_job` → trả ngay `jobId` → client polling status → khi `DONE` thì download.

```sql
CREATE TABLE export_jobs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    format      VARCHAR(10) NOT NULL,    -- PDF | EXCEL | CSV
    status      VARCHAR(20) NOT NULL,    -- PENDING | PROCESSING | DONE | FAILED
    file_url    VARCHAR(500),            -- URL download sau khi DONE
    file_name   VARCHAR(255),
    parameters  TEXT,                   -- JSON: {startDate, endDate, ...}
    error_msg   TEXT,                   -- Lỗi nếu FAILED
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    INDEX idx_export_jobs_user_id (user_id),
    INDEX idx_export_jobs_status  (status)
);
```

---

## 5. `notification_db` — Notifications

### Table: `notification_history`

```sql
CREATE TABLE notification_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT NOT NULL,
    type        VARCHAR(50),       -- TRANSACTION | BUDGET_ALERT | WELCOME | BALANCE
    is_read     TINYINT(1) DEFAULT 0,
    sent_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at     TIMESTAMP NULL,

    INDEX idx_notif_user_id (user_id),
    INDEX idx_notif_is_read (is_read)
);
```

---

### Table: `fcm_tokens`

> 💡 **FCM Token** — Firebase Cloud Messaging token là định danh của thiết bị di động. Mỗi thiết bị (điện thoại) có một token riêng để nhận push notification.

```sql
CREATE TABLE fcm_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(500) NOT NULL,
    device_type VARCHAR(20),          -- ANDROID | IOS | WEB
    device_name VARCHAR(100),
    is_active   TINYINT(1) DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_fcm_token (token),
    INDEX idx_fcm_user_id   (user_id)
);
```

---

### Table: `bank_notifications`

```sql
CREATE TABLE bank_notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    bank_name       VARCHAR(100),
    account_number  VARCHAR(50),
    amount          DECIMAL(15,2),
    transaction_type VARCHAR(20),        -- CREDIT | DEBIT
    raw_message     TEXT,               -- Nội dung SMS gốc từ ngân hàng
    parsed_status   VARCHAR(20),        -- PARSED | FAILED | IGNORED
    transaction_id  BIGINT,             -- ID transaction đã tạo từ notification này
    received_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Indexing Strategy

> 💡 **Database Index** — Cấu trúc dữ liệu phụ giúp MySQL tìm kiếm nhanh hơn mà không phải scan toàn bộ bảng. Đánh đổi: tốn thêm disk space và làm chậm INSERT/UPDATE, nhưng SELECT nhanh hơn đáng kể.

| Pattern | Index Type | Ví dụ |
|---------|-----------|-------|
| Filter by owner | Single column | `idx_wallets_user_id (user_id)` |
| Filter + active status | Composite | `idx_wallets_user_active (user_id, is_active, is_deleted)` |
| Unique constraints | UNIQUE | `uk_monthly_user_month (user_id, year_month)` |
| Date range queries | Single column | `idx_txn_date (transaction_date)` |

---

## Cross-Service Data References

Vì Database per Service, **không có foreign key vật lý** giữa các DB. Tham chiếu được đảm bảo qua application layer:

```
transaction_db.transactions.user_id
    → Verified via: JWT token (userId từ Gateway header)
    → NOT: DB foreign key → user_auth_db.users.id

transaction_db.transactions.wallet_id
    → Verified via: gRPC WalletService.ValidateWalletAccess()
    → NOT: DB foreign key → wallet_db.wallets.id

reporting_db.monthly_summaries.user_id
    → Populated via: Kafka transaction.created events
    → NOT: JOIN with transaction_db
```

---

> 📌 **Tiếp theo:** Xem `07_DEPLOYMENT_GUIDE.md` để biết cách deploy toàn bộ hệ thống.

-- ==============================================
-- MySQL Init Script for FPM-2025 Project
-- Tạo 3 databases cần thiết cho microservices
-- ==============================================

-- Tạo databases
CREATE DATABASE IF NOT EXISTS user_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS reporting_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant quyền cho user 'dev' (đã được tạo bởi MYSQL_USER env)
GRANT ALL PRIVILEGES ON user_auth_db.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON wallet_db.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON reporting_db.* TO 'dev'@'%';
FLUSH PRIVILEGES;

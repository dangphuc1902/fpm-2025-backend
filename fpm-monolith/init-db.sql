-- Create database schemas for Monolith MySQL
CREATE DATABASE IF NOT EXISTS auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS transaction CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS reporting CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to dev user
GRANT ALL PRIVILEGES ON auth.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON wallet.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON transaction.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON reporting.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON notification.* TO 'dev'@'%';

-- Grant privileges to root user
GRANT ALL PRIVILEGES ON auth.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON wallet.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON transaction.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON reporting.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON notification.* TO 'root'@'%';

FLUSH PRIVILEGES;

-- CQRS Database Setup Script
-- Creates two separate MySQL databases for Command and Query sides

-- =============================================
-- COMMAND DATABASE (Write Side)
-- =============================================
CREATE DATABASE IF NOT EXISTS guarantees_command_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Create user for command database
CREATE USER IF NOT EXISTS 'guarantees_command_user'@'localhost' IDENTIFIED BY 'guarantees_command_pass';
GRANT ALL PRIVILEGES ON guarantees_command_db.* TO 'guarantees_command_user'@'localhost';
FLUSH PRIVILEGES;

-- Use command database
USE guarantees_command_db;

-- Command database will contain the normalized entities
-- (This will be created by Hibernate based on your existing entities)

-- =============================================
-- QUERY DATABASE (Read Side)
-- =============================================
CREATE DATABASE IF NOT EXISTS guarantees_query_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Create user for query database
CREATE USER IF NOT EXISTS 'guarantees_query_user'@'localhost' IDENTIFIED BY 'guarantees_query_pass';
GRANT ALL PRIVILEGES ON guarantees_query_db.* TO 'guarantees_query_user'@'localhost';
FLUSH PRIVILEGES;

-- Use query database
USE guarantees_query_db;

-- Create denormalized view table for guarantees
CREATE TABLE IF NOT EXISTS guarantee_summary_view (
    guarantee_id VARCHAR(255) PRIMARY KEY,
    reference VARCHAR(255) NOT NULL,
    guarantee_type VARCHAR(100),
    amount DECIMAL(19,2),
    currency VARCHAR(3),
    issue_date DATE,
    expiry_date DATE,
    beneficiary_name VARCHAR(255),
    applicant_name VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Denormalized fields for better query performance
    currency_symbol VARCHAR(10),
    amount_in_usd DECIMAL(19,2),
    days_to_expiry INT,
    risk_level VARCHAR(20),
    region VARCHAR(100),
    
    -- Indexes for common queries
    INDEX idx_status (status),
    INDEX idx_currency (currency),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_guarantee_type (guarantee_type),
    INDEX idx_risk_level (risk_level),
    INDEX idx_applicant_name (applicant_name),
    INDEX idx_beneficiary_name (beneficiary_name),
    INDEX idx_created_at (created_at),
    INDEX idx_status_expiry (status, expiry_date),
    INDEX idx_currency_amount (currency, amount)
);

-- Create dashboard summary view
CREATE VIEW dashboard_summary AS
SELECT 
    COUNT(*) as total_guarantees,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_guarantees,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_guarantees,
    COUNT(CASE WHEN status = 'DRAFT' THEN 1 END) as draft_guarantees,
    COUNT(CASE WHEN risk_level = 'HIGH' THEN 1 END) as high_risk_guarantees,
    COUNT(CASE WHEN days_to_expiry <= 30 AND status = 'ACTIVE' THEN 1 END) as expiring_soon
FROM guarantee_summary_view;

-- Create monthly statistics view
CREATE VIEW monthly_statistics AS
SELECT 
    DATE_FORMAT(created_at, '%Y-%m') as month,
    COUNT(*) as guarantees_created,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_count,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_count
FROM guarantee_summary_view
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month DESC;

-- =============================================
-- KAFKA TOPICS SETUP (Manual)
-- =============================================
-- Note: These topics need to be created in Kafka
-- You can create them using Kafka commands or Kafka UI

-- Topics to create:
-- guarantee-created (3 partitions, 1 replication factor)
-- guarantee-updated (3 partitions, 1 replication factor)
-- guarantee-approved (3 partitions, 1 replication factor)
-- claim-submitted (3 partitions, 1 replication factor)
-- amendment-created (3 partitions, 1 replication factor)

-- =============================================
-- VERIFICATION QUERIES
-- =============================================

-- Verify command database
SELECT 'Command Database' as database_name, COUNT(*) as table_count
FROM information_schema.tables 
WHERE table_schema = 'guarantees_command_db';

-- Verify query database
SELECT 'Query Database' as database_name, COUNT(*) as table_count
FROM information_schema.tables 
WHERE table_schema = 'guarantees_query_db';

-- Show query database tables
SHOW TABLES FROM guarantees_query_db;

-- Show query database indexes
SHOW INDEX FROM guarantees_query_db.guarantee_summary_view;

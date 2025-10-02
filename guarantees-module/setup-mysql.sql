-- MySQL Database Setup Script for Guarantees Module
-- Run this script in MySQL to set up the database and user

-- Create database
CREATE DATABASE IF NOT EXISTS guarantees_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Create user and grant privileges
CREATE USER IF NOT EXISTS 'guarantees_user'@'localhost' IDENTIFIED BY 'guarantees_pass';
GRANT ALL PRIVILEGES ON guarantees_db.* TO 'guarantees_user'@'localhost';
FLUSH PRIVILEGES;

-- Use the database
USE guarantees_db;

-- Display database info
SELECT 'Database setup completed successfully!' as message;
SELECT 'Database name: guarantees_db' as info;
SELECT 'Username: guarantees_user' as info;
SELECT 'Password: guarantees_pass' as info;
SELECT 'JDBC URL: jdbc:mysql://localhost:3306/guarantees_db' as info;

-- Show database charset
SHOW VARIABLES LIKE 'character_set_database';
SHOW VARIABLES LIKE 'collation_database';





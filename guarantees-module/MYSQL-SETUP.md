# 🗄️ MySQL Database Setup for Guarantees Module

## 📋 Prerequisites

1. **Install MySQL Server** (version 8.0 or higher recommended)
   ```bash
   # macOS (with Homebrew)
   brew install mysql
   brew services start mysql
   
   # Ubuntu/Debian
   sudo apt update
   sudo apt install mysql-server
   sudo systemctl start mysql
   
   # Windows
   # Download from: https://dev.mysql.com/downloads/installer/
   ```

2. **Secure MySQL Installation** (recommended)
   ```bash
   sudo mysql_secure_installation
   ```

## 🚀 Quick Setup

### Step 1: Create Database and User
```bash
# Connect to MySQL as root
mysql -u root -p

# Run the setup script
source setup-mysql.sql

# Or manually run these commands:
CREATE DATABASE IF NOT EXISTS guarantees_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'guarantees_user'@'localhost' IDENTIFIED BY 'guarantees_pass';
GRANT ALL PRIVILEGES ON guarantees_db.* TO 'guarantees_user'@'localhost';
FLUSH PRIVILEGES;
exit;
```

### Step 2: Test Database Connection
```bash
# Test the new user connection
mysql -u guarantees_user -p guarantees_db
# Enter password: guarantees_pass

# Verify connection
SELECT 'Connection successful!' as message;
exit;
```

### Step 3: Start the Application
```bash
# Navigate to the guarantees module
cd guarantees-module

# Start both frontend and backend
./start-all.sh
```

## 🔧 Configuration Options

### Development Environment
The application will use these default settings:
- **Host**: localhost
- **Port**: 3306
- **Database**: guarantees_db
- **Username**: guarantees_user
- **Password**: guarantees_pass

### Production Environment
Set these environment variables:
```bash
export DATABASE_URL="jdbc:mysql://your-mysql-host:3306/guarantees_db?useSSL=true&serverTimezone=UTC"
export DATABASE_USERNAME="your_username"
export DATABASE_PASSWORD="your_password"

# Run with production profile
java -jar -Dspring.profiles.active=production target/guarantees-module-1.0.0-POC.jar
```

## 🔍 Troubleshooting

### Common Issues:

1. **Connection Refused**
   ```bash
   # Check MySQL is running
   sudo systemctl status mysql  # Linux
   brew services list | grep mysql  # macOS
   ```

2. **Authentication Failed**
   ```bash
   # Reset user password
   mysql -u root -p
   ALTER USER 'guarantees_user'@'localhost' IDENTIFIED BY 'guarantees_pass';
   FLUSH PRIVILEGES;
   ```

3. **Port Already in Use**
   ```bash
   # Check what's using port 3306
   sudo lsof -i :3306
   
   # Or use different port in application.yml
   url: jdbc:mysql://localhost:3307/guarantees_db
   ```

4. **Time Zone Issues**
   ```bash
   # Set MySQL time zone
   mysql -u root -p
   SET GLOBAL time_zone = '+00:00';
   ```

## 📊 Database Features Used

- **Character Set**: UTF-8 (utf8mb4) for international support
- **Connection Pooling**: HikariCP for optimal performance
- **Auto-DDL**: Tables created automatically on startup
- **Time Zone**: UTC for consistency
- **Indexes**: Automatically created by Hibernate

## 🔄 Migration from H2

Your existing H2 data won't be automatically migrated. To preserve data:

1. **Export data from H2** (if needed):
   - Access H2 console: http://localhost:8080/api/v1/h2-console
   - Export tables to SQL scripts

2. **Import to MySQL** (if needed):
   ```bash
   mysql -u guarantees_user -p guarantees_db < your_data_export.sql
   ```

3. **Fresh Start** (recommended for development):
   - The application will create all tables automatically
   - Test data will be loaded via DataInitializer

## ✅ Verification

Once setup is complete, verify everything works:

1. **Backend Health Check**:
   ```bash
   curl http://localhost:8080/api/v1/actuator/health
   ```

2. **Database Connection**:
   - Check application logs for successful database connection
   - Look for "HikariPool-1 - Start completed" message

3. **Test Data Loading**:
   - Navigate to http://localhost:3000
   - Login with admin/admin123
   - Verify test guarantees appear in the dashboard

## 🎯 Why MySQL?

- **Production Ready**: Enterprise-grade reliability and performance
- **Scalability**: Handles large datasets and concurrent users
- **ACID Compliance**: Ensures data integrity for financial operations
- **Rich Ecosystem**: Excellent tooling and monitoring options
- **Industry Standard**: Widely used in financial and business applications





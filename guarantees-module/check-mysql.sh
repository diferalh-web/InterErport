#!/bin/bash

# MySQL Setup Checker for Guarantees Module
echo "🔍 Checking MySQL setup for Guarantees Module..."
echo "================================================"

# Check if MySQL is installed
echo "1. Checking if MySQL is installed..."
if command -v mysql >/dev/null 2>&1; then
    echo "   ✅ MySQL client found"
    mysql --version
else
    echo "   ❌ MySQL client not found"
    echo "   📋 Install MySQL:"
    echo "      macOS: brew install mysql"
    echo "      Ubuntu: sudo apt install mysql-server"
    exit 1
fi

# Check if MySQL server is running
echo ""
echo "2. Checking if MySQL server is running..."
if pgrep -x "mysqld" > /dev/null; then
    echo "   ✅ MySQL server is running"
else
    echo "   ❌ MySQL server is not running"
    echo "   📋 Start MySQL:"
    echo "      macOS: brew services start mysql"
    echo "      Ubuntu: sudo systemctl start mysql"
    exit 1
fi

# Test connection (will prompt for root password)
echo ""
echo "3. Testing MySQL connection..."
echo "   (You may be prompted for MySQL root password)"
mysql -u root -p -e "SELECT 'Connection successful!' as status;" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "   ✅ MySQL root connection successful"
else
    echo "   ❌ Could not connect to MySQL as root"
    echo "   📋 Try: mysql_secure_installation"
fi

echo ""
echo "4. Next steps:"
echo "   📝 Run the database setup:"
echo "      mysql -u root -p < setup-mysql.sql"
echo ""
echo "   🚀 Then start the application:"
echo "      ./start-all.sh"
echo ""
echo "   📖 For detailed instructions, see: MYSQL-SETUP.md"
echo ""
echo "✅ MySQL check completed!"





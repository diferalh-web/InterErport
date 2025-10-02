#!/bin/bash

# Guarantees Module - Backend Startup Script
# This script ensures the backend starts reliably

echo "🚀 Starting Guarantees Module Backend..."

# Navigate to backend directory
cd "$(dirname "$0")/backend"

# Check if port 8080 is already in use
if lsof -ti :8080 > /dev/null 2>&1; then
    echo "⚠️  Port 8080 is already in use. Checking if it's our backend..."
    
    # Check if it's our Spring Boot app
    if curl -s -u admin:admin123 http://localhost:8080/api/v1/guarantees/health > /dev/null 2>&1; then
        echo "✅ Backend is already running and healthy!"
        echo "   Access: http://localhost:8080/api/v1"
        echo "   API Docs: http://localhost:8080/api/v1/swagger-ui.html"
        exit 0
    else
        echo "❌ Port 8080 is occupied by another process"
        echo "   Please stop the process using port 8080 or run:"
        echo "   lsof -ti :8080 | xargs kill -9"
        exit 1
    fi
fi

# Start the backend
echo "🔧 Starting Spring Boot application..."
mvn spring-boot:run -q &

# Wait for the application to start
echo "⏳ Waiting for backend to start..."
for i in {1..30}; do
    if curl -s -u admin:admin123 http://localhost:8080/api/v1/guarantees/health > /dev/null 2>&1; then
        echo "✅ Backend started successfully!"
        echo "   🌐 API Base URL: http://localhost:8080/api/v1"
        echo "   📚 API Documentation: http://localhost:8080/api/v1/swagger-ui.html"
        echo "   🗄️  H2 Database Console: http://localhost:8080/api/v1/h2-console"
        echo "      Username: sa (password: empty)"
        echo "   🔐 Authentication: admin / admin123"
        exit 0
    fi
    sleep 2
    echo -n "."
done

echo "❌ Backend failed to start within 60 seconds"
echo "   Check logs for errors: tail -f backend/app.log"
exit 1





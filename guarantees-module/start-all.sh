#!/bin/bash

# Guarantees Module - Complete Application Startup Script
# This script starts both backend and frontend in the correct order

echo "🚀 Starting Guarantees Module Application..."
echo "   This will start both backend (port 8080) and frontend (port 3000)"
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping applications..."
    # Kill background processes
    jobs -p | xargs -r kill
    exit
}
trap cleanup EXIT INT TERM

# Start backend first
echo "1️⃣  Starting Backend..."
./start-backend.sh
if [ $? -ne 0 ]; then
    echo "❌ Failed to start backend. Aborting."
    exit 1
fi

echo ""
echo "2️⃣  Starting Frontend..."
./start-frontend.sh
if [ $? -ne 0 ]; then
    echo "❌ Failed to start frontend. Backend is still running."
    exit 1
fi

echo ""
echo "🎉 Both applications started successfully!"
echo ""
echo "📋 Application Access:"
echo "   🖥️  Frontend (UI):          http://localhost:3000"
echo "   🔧 Backend API:            http://localhost:8080/api/v1"
echo "   📚 API Documentation:      http://localhost:8080/api/v1/swagger-ui.html"
echo "   🗄️  Database Console:       http://localhost:8080/api/v1/h2-console"
echo "   🔐 Authentication:         admin / admin123"
echo ""
echo "⚠️  Press Ctrl+C to stop both applications"

# Keep the script running
while true; do
    sleep 30
    # Check if both services are still running
    if ! curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "❌ Frontend stopped unexpectedly"
        break
    fi
    if ! curl -s -u admin:admin123 http://localhost:8080/api/v1/guarantees/health > /dev/null 2>&1; then
        echo "❌ Backend stopped unexpectedly"
        break
    fi
done





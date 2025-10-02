#!/bin/bash

# Guarantees Module - Frontend Startup Script
# This script ensures the frontend starts reliably

echo "🚀 Starting Guarantees Module Frontend..."

# Navigate to frontend directory
cd "$(dirname "$0")/frontend"

# Check if port 3000 is already in use
if lsof -ti :3000 > /dev/null 2>&1; then
    echo "⚠️  Port 3000 is already in use. Checking if it's our frontend..."
    
    # Check if it's our React app
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✅ Frontend is already running!"
        echo "   Access: http://localhost:3000"
        exit 0
    else
        echo "❌ Port 3000 is occupied by another process"
        echo "   Please stop the process using port 3000 or run:"
        echo "   lsof -ti :3000 | xargs kill -9"
        exit 1
    fi
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start the frontend
echo "🔧 Starting React development server..."
npm start &

# Wait for the application to start
echo "⏳ Waiting for frontend to start..."
for i in {1..30}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo "✅ Frontend started successfully!"
        echo "   🌐 Application URL: http://localhost:3000"
        echo "   🔗 Proxying API calls to: http://localhost:8080/api/v1"
        
        # Try to open in browser (macOS)
        if command -v open >/dev/null 2>&1; then
            open http://localhost:3000
        fi
        exit 0
    fi
    sleep 2
    echo -n "."
done

echo "❌ Frontend failed to start within 60 seconds"
echo "   Check the terminal output for errors"
exit 1





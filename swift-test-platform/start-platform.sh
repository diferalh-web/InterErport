#!/bin/bash

# SWIFT Test Platform Startup Script
# InterExport Guarantee Module

echo "🚀 Starting SWIFT Test Platform..."
echo "=================================="

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 16+ and try again."
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 16 ]; then
    echo "⚠️  Node.js version is $NODE_VERSION. Recommended version is 16 or higher."
fi

# Navigate to the platform directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

echo "📂 Working directory: $(pwd)"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    if command -v npm &> /dev/null; then
        npm install
    else
        echo "❌ npm is not available. Please install Node.js with npm."
        exit 1
    fi
else
    echo "✅ Dependencies already installed"
fi

# Create logs directory if it doesn't exist
mkdir -p logs

# Check if port 8081 is available
if lsof -i :8081 &> /dev/null; then
    echo "⚠️  Port 8081 is already in use. Attempting to kill existing process..."
    lsof -ti :8081 | xargs kill -9 2>/dev/null
    sleep 2
fi

# Set environment variables
export NODE_ENV=${NODE_ENV:-development}
export PORT=${PORT:-8081}
export MAX_HISTORY_SIZE=${MAX_HISTORY_SIZE:-1000}

echo "🔧 Configuration:"
echo "   - Environment: $NODE_ENV"
echo "   - Port: $PORT"
echo "   - Max History: $MAX_HISTORY_SIZE"

# Start the platform
echo ""
echo "🌟 Starting SWIFT Test Platform..."
echo "   📱 Web Interface: http://localhost:$PORT"
echo "   🔗 API Base: http://localhost:$PORT/api"
echo "   📡 WebSocket: ws://localhost:$PORT"
echo ""
echo "✅ Supported SWIFT Messages:"
echo "   - MT760: Issue of a Guarantee"
echo "   - MT765: Amendment to a Guarantee"
echo "   - MT767: Confirmation of Amendment"
echo "   - MT768: Acknowledgment"
echo "   - MT769: Advice of Discrepancy"
echo "   - MT798: Free Format Message"
echo ""
echo "🧪 Available Test Scenarios:"
echo "   - Guarantee Issuance (MT760 → MT768)"
echo "   - Guarantee Amendment (MT765 → MT767 → MT768)"
echo "   - Claim Processing (MT769)"
echo "   - Discrepancy Handling"
echo ""
echo "💡 Tips:"
echo "   - Use Ctrl+C to stop the platform"
echo "   - Check logs in the logs/ directory"
echo "   - Access API documentation at /api/health"
echo "   - Use the web interface for interactive testing"
echo ""
echo "=================================="

# Function to handle shutdown
cleanup() {
    echo ""
    echo "🛑 Shutting down SWIFT Test Platform..."
    echo "   Cleaning up resources..."
    # Kill any background processes if needed
    jobs -p | xargs -r kill 2>/dev/null
    echo "✅ Platform stopped successfully"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Start the server
if [ "$NODE_ENV" = "development" ]; then
    echo "🔄 Starting in development mode with auto-restart..."
    if command -v nodemon &> /dev/null; then
        nodemon server.js 2>&1 | tee logs/swift-platform.log
    else
        echo "⚠️  nodemon not found, falling back to node..."
        node server.js 2>&1 | tee logs/swift-platform.log
    fi
else
    echo "🚀 Starting in production mode..."
    node server.js 2>&1 | tee logs/swift-platform.log
fi





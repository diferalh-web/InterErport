#!/bin/bash

# Guarantees Module - Stop All Services Script

echo "🛑 Stopping Guarantees Module Applications..."

# Stop processes on port 3000 (Frontend)
echo "   Stopping Frontend (port 3000)..."
FRONTEND_PIDS=$(lsof -ti :3000)
if [ ! -z "$FRONTEND_PIDS" ]; then
    echo $FRONTEND_PIDS | xargs kill -TERM
    sleep 2
    # Force kill if still running
    FRONTEND_PIDS=$(lsof -ti :3000)
    if [ ! -z "$FRONTEND_PIDS" ]; then
        echo $FRONTEND_PIDS | xargs kill -9
        echo "   ✅ Frontend stopped (force killed)"
    else
        echo "   ✅ Frontend stopped gracefully"
    fi
else
    echo "   ℹ️  Frontend was not running"
fi

# Stop processes on port 8080 (Backend)
echo "   Stopping Backend (port 8080)..."
BACKEND_PIDS=$(lsof -ti :8080)
if [ ! -z "$BACKEND_PIDS" ]; then
    echo $BACKEND_PIDS | xargs kill -TERM
    sleep 3
    # Force kill if still running
    BACKEND_PIDS=$(lsof -ti :8080)
    if [ ! -z "$BACKEND_PIDS" ]; then
        echo $BACKEND_PIDS | xargs kill -9
        echo "   ✅ Backend stopped (force killed)"
    else
        echo "   ✅ Backend stopped gracefully"
    fi
else
    echo "   ℹ️  Backend was not running"
fi

echo "✅ All applications stopped"





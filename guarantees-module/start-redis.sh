#!/bin/bash

# Redis Startup Script for Guarantees Module
# This script starts Redis using Docker Compose

echo "🚀 Starting Redis for Guarantees Module..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Redis is already running
if docker ps | grep -q "guarantees-redis"; then
    echo "⚠️  Redis is already running. Stopping existing container..."
    docker-compose -f docker-compose.redis.yml down
fi

# Start Redis
echo "📦 Starting Redis container..."
docker-compose -f docker-compose.redis.yml up -d

# Wait for Redis to be ready
echo "⏳ Waiting for Redis to be ready..."
timeout=30
counter=0
while ! docker exec guarantees-redis redis-cli ping > /dev/null 2>&1; do
    if [ $counter -eq $timeout ]; then
        echo "❌ Redis failed to start within $timeout seconds"
        exit 1
    fi
    sleep 1
    counter=$((counter + 1))
done

echo "✅ Redis is ready!"
echo "🔗 Redis connection: localhost:6379"
echo "🌐 Redis Commander (Web UI): http://localhost:8081"
echo ""
echo "📊 To check Redis status:"
echo "   docker exec guarantees-redis redis-cli ping"
echo ""
echo "🛑 To stop Redis:"
echo "   docker-compose -f docker-compose.redis.yml down"


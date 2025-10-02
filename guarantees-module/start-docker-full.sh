#!/bin/bash

# Complete Docker Startup Script for CQRS Guarantees Module
# This script starts the entire application stack with all services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Navigate to the guarantees-module directory
cd "$(dirname "$0")" || exit

echo -e "${BLUE}🚀 Starting Complete CQRS Guarantees Module with Docker${NC}"
echo "=================================================="

# Check if Docker is running
echo -e "${YELLOW}📋 Checking Docker status...${NC}"
if ! docker info > /dev/null 2>&1; then
  echo -e "${RED}❌ Docker is not running. Please start Docker Desktop or Docker daemon first.${NC}"
  exit 1
fi
echo -e "${GREEN}✅ Docker is running${NC}"

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
  echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose.${NC}"
  exit 1
fi
echo -e "${GREEN}✅ Docker Compose is available${NC}"

# Clean up any existing containers
echo -e "${YELLOW}🧹 Cleaning up existing containers...${NC}"
docker-compose -f docker-compose.full.yml down --remove-orphans || true

# Remove any existing volumes (optional - uncomment if you want fresh start)
# echo -e "${YELLOW}🗑️  Removing existing volumes...${NC}"
# docker volume rm guarantees-module_mysql_command_data || true
# docker volume rm guarantees-module_mysql_query_data || true
# docker volume rm guarantees-module_kafka_data || true
# docker volume rm guarantees-module_redis_data || true

# Start all services
echo -e "${YELLOW}🚀 Starting all services...${NC}"
docker-compose -f docker-compose.full.yml --env-file docker.env up -d

# Wait for services to be healthy
echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"

# Wait for MySQL Command
echo -e "${YELLOW}📊 Waiting for MySQL Command database...${NC}"
while ! docker exec guarantees-mysql-command mysqladmin ping -h localhost -u root -prootpassword --silent; do
  echo "Still waiting for MySQL Command..."
  sleep 5
done
echo -e "${GREEN}✅ MySQL Command database is ready!${NC}"

# Wait for MySQL Query
echo -e "${YELLOW}📊 Waiting for MySQL Query database...${NC}"
while ! docker exec guarantees-mysql-query mysqladmin ping -h localhost -u root -prootpassword --silent; do
  echo "Still waiting for MySQL Query..."
  sleep 5
done
echo -e "${GREEN}✅ MySQL Query database is ready!${NC}"

# Wait for Kafka
echo -e "${YELLOW}📨 Waiting for Kafka...${NC}"
while ! docker exec guarantees-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "Still waiting for Kafka..."
  sleep 5
done
echo -e "${GREEN}✅ Kafka is ready!${NC}"

# Wait for Redis
echo -e "${YELLOW}🔴 Waiting for Redis...${NC}"
while ! docker exec guarantees-redis redis-cli -a redispassword ping > /dev/null 2>&1; do
  echo "Still waiting for Redis..."
  sleep 5
done
echo -e "${GREEN}✅ Redis is ready!${NC}"

# Create Kafka topics
echo -e "${YELLOW}📝 Creating Kafka topics...${NC}"
docker exec guarantees-kafka kafka-topics --create --topic guarantee-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec guarantees-kafka kafka-topics --create --topic guarantee-updated --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec guarantees-kafka kafka-topics --create --topic guarantee-approved --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec guarantees-kafka kafka-topics --create --topic claim-submitted --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
docker exec guarantees-kafka kafka-topics --create --topic amendment-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo -e "${GREEN}✅ All Kafka topics created!${NC}"

# Wait for Backend
echo -e "${YELLOW}🔧 Waiting for Backend application...${NC}"
while ! curl -f http://localhost:8082/api/v1/actuator/health > /dev/null 2>&1; do
  echo "Still waiting for Backend..."
  sleep 10
done
echo -e "${GREEN}✅ Backend application is ready!${NC}"

# Wait for Frontend
echo -e "${YELLOW}🎨 Waiting for Frontend application...${NC}"
while ! curl -f http://localhost:3000 > /dev/null 2>&1; do
  echo "Still waiting for Frontend..."
  sleep 5
done
echo -e "${GREEN}✅ Frontend application is ready!${NC}"

# Display service information
echo ""
echo -e "${GREEN}🎉 Complete CQRS Application is ready!${NC}"
echo "=================================================="
echo ""
echo -e "${BLUE}📊 Service URLs:${NC}"
echo "  🌐 Frontend Application:    http://localhost:3000"
echo "  🔧 Backend API:            http://localhost:8082/api/v1"
echo "  📊 CQRS Commands:          http://localhost:8082/api/v1/cqrs/commands"
echo "  📊 CQRS Queries:           http://localhost:8082/api/v1/cqrs/queries"
echo "  📈 Kafka UI:               http://localhost:8080"
echo "  🔴 Redis Commander:        http://localhost:8081"
echo "  🗄️  phpMyAdmin:            http://localhost:8083"
echo ""
echo -e "${BLUE}🗄️  Database Connections:${NC}"
echo "  Command DB: mysql://localhost:3306/guarantees_command_db"
echo "  Query DB:   mysql://localhost:3307/guarantees_query_db"
echo "  Kafka:      localhost:9092"
echo "  Redis:      redis://localhost:6379"
echo ""
echo -e "${BLUE}🔐 Default Credentials:${NC}"
echo "  Backend API: admin / admin123"
echo "  Command DB:  guarantees_command_user / guarantees_command_pass"
echo "  Query DB:    guarantees_query_user / guarantees_query_pass"
echo "  Redis:       (password: redispassword)"
echo "  phpMyAdmin:  root / rootpassword"
echo ""
echo -e "${BLUE}🧪 Test the Application:${NC}"
echo "  # Test Backend Health"
echo "  curl http://localhost:8082/api/v1/actuator/health"
echo ""
echo "  # Test CQRS Command (Create Guarantee)"
echo "  curl -X POST http://localhost:8082/api/v1/cqrs/commands/guarantees \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -u admin:admin123 \\"
echo "    -d '{\"reference\":\"GAR-2024-001\",\"guaranteeType\":\"BID_BOND\",\"amount\":100000.00,\"currency\":\"USD\"}'"
echo ""
echo "  # Test CQRS Query (Get All Guarantees)"
echo "  curl http://localhost:8082/api/v1/cqrs/queries/guarantees"
echo ""
echo -e "${BLUE}🛑 Management Commands:${NC}"
echo "  Stop all services:    docker-compose -f docker-compose.full.yml down"
echo "  View logs:           docker-compose -f docker-compose.full.yml logs -f"
echo "  Restart services:    docker-compose -f docker-compose.full.yml restart"
echo "  Scale backend:       docker-compose -f docker-compose.full.yml up -d --scale backend=2"
echo ""
echo -e "${GREEN}✨ Application is fully operational!${NC}"
echo "=================================================="

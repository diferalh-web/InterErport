#!/bin/bash

# Test Script for Docker CQRS Application
# This script tests all the main functionality of the application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧪 Testing CQRS Docker Application${NC}"
echo "=========================================="

# Test Backend Health
echo -e "${YELLOW}1. Testing Backend Health...${NC}"
if curl -f http://localhost:8082/api/v1/actuator/health > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Backend is healthy${NC}"
else
  echo -e "${RED}❌ Backend is not responding${NC}"
  exit 1
fi

# Test Frontend
echo -e "${YELLOW}2. Testing Frontend...${NC}"
if curl -f http://localhost:3000 > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Frontend is accessible${NC}"
else
  echo -e "${RED}❌ Frontend is not accessible${NC}"
  exit 1
fi

# Test CQRS Command - Create Guarantee
echo -e "${YELLOW}3. Testing CQRS Command (Create Guarantee)...${NC}"
GUARANTEE_ID=$(curl -s -X POST http://localhost:8082/api/v1/cqrs/commands/guarantees \
  -H 'Content-Type: application/json' \
  -u admin:admin123 \
  -d '{
    "reference": "GAR-TEST-001",
    "guaranteeType": "BID_BOND",
    "amount": 100000.00,
    "currency": "USD",
    "issueDate": "2024-01-01",
    "expiryDate": "2024-12-31",
    "beneficiaryName": "Test Beneficiary",
    "applicantId": 1,
    "guaranteeText": "Test guarantee for Docker testing",
    "language": "en"
  }' | jq -r '.guaranteeId')

if [ "$GUARANTEE_ID" != "null" ] && [ -n "$GUARANTEE_ID" ]; then
  echo -e "${GREEN}✅ Guarantee created successfully with ID: $GUARANTEE_ID${NC}"
else
  echo -e "${RED}❌ Failed to create guarantee${NC}"
  exit 1
fi

# Wait a moment for event processing
echo -e "${YELLOW}⏳ Waiting for event processing...${NC}"
sleep 5

# Test CQRS Query - Get All Guarantees
echo -e "${YELLOW}4. Testing CQRS Query (Get All Guarantees)...${NC}"
GUARANTEE_COUNT=$(curl -s http://localhost:8082/api/v1/cqrs/queries/guarantees | jq '. | length')

if [ "$GUARANTEE_COUNT" -gt 0 ]; then
  echo -e "${GREEN}✅ Retrieved $GUARANTEE_COUNT guarantees from query side${NC}"
else
  echo -e "${RED}❌ No guarantees found in query side${NC}"
  exit 1
fi

# Test Dashboard Summary
echo -e "${YELLOW}5. Testing Dashboard Summary...${NC}"
DASHBOARD_RESPONSE=$(curl -s http://localhost:8082/api/v1/cqrs/queries/guarantees/dashboard/summary)

if echo "$DASHBOARD_RESPONSE" | jq '.totalGuarantees' > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Dashboard summary is working${NC}"
  echo "Dashboard Data: $DASHBOARD_RESPONSE"
else
  echo -e "${RED}❌ Dashboard summary failed${NC}"
  exit 1
fi

# Test Redis Cache
echo -e "${YELLOW}6. Testing Redis Cache...${NC}"
if curl -s -u admin:admin123 http://localhost:8082/api/v1/redis/stats > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Redis cache is working${NC}"
else
  echo -e "${RED}❌ Redis cache is not accessible${NC}"
  exit 1
fi

# Test Kafka Topics
echo -e "${YELLOW}7. Testing Kafka Topics...${NC}"
TOPICS=$(docker exec guarantees-kafka kafka-topics --list --bootstrap-server localhost:9092)

if echo "$TOPICS" | grep -q "guarantee-created"; then
  echo -e "${GREEN}✅ Kafka topics are created${NC}"
else
  echo -e "${RED}❌ Kafka topics are missing${NC}"
  exit 1
fi

# Test Database Connections
echo -e "${YELLOW}8. Testing Database Connections...${NC}"

# Test Command DB
if docker exec guarantees-mysql-command mysql -u guarantees_command_user -pguarantees_command_pass -e "SELECT 1" > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Command database is accessible${NC}"
else
  echo -e "${RED}❌ Command database is not accessible${NC}"
  exit 1
fi

# Test Query DB
if docker exec guarantees-mysql-query mysql -u guarantees_query_user -pguarantees_query_pass -e "SELECT 1" > /dev/null 2>&1; then
  echo -e "${GREEN}✅ Query database is accessible${NC}"
else
  echo -e "${RED}❌ Query database is not accessible${NC}"
  exit 1
fi

# Test Event Processing
echo -e "${YELLOW}9. Testing Event Processing...${NC}"
sleep 10  # Wait for event processing

# Check if the guarantee appears in query side
QUERY_GUARANTEE=$(curl -s http://localhost:8082/api/v1/cqrs/queries/guarantees | jq '.[] | select(.reference == "GAR-TEST-001")')

if [ -n "$QUERY_GUARANTEE" ]; then
  echo -e "${GREEN}✅ Event processing is working - guarantee found in query side${NC}"
else
  echo -e "${RED}❌ Event processing failed - guarantee not found in query side${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}🎉 All tests passed! CQRS application is working correctly.${NC}"
echo "=========================================="
echo ""
echo -e "${BLUE}📊 Application Status:${NC}"
echo "  ✅ Backend API: Working"
echo "  ✅ Frontend: Working"
echo "  ✅ CQRS Commands: Working"
echo "  ✅ CQRS Queries: Working"
echo "  ✅ Event Processing: Working"
echo "  ✅ Redis Cache: Working"
echo "  ✅ Kafka: Working"
echo "  ✅ Command DB: Working"
echo "  ✅ Query DB: Working"
echo ""
echo -e "${BLUE}🌐 Access URLs:${NC}"
echo "  Frontend: http://localhost:3000"
echo "  Backend:  http://localhost:8082/api/v1"
echo "  Kafka UI: http://localhost:8080"
echo "  Redis UI: http://localhost:8081"
echo "  DB Admin: http://localhost:8083"
echo ""
echo -e "${GREEN}✨ Application is fully functional!${NC}"

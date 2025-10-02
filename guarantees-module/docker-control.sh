#!/bin/bash

# Master Docker Control Script for CQRS Guarantees Module
# This script provides easy control over the entire Docker setup

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to display menu
show_menu() {
    echo -e "${BLUE}🐳 CQRS Guarantees Module - Docker Control Panel${NC}"
    echo "=================================================="
    echo ""
    echo -e "${YELLOW}🚀 Quick Actions:${NC}"
    echo "  1. Start Complete Application (CQRS + All Services)"
    echo "  2. Start CQRS Infrastructure Only (DBs + Kafka + Redis)"
    echo "  3. Start Traditional Application (Single DB)"
    echo "  4. Test Application"
    echo ""
    echo -e "${YELLOW}🛠️  Management:${NC}"
    echo "  5. View Service Status"
    echo "  6. View Logs"
    echo "  7. Restart Services"
    echo "  8. Stop All Services"
    echo "  9. Clean Up (Remove Volumes)"
    echo ""
    echo -e "${YELLOW}🔧 Development:${NC}"
    echo "  10. Rebuild Backend"
    echo "  11. Rebuild Frontend"
    echo "  12. Access Database"
    echo "  13. Access Redis"
    echo "  14. Access Kafka"
    echo ""
    echo -e "${YELLOW}📊 Monitoring:${NC}"
    echo "  15. Open Kafka UI"
    echo "  16. Open Redis Commander"
    echo "  17. Open phpMyAdmin"
    echo "  18. Open Frontend"
    echo "  19. Open Backend API"
    echo ""
    echo -e "${YELLOW}❓ Help:${NC}"
    echo "  20. Show Help"
    echo "  0. Exit"
    echo ""
    echo -n "Enter your choice [0-20]: "
}

# Function to start complete application
start_complete() {
    echo -e "${GREEN}🚀 Starting Complete CQRS Application...${NC}"
    ./start-docker-full.sh
}

# Function to start CQRS infrastructure only
start_cqrs_infrastructure() {
    echo -e "${GREEN}🚀 Starting CQRS Infrastructure...${NC}"
    docker-compose -f docker-compose.cqrs.yml up -d
    echo -e "${GREEN}✅ CQRS Infrastructure started!${NC}"
    echo "Services: MySQL Command, MySQL Query, Kafka, Redis, Kafka UI, Redis Commander"
}

# Function to start traditional application
start_traditional() {
    echo -e "${GREEN}🚀 Starting Traditional Application...${NC}"
    docker-compose -f docker-compose.yml up -d
    echo -e "${GREEN}✅ Traditional Application started!${NC}"
}

# Function to test application
test_application() {
    echo -e "${GREEN}🧪 Testing Application...${NC}"
    ./test-docker-application.sh
}

# Function to view service status
view_status() {
    echo -e "${BLUE}📊 Service Status:${NC}"
    echo "=================="
    docker-compose -f docker-compose.full.yml ps
    echo ""
    echo -e "${BLUE}📈 Resource Usage:${NC}"
    echo "=================="
    docker stats --no-stream
}

# Function to view logs
view_logs() {
    echo -e "${YELLOW}📋 Available Services:${NC}"
    echo "1. Backend"
    echo "2. Frontend"
    echo "3. MySQL Command"
    echo "4. MySQL Query"
    echo "5. Kafka"
    echo "6. Redis"
    echo "7. All Services"
    echo ""
    read -p "Select service [1-7]: " service_choice
    
    case $service_choice in
        1) docker-compose -f docker-compose.full.yml logs -f backend ;;
        2) docker-compose -f docker-compose.full.yml logs -f frontend ;;
        3) docker-compose -f docker-compose.full.yml logs -f mysql-command ;;
        4) docker-compose -f docker-compose.full.yml logs -f mysql-query ;;
        5) docker-compose -f docker-compose.full.yml logs -f kafka ;;
        6) docker-compose -f docker-compose.full.yml logs -f redis ;;
        7) docker-compose -f docker-compose.full.yml logs -f ;;
        *) echo -e "${RED}Invalid choice${NC}" ;;
    esac
}

# Function to restart services
restart_services() {
    echo -e "${YELLOW}🔄 Restarting Services...${NC}"
    docker-compose -f docker-compose.full.yml restart
    echo -e "${GREEN}✅ Services restarted!${NC}"
}

# Function to stop all services
stop_all() {
    echo -e "${RED}🛑 Stopping All Services...${NC}"
    docker-compose -f docker-compose.full.yml down
    docker-compose -f docker-compose.cqrs.yml down
    docker-compose -f docker-compose.yml down
    echo -e "${GREEN}✅ All services stopped!${NC}"
}

# Function to clean up
cleanup() {
    echo -e "${RED}🧹 Cleaning Up (Removing Volumes)...${NC}"
    docker-compose -f docker-compose.full.yml down -v
    docker-compose -f docker-compose.cqrs.yml down -v
    docker-compose -f docker-compose.yml down -v
    docker system prune -f
    echo -e "${GREEN}✅ Cleanup completed!${NC}"
}

# Function to rebuild backend
rebuild_backend() {
    echo -e "${YELLOW}🔨 Rebuilding Backend...${NC}"
    docker-compose -f docker-compose.full.yml up -d --build backend
    echo -e "${GREEN}✅ Backend rebuilt!${NC}"
}

# Function to rebuild frontend
rebuild_frontend() {
    echo -e "${YELLOW}🔨 Rebuilding Frontend...${NC}"
    docker-compose -f docker-compose.full.yml up -d --build frontend
    echo -e "${GREEN}✅ Frontend rebuilt!${NC}"
}

# Function to access database
access_database() {
    echo -e "${YELLOW}🗄️  Database Access:${NC}"
    echo "1. Command Database (MySQL)"
    echo "2. Query Database (MySQL)"
    echo ""
    read -p "Select database [1-2]: " db_choice
    
    case $db_choice in
        1) docker exec -it guarantees-mysql-command mysql -u guarantees_command_user -pguarantees_command_pass ;;
        2) docker exec -it guarantees-mysql-query mysql -u guarantees_query_user -pguarantees_query_pass ;;
        *) echo -e "${RED}Invalid choice${NC}" ;;
    esac
}

# Function to access Redis
access_redis() {
    echo -e "${GREEN}🔴 Accessing Redis...${NC}"
    docker exec -it guarantees-redis redis-cli -a redispassword
}

# Function to access Kafka
access_kafka() {
    echo -e "${GREEN}📨 Accessing Kafka...${NC}"
    echo "Available commands:"
    echo "  kafka-topics --list --bootstrap-server localhost:9092"
    echo "  kafka-console-producer --topic test --bootstrap-server localhost:9092"
    echo "  kafka-console-consumer --topic test --bootstrap-server localhost:9092 --from-beginning"
    echo ""
    docker exec -it guarantees-kafka bash
}

# Function to open web interfaces
open_interface() {
    local interface=$1
    local url=$2
    local name=$3
    
    echo -e "${GREEN}🌐 Opening $name...${NC}"
    if command -v open &> /dev/null; then
        open $url
    elif command -v xdg-open &> /dev/null; then
        xdg-open $url
    else
        echo "Please open: $url"
    fi
}

# Function to show help
show_help() {
    echo -e "${BLUE}📚 Help - CQRS Guarantees Module Docker Setup${NC}"
    echo "=================================================="
    echo ""
    echo -e "${YELLOW}🏗️  Architecture:${NC}"
    echo "  Frontend (React) → Backend (Spring Boot) → Command DB (MySQL)"
    echo "  Backend → Kafka → Query Handler → Query DB (MySQL)"
    echo "  Backend → Redis (Caching & Sessions)"
    echo ""
    echo -e "${YELLOW}🚀 Quick Start:${NC}"
    echo "  1. Choose option 1 to start complete application"
    echo "  2. Wait for all services to be ready"
    echo "  3. Choose option 4 to test the application"
    echo "  4. Access Frontend at http://localhost:3000"
    echo ""
    echo -e "${YELLOW}🔧 Development:${NC}"
    echo "  - Use option 10 to rebuild backend after code changes"
    echo "  - Use option 11 to rebuild frontend after code changes"
    echo "  - Use option 6 to view logs for debugging"
    echo ""
    echo -e "${YELLOW}📊 Monitoring:${NC}"
    echo "  - Kafka UI: http://localhost:8080"
    echo "  - Redis Commander: http://localhost:8081"
    echo "  - phpMyAdmin: http://localhost:8083"
    echo ""
    echo -e "${YELLOW}🛠️  Troubleshooting:${NC}"
    echo "  - Check service status with option 5"
    echo "  - View logs with option 6"
    echo "  - Restart services with option 7"
    echo "  - Clean up with option 9 if needed"
    echo ""
    echo -e "${YELLOW}📞 Support:${NC}"
    echo "  - Check DOCKER-README.md for detailed documentation"
    echo "  - View container logs for error details"
    echo "  - Ensure all ports are available"
    echo ""
}

# Main menu loop
while true; do
    clear
    show_menu
    read choice
    
    case $choice in
        1) start_complete ;;
        2) start_cqrs_infrastructure ;;
        3) start_traditional ;;
        4) test_application ;;
        5) view_status ;;
        6) view_logs ;;
        7) restart_services ;;
        8) stop_all ;;
        9) cleanup ;;
        10) rebuild_backend ;;
        11) rebuild_frontend ;;
        12) access_database ;;
        13) access_redis ;;
        14) access_kafka ;;
        15) open_interface "kafka" "http://localhost:8080" "Kafka UI" ;;
        16) open_interface "redis" "http://localhost:8081" "Redis Commander" ;;
        17) open_interface "phpmyadmin" "http://localhost:8083" "phpMyAdmin" ;;
        18) open_interface "frontend" "http://localhost:3000" "Frontend" ;;
        19) open_interface "backend" "http://localhost:8082/api/v1" "Backend API" ;;
        20) show_help ;;
        0) echo -e "${GREEN}👋 Goodbye!${NC}"; exit 0 ;;
        *) echo -e "${RED}Invalid option. Please try again.${NC}" ;;
    esac
    
    echo ""
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read
done

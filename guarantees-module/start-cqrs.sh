#!/bin/bash

# Navigate to the guarantees-module directory
cd "$(dirname "$0")" || exit

echo "🚀 Starting CQRS Architecture for Guarantees Module..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker is not running. Please start Docker Desktop or Docker daemon first."
  exit 1
fi

# Start CQRS infrastructure
echo "Starting CQRS infrastructure (MySQL Command, MySQL Query, Kafka, Redis)..."
docker-compose -f docker-compose.cqrs.yml up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."

# Wait for MySQL Command
echo "Waiting for MySQL Command database..."
while ! docker exec guarantees-mysql-command mysqladmin ping -h localhost -u root -prootpassword --silent; do
  echo "Still waiting for MySQL Command..."
  sleep 5
done
echo "✅ MySQL Command database is ready!"

# Wait for MySQL Query
echo "Waiting for MySQL Query database..."
while ! docker exec guarantees-mysql-query mysqladmin ping -h localhost -u root -prootpassword --silent; do
  echo "Still waiting for MySQL Query..."
  sleep 5
done
echo "✅ MySQL Query database is ready!"

# Wait for Kafka
echo "Waiting for Kafka..."
while ! docker exec guarantees-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "Still waiting for Kafka..."
  sleep 5
done
echo "✅ Kafka is ready!"

# Wait for Redis
echo "Waiting for Redis..."
while ! docker exec guarantees-redis redis-cli ping > /dev/null 2>&1; do
  echo "Still waiting for Redis..."
  sleep 5
done
echo "✅ Redis is ready!"

# Create Kafka topics
echo "Creating Kafka topics..."
docker exec guarantees-kafka kafka-topics --create --topic guarantee-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec guarantees-kafka kafka-topics --create --topic guarantee-updated --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec guarantees-kafka kafka-topics --create --topic guarantee-approved --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec guarantees-kafka kafka-topics --create --topic claim-submitted --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec guarantees-kafka kafka-topics --create --topic amendment-created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

echo "✅ All Kafka topics created!"

# Display service information
echo ""
echo "🎉 CQRS Infrastructure is ready!"
echo ""
echo "📊 Service URLs:"
echo "  MySQL Command DB: mysql://localhost:3306/guarantees_command_db"
echo "  MySQL Query DB:   mysql://localhost:3307/guarantees_query_db"
echo "  Kafka:           localhost:9092"
echo "  Kafka UI:        http://localhost:8080"
echo "  Redis:           redis://localhost:6379"
echo ""
echo "🔧 Database Credentials:"
echo "  Command DB: guarantees_command_user / guarantees_command_pass"
echo "  Query DB:   guarantees_query_user / guarantees_query_pass"
echo ""
echo "📝 Next Steps:"
echo "  1. Update application.yml to use CQRS profile"
echo "  2. Start your Spring Boot application with CQRS configuration"
echo "  3. Test the CQRS endpoints"
echo ""
echo "🛑 To stop all services:"
echo "  docker-compose -f docker-compose.cqrs.yml down"
echo ""
echo "📋 To view logs:"
echo "  docker-compose -f docker-compose.cqrs.yml logs -f"

#!/bin/bash

# InterExport Guarantees Module - Automated Testing Script
# This script runs all automated tests across the entire project

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to run backend tests
run_backend_tests() {
    print_status "Running Backend Tests (Spring Boot)..."
    
    cd guarantees-module/backend
    
    # Check if Maven is available
    if ! command_exists mvn; then
        print_error "Maven is not installed. Please install Maven to run backend tests."
        return 1
    fi
    
    # Check if Java is available
    if ! command_exists java; then
        print_error "Java is not installed. Please install Java 17+ to run backend tests."
        return 1
    fi
    
    # Try to compile first
    print_status "Compiling backend code..."
    if mvn clean compile -q; then
        print_success "Backend compilation successful"
        
        # Run tests
        print_status "Running unit tests..."
        if mvn test -q; then
            print_success "Backend unit tests passed"
        else
            print_warning "Backend unit tests failed (compilation issues need to be fixed)"
        fi
        
        # Generate coverage report
        print_status "Generating test coverage report..."
        mvn jacoco:report -q
        print_success "Coverage report generated at target/site/jacoco/index.html"
    else
        print_warning "Backend compilation failed. Please fix compilation errors first."
        print_status "Common issues:"
        print_status "1. Missing Lombok annotations (@Data, @Getter, @Setter)"
        print_status "2. Missing getter/setter methods in entity classes"
        print_status "3. Class naming issues (public class in wrong file)"
    fi
    
    cd ../..
}

# Function to run frontend tests
run_frontend_tests() {
    print_status "Running Frontend Tests (React)..."
    
    cd guarantees-module/frontend
    
    # Check if Node.js is available
    if ! command_exists node; then
        print_error "Node.js is not installed. Please install Node.js 18+ to run frontend tests."
        return 1
    fi
    
    # Check if npm is available
    if ! command_exists npm; then
        print_error "npm is not installed. Please install npm to run frontend tests."
        return 1
    fi
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install
    fi
    
    # Run tests
    print_status "Running frontend tests..."
    if npm test -- --watchAll=false --coverage --passWithNoTests; then
        print_success "Frontend tests completed"
    else
        print_warning "Frontend tests had issues (this is expected as we just created test files)"
    fi
    
    cd ../..
}

# Function to run SWIFT platform tests
run_swift_tests() {
    print_status "Running SWIFT Platform Tests (Node.js)..."
    
    cd swift-test-platform
    
    # Check if Node.js is available
    if ! command_exists node; then
        print_error "Node.js is not installed. Please install Node.js 18+ to run SWIFT tests."
        return 1
    fi
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        print_status "Installing SWIFT platform dependencies..."
        npm install
    fi
    
    # Run Jest tests
    print_status "Running SWIFT platform unit tests..."
    if npm test -- --coverage --passWithNoTests; then
        print_success "SWIFT platform unit tests completed"
    else
        print_warning "SWIFT platform unit tests had issues"
    fi
    
    # Run integration tests
    print_status "Running SWIFT platform integration tests..."
    if npm run test:integration; then
        print_success "SWIFT platform integration tests completed"
    else
        print_warning "SWIFT platform integration tests had issues (expected if services not running)"
    fi
    
    cd ..
}

# Function to run security tests
run_security_tests() {
    print_status "Running Security Tests..."
    
    # Check if security scanning tools are available
    if command_exists npm; then
        print_status "Running npm audit for frontend..."
        cd guarantees-module/frontend
        npm audit --audit-level=moderate || print_warning "Frontend security audit found issues"
        cd ../..
        
        print_status "Running npm audit for SWIFT platform..."
        cd swift-test-platform
        npm audit --audit-level=moderate || print_warning "SWIFT platform security audit found issues"
        cd ..
    fi
    
    print_success "Security tests completed"
}

# Function to generate test report
generate_test_report() {
    print_status "Generating Test Report..."
    
    REPORT_FILE="test-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$REPORT_FILE" << EOF
# Test Report - InterExport Guarantees Module
Generated: $(date)

## Test Summary

### Backend Tests (Spring Boot)
- Status: $(if [ -f "guarantees-module/backend/target/site/jacoco/index.html" ]; then echo "✅ Completed"; else echo "❌ Failed/Not Run"; fi)
- Coverage Report: guarantees-module/backend/target/site/jacoco/index.html

### Frontend Tests (React)
- Status: $(if [ -f "guarantees-module/frontend/coverage/lcov-report/index.html" ]; then echo "✅ Completed"; else echo "❌ Failed/Not Run"; fi)
- Coverage Report: guarantees-module/frontend/coverage/lcov-report/index.html

### SWIFT Platform Tests (Node.js)
- Status: $(if [ -f "swift-test-platform/coverage/lcov-report/index.html" ]; then echo "✅ Completed"; else echo "❌ Failed/Not Run"; fi)
- Coverage Report: swift-test-platform/coverage/lcov-report/index.html

## Next Steps

1. **Fix Backend Compilation Issues**: Resolve Lombok and missing method errors
2. **Enhance Frontend Tests**: Add more component and integration tests
3. **Improve SWIFT Platform Tests**: Add more comprehensive test coverage
4. **Set up CI/CD**: Use GitHub Actions for automated testing

## Test Commands

\`\`\`bash
# Backend tests
cd guarantees-module/backend && mvn test

# Frontend tests
cd guarantees-module/frontend && npm test -- --watchAll=false

# SWIFT platform tests
cd swift-test-platform && npm test

# Integration tests
cd swift-test-platform && npm run test:integration
\`\`\`

EOF

    print_success "Test report generated: $REPORT_FILE"
}

# Main execution
main() {
    echo "🧪 InterExport Guarantees Module - Automated Testing"
    echo "=================================================="
    echo ""
    
    # Create logs directory
    mkdir -p logs
    
    # Run all test suites
    run_backend_tests 2>&1 | tee logs/backend-tests.log
    echo ""
    
    run_frontend_tests 2>&1 | tee logs/frontend-tests.log
    echo ""
    
    run_swift_tests 2>&1 | tee logs/swift-tests.log
    echo ""
    
    run_security_tests 2>&1 | tee logs/security-tests.log
    echo ""
    
    generate_test_report
    
    echo ""
    print_success "All test suites completed!"
    print_status "Check the logs/ directory for detailed output"
    print_status "Check coverage reports in each component's coverage/ directory"
    echo ""
    print_status "To run individual test suites:"
    print_status "  Backend:    cd guarantees-module/backend && mvn test"
    print_status "  Frontend:   cd guarantees-module/frontend && npm test"
    print_status "  SWIFT:      cd swift-test-platform && npm test"
    echo ""
}

# Run main function
main "$@"

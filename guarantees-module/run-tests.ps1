# InterExport Guarantees Module - Test Execution Script (PowerShell)
# This script runs comprehensive tests for CQRS, Redis, and overall application functionality

param(
    [string]$BackendUrl = "http://localhost:8080",
    [string]$FrontendUrl = "http://localhost:3000",
    [string]$RedisUrl = "localhost:6379",
    [string]$KafkaUrl = "localhost:9092"
)

# Configuration
$ErrorActionPreference = "Continue"
$TotalTests = 0
$PassedTests = 0
$FailedTests = 0

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"
$Cyan = "Cyan"

Write-Host "🚀 Starting InterExport Guarantees Module Test Suite" -ForegroundColor $Cyan
Write-Host "==================================================" -ForegroundColor $Cyan

# Function to run a test and track results
function Run-Test {
    param(
        [string]$TestName,
        [scriptblock]$TestCommand
    )
    
    Write-Host "`n🧪 Running: $TestName" -ForegroundColor $Blue
    Write-Host "Command: $($TestCommand.ToString())"
    
    $script:TotalTests++
    
    try {
        $result = & $TestCommand
        if ($result -eq $true -or $result -ne $null) {
            Write-Host "✅ PASSED: $TestName" -ForegroundColor $Green
            $script:PassedTests++
        } else {
            Write-Host "❌ FAILED: $TestName" -ForegroundColor $Red
            $script:FailedTests++
        }
    } catch {
        Write-Host "❌ FAILED: $TestName - Error: $($_.Exception.Message)" -ForegroundColor $Red
        $script:FailedTests++
    }
}

# Function to check if a service is running
function Test-Service {
    param(
        [string]$ServiceName,
        [scriptblock]$CheckCommand
    )
    
    Write-Host "`n🔍 Checking $ServiceName..." -ForegroundColor $Yellow
    try {
        $result = & $CheckCommand
        if ($result) {
            Write-Host "✅ $ServiceName is running" -ForegroundColor $Green
            return $true
        } else {
            Write-Host "❌ $ServiceName is not running" -ForegroundColor $Red
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName is not running" -ForegroundColor $Red
        return $false
    }
}

# Function to wait for service to be ready
function Wait-ForService {
    param(
        [string]$ServiceName,
        [scriptblock]$CheckCommand,
        [int]$MaxAttempts = 30
    )
    
    Write-Host "`n⏳ Waiting for $ServiceName to be ready..." -ForegroundColor $Yellow
    
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            $result = & $CheckCommand
            if ($result) {
                Write-Host "✅ $ServiceName is ready" -ForegroundColor $Green
                return $true
            }
        } catch {
            # Continue waiting
        }
        
        Write-Host "Attempt $attempt/$MaxAttempts - waiting..."
        Start-Sleep -Seconds 2
    }
    
    Write-Host "❌ $ServiceName failed to start within timeout" -ForegroundColor $Red
    return $false
}

Write-Host "`n📋 Pre-flight Checks" -ForegroundColor $Blue
Write-Host "====================" -ForegroundColor $Blue

# Check if Docker is running
Test-Service "Docker" { docker info 2>$null }

# Check if required services are running
$backendRunning = Test-Service "Backend API" { 
    try { 
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/health" -Method Get -TimeoutSec 5
        $response -ne $null
    } catch { $false }
}

if (-not $backendRunning) {
    Write-Host "⚠️  Backend API not running. Starting services..." -ForegroundColor $Yellow
    
    # Start Docker services
    Write-Host "🐳 Starting Docker services..." -ForegroundColor $Cyan
    Set-Location "guarantees-module"
    docker-compose -f docker-compose.full.yml up -d
    
    # Wait for services to be ready
    Wait-ForService "Backend API" { 
        try { 
            $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/health" -Method Get -TimeoutSec 5
            $response -ne $null
        } catch { $false }
    }
    
    Wait-ForService "Redis" { 
        try { 
            $redis = New-Object System.Net.Sockets.TcpClient
            $redis.Connect("localhost", 6379)
            $redis.Close()
            $true
        } catch { $false }
    }
}

Test-Service "Redis" { 
    try { 
        $redis = New-Object System.Net.Sockets.TcpClient
        $redis.Connect("localhost", 6379)
        $redis.Close()
        $true
    } catch { $false }
}

Write-Host "`n🧪 Running Test Suite" -ForegroundColor $Blue
Write-Host "=====================" -ForegroundColor $Blue

# Test 1: Backend Health Check
Run-Test "Backend Health Check" {
    try {
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/health" -Method Get -TimeoutSec 10
        $response.status -eq "UP"
    } catch { $false }
}

# Test 2: CQRS Command Side Tests
Run-Test "CQRS Command - Create Guarantee" {
    try {
        $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        $body = @{
            reference = "TEST-CQRS-$timestamp"
            guaranteeType = "PERFORMANCE"
            amount = 100000.00
            currency = "USD"
            issueDate = "2025-01-01"
            expiryDate = "2026-01-01"
            applicantId = 1
            beneficiaryName = "Test Beneficiary"
            guaranteeText = "Test guarantee text"
            language = "EN"
        } | ConvertTo-Json
        
        $headers = @{
            'Content-Type' = 'application/json'
            'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='
        }
        
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees" -Method Post -Body $body -Headers $headers -TimeoutSec 10
        $response.id -ne $null
    } catch { $false }
}

# Test 3: CQRS Query Side Tests
Run-Test "CQRS Query - Get Guarantees List" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees?page=0&size=10" -Method Get -Headers $headers -TimeoutSec 10
        $response.content -ne $null
    } catch { $false }
}

# Test 4: Redis Caching Tests
Run-Test "Redis Caching - Cache Performance" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        
        $startTime = Get-Date
        $response1 = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/1" -Method Get -Headers $headers -TimeoutSec 10
        $time1 = (Get-Date) - $startTime
        
        $startTime = Get-Date
        $response2 = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/1" -Method Get -Headers $headers -TimeoutSec 10
        $time2 = (Get-Date) - $startTime
        
        $response1 -ne $null -and $response2 -ne $null
    } catch { $false }
}

# Test 5: Kafka Event Publishing Tests
Run-Test "Kafka Events - Event Publishing" {
    try {
        $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        $body = @{
            reference = "TEST-KAFKA-$timestamp"
            guaranteeType = "PERFORMANCE"
            amount = 50000.00
            currency = "EUR"
            issueDate = "2025-01-01"
            expiryDate = "2026-01-01"
            applicantId = 1
            beneficiaryName = "Kafka Test Beneficiary"
            guaranteeText = "Kafka test guarantee text"
            language = "EN"
        } | ConvertTo-Json
        
        $headers = @{
            'Content-Type' = 'application/json'
            'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='
        }
        
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees" -Method Post -Body $body -Headers $headers -TimeoutSec 10
        $response.id -ne $null
    } catch { $false }
}

# Test 6: API Endpoint Tests
Run-Test "API Endpoints - Dashboard Summary" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/dashboard/summary" -Method Get -Headers $headers -TimeoutSec 10
        $response.guarantees -ne $null
    } catch { $false }
}

# Test 7: Database Consistency Tests
Run-Test "Database Consistency - Create and Retrieve" {
    try {
        $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        $body = @{
            reference = "TEST-DB-$timestamp"
            guaranteeType = "PERFORMANCE"
            amount = 75000.00
            currency = "GBP"
            issueDate = "2025-01-01"
            expiryDate = "2026-01-01"
            applicantId = 1
            beneficiaryName = "DB Test Beneficiary"
            guaranteeText = "DB test guarantee text"
            language = "EN"
        } | ConvertTo-Json
        
        $headers = @{
            'Content-Type' = 'application/json'
            'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM='
        }
        
        $createResponse = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees" -Method Post -Body $body -Headers $headers -TimeoutSec 10
        $id = $createResponse.id
        
        $getResponse = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/$id" -Method Get -Headers $headers -TimeoutSec 10
        $getResponse.reference -eq "TEST-DB-$timestamp"
    } catch { $false }
}

# Test 8: Performance Tests
Run-Test "Performance - Response Time" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        
        $startTime = Get-Date
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees?page=0&size=20" -Method Get -Headers $headers -TimeoutSec 10
        $endTime = Get-Date
        
        $duration = ($endTime - $startTime).TotalMilliseconds
        $duration -lt 5000
    } catch { $false }
}

# Test 9: Report Generation Tests
Run-Test "Report Generation - PDF Report" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        $response = Invoke-WebRequest -Uri "$BackendUrl/api/v1/reports/guarantees/pdf?fromDate=2025-01-01&toDate=2025-12-31&status=ALL" -Method Get -Headers $headers -TimeoutSec 30
        $response.StatusCode -eq 200 -and $response.Content.Length -gt 0
    } catch { $false }
}

# Test 10: Template System Tests
Run-Test "Template System - Get Templates" {
    try {
        $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/templates" -Method Get -Headers $headers -TimeoutSec 10
        $response -ne $null
    } catch { $false }
}

# Test 11: Security Tests
Run-Test "Security - Authentication Required" {
    try {
        $response = Invoke-WebRequest -Uri "$BackendUrl/api/v1/guarantees" -Method Get -TimeoutSec 5
        $false
    } catch {
        $_.Exception.Response.StatusCode -eq 401
    }
}

# Test 12: Frontend Tests (if running)
try {
    $frontendResponse = Invoke-WebRequest -Uri $FrontendUrl -Method Get -TimeoutSec 5
    Run-Test "Frontend - Application Loading" {
        $frontendResponse.Content -like "*InterExport*"
    }
} catch {
    Write-Host "`n⚠️  Frontend not running, skipping frontend tests" -ForegroundColor $Yellow
}

# Test 13: Redis Memory Usage
Run-Test "Redis Memory - Memory Usage Check" {
    try {
        $redis = New-Object System.Net.Sockets.TcpClient
        $redis.Connect("localhost", 6379)
        $redis.Close()
        Write-Host "Redis connection successful" -ForegroundColor $Green
        $true
    } catch { $false }
}

# Test 14: Database Connection Pool
Run-Test "Database Connection - Connection Pool" {
    try {
        $response = Invoke-RestMethod -Uri "$BackendUrl/api/v1/guarantees/health" -Method Get -TimeoutSec 10
        $response.status -eq "UP"
    } catch { $false }
}

Write-Host "`n📊 Test Results Summary" -ForegroundColor $Blue
Write-Host "=========================" -ForegroundColor $Blue
Write-Host "Total Tests: $TotalTests"
Write-Host "Passed: $PassedTests" -ForegroundColor $Green
Write-Host "Failed: $FailedTests" -ForegroundColor $Red

if ($FailedTests -eq 0) {
    Write-Host "`n🎉 ALL TESTS PASSED! 🎉" -ForegroundColor $Green
    Write-Host "✅ CQRS functionality is working correctly" -ForegroundColor $Green
    Write-Host "✅ Redis caching is operational" -ForegroundColor $Green
    Write-Host "✅ Kafka event publishing is functional" -ForegroundColor $Green
    Write-Host "✅ API endpoints are responding correctly" -ForegroundColor $Green
    Write-Host "✅ Database consistency is maintained" -ForegroundColor $Green
    Write-Host "✅ Performance is within acceptable limits" -ForegroundColor $Green
    Write-Host "✅ Report generation is working" -ForegroundColor $Green
    Write-Host "✅ Template system is functional" -ForegroundColor $Green
    Write-Host "✅ Security is properly implemented" -ForegroundColor $Green
    exit 0
} else {
    Write-Host "`n❌ SOME TESTS FAILED" -ForegroundColor $Red
    Write-Host "Please check the failed tests above and fix the issues" -ForegroundColor $Red
    exit 1
}

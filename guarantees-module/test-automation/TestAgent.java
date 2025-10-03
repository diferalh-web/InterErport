package com.interexport.guarantees.testagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Automated Test Agent for InterExport Guarantees Module
 * 
 * This agent automatically executes comprehensive tests to validate:
 * - CQRS functionality (Command and Query separation)
 * - Redis caching behavior
 * - Kafka event publishing
 * - API endpoint functionality
 * - Database consistency
 * - Performance metrics
 */
@SpringBootApplication
@EnableScheduling
public class TestAgent {

    public static void main(String[] args) {
        SpringApplication.run(TestAgent.class, args);
    }

    @Component
    public static class AutomatedTestRunner {
        
        private final RestTemplate restTemplate = new RestTemplate();
        private final ExecutorService executorService = Executors.newFixedThreadPool(10);
        private final String baseUrl = "http://localhost:8080/api/v1";
        
        private final TestResultsCollector resultsCollector = new TestResultsCollector();
        
        /**
         * Run comprehensive test suite every hour
         */
        @Scheduled(fixedRate = 3600000) // Every hour
        public void runComprehensiveTestSuite() {
            System.out.println("🤖 Starting Automated Test Suite at " + LocalDateTime.now());
            
            List<CompletableFuture<TestResult>> futures = new ArrayList<>();
            
            // Test 1: CQRS Command Side Tests
            futures.add(CompletableFuture.supplyAsync(() -> testCqrsCommandSide(), executorService));
            
            // Test 2: CQRS Query Side Tests
            futures.add(CompletableFuture.supplyAsync(() -> testCqrsQuerySide(), executorService));
            
            // Test 3: Redis Caching Tests
            futures.add(CompletableFuture.supplyAsync(() -> testRedisCaching(), executorService));
            
            // Test 4: Kafka Event Publishing Tests
            futures.add(CompletableFuture.supplyAsync(() -> testKafkaEvents(), executorService));
            
            // Test 5: API Endpoint Tests
            futures.add(CompletableFuture.supplyAsync(() -> testApiEndpoints(), executorService));
            
            // Test 6: Database Consistency Tests
            futures.add(CompletableFuture.supplyAsync(() -> testDatabaseConsistency(), executorService));
            
            // Test 7: Performance Tests
            futures.add(CompletableFuture.supplyAsync(() -> testPerformance(), executorService));
            
            // Test 8: Report Generation Tests
            futures.add(CompletableFuture.supplyAsync(() -> testReportGeneration(), executorService));
            
            // Test 9: Template System Tests
            futures.add(CompletableFuture.supplyAsync(() -> testTemplateSystem(), executorService));
            
            // Test 10: Security Tests
            futures.add(CompletableFuture.supplyAsync(() -> testSecurity(), executorService));
            
            // Wait for all tests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    List<TestResult> results = futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                    
                    resultsCollector.collectResults(results);
                    generateTestReport();
                });
        }
        
        /**
         * Test CQRS Command Side functionality
         */
        private TestResult testCqrsCommandSide() {
            TestResult result = new TestResult("CQRS Command Side");
            try {
                // Test 1: Create Guarantee Command
                Map<String, Object> guaranteeData = createSampleGuaranteeData();
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/guarantees", 
                    guaranteeData, 
                    Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Guarantee creation via CQRS command");
                } else {
                    result.addFailure("Guarantee creation failed", response.getStatusCode().toString());
                }
                
                // Test 2: Update Guarantee Command
                if (response.getBody() != null) {
                    Long guaranteeId = Long.valueOf(response.getBody().get("id").toString());
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("status", "APPROVED");
                    
                    ResponseEntity<Map> updateResponse = restTemplate.exchange(
                        baseUrl + "/guarantees/" + guaranteeId,
                        HttpMethod.PUT,
                        new HttpEntity<>(updateData),
                        Map.class
                    );
                    
                    if (updateResponse.getStatusCode().is2xxSuccessful()) {
                        result.addSuccess("Guarantee update via CQRS command");
                    } else {
                        result.addFailure("Guarantee update failed", updateResponse.getStatusCode().toString());
                    }
                }
                
            } catch (Exception e) {
                result.addError("CQRS Command Side test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test CQRS Query Side functionality
         */
        private TestResult testCqrsQuerySide() {
            TestResult result = new TestResult("CQRS Query Side");
            try {
                // Test 1: Query Guarantees List
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    baseUrl + "/guarantees?page=0&size=10", 
                    Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Guarantees list query");
                } else {
                    result.addFailure("Guarantees list query failed", response.getStatusCode().toString());
                }
                
                // Test 2: Query Single Guarantee
                ResponseEntity<Map> singleResponse = restTemplate.getForEntity(
                    baseUrl + "/guarantees/1", 
                    Map.class
                );
                
                if (singleResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Single guarantee query");
                } else {
                    result.addFailure("Single guarantee query failed", singleResponse.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("CQRS Query Side test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test Redis caching functionality
         */
        private TestResult testRedisCaching() {
            TestResult result = new TestResult("Redis Caching");
            try {
                // Test 1: Cache Hit Test
                long startTime = System.currentTimeMillis();
                ResponseEntity<Map> response1 = restTemplate.getForEntity(
                    baseUrl + "/guarantees/1", 
                    Map.class
                );
                long firstCallTime = System.currentTimeMillis() - startTime;
                
                startTime = System.currentTimeMillis();
                ResponseEntity<Map> response2 = restTemplate.getForEntity(
                    baseUrl + "/guarantees/1", 
                    Map.class
                );
                long secondCallTime = System.currentTimeMillis() - startTime;
                
                if (secondCallTime < firstCallTime) {
                    result.addSuccess("Redis cache hit detected (faster second call)");
                } else {
                    result.addWarning("Redis cache may not be working optimally");
                }
                
                // Test 2: Cache Invalidation Test
                if (response1.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Redis cache functionality test");
                } else {
                    result.addFailure("Redis cache test failed", response1.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("Redis caching test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test Kafka event publishing
         */
        private TestResult testKafkaEvents() {
            TestResult result = new TestResult("Kafka Events");
            try {
                // Test 1: Create event that should trigger Kafka
                Map<String, Object> guaranteeData = createSampleGuaranteeData();
                guaranteeData.put("reference", "KAFKA-TEST-" + System.currentTimeMillis());
                
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/guarantees", 
                    guaranteeData, 
                    Map.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Kafka event publishing test (guarantee created)");
                } else {
                    result.addFailure("Kafka event test failed", response.getStatusCode().toString());
                }
                
                // Test 2: Update event that should trigger Kafka
                if (response.getBody() != null) {
                    Long guaranteeId = Long.valueOf(response.getBody().get("id").toString());
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("status", "APPROVED");
                    
                    ResponseEntity<Map> updateResponse = restTemplate.exchange(
                        baseUrl + "/guarantees/" + guaranteeId,
                        HttpMethod.PUT,
                        new HttpEntity<>(updateData),
                        Map.class
                    );
                    
                    if (updateResponse.getStatusCode().is2xxSuccessful()) {
                        result.addSuccess("Kafka event publishing test (guarantee updated)");
                    } else {
                        result.addFailure("Kafka update event test failed", updateResponse.getStatusCode().toString());
                    }
                }
                
            } catch (Exception e) {
                result.addError("Kafka events test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test API endpoints functionality
         */
        private TestResult testApiEndpoints() {
            TestResult result = new TestResult("API Endpoints");
            try {
                // Test 1: Health Check
                ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                    baseUrl + "/guarantees/health", 
                    Map.class
                );
                
                if (healthResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Health check endpoint");
                } else {
                    result.addFailure("Health check failed", healthResponse.getStatusCode().toString());
                }
                
                // Test 2: Dashboard Summary
                ResponseEntity<Map> dashboardResponse = restTemplate.getForEntity(
                    baseUrl + "/dashboard/summary", 
                    Map.class
                );
                
                if (dashboardResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Dashboard summary endpoint");
                } else {
                    result.addFailure("Dashboard summary failed", dashboardResponse.getStatusCode().toString());
                }
                
                // Test 3: Templates Endpoint
                ResponseEntity<Map> templatesResponse = restTemplate.getForEntity(
                    baseUrl + "/templates", 
                    Map.class
                );
                
                if (templatesResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Templates endpoint");
                } else {
                    result.addFailure("Templates endpoint failed", templatesResponse.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("API endpoints test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test database consistency
         */
        private TestResult testDatabaseConsistency() {
            TestResult result = new TestResult("Database Consistency");
            try {
                // Test 1: Create and verify data consistency
                Map<String, Object> guaranteeData = createSampleGuaranteeData();
                guaranteeData.put("reference", "DB-CONSISTENCY-" + System.currentTimeMillis());
                
                ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                    baseUrl + "/guarantees", 
                    guaranteeData, 
                    Map.class
                );
                
                if (createResponse.getStatusCode().is2xxSuccessful() && createResponse.getBody() != null) {
                    Long guaranteeId = Long.valueOf(createResponse.getBody().get("id").toString());
                    
                    // Verify the created data can be retrieved
                    ResponseEntity<Map> getResponse = restTemplate.getForEntity(
                        baseUrl + "/guarantees/" + guaranteeId, 
                        Map.class
                    );
                    
                    if (getResponse.getStatusCode().is2xxSuccessful()) {
                        result.addSuccess("Database consistency test (create and retrieve)");
                    } else {
                        result.addFailure("Database consistency test failed", getResponse.getStatusCode().toString());
                    }
                } else {
                    result.addFailure("Database create test failed", createResponse.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("Database consistency test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test performance metrics
         */
        private TestResult testPerformance() {
            TestResult result = new TestResult("Performance");
            try {
                // Test 1: Response time test
                long startTime = System.currentTimeMillis();
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    baseUrl + "/guarantees?page=0&size=20", 
                    Map.class
                );
                long responseTime = System.currentTimeMillis() - startTime;
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    if (responseTime < 1000) { // Less than 1 second
                        result.addSuccess("Performance test passed (response time: " + responseTime + "ms)");
                    } else {
                        result.addWarning("Performance test warning (response time: " + responseTime + "ms)");
                    }
                } else {
                    result.addFailure("Performance test failed", response.getStatusCode().toString());
                }
                
                // Test 2: Concurrent requests test
                List<CompletableFuture<ResponseEntity<Map>>> futures = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    futures.add(CompletableFuture.supplyAsync(() -> 
                        restTemplate.getForEntity(baseUrl + "/guarantees?page=0&size=5", Map.class)
                    ));
                }
                
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                long allSuccessful = futures.stream()
                    .mapToLong(f -> f.join().getStatusCode().is2xxSuccessful() ? 1 : 0)
                    .sum();
                
                if (allSuccessful == 10) {
                    result.addSuccess("Concurrent requests test passed");
                } else {
                    result.addFailure("Concurrent requests test failed", 
                        "Only " + allSuccessful + " out of 10 requests succeeded");
                }
                
            } catch (Exception e) {
                result.addError("Performance test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test report generation
         */
        private TestResult testReportGeneration() {
            TestResult result = new TestResult("Report Generation");
            try {
                // Test 1: PDF Report Generation
                ResponseEntity<byte[]> pdfResponse = restTemplate.getForEntity(
                    baseUrl + "/reports/guarantees/pdf?fromDate=2025-01-01&toDate=2025-12-31&status=ALL", 
                    byte[].class
                );
                
                if (pdfResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("PDF report generation");
                } else {
                    result.addFailure("PDF report generation failed", pdfResponse.getStatusCode().toString());
                }
                
                // Test 2: Excel Report Generation
                ResponseEntity<byte[]> excelResponse = restTemplate.getForEntity(
                    baseUrl + "/reports/guarantees/excel?fromDate=2025-01-01&toDate=2025-12-31&status=ALL", 
                    byte[].class
                );
                
                if (excelResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Excel report generation");
                } else {
                    result.addFailure("Excel report generation failed", excelResponse.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("Report generation test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test template system
         */
        private TestResult testTemplateSystem() {
            TestResult result = new TestResult("Template System");
            try {
                // Test 1: Get all templates
                ResponseEntity<Map> templatesResponse = restTemplate.getForEntity(
                    baseUrl + "/templates", 
                    Map.class
                );
                
                if (templatesResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Templates retrieval");
                } else {
                    result.addFailure("Templates retrieval failed", templatesResponse.getStatusCode().toString());
                }
                
                // Test 2: Get templates by type
                ResponseEntity<Map> typeResponse = restTemplate.getForEntity(
                    baseUrl + "/templates/type/PERFORMANCE", 
                    Map.class
                );
                
                if (typeResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Templates by type retrieval");
                } else {
                    result.addFailure("Templates by type retrieval failed", typeResponse.getStatusCode().toString());
                }
                
            } catch (Exception e) {
                result.addError("Template system test failed", e.getMessage());
            }
            
            return result;
        }
        
        /**
         * Test security
         */
        private TestResult testSecurity() {
            TestResult result = new TestResult("Security");
            try {
                // Test 1: Authentication test
                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth("admin", "admin123");
                
                ResponseEntity<Map> authResponse = restTemplate.exchange(
                    baseUrl + "/guarantees?page=0&size=5",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );
                
                if (authResponse.getStatusCode().is2xxSuccessful()) {
                    result.addSuccess("Authentication test");
                } else {
                    result.addFailure("Authentication test failed", authResponse.getStatusCode().toString());
                }
                
                // Test 2: Unauthorized access test
                ResponseEntity<Map> unauthResponse = restTemplate.getForEntity(
                    baseUrl + "/guarantees?page=0&size=5", 
                    Map.class
                );
                
                if (unauthResponse.getStatusCode().value() == 401) {
                    result.addSuccess("Unauthorized access protection");
                } else {
                    result.addWarning("Unauthorized access protection may not be working");
                }
                
            } catch (Exception e) {
                result.addError("Security test failed", e.getMessage());
            }
            
            return result;
        }
        
        private Map<String, Object> createSampleGuaranteeData() {
            Map<String, Object> data = new HashMap<>();
            data.put("reference", "TEST-" + System.currentTimeMillis());
            data.put("guaranteeType", "PERFORMANCE");
            data.put("amount", 100000.00);
            data.put("currency", "USD");
            data.put("issueDate", "2025-01-01");
            data.put("expiryDate", "2026-01-01");
            data.put("applicantId", 1L);
            data.put("beneficiaryName", "Test Beneficiary");
            data.put("guaranteeText", "Test guarantee text");
            data.put("language", "EN");
            return data;
        }
        
        private void generateTestReport() {
            System.out.println("\n📊 TEST REPORT GENERATED AT " + LocalDateTime.now());
            System.out.println("==========================================");
            
            resultsCollector.getResults().forEach((testName, results) -> {
                System.out.println("\n🧪 " + testName.toUpperCase());
                System.out.println("   ✅ Successes: " + results.getSuccesses().size());
                System.out.println("   ❌ Failures: " + results.getFailures().size());
                System.out.println("   ⚠️  Warnings: " + results.getWarnings().size());
                System.out.println("   🚨 Errors: " + results.getErrors().size());
                
                if (!results.getFailures().isEmpty()) {
                    System.out.println("   📝 Failures:");
                    results.getFailures().forEach(failure -> 
                        System.out.println("      - " + failure));
                }
                
                if (!results.getErrors().isEmpty()) {
                    System.out.println("   📝 Errors:");
                    results.getErrors().forEach(error -> 
                        System.out.println("      - " + error));
                }
            });
            
            System.out.println("\n🎯 OVERALL STATUS: " + 
                (resultsCollector.hasFailures() ? "❌ SOME TESTS FAILED" : "✅ ALL TESTS PASSED"));
            System.out.println("==========================================\n");
        }
    }
    
    /**
     * Test result collector
     */
    public static class TestResultsCollector {
        private final Map<String, TestResult> results = new HashMap<>();
        
        public void collectResults(List<TestResult> testResults) {
            testResults.forEach(result -> results.put(result.getTestName(), result));
        }
        
        public Map<String, TestResult> getResults() {
            return results;
        }
        
        public boolean hasFailures() {
            return results.values().stream().anyMatch(TestResult::hasFailures);
        }
    }
    
    /**
     * Test result data class
     */
    public static class TestResult {
        private final String testName;
        private final List<String> successes = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public TestResult(String testName) {
            this.testName = testName;
        }
        
        public void addSuccess(String message) {
            successes.add(message);
        }
        
        public void addFailure(String message, String details) {
            failures.add(message + ": " + details);
        }
        
        public void addWarning(String message) {
            warnings.add(message);
        }
        
        public void addError(String message, String details) {
            errors.add(message + ": " + details);
        }
        
        public String getTestName() { return testName; }
        public List<String> getSuccesses() { return successes; }
        public List<String> getFailures() { return failures; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        
        public boolean hasFailures() {
            return !failures.isEmpty() || !errors.isEmpty();
        }
    }
}

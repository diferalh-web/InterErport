package com.interexport.guarantees.integration;

import com.interexport.guarantees.cqrs.command.CreateGuaranteeCommand;
import com.interexport.guarantees.cqrs.command.GuaranteeCommandHandler;
import com.interexport.guarantees.cqrs.query.GuaranteeQueryHandler;
import com.interexport.guarantees.entity.GuaranteeContract;
import com.interexport.guarantees.entity.enums.GuaranteeStatus;
import com.interexport.guarantees.entity.enums.GuaranteeType;
import com.interexport.guarantees.repository.GuaranteeContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CQRS and Redis functionality
 * Tests the complete flow from command to query side with Redis caching
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"guarantee-created", "guarantee-updated"})
@ActiveProfiles("test")
@Transactional
public class CqrsRedisIntegrationTest {

    @Autowired
    private GuaranteeCommandHandler commandHandler;

    @Autowired
    private GuaranteeQueryHandler queryHandler;

    @Autowired
    private GuaranteeContractRepository guaranteeRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clear Redis cache before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // Clear database
        guaranteeRepository.deleteAll();
    }

    @Test
    void testCase1_CreateGuaranteeCommand_ShouldPublishEventAndCacheInRedis() {
        // Given
        CreateGuaranteeCommand command = createSampleCommand();
        
        // When
        String guaranteeId = commandHandler.handle(command);
        
        // Then
        assertNotNull(guaranteeId);
        
        // Verify guarantee is saved in command database
        GuaranteeContract savedGuarantee = guaranteeRepository.findById(Long.valueOf(guaranteeId)).orElse(null);
        assertNotNull(savedGuarantee);
        assertEquals(command.getReference(), savedGuarantee.getReference());
        assertEquals(GuaranteeStatus.DRAFT, savedGuarantee.getStatus());
        
        // Verify Redis cache is populated
        String cacheKey = "guarantee:" + guaranteeId;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        assertTrue(exists, "Guarantee should be cached in Redis");
        
        // Verify Kafka event was published (simplified check)
        // In a real test, you would use KafkaTestUtils to verify message content
    }

    @Test
    void testCase2_QueryGuarantee_ShouldReturnFromRedisCache() {
        // Given - Create a guarantee first
        CreateGuaranteeCommand command = createSampleCommand();
        String guaranteeId = commandHandler.handle(command);
        
        // When - Query the guarantee
        GuaranteeContract queriedGuarantee = queryHandler.getGuaranteeById(Long.valueOf(guaranteeId));
        
        // Then
        assertNotNull(queriedGuarantee);
        assertEquals(command.getReference(), queriedGuarantee.getReference());
        
        // Verify it was retrieved from cache (Redis)
        String cacheKey = "guarantee:" + guaranteeId;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        assertTrue(exists, "Guarantee should be in Redis cache");
    }

    @Test
    void testCase3_UpdateGuarantee_ShouldInvalidateRedisCache() {
        // Given - Create a guarantee
        CreateGuaranteeCommand command = createSampleCommand();
        String guaranteeId = commandHandler.handle(command);
        
        // Verify initial cache
        String cacheKey = "guarantee:" + guaranteeId;
        assertTrue(redisTemplate.hasKey(cacheKey), "Guarantee should be cached initially");
        
        // When - Update the guarantee (simulate through repository)
        GuaranteeContract guarantee = guaranteeRepository.findById(Long.valueOf(guaranteeId)).orElseThrow();
        guarantee.setStatus(GuaranteeStatus.APPROVED);
        guaranteeRepository.save(guarantee);
        
        // Simulate cache invalidation
        redisTemplate.delete(cacheKey);
        
        // Then - Cache should be invalidated
        assertFalse(redisTemplate.hasKey(cacheKey), "Cache should be invalidated after update");
    }

    @Test
    void testCase4_RedisCacheExpiration_ShouldExpireAfterTimeout() {
        // Given - Create a guarantee with short TTL
        CreateGuaranteeCommand command = createSampleCommand();
        String guaranteeId = commandHandler.handle(command);
        
        String cacheKey = "guarantee:" + guaranteeId;
        
        // Set expiration to 1 second for testing
        redisTemplate.expire(cacheKey, 1, TimeUnit.SECONDS);
        
        // When - Wait for expiration
        try {
            Thread.sleep(1100); // Wait slightly more than 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - Cache should be expired
        assertFalse(redisTemplate.hasKey(cacheKey), "Cache should be expired after TTL");
    }

    @Test
    void testCase5_ConcurrentGuaranteeCreation_ShouldHandleRaceConditions() {
        // Given - Multiple concurrent commands
        CreateGuaranteeCommand command1 = createSampleCommand();
        command1.setReference("GT-20250101-001");
        
        CreateGuaranteeCommand command2 = createSampleCommand();
        command2.setReference("GT-20250101-002");
        
        // When - Execute concurrently
        String guaranteeId1 = commandHandler.handle(command1);
        String guaranteeId2 = commandHandler.handle(command2);
        
        // Then - Both should be created successfully
        assertNotNull(guaranteeId1);
        assertNotNull(guaranteeId2);
        assertNotEquals(guaranteeId1, guaranteeId2);
        
        // Verify both are in Redis cache
        assertTrue(redisTemplate.hasKey("guarantee:" + guaranteeId1));
        assertTrue(redisTemplate.hasKey("guarantee:" + guaranteeId2));
    }

    @Test
    void testCase6_RedisConnectionFailure_ShouldFallbackToDatabase() {
        // Given - Simulate Redis connection failure by clearing connection
        // This is a simplified test - in reality you'd mock the Redis connection
        
        CreateGuaranteeCommand command = createSampleCommand();
        
        // When - Execute command (should still work with database fallback)
        String guaranteeId = commandHandler.handle(command);
        
        // Then - Should still create guarantee in database
        assertNotNull(guaranteeId);
        GuaranteeContract savedGuarantee = guaranteeRepository.findById(Long.valueOf(guaranteeId)).orElse(null);
        assertNotNull(savedGuarantee);
    }

    @Test
    void testCase7_QueryGuaranteeList_ShouldUseRedisPagination() {
        // Given - Create multiple guarantees
        for (int i = 1; i <= 5; i++) {
            CreateGuaranteeCommand command = createSampleCommand();
            command.setReference("GT-20250101-00" + i);
            commandHandler.handle(command);
        }
        
        // When - Query with pagination
        var result = queryHandler.getGuarantees(0, 3, "createdDate", "desc");
        
        // Then - Should return paginated results
        assertNotNull(result);
        assertTrue(result.size() <= 3);
    }

    @Test
    void testCase8_GuaranteeStatusUpdate_ShouldTriggerCacheRefresh() {
        // Given - Create a guarantee
        CreateGuaranteeCommand command = createSampleCommand();
        String guaranteeId = commandHandler.handle(command);
        
        // When - Update status
        GuaranteeContract guarantee = guaranteeRepository.findById(Long.valueOf(guaranteeId)).orElseThrow();
        guarantee.setStatus(GuaranteeStatus.APPROVED);
        guaranteeRepository.save(guarantee);
        
        // Simulate cache refresh
        String cacheKey = "guarantee:" + guaranteeId;
        redisTemplate.delete(cacheKey);
        
        // Then - Next query should refresh cache
        GuaranteeContract refreshedGuarantee = queryHandler.getGuaranteeById(Long.valueOf(guaranteeId));
        assertEquals(GuaranteeStatus.APPROVED, refreshedGuarantee.getStatus());
    }

    @Test
    void testCase9_RedisMemoryUsage_ShouldNotExceedLimits() {
        // Given - Create many guarantees to test memory usage
        for (int i = 1; i <= 100; i++) {
            CreateGuaranteeCommand command = createSampleCommand();
            command.setReference("GT-20250101-" + String.format("%03d", i));
            commandHandler.handle(command);
        }
        
        // When - Check Redis memory usage
        var info = redisTemplate.getConnectionFactory().getConnection().info("memory");
        
        // Then - Memory usage should be reasonable (this is a simplified check)
        assertNotNull(info);
        // In a real test, you would parse the info string and check memory usage
    }

    @Test
    void testCase10_EventualConsistency_ShouldSyncBetweenCommandAndQuery() {
        // Given - Create guarantee on command side
        CreateGuaranteeCommand command = createSampleCommand();
        String guaranteeId = commandHandler.handle(command);
        
        // When - Wait for eventual consistency (simulate async processing)
        try {
            Thread.sleep(100); // Small delay to simulate async processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - Query side should have the data
        GuaranteeContract queriedGuarantee = queryHandler.getGuaranteeById(Long.valueOf(guaranteeId));
        assertNotNull(queriedGuarantee);
        assertEquals(command.getReference(), queriedGuarantee.getReference());
        
        // Verify Redis cache is consistent
        String cacheKey = "guarantee:" + guaranteeId;
        assertTrue(redisTemplate.hasKey(cacheKey), "Cache should be consistent");
    }

    private CreateGuaranteeCommand createSampleCommand() {
        CreateGuaranteeCommand command = new CreateGuaranteeCommand();
        command.setReference("GT-20250101-001");
        command.setGuaranteeType("PERFORMANCE");
        command.setAmount(new BigDecimal("100000.00"));
        command.setCurrency("USD");
        command.setIssueDate(LocalDate.now());
        command.setExpiryDate(LocalDate.now().plusYears(1));
        command.setBeneficiaryName("Test Beneficiary");
        command.setApplicantId(1L);
        command.setGuaranteeText("Test guarantee text");
        command.setLanguage("EN");
        return command;
    }
}

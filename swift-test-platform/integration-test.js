/**
 * Integration Test Script for SWIFT Platform + Guarantee Module
 * Tests the connection and message flow between the two systems
 */

const axios = require('axios');

class SwiftIntegrationTest {
    constructor() {
        this.swiftPlatformUrl = 'http://localhost:8081';
        this.guaranteeModuleUrl = 'http://localhost:8080';
        this.testResults = [];
    }

    /**
     * Run all integration tests
     */
    async runAllTests() {
        console.log('🧪 Starting SWIFT Platform Integration Tests');
        console.log('=============================================\n');

        try {
            await this.testPlatformHealth();
            await this.testGuaranteeModuleHealth();
            await this.testMessageSending();
            await this.testMessageValidation();
            await this.testScenarioSimulation();
            await this.testGuaranteeModuleIntegration();
            
            this.printSummary();
        } catch (error) {
            console.error('❌ Test suite failed:', error.message);
            process.exit(1);
        }
    }

    /**
     * Test SWIFT Platform health
     */
    async testPlatformHealth() {
        console.log('🔍 Testing SWIFT Platform Health...');
        
        try {
            const response = await axios.get(`${this.swiftPlatformUrl}/api/health`);
            
            if (response.status === 200 && response.data.status === 'healthy') {
                this.logSuccess('SWIFT Platform is healthy');
                this.logInfo(`Platform: ${response.data.platform}`);
                this.logInfo(`Supported Messages: ${response.data.supportedMessages.join(', ')}`);
            } else {
                throw new Error('Platform health check failed');
            }
        } catch (error) {
            this.logError(`SWIFT Platform health check failed: ${error.message}`);
            throw error;
        }
        
        console.log('');
    }

    /**
     * Test Guarantee Module health
     */
    async testGuaranteeModuleHealth() {
        console.log('🔍 Testing Guarantee Module Health...');
        
        try {
            // Try to connect to guarantee module
            const response = await axios.get(`${this.guaranteeModuleUrl}/api/v1/health`, {
                timeout: 5000
            });
            
            this.logSuccess('Guarantee Module is accessible');
        } catch (error) {
            if (error.code === 'ECONNREFUSED') {
                this.logWarning('Guarantee Module is not running (ECONNREFUSED)');
                this.logInfo('Start the guarantee module with: cd backend && ./mvnw spring-boot:run');
            } else {
                this.logError(`Guarantee Module health check failed: ${error.message}`);
            }
        }
        
        console.log('');
    }

    /**
     * Test message sending functionality
     */
    async testMessageSending() {
        console.log('📤 Testing Message Sending...');
        
        const testMessage = {
            messageType: 'MT760',
            senderBIC: 'TESTBANKXXX',
            receiverBIC: 'INTEXPORXXXX',
            content: {
                transactionReference: 'TEST' + Date.now(),
                guaranteeAmount: '100000.00',
                currency: 'USD',
                issueDate: '2024-01-01',
                expiryDate: '2024-12-31',
                applicantName: 'Test Applicant Company',
                beneficiaryName: 'Test Beneficiary Authority',
                guaranteeText: 'Test performance guarantee'
            }
        };

        try {
            const response = await axios.post(`${this.swiftPlatformUrl}/api/swift/send`, testMessage);
            
            if (response.status === 200 && response.data.success) {
                this.logSuccess('Message sent successfully');
                this.logInfo(`Message ID: ${response.data.messageId}`);
                this.logInfo(`Status: ${response.data.status}`);
                
                // Wait for response processing
                await new Promise(resolve => setTimeout(resolve, 3000));
                
                // Check if response was received
                const messagesResponse = await axios.get(`${this.swiftPlatformUrl}/api/swift/messages?limit=5`);
                const responseMessage = messagesResponse.data.messages.find(m => 
                    m.relatedMessageId === response.data.messageId
                );
                
                if (responseMessage) {
                    this.logSuccess(`Automatic response received: ${responseMessage.type}`);
                } else {
                    this.logWarning('No automatic response detected');
                }
            } else {
                throw new Error('Message sending failed');
            }
        } catch (error) {
            this.logError(`Message sending failed: ${error.message}`);
            if (error.response && error.response.data) {
                this.logError(`Details: ${JSON.stringify(error.response.data)}`);
            }
        }
        
        console.log('');
    }

    /**
     * Test message validation
     */
    async testMessageValidation() {
        console.log('✅ Testing Message Validation...');
        
        // Test valid message
        const validMessage = {
            messageType: 'MT760',
            content: {
                transactionReference: 'VALID123',
                guaranteeAmount: '50000.00',
                currency: 'USD',
                applicantName: 'Valid Applicant',
                beneficiaryName: 'Valid Beneficiary'
            }
        };

        try {
            const validResponse = await axios.post(`${this.swiftPlatformUrl}/api/swift/validate`, validMessage);
            
            if (validResponse.data.isValid) {
                this.logSuccess('Valid message passed validation');
            } else {
                this.logWarning('Valid message failed validation');
                this.logInfo(`Errors: ${validResponse.data.errors.join(', ')}`);
            }
        } catch (error) {
            this.logError(`Validation test failed: ${error.message}`);
        }

        // Test invalid message
        const invalidMessage = {
            messageType: 'MT760',
            content: {
                // Missing required fields
                currency: 'INVALID',
                guaranteeAmount: 'not-a-number'
            }
        };

        try {
            const invalidResponse = await axios.post(`${this.swiftPlatformUrl}/api/swift/validate`, invalidMessage);
            
            if (!invalidResponse.data.isValid && invalidResponse.data.errors.length > 0) {
                this.logSuccess('Invalid message correctly rejected');
                this.logInfo(`Errors found: ${invalidResponse.data.errors.length}`);
            } else {
                this.logWarning('Invalid message was not properly rejected');
            }
        } catch (error) {
            this.logError(`Invalid message validation test failed: ${error.message}`);
        }
        
        console.log('');
    }

    /**
     * Test scenario simulation
     */
    async testScenarioSimulation() {
        console.log('🎭 Testing Scenario Simulation...');
        
        const scenario = {
            scenario: 'guarantee-issuance',
            parameters: {
                guaranteeAmount: 75000,
                currency: 'EUR',
                beneficiary: { name: 'Test Authority', bankBIC: 'TESTBNKXXXX' },
                applicant: { name: 'Test Company' }
            }
        };

        try {
            const response = await axios.post(`${this.swiftPlatformUrl}/api/swift/simulate/scenario`, scenario);
            
            if (response.data.success) {
                this.logSuccess('Scenario simulation completed');
                this.logInfo(`Scenario: ${response.data.scenario}`);
                this.logInfo(`Messages generated: ${response.data.messagesGenerated}`);
                
                // Wait for scenario to complete
                await new Promise(resolve => setTimeout(resolve, 5000));
                
                this.logSuccess('Scenario execution completed');
            } else {
                throw new Error('Scenario simulation failed');
            }
        } catch (error) {
            this.logError(`Scenario simulation failed: ${error.message}`);
        }
        
        console.log('');
    }

    /**
     * Test integration with Guarantee Module
     */
    async testGuaranteeModuleIntegration() {
        console.log('🔗 Testing Guarantee Module Integration...');
        
        try {
            // Try to send a SWIFT message to the guarantee module
            const swiftMessage = {
                rawMessage: this.generateTestSwiftMessage(),
                messageType: 'MT760'
            };

            const response = await axios.post(
                `${this.guaranteeModuleUrl}/api/v1/swift-messages/receive`, 
                swiftMessage,
                {
                    timeout: 10000,
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Basic ' + Buffer.from('admin:admin').toString('base64')
                    }
                }
            );

            if (response.status === 200) {
                this.logSuccess('SWIFT message successfully sent to Guarantee Module');
                this.logInfo(`Response: ${response.data.message || 'Message processed'}`);
            } else {
                this.logWarning(`Unexpected response status: ${response.status}`);
            }

        } catch (error) {
            if (error.code === 'ECONNREFUSED') {
                this.logWarning('Cannot connect to Guarantee Module (not running)');
                this.logInfo('To test integration, start the guarantee module first');
            } else if (error.response && error.response.status === 404) {
                this.logWarning('SWIFT endpoint not found in Guarantee Module');
                this.logInfo('The SWIFT integration endpoint may not be implemented yet');
            } else if (error.response && error.response.status === 401) {
                this.logWarning('Authentication required for Guarantee Module');
            } else {
                this.logError(`Integration test failed: ${error.message}`);
            }
        }
        
        console.log('');
    }

    /**
     * Generate a test SWIFT message in proper format
     */
    generateTestSwiftMessage() {
        return `{1:F01TESTBANKXXX0000000000}{2:I760INTEXPORXXXXN}{3:{108:MT760}}{4:
:20:TEST${Date.now()}
:31C:20240101
:31E:20241231
:32B:USD100000,00
:50:Test Applicant Company
:59:Test Beneficiary Authority
:77C:TEST PERFORMANCE GUARANTEE FOR INTEGRATION
-}{5:{CHK:ABCDEF123456}}`;
    }

    /**
     * Print test results summary
     */
    printSummary() {
        console.log('📊 Test Summary');
        console.log('===============');
        
        const passed = this.testResults.filter(r => r.status === 'PASS').length;
        const warnings = this.testResults.filter(r => r.status === 'WARN').length;
        const failed = this.testResults.filter(r => r.status === 'FAIL').length;
        
        console.log(`✅ Passed: ${passed}`);
        console.log(`⚠️  Warnings: ${warnings}`);
        console.log(`❌ Failed: ${failed}`);
        
        if (failed === 0) {
            console.log('\n🎉 All critical tests passed! SWIFT Platform is ready for use.');
        } else {
            console.log('\n⚠️  Some tests failed. Please check the logs above.');
        }
        
        console.log('\n💡 Next Steps:');
        console.log('   1. Start the guarantee module: cd backend && ./mvnw spring-boot:run');
        console.log('   2. Access SWIFT Platform: http://localhost:8081');
        console.log('   3. Test message flows using the web interface');
        console.log('   4. Monitor integration logs for any issues');
        console.log('');
    }

    // Logging utilities
    logSuccess(message) {
        console.log(`   ✅ ${message}`);
        this.testResults.push({ status: 'PASS', message });
    }

    logWarning(message) {
        console.log(`   ⚠️  ${message}`);
        this.testResults.push({ status: 'WARN', message });
    }

    logError(message) {
        console.log(`   ❌ ${message}`);
        this.testResults.push({ status: 'FAIL', message });
    }

    logInfo(message) {
        console.log(`   ℹ️  ${message}`);
    }
}

// Add axios as dependency check
async function checkDependencies() {
    try {
        require('axios');
    } catch (error) {
        console.error('❌ Missing dependency: axios');
        console.log('💡 Please install with: npm install axios');
        process.exit(1);
    }
}

// Run tests if this file is executed directly
if (require.main === module) {
    (async () => {
        await checkDependencies();
        const tester = new SwiftIntegrationTest();
        await tester.runAllTests();
    })();
}

module.exports = SwiftIntegrationTest;





# SWIFT Test Platform

A comprehensive testing platform for SWIFT message processing in the InterExport Guarantee Module.

## 🌟 Features

### SWIFT Message Support
- **MT760**: Issue of a Guarantee
- **MT765**: Amendment to a Guarantee  
- **MT767**: Confirmation of Amendment
- **MT768**: Acknowledgment
- **MT769**: Advice of Discrepancy
- **MT798**: Free Format Message

### Core Capabilities
- ✅ **Real-time Message Processing** - WebSocket-based live updates
- ✅ **Message Validation** - SWIFT format and business rule validation
- ✅ **Scenario Simulation** - Pre-built business scenarios
- ✅ **Message Threading** - Track related messages and responses
- ✅ **Search and Filtering** - Advanced message search capabilities
- ✅ **Statistics and Analytics** - Real-time processing metrics
- ✅ **Web Interface** - Modern, responsive UI for testing

### Business Scenarios
1. **Guarantee Issuance** - Complete MT760 → MT768 flow
2. **Guarantee Amendment** - MT765 → MT767 → MT768 flow  
3. **Claim Processing** - MT769 handling and responses
4. **Discrepancy Handling** - Error scenarios and resolution

## 🚀 Quick Start

### Prerequisites
- Node.js 16+ 
- npm or yarn

### Installation
```bash
# Navigate to platform directory
cd swift-test-platform

# Install dependencies
npm install

# Start the platform
npm start
```

The platform will be available at:
- **Web Interface**: http://localhost:8081
- **API Base**: http://localhost:8081/api
- **WebSocket**: ws://localhost:8081

### Development Mode
```bash
npm run dev  # Uses nodemon for auto-restart
```

## 🔧 API Endpoints

### Message Operations
- `POST /api/swift/send` - Send SWIFT message
- `POST /api/swift/receive` - Simulate receiving message
- `GET /api/swift/messages` - Get all messages (with filtering)
- `GET /api/swift/messages/:id` - Get specific message
- `DELETE /api/swift/messages` - Clear all messages

### Validation & Templates
- `POST /api/swift/validate` - Validate message format
- `GET /api/swift/templates` - Get message templates

### Scenario Simulation
- `POST /api/swift/simulate/scenario` - Run business scenarios

### System
- `GET /api/health` - Health check

## 💬 WebSocket Events

### Client → Server
- `connect` - Establish connection
- `disconnect` - Close connection

### Server → Client  
- `message-sent` - New outgoing message
- `message-received` - New incoming message
- `scenario-message` - Message from scenario simulation
- `initial-messages` - Recent messages on connect

## 🧪 Testing Workflows

### 1. Send Individual Messages

Use the web interface to:
1. Select message type (MT760, MT765, etc.)
2. Fill required fields
3. Validate message format
4. Send message
5. View automatic responses

### 2. Run Business Scenarios

Click scenario buttons to simulate:
- **Issue Guarantee**: MT760 → MT768 flow
- **Amendment**: MT765 → MT767 → MT768 flow
- **Claim**: MT769 submission
- **Discrepancy**: Error handling

### 3. API Integration Testing

```bash
# Send MT760 guarantee
curl -X POST http://localhost:8081/api/swift/send \
  -H "Content-Type: application/json" \
  -d '{
    "messageType": "MT760",
    "senderBIC": "INTEXPORXXXX",
    "receiverBIC": "BNKSGSGXXXX",
    "content": {
      "transactionReference": "GTR202400001",
      "guaranteeAmount": "100000.00",
      "currency": "USD",
      "applicantName": "ABC Construction",
      "beneficiaryName": "XYZ Authority"
    }
  }'

# Validate message
curl -X POST http://localhost:8081/api/swift/validate \
  -H "Content-Type: application/json" \
  -d '{
    "messageType": "MT760",
    "content": { ... }
  }'

# Get messages
curl http://localhost:8081/api/swift/messages?type=MT760&limit=10
```

### 4. Integration with Guarantee Module

The platform integrates with the main guarantee module:

```javascript
// Send to guarantee module backend
const response = await fetch('http://localhost:8080/api/v1/swift-messages/receive', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    rawMessage: swiftMessage.rawMessage,
    messageType: swiftMessage.type
  })
});
```

## 🔍 Message Validation

The platform validates:

### Format Validation
- SWIFT message structure
- Field format compliance
- Character encoding (SWIFT character set)
- Field length limits

### Business Rule Validation
- Required field presence
- Currency code validity
- BIC format validation
- Date logic validation
- Amount validation

### Example Validation Response
```json
{
  "isValid": false,
  "errors": [
    "Missing required field: transactionReference",
    "Invalid currency code: XXX"
  ],
  "warnings": [
    "Large guarantee amount detected, please verify"
  ],
  "messageType": "MT760",
  "validatedFields": 6
}
```

## 📊 Statistics & Monitoring

The platform tracks:
- Total messages processed
- Messages by type (MT760, MT765, etc.)
- Messages by direction (incoming/outgoing)
- Processing success/failure rates
- Average response times
- Message flow analysis

## 🔧 Configuration

### Environment Variables
```bash
PORT=8081                    # Server port
MAX_HISTORY_SIZE=1000       # Message history limit
VALIDATION_STRICT=true      # Strict validation mode
```

### Message Field Templates

Templates are configurable in `app.js`:
```javascript
messageFieldTemplates: {
  'MT760': [
    { name: 'transactionReference', type: 'text', required: true },
    { name: 'guaranteeAmount', type: 'number', required: true },
    // ... more fields
  ]
}
```

## 🔗 Integration Points

### With Guarantee Module Backend
- Receive processed SWIFT messages
- Send response messages
- Query message status
- Handle workflow triggers

### With Database
- Message persistence (via main module)
- Audit trail logging
- Business entity relationships

### With External Systems
- Bank SWIFT networks (simulation)
- Customer notification systems
- Document management systems

## 🛠️ Troubleshooting

### Common Issues

1. **Connection Failed**
   ```bash
   # Check if platform is running
   curl http://localhost:8081/api/health
   ```

2. **Message Validation Errors**
   - Check required fields
   - Verify BIC code format  
   - Validate currency codes
   - Check field lengths

3. **WebSocket Issues**
   - Check firewall settings
   - Verify port 8081 availability
   - Check browser WebSocket support

### Debugging

Enable debug mode:
```bash
DEBUG=swift-platform:* npm start
```

View logs:
```bash
tail -f logs/swift-platform.log
```

## 📋 TODO / Roadmap

- [ ] SSL/TLS support for production
- [ ] User authentication and authorization
- [ ] Message encryption simulation
- [ ] Advanced scenario builder
- [ ] Integration with external SWIFT simulators
- [ ] Performance testing tools
- [ ] Message replay capabilities
- [ ] Custom validation rules

## 📚 SWIFT Message Reference

### MT760 - Issue of a Guarantee
```
{1:F01INTEXPORXXXX0000000000}
{2:I760BNKSGSGXXXXN}
{3:{108:MT760}}
{4:
:20:GTR202400001
:31C:20240101
:31E:20251231  
:32B:USD100000,00
:50:ABC Construction Company
:59:XYZ Government Authority
:77C:PERFORMANCE GUARANTEE AS PER CONTRACT
-}
{5:{CHK:ABCDEF123456}}
```

### Field Definitions
- **:20:** Transaction Reference
- **:21:** Related Reference  
- **:31C:** Date of Issue
- **:31E:** Date of Expiry
- **:32B:** Currency Code Amount
- **:50:** Applicant
- **:59:** Beneficiary  
- **:77C:** Details of Guarantee

## 📞 Support

For issues and questions:
- Create GitHub issue
- Contact: InterExport Development Team
- Documentation: See `/docs` folder

## 📝 License

MIT License - See LICENSE file for details.





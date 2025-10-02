const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const { Server } = require('socket.io');
const http = require('http');
const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const path = require('path');

const SwiftMessageProcessor = require('./services/SwiftMessageProcessor');
const SwiftValidationService = require('./services/SwiftValidationService');
const MessageStore = require('./services/MessageStore');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.text({ type: 'text/plain' }));
app.use(express.static(path.join(__dirname, 'public')));

// Initialize services
const messageProcessor = new SwiftMessageProcessor();
const validator = new SwiftValidationService();
const messageStore = new MessageStore();

// SWIFT Test Platform API Routes

/**
 * GET /api/health - Health check
 */
app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: moment().toISOString(),
    platform: 'SWIFT Test Platform v1.0',
    supportedMessages: ['MT760', 'MT765', 'MT767', 'MT768', 'MT769', 'MT798']
  });
});

/**
 * POST /api/swift/send - Send SWIFT message
 */
app.post('/api/swift/send', async (req, res) => {
  try {
    const { messageType, content, senderBIC, receiverBIC, guaranteeData } = req.body;
    
    // Validate message
    const validationResult = validator.validateMessage(messageType, content);
    if (!validationResult.isValid) {
      return res.status(400).json({
        error: 'Message validation failed',
        details: validationResult.errors
      });
    }

    // Generate message ID
    const messageId = uuidv4();
    
    // Process message
    const processedMessage = await messageProcessor.processOutgoingMessage({
      id: messageId,
      type: messageType,
      content: content,
      senderBIC: senderBIC,
      receiverBIC: receiverBIC,
      guaranteeData: guaranteeData,
      timestamp: moment().toISOString(),
      status: 'SENT'
    });

    // Store message
    messageStore.storeMessage(processedMessage);

    // Emit real-time update
    io.emit('message-sent', processedMessage);

    // Simulate response based on message type
    setTimeout(async () => {
      const response = await messageProcessor.generateResponse(processedMessage);
      messageStore.storeMessage(response);
      io.emit('message-received', response);
    }, 2000); // 2-second delay to simulate network latency

    res.json({
      success: true,
      messageId: messageId,
      status: 'SENT',
      timestamp: processedMessage.timestamp,
      message: 'SWIFT message sent successfully'
    });

  } catch (error) {
    console.error('Error sending SWIFT message:', error);
    res.status(500).json({
      error: 'Failed to send SWIFT message',
      details: error.message
    });
  }
});

/**
 * POST /api/swift/receive - Simulate receiving SWIFT message
 */
app.post('/api/swift/receive', async (req, res) => {
  try {
    const { rawMessage, senderBIC } = req.body;
    
    // Parse and validate incoming message
    const parsedMessage = messageProcessor.parseIncomingMessage(rawMessage, senderBIC);
    const validationResult = validator.validateMessage(parsedMessage.type, parsedMessage.content);
    
    if (!validationResult.isValid) {
      return res.status(400).json({
        error: 'Invalid incoming message format',
        details: validationResult.errors
      });
    }

    // Process incoming message
    const processedMessage = await messageProcessor.processIncomingMessage(parsedMessage);
    messageStore.storeMessage(processedMessage);

    // Emit real-time update
    io.emit('message-received', processedMessage);

    res.json({
      success: true,
      messageId: processedMessage.id,
      status: 'RECEIVED',
      timestamp: processedMessage.timestamp,
      message: 'SWIFT message received and processed'
    });

  } catch (error) {
    console.error('Error processing incoming SWIFT message:', error);
    res.status(500).json({
      error: 'Failed to process incoming message',
      details: error.message
    });
  }
});

/**
 * GET /api/swift/messages - Get all messages
 */
app.get('/api/swift/messages', (req, res) => {
  const { type, status, limit = 100 } = req.query;
  const messages = messageStore.getMessages({ type, status, limit: parseInt(limit) });
  
  res.json({
    messages: messages,
    total: messages.length,
    timestamp: moment().toISOString()
  });
});

/**
 * GET /api/swift/messages/:id - Get specific message
 */
app.get('/api/swift/messages/:id', (req, res) => {
  const message = messageStore.getMessage(req.params.id);
  
  if (!message) {
    return res.status(404).json({ error: 'Message not found' });
  }
  
  res.json(message);
});

/**
 * POST /api/swift/simulate/scenario - Simulate complex scenarios
 */
app.post('/api/swift/simulate/scenario', async (req, res) => {
  try {
    const { scenario, parameters } = req.body;
    
    const result = await messageProcessor.simulateScenario(scenario, parameters);
    
    // Emit each message in the scenario
    for (const message of result.messages) {
      messageStore.storeMessage(message);
      io.emit('scenario-message', message);
      
      // Add delays between messages for realism
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    res.json({
      success: true,
      scenario: scenario,
      messagesGenerated: result.messages.length,
      timeline: result.timeline,
      summary: result.summary
    });

  } catch (error) {
    console.error('Error simulating scenario:', error);
    res.status(500).json({
      error: 'Failed to simulate scenario',
      details: error.message
    });
  }
});

/**
 * GET /api/swift/templates - Get message templates
 */
app.get('/api/swift/templates', (req, res) => {
  const templates = messageProcessor.getMessageTemplates();
  res.json({ templates });
});

/**
 * POST /api/swift/validate - Validate SWIFT message format
 */
app.post('/api/swift/validate', (req, res) => {
  try {
    const { messageType, content } = req.body;
    const validationResult = validator.validateMessage(messageType, content);
    
    res.json(validationResult);
  } catch (error) {
    res.status(500).json({
      error: 'Validation error',
      details: error.message
    });
  }
});

/**
 * DELETE /api/swift/messages - Clear all messages (for testing)
 */
app.delete('/api/swift/messages', (req, res) => {
  messageStore.clearAllMessages();
  res.json({ success: true, message: 'All messages cleared' });
});

// Socket.IO connection handling
io.on('connection', (socket) => {
  console.log('Client connected to SWIFT Test Platform:', socket.id);
  
  // Send recent messages to new clients
  const recentMessages = messageStore.getMessages({ limit: 20 });
  socket.emit('initial-messages', recentMessages);
  
  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

// Serve the web interface
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start server
const PORT = process.env.PORT || 8081;
server.listen(PORT, () => {
  console.log(`🚀 SWIFT Test Platform running on port ${PORT}`);
  console.log(`📱 Web Interface: http://localhost:${PORT}`);
  console.log(`🔗 API Base: http://localhost:${PORT}/api`);
  console.log(`📡 WebSocket: Available for real-time updates`);
  console.log(`✅ Supported Messages: MT760, MT765, MT767, MT768, MT769, MT798`);
});

module.exports = { app, server, io };





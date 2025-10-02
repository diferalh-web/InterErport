const moment = require('moment');

class MessageStore {
  constructor() {
    this.messages = new Map();
    this.messageHistory = [];
    this.maxHistorySize = 1000;
    this.statistics = {
      totalMessages: 0,
      messagesByType: {},
      messagesByStatus: {},
      averageProcessingTime: 0
    };
  }

  /**
   * Store a SWIFT message
   */
  storeMessage(message) {
    // Validate message structure
    if (!message.id || !message.type || !message.timestamp) {
      throw new Error('Invalid message structure: id, type, and timestamp are required');
    }

    // Add storage metadata
    const storedMessage = {
      ...message,
      storedAt: moment().toISOString(),
      searchableContent: this.createSearchableContent(message)
    };

    // Store in main collection
    this.messages.set(message.id, storedMessage);

    // Add to history (maintaining size limit)
    this.messageHistory.unshift(storedMessage);
    if (this.messageHistory.length > this.maxHistorySize) {
      this.messageHistory = this.messageHistory.slice(0, this.maxHistorySize);
    }

    // Update statistics
    this.updateStatistics(storedMessage);

    return storedMessage;
  }

  /**
   * Get a specific message by ID
   */
  getMessage(messageId) {
    return this.messages.get(messageId) || null;
  }

  /**
   * Get messages with optional filtering
   */
  getMessages(options = {}) {
    const { type, status, direction, limit = 100, offset = 0, search, sortBy = 'timestamp', sortOrder = 'desc' } = options;

    let messages = Array.from(this.messages.values());

    // Apply filters
    if (type) {
      messages = messages.filter(msg => msg.type === type);
    }

    if (status) {
      messages = messages.filter(msg => msg.status === status);
    }

    if (direction) {
      messages = messages.filter(msg => msg.direction === direction);
    }

    if (search) {
      const searchLower = search.toLowerCase();
      messages = messages.filter(msg => 
        msg.searchableContent && msg.searchableContent.includes(searchLower)
      );
    }

    // Sort messages
    messages.sort((a, b) => {
      let aValue = a[sortBy];
      let bValue = b[sortBy];

      if (sortBy === 'timestamp' || sortBy === 'storedAt') {
        aValue = new Date(aValue);
        bValue = new Date(bValue);
      }

      if (sortOrder === 'desc') {
        return bValue > aValue ? 1 : bValue < aValue ? -1 : 0;
      } else {
        return aValue > bValue ? 1 : aValue < bValue ? -1 : 0;
      }
    });

    // Apply pagination
    const startIndex = offset;
    const endIndex = startIndex + limit;

    return {
      messages: messages.slice(startIndex, endIndex),
      total: messages.length,
      offset: startIndex,
      limit: limit,
      hasMore: endIndex < messages.length
    };
  }

  /**
   * Get message history (recent messages)
   */
  getMessageHistory(limit = 50) {
    return this.messageHistory.slice(0, limit);
  }

  /**
   * Get messages related to a specific transaction
   */
  getRelatedMessages(messageId) {
    const originalMessage = this.getMessage(messageId);
    if (!originalMessage) {
      return [];
    }

    // Find messages that reference this message or share transaction references
    const relatedMessages = Array.from(this.messages.values()).filter(msg => {
      if (msg.id === messageId) return true;
      if (msg.relatedMessageId === messageId) return true;
      if (originalMessage.relatedMessageId && msg.id === originalMessage.relatedMessageId) return true;
      
      // Check for same transaction reference
      if (originalMessage.content?.transactionReference && 
          msg.content?.transactionReference === originalMessage.content.transactionReference) {
        return true;
      }

      // Check for amendment relationships
      if (originalMessage.content?.amendmentReference && 
          (msg.content?.amendmentReference === originalMessage.content.amendmentReference ||
           msg.content?.originalReference === originalMessage.content.amendmentReference)) {
        return true;
      }

      return false;
    });

    // Sort by timestamp
    return relatedMessages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
  }

  /**
   * Get message conversation thread
   */
  getMessageThread(messageId) {
    const relatedMessages = this.getRelatedMessages(messageId);
    
    // Build conversation thread
    const thread = {
      originalMessage: relatedMessages[0] || null,
      responses: relatedMessages.slice(1),
      messageCount: relatedMessages.length,
      threadStatus: this.determineThreadStatus(relatedMessages),
      lastActivity: relatedMessages.length > 0 ? relatedMessages[relatedMessages.length - 1].timestamp : null
    };

    return thread;
  }

  /**
   * Update message status
   */
  updateMessageStatus(messageId, newStatus, notes = null) {
    const message = this.messages.get(messageId);
    if (!message) {
      throw new Error(`Message not found: ${messageId}`);
    }

    const updatedMessage = {
      ...message,
      status: newStatus,
      lastUpdated: moment().toISOString(),
      statusHistory: [
        ...(message.statusHistory || []),
        {
          previousStatus: message.status,
          newStatus: newStatus,
          timestamp: moment().toISOString(),
          notes: notes
        }
      ]
    };

    this.messages.set(messageId, updatedMessage);
    this.updateStatistics(updatedMessage);

    return updatedMessage;
  }

  /**
   * Delete a message
   */
  deleteMessage(messageId) {
    const message = this.messages.get(messageId);
    if (!message) {
      return false;
    }

    // Remove from main collection
    this.messages.delete(messageId);

    // Remove from history
    this.messageHistory = this.messageHistory.filter(msg => msg.id !== messageId);

    // Update statistics
    this.statistics.totalMessages--;
    this.statistics.messagesByType[message.type]--;
    this.statistics.messagesByStatus[message.status]--;

    return true;
  }

  /**
   * Clear all messages
   */
  clearAllMessages() {
    this.messages.clear();
    this.messageHistory = [];
    this.statistics = {
      totalMessages: 0,
      messagesByType: {},
      messagesByStatus: {},
      averageProcessingTime: 0
    };
  }

  /**
   * Get statistics
   */
  getStatistics() {
    return {
      ...this.statistics,
      currentTime: moment().toISOString(),
      messagesInMemory: this.messages.size,
      historySize: this.messageHistory.length
    };
  }

  /**
   * Get messages by date range
   */
  getMessagesByDateRange(startDate, endDate, options = {}) {
    const start = moment(startDate);
    const end = moment(endDate);

    let messages = Array.from(this.messages.values()).filter(msg => {
      const msgDate = moment(msg.timestamp);
      return msgDate.isBetween(start, end, null, '[]');
    });

    // Apply additional filters
    const { type, status, direction } = options;
    if (type) messages = messages.filter(msg => msg.type === type);
    if (status) messages = messages.filter(msg => msg.status === status);
    if (direction) messages = messages.filter(msg => msg.direction === direction);

    return messages.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
  }

  /**
   * Search messages
   */
  searchMessages(query, options = {}) {
    const { type, status, direction, limit = 50 } = options;
    const queryLower = query.toLowerCase();

    let messages = Array.from(this.messages.values()).filter(msg => {
      if (msg.searchableContent && msg.searchableContent.includes(queryLower)) {
        return true;
      }
      return false;
    });

    // Apply filters
    if (type) messages = messages.filter(msg => msg.type === type);
    if (status) messages = messages.filter(msg => msg.status === status);
    if (direction) messages = messages.filter(msg => msg.direction === direction);

    // Sort by relevance (could be enhanced with scoring)
    messages.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

    return {
      results: messages.slice(0, limit),
      total: messages.length,
      query: query
    };
  }

  /**
   * Export messages to JSON
   */
  exportMessages(options = {}) {
    const { type, status, startDate, endDate } = options;
    let messages = Array.from(this.messages.values());

    // Apply filters
    if (type) messages = messages.filter(msg => msg.type === type);
    if (status) messages = messages.filter(msg => msg.status === status);
    if (startDate || endDate) {
      const start = startDate ? moment(startDate) : moment('1970-01-01');
      const end = endDate ? moment(endDate) : moment();
      messages = messages.filter(msg => {
        const msgDate = moment(msg.timestamp);
        return msgDate.isBetween(start, end, null, '[]');
      });
    }

    return {
      exportedAt: moment().toISOString(),
      totalMessages: messages.length,
      filters: options,
      messages: messages
    };
  }

  /**
   * Create searchable content for a message
   */
  createSearchableContent(message) {
    const searchParts = [
      message.type,
      message.status,
      message.direction,
      message.senderBIC,
      message.receiverBIC
    ];

    // Add content fields
    if (message.content) {
      Object.values(message.content).forEach(value => {
        if (typeof value === 'string') {
          searchParts.push(value);
        }
      });
    }

    // Add field values
    if (message.fields) {
      Object.values(message.fields).forEach(value => {
        if (typeof value === 'string') {
          searchParts.push(value);
        }
      });
    }

    return searchParts.join(' ').toLowerCase();
  }

  /**
   * Update statistics
   */
  updateStatistics(message) {
    this.statistics.totalMessages++;

    // Count by type
    this.statistics.messagesByType[message.type] = 
      (this.statistics.messagesByType[message.type] || 0) + 1;

    // Count by status
    this.statistics.messagesByStatus[message.status] = 
      (this.statistics.messagesByStatus[message.status] || 0) + 1;

    // Calculate average processing time (if available)
    if (message.processingTime && message.timestamp) {
      const processingStart = moment(message.timestamp);
      const processingEnd = moment(message.processingTime);
      const processingDuration = processingEnd.diff(processingStart, 'milliseconds');
      
      // Simple moving average (could be enhanced)
      if (this.statistics.averageProcessingTime === 0) {
        this.statistics.averageProcessingTime = processingDuration;
      } else {
        this.statistics.averageProcessingTime = 
          (this.statistics.averageProcessingTime + processingDuration) / 2;
      }
    }
  }

  /**
   * Determine thread status based on messages
   */
  determineThreadStatus(messages) {
    if (messages.length === 0) return 'EMPTY';
    if (messages.length === 1) return 'PENDING';

    const lastMessage = messages[messages.length - 1];
    
    if (lastMessage.type === 'MT768') return 'ACKNOWLEDGED';
    if (lastMessage.type === 'MT767') return 'CONFIRMED';
    if (lastMessage.type === 'MT769') return 'DISPUTED';
    
    return 'IN_PROGRESS';
  }

  /**
   * Get message flow analysis
   */
  getMessageFlowAnalysis(timeframe = 'day') {
    const now = moment();
    let startTime;

    switch (timeframe) {
      case 'hour':
        startTime = now.clone().subtract(1, 'hour');
        break;
      case 'day':
        startTime = now.clone().subtract(1, 'day');
        break;
      case 'week':
        startTime = now.clone().subtract(1, 'week');
        break;
      case 'month':
        startTime = now.clone().subtract(1, 'month');
        break;
      default:
        startTime = now.clone().subtract(1, 'day');
    }

    const messages = Array.from(this.messages.values()).filter(msg => 
      moment(msg.timestamp).isAfter(startTime)
    );

    const inbound = messages.filter(msg => msg.direction === 'INCOMING');
    const outbound = messages.filter(msg => msg.direction === 'OUTGOING');

    return {
      timeframe,
      startTime: startTime.toISOString(),
      endTime: now.toISOString(),
      total: messages.length,
      inbound: inbound.length,
      outbound: outbound.length,
      byType: this.groupMessagesByType(messages),
      byStatus: this.groupMessagesByStatus(messages),
      averageResponseTime: this.calculateAverageResponseTime(messages)
    };
  }

  /**
   * Group messages by type
   */
  groupMessagesByType(messages) {
    return messages.reduce((acc, msg) => {
      acc[msg.type] = (acc[msg.type] || 0) + 1;
      return acc;
    }, {});
  }

  /**
   * Group messages by status
   */
  groupMessagesByStatus(messages) {
    return messages.reduce((acc, msg) => {
      acc[msg.status] = (acc[msg.status] || 0) + 1;
      return acc;
    }, {});
  }

  /**
   * Calculate average response time
   */
  calculateAverageResponseTime(messages) {
    const responseMessages = messages.filter(msg => msg.isResponse && msg.relatedMessageId);
    
    if (responseMessages.length === 0) return 0;

    const totalResponseTime = responseMessages.reduce((sum, response) => {
      const original = this.getMessage(response.relatedMessageId);
      if (original) {
        const responseTime = moment(response.timestamp).diff(moment(original.timestamp), 'milliseconds');
        return sum + responseTime;
      }
      return sum;
    }, 0);

    return Math.round(totalResponseTime / responseMessages.length);
  }
}

module.exports = MessageStore;





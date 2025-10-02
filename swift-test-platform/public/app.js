// SWIFT Test Platform Client Application
class SwiftTestPlatform {
    constructor() {
        this.socket = null;
        this.messages = [];
        this.selectedMessage = null;
        this.statistics = {
            total: 0,
            sent: 0,
            received: 0,
            failed: 0
        };
        
        this.messageFieldTemplates = {
            'MT760': [
                { name: 'transactionReference', label: 'Transaction Reference', type: 'text', required: true, placeholder: 'GTR202400001' },
                { name: 'guaranteeAmount', label: 'Guarantee Amount', type: 'number', required: true, placeholder: '100000.00' },
                { name: 'currency', label: 'Currency', type: 'select', required: true, options: ['USD', 'EUR', 'GBP', 'SGD', 'JPY'] },
                { name: 'issueDate', label: 'Issue Date', type: 'date', required: true },
                { name: 'expiryDate', label: 'Expiry Date', type: 'date', required: true },
                { name: 'applicantName', label: 'Applicant Name', type: 'text', required: true, placeholder: 'ABC Construction Company' },
                { name: 'beneficiaryName', label: 'Beneficiary Name', type: 'text', required: true, placeholder: 'XYZ Government Authority' },
                { name: 'guaranteeText', label: 'Guarantee Text', type: 'textarea', required: false, placeholder: 'Performance guarantee as per contract terms...' }
            ],
            'MT765': [
                { name: 'amendmentReference', label: 'Amendment Reference', type: 'text', required: true, placeholder: 'AMD202400001' },
                { name: 'originalReference', label: 'Original Reference', type: 'text', required: true, placeholder: 'GTR202400001' },
                { name: 'amendmentType', label: 'Amendment Type', type: 'select', required: true, options: ['AMOUNT_INCREASE', 'AMOUNT_DECREASE', 'EXPIRY_EXTENSION', 'TEXT_AMENDMENT'] },
                { name: 'newValue', label: 'New Value', type: 'text', required: false, placeholder: 'New amount or date' },
                { name: 'amendmentReason', label: 'Amendment Reason', type: 'textarea', required: false, placeholder: 'Contract modification as requested...' }
            ],
            'MT767': [
                { name: 'amendmentReference', label: 'Amendment Reference', type: 'text', required: true, placeholder: 'AMD202400001' },
                { name: 'confirmationType', label: 'Confirmation Type', type: 'select', required: true, options: ['ACCEPTED', 'REJECTED', 'PARTIAL'] },
                { name: 'confirmationText', label: 'Confirmation Text', type: 'textarea', required: false, placeholder: 'Amendment accepted and processed...' }
            ],
            'MT768': [
                { name: 'originalReference', label: 'Original Reference', type: 'text', required: true, placeholder: 'GTR202400001' },
                { name: 'acknowledgmentType', label: 'Acknowledgment Type', type: 'select', required: true, options: ['RECEIVED', 'PROCESSED', 'REJECTED'] },
                { name: 'acknowledgmentText', label: 'Acknowledgment Text', type: 'textarea', required: false, placeholder: 'Message received and acknowledged...' }
            ],
            'MT769': [
                { name: 'claimReference', label: 'Claim Reference', type: 'text', required: true, placeholder: 'CLM202400001' },
                { name: 'originalReference', label: 'Original Reference', type: 'text', required: true, placeholder: 'GTR202400001' },
                { name: 'claimAmount', label: 'Claim Amount', type: 'number', required: false, placeholder: '50000.00' },
                { name: 'claimReason', label: 'Claim Reason', type: 'textarea', required: true, placeholder: 'Non-performance of contract obligations...' }
            ],
            'MT798': [
                { name: 'transactionReference', label: 'Transaction Reference', type: 'text', required: true, placeholder: 'MSG202400001' },
                { name: 'messageText', label: 'Message Text', type: 'textarea', required: true, placeholder: 'Free format message content...' }
            ]
        };

        this.init();
    }

    async init() {
        this.setupEventListeners();
        await this.connectSocket();
        this.updateMessageFields();
        await this.loadInitialData();
    }

    setupEventListeners() {
        // Message type change
        document.getElementById('messageType').addEventListener('change', () => {
            this.updateMessageFields();
        });

        // Form submission
        document.getElementById('messageForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.sendMessage();
        });

        // Validation button
        document.getElementById('validateBtn').addEventListener('click', async () => {
            await this.validateMessage();
        });

        // Clear messages button
        document.getElementById('clearMessages').addEventListener('click', async () => {
            await this.clearAllMessages();
        });

        // Search and filters
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.filterMessages();
        });

        document.getElementById('searchInput').addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                this.filterMessages();
            }
        });

        document.getElementById('filterType').addEventListener('change', () => {
            this.filterMessages();
        });

        document.getElementById('filterDirection').addEventListener('change', () => {
            this.filterMessages();
        });

        // Refresh button
        document.getElementById('refreshBtn').addEventListener('click', async () => {
            await this.loadMessages();
        });

        // Modal close
        document.getElementById('closeValidationModal').addEventListener('click', () => {
            document.getElementById('validationModal').classList.add('hidden');
        });
    }

    async connectSocket() {
        this.socket = io();

        this.socket.on('connect', () => {
            this.updateConnectionStatus(true);
            console.log('Connected to SWIFT Test Platform');
        });

        this.socket.on('disconnect', () => {
            this.updateConnectionStatus(false);
            console.log('Disconnected from SWIFT Test Platform');
        });

        this.socket.on('message-sent', (message) => {
            this.addMessageToFeed(message);
            this.updateStatistics();
            this.showToast('Message sent successfully', 'success');
        });

        this.socket.on('message-received', (message) => {
            this.addMessageToFeed(message);
            this.updateStatistics();
            this.showToast(`${message.type} received`, 'info');
        });

        this.socket.on('scenario-message', (message) => {
            this.addMessageToFeed(message);
            this.updateStatistics();
        });

        this.socket.on('initial-messages', (messages) => {
            this.messages = messages;
            this.renderMessageFeed();
            this.updateStatistics();
        });
    }

    updateConnectionStatus(connected) {
        const statusElement = document.getElementById('connectionStatus');
        if (connected) {
            statusElement.innerHTML = `
                <span class="status-indicator status-SENT"></span>
                <span class="text-sm">Connected</span>
            `;
        } else {
            statusElement.innerHTML = `
                <span class="status-indicator status-FAILED animate-pulse-custom"></span>
                <span class="text-sm">Disconnected</span>
            `;
        }
    }

    updateMessageFields() {
        const messageType = document.getElementById('messageType').value;
        const container = document.getElementById('dynamicFields');
        const fields = this.messageFieldTemplates[messageType] || [];

        container.innerHTML = '';

        fields.forEach(field => {
            const fieldDiv = document.createElement('div');
            fieldDiv.className = 'space-y-2';

            const label = document.createElement('label');
            label.className = 'block text-sm font-medium text-gray-700';
            label.textContent = field.label + (field.required ? ' *' : '');

            let input;
            if (field.type === 'select') {
                input = document.createElement('select');
                input.className = 'w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500';
                
                // Add default option
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = `Select ${field.label}`;
                input.appendChild(defaultOption);

                field.options.forEach(option => {
                    const optionElement = document.createElement('option');
                    optionElement.value = option;
                    optionElement.textContent = option;
                    input.appendChild(optionElement);
                });
            } else if (field.type === 'textarea') {
                input = document.createElement('textarea');
                input.className = 'w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500';
                input.rows = 3;
                input.placeholder = field.placeholder || '';
            } else {
                input = document.createElement('input');
                input.type = field.type;
                input.className = 'w-full p-2 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500';
                input.placeholder = field.placeholder || '';
                
                if (field.type === 'date') {
                    input.value = new Date().toISOString().split('T')[0];
                }
            }

            input.name = field.name;
            input.id = field.name;
            if (field.required) {
                input.required = true;
            }

            fieldDiv.appendChild(label);
            fieldDiv.appendChild(input);
            container.appendChild(fieldDiv);
        });
    }

    async sendMessage() {
        const messageType = document.getElementById('messageType').value;
        const senderBIC = document.getElementById('senderBIC').value;
        const receiverBIC = document.getElementById('receiverBIC').value;

        // Collect field data
        const content = {};
        const fields = this.messageFieldTemplates[messageType] || [];
        
        fields.forEach(field => {
            const input = document.getElementById(field.name);
            if (input && input.value) {
                content[field.name] = input.value;
            }
        });

        try {
            const response = await fetch('/api/swift/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    messageType,
                    content,
                    senderBIC,
                    receiverBIC
                }),
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.details || 'Failed to send message');
            }

            const result = await response.json();
            this.showToast('Message sent successfully!', 'success');
            
            // Clear form
            document.getElementById('messageForm').reset();
            this.updateMessageFields();

        } catch (error) {
            console.error('Error sending message:', error);
            this.showToast(`Error: ${error.message}`, 'error');
        }
    }

    async validateMessage() {
        const messageType = document.getElementById('messageType').value;

        // Collect field data
        const content = {};
        const fields = this.messageFieldTemplates[messageType] || [];
        
        fields.forEach(field => {
            const input = document.getElementById(field.name);
            if (input && input.value) {
                content[field.name] = input.value;
            }
        });

        try {
            const response = await fetch('/api/swift/validate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    messageType,
                    content
                }),
            });

            const result = await response.json();
            this.showValidationResults(result);

        } catch (error) {
            console.error('Error validating message:', error);
            this.showToast(`Validation error: ${error.message}`, 'error');
        }
    }

    showValidationResults(result) {
        const modal = document.getElementById('validationModal');
        const content = document.getElementById('validationResults');

        let html = '';

        if (result.isValid) {
            html += `
                <div class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
                    <div class="flex items-center">
                        <i class="fas fa-check-circle mr-2"></i>
                        <span class="font-semibold">Validation Passed</span>
                    </div>
                    <p class="text-sm mt-2">Message is valid and ready to send.</p>
                </div>
            `;
        } else {
            html += `
                <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    <div class="flex items-center">
                        <i class="fas fa-exclamation-circle mr-2"></i>
                        <span class="font-semibold">Validation Failed</span>
                    </div>
                </div>
            `;
        }

        if (result.errors && result.errors.length > 0) {
            html += `
                <div class="mb-4">
                    <h4 class="font-semibold text-red-700 mb-2">Errors:</h4>
                    <ul class="list-disc list-inside space-y-1">
                        ${result.errors.map(error => `<li class="text-red-600 text-sm">${error}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        if (result.warnings && result.warnings.length > 0) {
            html += `
                <div class="mb-4">
                    <h4 class="font-semibold text-yellow-700 mb-2">Warnings:</h4>
                    <ul class="list-disc list-inside space-y-1">
                        ${result.warnings.map(warning => `<li class="text-yellow-600 text-sm">${warning}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        html += `
            <div class="text-sm text-gray-600 mt-4">
                <p><strong>Message Type:</strong> ${result.messageType}</p>
                <p><strong>Fields Validated:</strong> ${result.validatedFields}</p>
            </div>
        `;

        content.innerHTML = html;
        modal.classList.remove('hidden');
    }

    async loadMessages() {
        try {
            const response = await fetch('/api/swift/messages?limit=50');
            const data = await response.json();
            this.messages = data.messages || [];
            this.renderMessageFeed();
            this.updateStatistics();
        } catch (error) {
            console.error('Error loading messages:', error);
            this.showToast('Error loading messages', 'error');
        }
    }

    addMessageToFeed(message) {
        this.messages.unshift(message);
        if (this.messages.length > 100) {
            this.messages = this.messages.slice(0, 100);
        }
        this.renderMessageFeed();
    }

    renderMessageFeed() {
        const feed = document.getElementById('messagesFeed');
        
        if (this.messages.length === 0) {
            feed.innerHTML = `
                <div class="text-center text-gray-500 py-8">
                    <i class="fas fa-inbox text-3xl mb-4"></i>
                    <p>No messages yet. Send a message to get started!</p>
                </div>
            `;
            return;
        }

        const messagesHtml = this.messages.map(message => {
            const timeAgo = moment(message.timestamp).fromNow();
            const statusColor = this.getStatusColor(message.status);
            const directionIcon = message.direction === 'INCOMING' ? 'fa-arrow-down' : 'fa-arrow-up';
            
            return `
                <div class="message-box p-4 border rounded-lg cursor-pointer direction-${message.direction}" 
                     onclick="app.showMessageDetails('${message.id}')">
                    <div class="flex items-start justify-between">
                        <div class="flex-1">
                            <div class="flex items-center space-x-2 mb-1">
                                <span class="font-semibold text-gray-900">${message.type}</span>
                                <i class="fas ${directionIcon} text-sm text-gray-500"></i>
                                <span class="status-indicator status-${message.status}"></span>
                                <span class="text-xs text-gray-500 uppercase">${message.status}</span>
                            </div>
                            <div class="text-sm text-gray-600 mb-1">
                                ${message.senderBIC} → ${message.receiverBIC}
                            </div>
                            ${message.content?.transactionReference ? 
                                `<div class="text-xs text-gray-500">Ref: ${message.content.transactionReference}</div>` : ''
                            }
                        </div>
                        <div class="text-xs text-gray-500 text-right">
                            <div>${timeAgo}</div>
                            ${message.isResponse ? '<div class="text-blue-600">Response</div>' : ''}
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        feed.innerHTML = messagesHtml;
    }

    showMessageDetails(messageId) {
        const message = this.messages.find(m => m.id === messageId);
        if (!message) return;

        this.selectedMessage = message;
        const detailsContainer = document.getElementById('messageDetails');
        const content = document.getElementById('messageDetailsContent');

        const timeFormatted = moment(message.timestamp).format('YYYY-MM-DD HH:mm:ss');
        
        let html = `
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <h3 class="font-semibold text-gray-900 mb-3">Basic Information</h3>
                    <div class="space-y-2 text-sm">
                        <div class="flex justify-between">
                            <span class="text-gray-600">Message Type:</span>
                            <span class="font-medium">${message.type}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Status:</span>
                            <span class="font-medium">
                                <span class="status-indicator status-${message.status}"></span>
                                ${message.status}
                            </span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Direction:</span>
                            <span class="font-medium">${message.direction}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Sender:</span>
                            <span class="font-medium">${message.senderBIC}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Receiver:</span>
                            <span class="font-medium">${message.receiverBIC}</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-gray-600">Timestamp:</span>
                            <span class="font-medium">${timeFormatted}</span>
                        </div>
                        ${message.relatedMessageId ? `
                            <div class="flex justify-between">
                                <span class="text-gray-600">Related To:</span>
                                <span class="font-medium text-blue-600 cursor-pointer" 
                                      onclick="app.showMessageDetails('${message.relatedMessageId}')">
                                    ${message.relatedMessageId}
                                </span>
                            </div>
                        ` : ''}
                    </div>
                </div>

                <div>
                    <h3 class="font-semibold text-gray-900 mb-3">Message Content</h3>
                    <div class="space-y-2 text-sm">
                        ${Object.entries(message.content || {}).map(([key, value]) => `
                            <div class="flex justify-between">
                                <span class="text-gray-600">${this.formatFieldName(key)}:</span>
                                <span class="font-medium text-right ml-2">${this.formatFieldValue(value)}</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>

            ${message.rawMessage ? `
                <div class="mt-6">
                    <h3 class="font-semibold text-gray-900 mb-3">Raw SWIFT Message</h3>
                    <pre class="bg-gray-100 p-4 rounded text-xs overflow-x-auto font-mono">${message.rawMessage}</pre>
                </div>
            ` : ''}
        `;

        content.innerHTML = html;
        detailsContainer.classList.remove('hidden');
    }

    formatFieldName(fieldName) {
        return fieldName.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
    }

    formatFieldValue(value) {
        if (typeof value === 'string' && value.length > 50) {
            return value.substring(0, 50) + '...';
        }
        return value;
    }

    getStatusColor(status) {
        const colors = {
            'SENT': 'green',
            'RECEIVED': 'blue',
            'FAILED': 'red',
            'PENDING': 'yellow',
            'PROCESSED': 'blue',
            'ACKNOWLEDGED': 'green'
        };
        return colors[status] || 'gray';
    }

    updateStatistics() {
        const stats = {
            total: this.messages.length,
            sent: this.messages.filter(m => m.direction === 'OUTGOING').length,
            received: this.messages.filter(m => m.direction === 'INCOMING').length,
            failed: this.messages.filter(m => m.status === 'FAILED').length
        };

        document.getElementById('statTotal').textContent = stats.total;
        document.getElementById('statSent').textContent = stats.sent;
        document.getElementById('statReceived').textContent = stats.received;
        document.getElementById('statFailed').textContent = stats.failed;
    }

    filterMessages() {
        const type = document.getElementById('filterType').value;
        const direction = document.getElementById('filterDirection').value;
        const search = document.getElementById('searchInput').value.toLowerCase();

        let filtered = [...this.messages];

        if (type) {
            filtered = filtered.filter(m => m.type === type);
        }

        if (direction) {
            filtered = filtered.filter(m => m.direction === direction);
        }

        if (search) {
            filtered = filtered.filter(m => 
                JSON.stringify(m).toLowerCase().includes(search)
            );
        }

        // Temporarily replace messages for rendering
        const originalMessages = this.messages;
        this.messages = filtered;
        this.renderMessageFeed();
        this.messages = originalMessages;
    }

    async clearAllMessages() {
        if (!confirm('Are you sure you want to clear all messages? This cannot be undone.')) {
            return;
        }

        try {
            const response = await fetch('/api/swift/messages', {
                method: 'DELETE'
            });

            if (response.ok) {
                this.messages = [];
                this.renderMessageFeed();
                this.updateStatistics();
                this.showToast('All messages cleared', 'success');
                document.getElementById('messageDetails').classList.add('hidden');
            }
        } catch (error) {
            console.error('Error clearing messages:', error);
            this.showToast('Error clearing messages', 'error');
        }
    }

    async loadScenario(scenarioName) {
        const scenarios = {
            'guarantee-issuance': {
                guaranteeAmount: 100000,
                currency: 'USD',
                beneficiary: { name: 'ABC Government Authority', bankBIC: 'BNKSGSGXXXX' },
                applicant: { name: 'XYZ Construction Ltd' }
            },
            'guarantee-amendment': {
                originalGuarantee: { reference: 'GTR202400001', beneficiaryBIC: 'BNKSGSGXXXX' },
                amendmentType: 'AMOUNT_INCREASE',
                newValue: '150000.00'
            },
            'claim-process': {
                guaranteeReference: 'GTR202400001',
                claimAmount: 50000,
                claimReason: 'Non-performance of contract obligations'
            },
            'discrepancy-handling': {
                guaranteeReference: 'GTR202400001',
                discrepancyType: 'DOCUMENT_MISSING',
                description: 'Required performance certificate not provided'
            }
        };

        const parameters = scenarios[scenarioName];
        if (!parameters) {
            this.showToast('Unknown scenario', 'error');
            return;
        }

        try {
            const response = await fetch('/api/swift/simulate/scenario', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    scenario: scenarioName,
                    parameters
                }),
            });

            if (response.ok) {
                const result = await response.json();
                this.showToast(`Scenario "${scenarioName}" started (${result.messagesGenerated} messages)`, 'success');
            } else {
                throw new Error('Failed to start scenario');
            }

        } catch (error) {
            console.error('Error loading scenario:', error);
            this.showToast(`Error: ${error.message}`, 'error');
        }
    }

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast p-4 rounded-lg shadow-lg text-white max-w-sm transform transition-all duration-300 translate-x-full`;
        
        const colors = {
            'success': 'bg-green-500',
            'error': 'bg-red-500',
            'warning': 'bg-yellow-500',
            'info': 'bg-blue-500'
        };
        
        const icons = {
            'success': 'fa-check-circle',
            'error': 'fa-exclamation-circle',
            'warning': 'fa-exclamation-triangle',
            'info': 'fa-info-circle'
        };

        toast.classList.add(colors[type] || colors.info);
        
        toast.innerHTML = `
            <div class="flex items-center">
                <i class="fas ${icons[type] || icons.info} mr-2"></i>
                <span>${message}</span>
                <button onclick="this.parentElement.parentElement.remove()" class="ml-4 text-white hover:text-gray-200">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;

        document.getElementById('toastContainer').appendChild(toast);

        // Animate in
        setTimeout(() => {
            toast.classList.remove('translate-x-full');
        }, 100);

        // Remove after 5 seconds
        setTimeout(() => {
            toast.classList.add('translate-x-full');
            setTimeout(() => {
                if (toast.parentElement) {
                    toast.parentElement.removeChild(toast);
                }
            }, 300);
        }, 5000);
    }

    async loadInitialData() {
        await this.loadMessages();
        
        // Load platform health check
        try {
            const response = await fetch('/api/health');
            const health = await response.json();
            console.log('SWIFT Test Platform Status:', health);
        } catch (error) {
            console.error('Health check failed:', error);
        }
    }
}

// Initialize application
const app = new SwiftTestPlatform();





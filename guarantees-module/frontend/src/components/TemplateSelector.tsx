import React, { useState, useEffect, useCallback } from 'react';
import {
  Modal,
  Select,
  Button,
  Typography,
  Space,
  Card,
  Row,
  Col,
  Spin,
  Alert,
  Tag,
  Input,
  message
} from 'antd';
import {
  FileTextOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { apiService } from '../services/api';
import { GuaranteeTemplate } from '../types/guarantee';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;
const { Option } = Select;

export interface TemplatePreview {
  renderedText: string;
  missingRequired: string[];
  allVariables: string[];
  variableValues: Record<string, string>;
  isValid: boolean;
}

interface TemplateSelectorProps {
  visible: boolean;
  onCancel: () => void;
  onSelect: (template: GuaranteeTemplate, renderedText: string) => void;
  guarantee: {
    id?: number;
    guaranteeType: string;
    language?: string;
    amount?: number;
    currency?: string;
    issueDate?: string;
    expiryDate?: string;
    beneficiaryName?: string;
    beneficiaryAddress?: string;
    isDomestic?: boolean;
  };
}

const TemplateSelector: React.FC<TemplateSelectorProps> = ({
  visible,
  onCancel,
  onSelect,
  guarantee
}) => {
  const [templates, setTemplates] = useState<GuaranteeTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<GuaranteeTemplate | null>(null);
  const [preview, setPreview] = useState<TemplatePreview | null>(null);
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [customVariables, setCustomVariables] = useState<Record<string, string>>({});

  const createMockPreview = useCallback((template: GuaranteeTemplate) => {
    const mockVariables: Record<string, string> = {
      GUARANTEE_REFERENCE: 'GAR-2024-001',
      GUARANTEE_AMOUNT: guarantee.amount?.toString() || '100,000.00',
      GUARANTEE_AMOUNT_WORDS: 'One hundred thousand',
      CURRENCY: guarantee.currency || 'USD',
      ISSUE_DATE: guarantee.issueDate || new Date().toISOString().split('T')[0],
      EXPIRY_DATE: guarantee.expiryDate || new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      BENEFICIARY_NAME: guarantee.beneficiaryName || '[BENEFICIARY NAME]',
      BENEFICIARY_ADDRESS: guarantee.beneficiaryAddress || '[BENEFICIARY ADDRESS]',
      APPLICANT_NAME: '[APPLICANT NAME]',
      IS_DOMESTIC: guarantee.isDomestic ? 'YES' : 'NO',
      GUARANTEE_TYPE: guarantee.guaranteeType || 'PERFORMANCE',
      BANK_NAME: '[BANK NAME]',
      CONTRACT_REFERENCE: '[CONTRACT REFERENCE]',
      CONTRACT_DATE: '[CONTRACT DATE]',
      GOVERNING_LAW: '[GOVERNING LAW]'
    };

    // Merge with custom variables - custom variables take priority
    const allVariables: Record<string, string> = { ...mockVariables, ...customVariables };

        // Simple template rendering - replace {{VARIABLE}} with values
        let renderedText = template.templateText || template.templateContent || 'Template content not available';
    
    // Ensure renderedText is a string
    if (typeof renderedText !== 'string') {
      renderedText = String(renderedText);
    }
    
    Object.keys(allVariables).forEach(key => {
      try {
        // Escape special regex characters in the key (including square brackets)
        const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(`\\{\\{${escapedKey}\\}\\}`, 'g');
        const value = String(allVariables[key] || ''); // Ensure value is string
        renderedText = renderedText.replace(regex, value);
      } catch (error) {
        console.warn(`Error replacing variable ${key}:`, error);
      }
    });

    // Handle conditional sections (simplified)
    renderedText = renderedText.replace(/\{\{#IS_DOMESTIC\}\}(.*?)\{\{\/IS_DOMESTIC\}\}/gs, 
      guarantee.isDomestic ? '$1' : '');

    // Parse requiredVariables - it's a JSON string, not comma-separated
    let requiredVars: string[] = [];
    if (template.requiredVariables) {
      try {
        requiredVars = JSON.parse(template.requiredVariables);
      } catch (e) {
        // Fallback to comma-separated if JSON parsing fails
        requiredVars = template.requiredVariables.split(',').map(v => v.trim());
      }
    }
    const missingRequired = requiredVars.filter(variable => {
      const value = String(allVariables[variable] || '');
      return !value || value === `[${variable}]` || value.trim() === '';
    });

    setPreview({
      renderedText,
      missingRequired,
      allVariables: Object.keys(allVariables),
      variableValues: allVariables,
      isValid: missingRequired.length === 0
    });
  }, [guarantee, customVariables]);

  const loadTemplates = useCallback(async () => {
    setLoading(true);
    try {
      const templateList = await apiService.getTemplatesByType(guarantee.guaranteeType);
      setTemplates(templateList);
    } catch (error) {
      console.error('Failed to load templates:', error);
      message.error('Failed to load templates');
    } finally {
      setLoading(false);
    }
  }, [guarantee.guaranteeType]);

  const generatePreview = useCallback(async (template: GuaranteeTemplate) => {
    if (!template) {
      return;
    }

    // Store IDs in local variables for type safety
    const templateId = template.id;
    const guaranteeId = guarantee.id;
    
    if (!templateId || !guaranteeId) {
      // If no template ID or guarantee ID, create a mock preview
      createMockPreview(template);
      return;
    }

    setPreviewLoading(true);
    try {
      const previewData = await apiService.renderTemplatePreview(templateId, guaranteeId);
      setPreview(previewData);
    } catch (error) {
      console.error('Failed to generate preview:', error);
      message.error('Failed to generate preview');
      createMockPreview(template);
    } finally {
      setPreviewLoading(false);
    }
  }, [guarantee.id, createMockPreview]);

  // Load templates when modal opens
  useEffect(() => {
    if (visible && guarantee.guaranteeType) {
      loadTemplates();
    }
  }, [visible, guarantee.guaranteeType, loadTemplates]);

  // Auto-select default template
  useEffect(() => {
    if (templates.length > 0 && !selectedTemplate) {
      const defaultTemplate = templates.find(t => t.isDefault) || templates[0];
      setSelectedTemplate(defaultTemplate);
      generatePreview(defaultTemplate);
    }
  }, [templates, selectedTemplate, generatePreview]);


  const handleTemplateSelect = (templateId: number) => {
    const template = templates.find(t => t.id === templateId);
    if (template) {
      setSelectedTemplate(template);
      generatePreview(template);
    }
  };

  const handleCustomVariableChange = (variable: string, value: string) => {
    setCustomVariables(prev => ({
      ...prev,
      [variable]: String(value || '') // Ensure value is always a string
    }));

    // Regenerate preview with updated variables immediately
    if (selectedTemplate) {
      createMockPreview(selectedTemplate);
    }
  };

  const handleSelectTemplate = () => {
    if (selectedTemplate && preview) {
      if (preview.missingRequired.length > 0) {
        message.warning('Please provide values for all required variables before selecting the template.');
        return;
      }
      onSelect(selectedTemplate, preview.renderedText);
    }
  };

  const renderVariableEditor = () => {
    if (!selectedTemplate || !preview) return null;

    // Parse variables - they're stored as JSON strings
    let requiredVars: string[] = [];
    let optionalVars: string[] = [];
    
    if (selectedTemplate.requiredVariables) {
      try {
        requiredVars = JSON.parse(selectedTemplate.requiredVariables);
      } catch (e) {
        // Fallback to comma-separated if JSON parsing fails
        requiredVars = selectedTemplate.requiredVariables.split(',').map(v => v.trim());
      }
    }
    
    if (selectedTemplate.optionalVariables) {
      try {
        optionalVars = JSON.parse(selectedTemplate.optionalVariables);
      } catch (e) {
        // Fallback to comma-separated if JSON parsing fails
        optionalVars = selectedTemplate.optionalVariables.split(',').map(v => v.trim());
      }
    }
    
    const allVariables = [...requiredVars, ...optionalVars];

    return (
      <Card size="small" title="Variable Values" style={{ marginTop: 16 }}>
        <Row gutter={[16, 8]}>
          {allVariables.map(variable => {
            const isRequired = requiredVars.includes(variable);
            // Use customVariables as primary source, fallback to preview values
            // Ensure currentValue is always a string
            const rawValue = customVariables[variable] || preview.variableValues[variable] || '';
            const currentValue = typeof rawValue === 'string' ? rawValue : String(rawValue || '');
            const isMissing = preview.missingRequired.includes(variable);

            return (
              <Col span={12} key={variable}>
                <div style={{ marginBottom: 8 }}>
                  <Text strong={isRequired} type={isMissing ? 'danger' : undefined}>
                    {variable}
                    {isRequired && <Text type="danger"> *</Text>}
                  </Text>
                  <Input
                    size="small"
                    placeholder={`Enter ${variable.toLowerCase()}`}
                    value={currentValue.startsWith('[') && currentValue.endsWith(']') ? '' : currentValue}
                    onChange={(e) => handleCustomVariableChange(variable, e.target.value)}
                    status={isMissing ? 'error' : undefined}
                  />
                </div>
              </Col>
            );
          })}
        </Row>
      </Card>
    );
  };

  return (
    <Modal
      title={
        <Space>
          <FileTextOutlined />
          Select Guarantee Template
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      width={1200}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          Cancel
        </Button>,
        <Button
          key="select"
          type="primary"
          onClick={handleSelectTemplate}
          disabled={!selectedTemplate || !preview?.isValid}
          icon={<CheckCircleOutlined />}
        >
          Use This Template
        </Button>
      ]}
    >
      <Row gutter={24}>
        {/* Template Selection */}
        <Col span={10}>
          <Card size="small" title="Available Templates">
            {loading ? (
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <Spin size="large" />
              </div>
            ) : (
              <>
                <Select
                  style={{ width: '100%', marginBottom: 16 }}
                  placeholder="Select a template"
                  value={selectedTemplate?.id}
                  onChange={handleTemplateSelect}
                  showSearch
                  optionFilterProp="children"
                >
                  {templates.map(template => (
                    <Option key={template.id} value={template.id}>
                      <Space>
                        {template.templateName}
                        {template.isDefault && <Tag color="blue">Default</Tag>}
                      </Space>
                    </Option>
                  ))}
                </Select>

                {selectedTemplate && (
                  <Card size="small" style={{ marginTop: 16 }}>
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <div>
                        <Text strong>Template:</Text> {selectedTemplate.templateName}
                      </div>
                      <div>
                        <Text strong>Type:</Text> {selectedTemplate.guaranteeType}
                      </div>
                      <div>
                        <Text strong>Language:</Text> {selectedTemplate.language}
                      </div>
                      {selectedTemplate.isDomestic !== null && (
                        <div>
                          <Text strong>Scope:</Text> {selectedTemplate.isDomestic ? 'Domestic' : 'International'}
                        </div>
                      )}
                      {selectedTemplate.description && (
                        <div>
                          <Text strong>Description:</Text>
                          <Paragraph style={{ margin: 0, marginTop: 4 }}>
                            {selectedTemplate.description}
                          </Paragraph>
                        </div>
                      )}
                    </Space>
                  </Card>
                )}
              </>
            )}
          </Card>

          {renderVariableEditor()}
        </Col>

        {/* Preview */}
        <Col span={14}>
          <Card
            size="small"
            title={
              <Space>
                <EyeOutlined />
                Template Preview
                {preview && (
                  <Tag color={preview.isValid ? 'success' : 'error'}>
                    {preview.isValid ? 'Valid' : 'Incomplete'}
                  </Tag>
                )}
              </Space>
            }
          >
            {previewLoading ? (
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <Spin size="large" />
                <div style={{ marginTop: 8 }}>Generating preview...</div>
              </div>
            ) : preview ? (
              <div>
                {preview.missingRequired.length > 0 && (
                  <Alert
                    message="Missing Required Variables"
                    description={`Please provide values for: ${preview.missingRequired.join(', ')}`}
                    type="warning"
                    style={{ marginBottom: 16 }}
                    showIcon
                  />
                )}
                
                <TextArea
                  value={preview.renderedText}
                  autoSize={{ minRows: 15, maxRows: 25 }}
                  readOnly
                  style={{
                    fontFamily: 'monospace',
                    fontSize: '12px',
                    backgroundColor: '#f5f5f5'
                  }}
                />

                <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                  <Text type="secondary">
                    Variables used: {preview.allVariables.length} | 
                    Required: {selectedTemplate?.requiredVariables?.split(',').length || 0} | 
                    Optional: {selectedTemplate?.optionalVariables?.split(',').length || 0}
                  </Text>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                <InfoCircleOutlined style={{ fontSize: 24, marginBottom: 8 }} />
                <div>Select a template to see preview</div>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </Modal>
  );
};

export default TemplateSelector;

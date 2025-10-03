import React, { useState } from 'react';
import {
  Card,
  Form,
  Input,
  Select,
  DatePicker,
  InputNumber,
  Button,
  Row,
  Col,
  Typography,
  Space,
  message,
  Switch,
  Divider,
} from 'antd';
import { useForm } from 'antd/es/form/Form';
import { useMutation, useQuery } from 'react-query';
import { useNavigate } from 'react-router-dom';
import { SaveOutlined, SendOutlined, ArrowLeftOutlined, FileTextOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

import { apiService } from '../services/api';
import {
  GuaranteeFormData,
  GuaranteeType,
  Client,
} from '../types/guarantee';
import TemplateSelector from '../components/TemplateSelector';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { TextArea } = Input;

const CreateGuarantee: React.FC = () => {
  const navigate = useNavigate();
  const [form] = useForm<GuaranteeFormData>();
  const [saveAsDraft, setSaveAsDraft] = useState(true);
  const [templateSelectorVisible, setTemplateSelectorVisible] = useState(false);
  const { t } = useAppTranslation();

  // Fetch clients for applicant selection
  const { data: clientsData, isLoading: clientsLoading } = useQuery(
    'clients',
    () => apiService.getClients(0, 100)
  );

  // Mutation for creating guarantee
  const createMutation = useMutation((data: GuaranteeFormData) => apiService.createGuarantee(data), {
    onSuccess: (guarantee) => {
      if (saveAsDraft) {
        message.success(t('messages.saveSuccess'));
        navigate(`/guarantees/${guarantee.id}`);
      } else {
        // If not saving as draft, submit immediately
        submitMutation.mutate(guarantee.id!);
      }
    },
    onError: () => {
      message.error(t('messages.createError'));
    },
  });

  // Mutation for submitting guarantee
  const submitMutation = useMutation((id: number) => apiService.submitGuarantee(id), {
    onSuccess: (guarantee) => {
      message.success(t('messages.createSuccess'));
      navigate(`/guarantees/${guarantee.id}`);
    },
    onError: () => {
      message.error(t('messages.updateError'));
    },
  });

  const handleSubmit = async (values: GuaranteeFormData) => {
    try {
      // Format dates and generate reference if not provided
      const formattedValues = {
        ...values,
        issueDate: dayjs(values.issueDate).format('YYYY-MM-DD'),
        expiryDate: dayjs(values.expiryDate).format('YYYY-MM-DD'),
        // Generate reference if not provided (backend will also generate if empty)
        reference: (values.reference && values.reference.trim()) || `GT-${dayjs().format('YYYYMMDD')}-${Date.now().toString().slice(-6)}`,
      };

      console.log('Submitting guarantee with data:', formattedValues);
      createMutation.mutate(formattedValues);
    } catch (error) {
      message.error(t('validation.required'));
    }
  };

  const handleSave = () => {
    setSaveAsDraft(true);
    form.submit();
  };

  const handleSubmitForApproval = () => {
    setSaveAsDraft(false);
    form.submit();
  };

  const handleTemplateSelect = (template: any, renderedText: string) => {
    form.setFieldsValue({
      guaranteeText: renderedText
    });
    setTemplateSelectorVisible(false);
    message.success(t('templates.templateReadyToUse'));
  };

  const openTemplateSelector = () => {
    const currentValues = form.getFieldsValue();
    if (!currentValues.guaranteeType) {
      message.warning(t('guarantees.selectTypeFirst', { defaultValue: 'Please select a guarantee type first' }));
      return;
    }
    setTemplateSelectorVisible(true);
  };

  // Currency options
  const currencyOptions = [
    { value: 'USD', label: 'USD - US Dollar' },
    { value: 'EUR', label: 'EUR - Euro' },
    { value: 'GBP', label: 'GBP - British Pound' },
    { value: 'JPY', label: 'JPY - Japanese Yen' },
    { value: 'CHF', label: 'CHF - Swiss Franc' },
    { value: 'CAD', label: 'CAD - Canadian Dollar' },
    { value: 'AUD', label: 'AUD - Australian Dollar' },
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '16px' }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/guarantees')}
        >
          {t('common.back')}
        </Button>
        <div>
          <Title level={2} style={{ margin: 0 }}>{t('guarantees.createNew')}</Title>
          <Text type="secondary">{t('guarantees.guaranteeDetails')}</Text>
        </div>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          guaranteeType: GuaranteeType.PERFORMANCE,
          currency: 'USD',
          issueDate: dayjs(),
          expiryDate: dayjs().add(1, 'year'),
          isDomestic: false,
          language: 'EN',
        }}
      >
        <Row gutter={24}>
          <Col span={16}>
            {/* Basic Information */}
            <Card title={t('forms.basicInformation', { defaultValue: 'Basic Information' })} style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.guaranteeType')}
                    name="guaranteeType"
                    rules={[{ required: true, message: t('validation.required') }]}
                  >
                    <Select placeholder={t('forms.placeholders.selectOption')}>
                      {Object.keys(GuaranteeType).map((key) => (
                        <Select.Option key={key} value={key}>
                          {t(`guarantees.guaranteeTypes.${key}`)}
                        </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.guaranteeReference')}
                    name="reference"
                    rules={[{ max: 35, message: t('validation.maxLength', { max: 35 }) }]}
                  >
                    <Input placeholder={t('forms.autoGenerated', { defaultValue: 'Auto-generated if not provided' })} />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.applicantName')}
                    name="applicantId"
                    rules={[{ required: true, message: t('validation.required') }]}
                  >
                    <Select
                      placeholder={t('forms.placeholders.selectOption')}
                      loading={clientsLoading}
                      showSearch
                      filterOption={(input, option) =>
                        (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                      }
                      options={clientsData?.content.map((client: Client) => ({
                        value: client.id,
                        label: `${client.clientCode} - ${client.name}`,
                      })) || []}
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={8}>
                  <Form.Item
                    label={t('guarantees.guaranteeAmount')}
                    name="amount"
                    rules={[
                      { required: true, message: t('validation.required') },
                      { type: 'number', min: 0.01, message: t('validation.min', { min: 0.01 }) },
                    ]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      placeholder={t('forms.placeholders.enterAmount')}
                      min={0.01}
                      precision={2}
                      formatter={(value) => {
                        if (!value) return '';
                        const num = parseFloat(value.toString());
                        if (isNaN(num)) return '';
                        return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                      }}
                      parser={(value) => {
                        if (!value) return '';
                        return parseFloat(value.replace(/,/g, ''));
                      }}
                    />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label={t('guarantees.currency')}
                    name="currency"
                    rules={[{ required: true, message: t('validation.required') }]}
                  >
                    <Select placeholder={t('forms.placeholders.selectOption')} options={currencyOptions} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label={t('guarantees.language')}
                    name="language"
                  >
                    <Select>
                      <Select.Option value="EN">{t('settings.languages.en')}</Select.Option>
                      <Select.Option value="ES">{t('settings.languages.es')}</Select.Option>
                      <Select.Option value="DE">{t('settings.languages.de')}</Select.Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.issueDate')}
                    name="issueDate"
                    rules={[{ required: true, message: t('validation.required') }]}
                  >
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.expiryDate')}
                    name="expiryDate"
                    rules={[
                      { required: true, message: t('validation.required') },
                      ({ getFieldValue }) => ({
                        validator(_, value) {
                          if (!value || !getFieldValue('issueDate')) {
                            return Promise.resolve();
                          }
                          if (dayjs(value).isAfter(dayjs(getFieldValue('issueDate')))) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error(t('validation.expiryAfterIssue', { 
                            defaultValue: 'Expiry date must be after issue date' 
                          })));
                        },
                      }),
                    ]}
                  >
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            {/* Beneficiary Information */}
            <Card title={t('forms.beneficiaryInformation', { defaultValue: 'Beneficiary Information' })} style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.beneficiaryName')}
                    name="beneficiaryName"
                    rules={[
                      { required: true, message: t('validation.required') },
                      { max: 140, message: t('validation.maxLength', { max: 140 }) },
                    ]}
                  >
                    <Input placeholder={t('forms.placeholders.enterText')} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label={t('forms.advisingBankBic', { defaultValue: 'Advising Bank BIC' })}
                    name="advisingBankBic"
                    rules={[{ max: 11, message: t('validation.maxLength', { max: 11 }) }]}
                  >
                    <Input placeholder={t('forms.placeholders.enterText')} />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label={t('guarantees.beneficiaryAddress')}
                name="beneficiaryAddress"
                rules={[{ max: 350, message: t('validation.maxLength', { max: 350 }) }]}
              >
                <TextArea
                  rows={3}
                  placeholder={t('forms.placeholders.enterText')}
                />
              </Form.Item>
            </Card>

            {/* Additional Information */}
            <Card title={t('forms.additionalInformation', { defaultValue: 'Additional Information' })}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.underlyingContract')}
                    name="underlyingContractRef"
                    rules={[{ max: 35, message: t('validation.maxLength', { max: 35 }) }]}
                  >
                    <Input placeholder={t('forms.placeholders.enterText')} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label={t('guarantees.isDomestic')}
                    name="isDomestic"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label={t('forms.specialConditions', { defaultValue: 'Special Conditions' })}
                name="specialConditions"
                rules={[{ max: 2000, message: t('validation.maxLength', { max: 2000 }) }]}
              >
                <TextArea
                  rows={4}
                  placeholder={t('forms.placeholders.enterText')}
                />
              </Form.Item>
            </Card>

            {/* Guarantee Text */}
            <Card 
              title={t('guarantees.guaranteeText')} 
              style={{ marginBottom: '24px' }}
              extra={
                <Button
                  type="link"
                  icon={<FileTextOutlined />}
                  onClick={openTemplateSelector}
                >
                  {t('templates.useThisTemplate')}
                </Button>
              }
            >
              <Form.Item
                label={t('guarantees.guaranteeText')}
                name="guaranteeText"
                rules={[
                  { required: true, message: t('validation.required') },
                  { max: 10000, message: t('validation.maxLength', { max: 10000 }) }
                ]}
              >
                <TextArea
                  rows={6}
                  placeholder={t('forms.guaranteeTextPlaceholder', { 
                    defaultValue: 'Enter the guarantee text manually or click Use Template to select from predefined templates...' 
                  })}
                  showCount
                  maxLength={10000}
                />
              </Form.Item>
            </Card>
          </Col>

          <Col span={8}>
            {/* Actions */}
            <Card title={t('common.actions')}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button
                  type="default"
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                  loading={createMutation.isLoading && saveAsDraft}
                  block
                >
                  {t('forms.saveAsDraft', { defaultValue: 'Save as Draft' })}
                </Button>
                
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  onClick={handleSubmitForApproval}
                  loading={createMutation.isLoading && !saveAsDraft}
                  block
                >
                  {t('forms.createAndSubmit', { defaultValue: 'Create & Submit for Approval' })}
                </Button>

                <Divider />

                <Text type="secondary" style={{ fontSize: '12px' }}>
                  <strong>{t('forms.saveAsDraft')}:</strong> {t('forms.saveAsDraftDescription', { 
                    defaultValue: 'Create the guarantee in draft status. You can edit and submit it later.' 
                  })}
                </Text>

                <Text type="secondary" style={{ fontSize: '12px' }}>
                  <strong>{t('forms.createAndSubmit')}:</strong> {t('forms.createAndSubmitDescription', { 
                    defaultValue: 'Create the guarantee and immediately submit it for approval.' 
                  })}
                </Text>
              </Space>
            </Card>

            {/* Validation Summary */}
            <Card title={t('forms.requirements', { defaultValue: 'Requirements' })} style={{ marginTop: '16px' }}>
              <ul style={{ fontSize: '12px', margin: 0, paddingLeft: '16px' }}>
                <li>{t('validation.allRequiredFields', { defaultValue: 'All required fields must be completed' })}</li>
                <li>{t('validation.amountGreaterThanZero', { defaultValue: 'Amount must be greater than 0' })}</li>
                <li>{t('validation.expiryAfterIssue', { defaultValue: 'Expiry date must be after issue date' })}</li>
                <li>{t('validation.beneficiaryNameRequired', { defaultValue: 'Beneficiary name is required' })}</li>
                <li>{t('validation.validApplicantRequired', { defaultValue: 'Valid applicant must be selected' })}</li>
              </ul>
            </Card>
          </Col>
        </Row>
      </Form>

      {/* Template Selector Modal */}
      <TemplateSelector
        visible={templateSelectorVisible}
        onCancel={() => setTemplateSelectorVisible(false)}
        onSelect={handleTemplateSelect}
        guarantee={form.getFieldsValue()}
      />
    </div>
  );
};

export default CreateGuarantee;

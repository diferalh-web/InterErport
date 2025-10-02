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
  Spin,
} from 'antd';
import { useForm } from 'antd/es/form/Form';
import { useMutation, useQuery } from 'react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { SaveOutlined, ArrowLeftOutlined, FileTextOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

import { apiService } from '../services/api';
import {
  GuaranteeFormData,
  GuaranteeTypeLabels,
  Client,
  GuaranteeStatus,
} from '../types/guarantee';
import TemplateSelector from '../components/TemplateSelector';

const { Title, Text } = Typography;
const { TextArea } = Input;

const EditGuarantee: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = useForm();
  const [templateSelectorVisible, setTemplateSelectorVisible] = useState(false);

  // Fetch guarantee data
  const { data: guarantee, isLoading: guaranteeLoading } = useQuery(
    ['guarantee', id],
    () => apiService.getGuarantee(Number(id)),
    {
      enabled: !!id,
      onSuccess: (data) => {
        // Populate form with existing data
        form.setFieldsValue({
          reference: data.reference,
          guaranteeType: data.guaranteeType,
          amount: data.amount,
          currency: data.currency,
          applicantId: data.applicantId,
          beneficiaryName: data.beneficiaryName,
          beneficiaryAddress: data.beneficiaryAddress,
          advisingBankBic: data.advisingBankBic,
          underlyingContractRef: data.underlyingContractRef,
          isDomestic: data.isDomestic,
          language: data.language,
          guaranteeText: data.guaranteeText,
          specialConditions: data.specialConditions,
          issueDate: data.issueDate ? dayjs(data.issueDate) : null,
          expiryDate: data.expiryDate ? dayjs(data.expiryDate) : null,
        });
      },
    }
  );

  // Fetch clients for applicant selection
  const { data: clientsData, isLoading: clientsLoading } = useQuery(
    'clients',
    () => apiService.getClients(0, 100)
  );

  // Mutation for updating guarantee
  const updateMutation = useMutation(
    (data: GuaranteeFormData) => apiService.updateGuarantee(Number(id), data),
    {
      onSuccess: (updatedGuarantee) => {
        message.success('Guarantee updated successfully');
        navigate(`/guarantees/${updatedGuarantee.id}`);
      },
      onError: () => {
        message.error('Failed to update guarantee');
      },
    }
  );

  const handleSubmit = async (values: GuaranteeFormData) => {
    try {
      // Format dates
      const formattedValues = {
        ...values,
        issueDate: dayjs(values.issueDate).format('YYYY-MM-DD'),
        expiryDate: dayjs(values.expiryDate).format('YYYY-MM-DD'),
      };

      updateMutation.mutate(formattedValues);
    } catch (error) {
      message.error('Please check all required fields');
    }
  };

  const handleTemplateSelect = (template: any, renderedText: string) => {
    form.setFieldsValue({
      guaranteeText: renderedText
    });
    setTemplateSelectorVisible(false);
    message.success('Template applied successfully');
  };

  const openTemplateSelector = () => {
    const currentValues = form.getFieldsValue();
    if (!currentValues.guaranteeType) {
      message.warning('Please select a guarantee type first');
      return;
    }
    setTemplateSelectorVisible(true);
  };

  // Check if guarantee can be edited
  const canEdit = guarantee?.status === GuaranteeStatus.DRAFT;

  if (guaranteeLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!guarantee) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Title level={3}>Guarantee not found</Title>
      </div>
    );
  }

  if (!canEdit) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Title level={3}>Cannot Edit Guarantee</Title>
        <Text type="secondary">
          Only guarantees in DRAFT status can be edited. Current status: {guarantee.status}
        </Text>
        <br />
        <Button type="primary" onClick={() => navigate(`/guarantees/${id}`)}>
          Back to Guarantee Details
        </Button>
      </div>
    );
  }

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
          onClick={() => navigate(`/guarantees/${id}`)}
        >
          Back to Details
        </Button>
        <div>
          <Title level={2} style={{ margin: 0 }}>Edit Guarantee {guarantee.reference}</Title>
          <Text type="secondary">Update guarantee details</Text>
        </div>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
      >
        <Row gutter={24}>
          <Col span={16}>
            {/* Basic Information */}
            <Card title="Basic Information" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Guarantee Type"
                    name="guaranteeType"
                    rules={[{ required: true, message: 'Please select guarantee type' }]}
                  >
                    <Select placeholder="Select guarantee type">
                      {Object.entries(GuaranteeTypeLabels).map(([key, label]) => (
                        <Select.Option key={key} value={key}>
                          {label}
                        </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Reference"
                    name="reference"
                    rules={[
                      { required: true, message: 'Please enter reference' },
                      { max: 35, message: 'Reference cannot exceed 35 characters' }
                    ]}
                  >
                    <Input placeholder="Enter guarantee reference" />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Applicant"
                    name="applicantId"
                    rules={[{ required: true, message: 'Please select applicant' }]}
                  >
                    <Select
                      placeholder="Select applicant"
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
                    label="Amount"
                    name="amount"
                    rules={[
                      { required: true, message: 'Please enter amount' },
                      { type: 'number', min: 0.01, message: 'Amount must be greater than 0' },
                    ]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      placeholder="Enter amount"
                      min={0.01}
                      precision={2}
                      formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                      parser={(value) => value!.replace(/\$\s?|(,*)/g, '') as any}
                    />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="Currency"
                    name="currency"
                    rules={[{ required: true, message: 'Please select currency' }]}
                  >
                    <Select placeholder="Select currency" options={currencyOptions} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="Language"
                    name="language"
                  >
                    <Select>
                      <Select.Option value="EN">English</Select.Option>
                      <Select.Option value="ES">Spanish</Select.Option>
                      <Select.Option value="FR">French</Select.Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Issue Date"
                    name="issueDate"
                    rules={[{ required: true, message: 'Please select issue date' }]}
                  >
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Expiry Date"
                    name="expiryDate"
                    rules={[
                      { required: true, message: 'Please select expiry date' },
                      ({ getFieldValue }) => ({
                        validator(_, value) {
                          if (!value || !getFieldValue('issueDate')) {
                            return Promise.resolve();
                          }
                          if (dayjs(value).isAfter(dayjs(getFieldValue('issueDate')))) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error('Expiry date must be after issue date'));
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
            <Card title="Beneficiary Information" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Beneficiary Name"
                    name="beneficiaryName"
                    rules={[
                      { required: true, message: 'Please enter beneficiary name' },
                      { max: 140, message: 'Name cannot exceed 140 characters' },
                    ]}
                  >
                    <Input placeholder="Enter beneficiary name" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Advising Bank BIC"
                    name="advisingBankBic"
                    rules={[{ max: 11, message: 'BIC cannot exceed 11 characters' }]}
                  >
                    <Input placeholder="Enter BIC code (optional)" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="Beneficiary Address"
                name="beneficiaryAddress"
                rules={[{ max: 350, message: 'Address cannot exceed 350 characters' }]}
              >
                <TextArea
                  rows={3}
                  placeholder="Enter beneficiary address (optional)"
                />
              </Form.Item>
            </Card>

            {/* Additional Information */}
            <Card title="Additional Information" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Underlying Contract Reference"
                    name="underlyingContractRef"
                    rules={[{ max: 35, message: 'Reference cannot exceed 35 characters' }]}
                  >
                    <Input placeholder="Enter contract reference (optional)" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Domestic Guarantee"
                    name="isDomestic"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="Special Conditions"
                name="specialConditions"
                rules={[{ max: 2000, message: 'Conditions cannot exceed 2000 characters' }]}
              >
                <TextArea
                  rows={4}
                  placeholder="Enter any special conditions or notes (optional)"
                />
              </Form.Item>
            </Card>

            {/* Guarantee Text */}
            <Card 
              title="Guarantee Text"
              extra={canEdit && (
                <Button
                  type="link"
                  icon={<FileTextOutlined />}
                  onClick={openTemplateSelector}
                >
                  Use Template
                </Button>
              )}
            >
              <Form.Item
                label="Guarantee Text"
                name="guaranteeText"
                rules={[
                  { required: true, message: 'Please enter guarantee text' },
                  { max: 10000, message: 'Guarantee text cannot exceed 10000 characters' }
                ]}
              >
                <TextArea
                  rows={6}
                  placeholder={canEdit 
                    ? "Enter the guarantee text manually or click 'Use Template' to select from predefined templates..."
                    : "Guarantee text cannot be edited after submission"
                  }
                  showCount
                  maxLength={10000}
                  disabled={!canEdit}
                />
              </Form.Item>
            </Card>
          </Col>

          <Col span={8}>
            {/* Actions */}
            <Card title="Actions">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={() => form.submit()}
                  loading={updateMutation.isLoading}
                  block
                >
                  Update Guarantee
                </Button>

                <Button
                  onClick={() => navigate(`/guarantees/${id}`)}
                  block
                >
                  Cancel
                </Button>

                <Divider />

                <Text type="secondary" style={{ fontSize: '12px' }}>
                  <strong>Note:</strong> Only guarantees in DRAFT status can be edited. 
                  After submission for approval, amendments must be used to modify the guarantee.
                </Text>
              </Space>
            </Card>
          </Col>
        </Row>
      </Form>

      {/* Template Selector Modal */}
      {canEdit && (
        <TemplateSelector
          visible={templateSelectorVisible}
          onCancel={() => setTemplateSelectorVisible(false)}
          onSelect={handleTemplateSelect}
          guarantee={{
            ...form.getFieldsValue(),
            id: guarantee?.id
          }}
        />
      )}
    </div>
  );
};

export default EditGuarantee;

/**
 * Amendment Management Component
 * Implements F5 - Amendments functionality with forms and management UI
 */

import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  InputNumber,
  Space,
  Typography,
  Tag,
  Drawer,
  Row,
  Col,
  Divider,
  Alert,
  notification,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  EyeOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { Amendment, AmendmentFormData, GuaranteeContract, AmendmentType } from '../types/guarantee';
import { apiService } from '../services/api';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

interface AmendmentManagementProps {
  guaranteeId?: number;
  guarantee?: GuaranteeContract;
}

const AmendmentManagement: React.FC<AmendmentManagementProps> = ({ 
  guaranteeId: propGuaranteeId,
  guarantee: propGuarantee 
}) => {
  const { id: paramGuaranteeId } = useParams();
  const guaranteeId = propGuaranteeId || (paramGuaranteeId ? parseInt(paramGuaranteeId) : null);
  const { t } = useAppTranslation();
  
  const [amendments, setAmendments] = useState<Amendment[]>([]);
  const [guarantee, setGuarantee] = useState<GuaranteeContract | null>(propGuarantee || null);
  const [loading, setLoading] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isDetailDrawerVisible, setIsDetailDrawerVisible] = useState(false);
  const [selectedAmendment, setSelectedAmendment] = useState<Amendment | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    if (guaranteeId) {
      loadAmendments();
      if (!guarantee) {
        loadGuarantee();
      }
    }
  }, [guaranteeId]);

  const loadGuarantee = async () => {
    if (!guaranteeId) return;
    try {
      const data = await apiService.getGuarantee(guaranteeId);
      setGuarantee(data);
    } catch (error) {
      notification.error({
        message: 'Error Loading Guarantee',
        description: 'Failed to load guarantee details'
      });
    }
  };

  const loadAmendments = async () => {
    if (!guaranteeId) return;
    setLoading(true);
    try {
      const data = await apiService.getAmendments(guaranteeId);
      setAmendments(data);
    } catch (error) {
      notification.error({
        message: 'Error Loading Amendments',
        description: 'Failed to load amendments'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAmendment = () => {
    form.resetFields();
    setSelectedAmendment(null);
    setIsModalVisible(true);
  };

  const handleEditAmendment = (amendment: Amendment) => {
    setSelectedAmendment(amendment);
    form.setFieldsValue({
      amendmentType: amendment.amendmentType,
      description: amendment.description,
      reason: amendment.reason,
      requiresConsent: amendment.requiresConsent,
      // Set amendment-specific fields based on type
      ...getAmendmentTypeFields(amendment)
    });
    setIsModalVisible(true);
  };

  const getAmendmentTypeFields = (amendment: Amendment) => {
    const changes = amendment.changesJson ? JSON.parse(amendment.changesJson) : {};
    return {
      newAmount: changes.newAmount,
      newExpiryDate: changes.newExpiryDate ? dayjs(changes.newExpiryDate) : null,
      newBeneficiaryName: changes.newBeneficiaryName,
      newCurrency: changes.newCurrency
    };
  };

  const handleSubmitAmendment = async (values: any) => {
    if (!guaranteeId) return;
    
    try {
      const changesJson = buildChangesJson(values);
      const amendmentData: AmendmentFormData = {
        amendmentType: values.amendmentType,
        description: values.description,
        reason: values.reason,
        requiresConsent: values.requiresConsent || false,
        changesJson
      };

      if (selectedAmendment) {
        // Update amendment (if editing)
        notification.info({
          message: 'Amendment Update',
          description: 'Amendment update functionality will be available in the next version'
        });
      } else {
        // Create new amendment
        await apiService.createAmendment(guaranteeId, amendmentData);
        notification.success({
          message: 'Amendment Created',
          description: 'Amendment has been successfully created'
        });
        loadAmendments();
      }
      
      setIsModalVisible(false);
      form.resetFields();
    } catch (error: any) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : error?.response?.data?.message || error?.response?.data?.error || 'Unknown error occurred';
      notification.error({
        message: 'Error',
        description: selectedAmendment 
          ? `Failed to update amendment: ${errorMessage}` 
          : `Failed to create amendment: ${errorMessage}`
      });
    }
  };

  const buildChangesJson = (values: any) => {
    const changes: any = {};
    
    if (values.newAmount) changes.newAmount = values.newAmount;
    if (values.newExpiryDate) changes.newExpiryDate = values.newExpiryDate.format('YYYY-MM-DD');
    if (values.newBeneficiaryName) changes.newBeneficiaryName = values.newBeneficiaryName;
    if (values.newCurrency) changes.newCurrency = values.newCurrency;
    
    return JSON.stringify(changes);
  };

  const handleViewAmendment = (amendment: Amendment) => {
    setSelectedAmendment(amendment);
    setIsDetailDrawerVisible(true);
  };

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      PENDING: 'orange',
      APPROVED: 'green',
      REJECTED: 'red',
      PROCESSING: 'blue'
    };
    return colors[status] || 'default';
  };

  const getAmendmentTypeColor = (type: string) => {
    const colors: { [key: string]: string } = {
      AMOUNT_INCREASE: 'green',
      AMOUNT_DECREASE: 'orange',
      EXTEND_VALIDITY: 'blue',
      REDUCE_VALIDITY: 'orange',
      CHANGE_BENEFICIARY: 'purple',
      CHANGE_CURRENCY: 'cyan',
      OTHER: 'default'
    };
    return colors[type] || 'default';
  };

  const columns = [
    {
      title: 'Amendment Ref',
      dataIndex: 'amendmentReference',
      key: 'amendmentReference',
      render: (ref: string) => <Text strong>{ref}</Text>
    },
    {
      title: 'Type',
      dataIndex: 'amendmentType',
      key: 'amendmentType',
      render: (type: string) => (
        <Tag color={getAmendmentTypeColor(type)}>
          {type.replace('_', ' ')}
        </Tag>
      )
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      )
    },
    {
      title: 'Requires Consent',
      dataIndex: 'requiresConsent',
      key: 'requiresConsent',
      render: (requires: boolean) => requires ? 
        <CheckOutlined style={{ color: 'orange' }} /> : 
        <CloseOutlined style={{ color: 'green' }} />
    },
    {
      title: 'Submitted Date',
      dataIndex: 'submittedDate',
      key: 'submittedDate',
      render: (date: string) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Amendment) => (
        <Space>
          <Button 
            icon={<EyeOutlined />} 
            size="small" 
            onClick={() => handleViewAmendment(record)}
          >
            View
          </Button>
          <Button 
            icon={<EditOutlined />} 
            size="small" 
            onClick={() => handleEditAmendment(record)}
          >
            Edit
          </Button>
        </Space>
      )
    }
  ];

  if (!guaranteeId) {
    return (
      <Alert 
        message="Invalid Guarantee" 
        description="No guarantee ID provided" 
        type="error" 
      />
    );
  }

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div>
                <Title level={4} style={{ margin: 0 }}>
                  Amendments for Guarantee {guarantee?.reference}
                </Title>
                <Text type="secondary">
                  Manage amendments for this guarantee contract
                </Text>
              </div>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={handleCreateAmendment}
              >
                Create Amendment
              </Button>
            </div>

            {guarantee && (
              <Alert
                message={`Guarantee Details: ${guarantee.guaranteeType} - ${guarantee.amount} ${guarantee.currency}`}
                description={`Beneficiary: ${guarantee.beneficiaryName} | Status: ${guarantee.status}`}
                type="info"
                style={{ marginBottom: 16 }}
              />
            )}

            <Table 
              columns={columns}
              dataSource={amendments}
              rowKey="id"
              loading={loading}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} amendments`
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* Create/Edit Amendment Modal */}
      <Modal
        title={selectedAmendment ? 'Edit Amendment' : 'Create Amendment'}
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={700}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmitAmendment}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="amendmentType"
                label="Amendment Type"
                rules={[{ required: true, message: 'Please select amendment type' }]}
              >
                <Select placeholder="Select type">
                  <Option value={AmendmentType.AMOUNT_INCREASE}>Amount Increase</Option>
                  <Option value={AmendmentType.AMOUNT_DECREASE}>Amount Decrease</Option>
                  <Option value={AmendmentType.EXTEND_VALIDITY}>Extend Validity</Option>
                  <Option value={AmendmentType.REDUCE_VALIDITY}>Reduce Validity</Option>
                  <Option value={AmendmentType.CHANGE_BENEFICIARY}>Change Beneficiary</Option>
                  <Option value={AmendmentType.CHANGE_CURRENCY}>Change Currency</Option>
                  <Option value={AmendmentType.OTHER}>Other</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="requiresConsent"
                label="Requires Consent"
              >
                <Select placeholder="Select">
                  <Option value={true}>Yes (GITRAM)</Option>
                  <Option value={false}>No (GITAME)</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="Description"
            rules={[{ required: true, message: 'Please enter description' }]}
          >
            <TextArea rows={3} placeholder="Enter amendment description" />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Reason"
            rules={[{ required: true, message: 'Please enter reason' }]}
          >
            <TextArea rows={2} placeholder="Enter reason for amendment" />
          </Form.Item>

          <Divider>Amendment Details</Divider>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="newAmount"
                label="New Amount"
              >
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="Enter new amount"
                  min={0.01}
                  precision={2}
                  formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={(value) => value!.replace(/\$\s?|(,*)/g, '') as any}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="newCurrency"
                label="New Currency"
              >
                <Select placeholder="Select currency">
                  <Option value="USD">USD</Option>
                  <Option value="EUR">EUR</Option>
                  <Option value="GBP">GBP</Option>
                  <Option value="JPY">JPY</Option>
                  <Option value="CAD">CAD</Option>
                  <Option value="AUD">AUD</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="newExpiryDate"
                label="New Expiry Date"
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="newBeneficiaryName"
                label="New Beneficiary"
              >
                <Input placeholder="Enter new beneficiary name" />
              </Form.Item>
            </Col>
          </Row>

          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsModalVisible(false)}>
                Cancel
              </Button>
            </Col>
            <Col>
              <Button type="primary" htmlType="submit">
                {selectedAmendment ? 'Update' : 'Create'} Amendment
              </Button>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Amendment Detail Drawer */}
      <Drawer
        title="Amendment Details"
        placement="right"
        width={600}
        open={isDetailDrawerVisible}
        onClose={() => setIsDetailDrawerVisible(false)}
      >
        {selectedAmendment && (
          <div>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Text strong>Amendment Reference:</Text>
                <br />
                <Text>{selectedAmendment.amendmentReference}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Type:</Text>
                <br />
                <Tag color={getAmendmentTypeColor(selectedAmendment.amendmentType)}>
                  {selectedAmendment.amendmentType.replace('_', ' ')}
                </Tag>
              </Col>
              
              <Col span={12}>
                <Text strong>Status:</Text>
                <br />
                <Tag color={getStatusColor(selectedAmendment.status)}>
                  {selectedAmendment.status}
                </Tag>
              </Col>
              <Col span={12}>
                <Text strong>Requires Consent:</Text>
                <br />
                <Text>{selectedAmendment.requiresConsent ? 'Yes (GITRAM)' : 'No (GITAME)'}</Text>
              </Col>

              <Col span={24}>
                <Text strong>Description:</Text>
                <br />
                <Text>{selectedAmendment.description}</Text>
              </Col>

              <Col span={24}>
                <Text strong>Reason:</Text>
                <br />
                <Text>{selectedAmendment.reason}</Text>
              </Col>

              {selectedAmendment.changesJson && (
                <Col span={24}>
                  <Divider>Proposed Changes</Divider>
                  <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                    {JSON.stringify(JSON.parse(selectedAmendment.changesJson), null, 2)}
                  </pre>
                </Col>
              )}

              <Col span={12}>
                <Text strong>Submitted Date:</Text>
                <br />
                <Text>
                  {selectedAmendment.submittedDate ? 
                    dayjs(selectedAmendment.submittedDate).format('YYYY-MM-DD HH:mm') : 
                    'Not submitted'
                  }
                </Text>
              </Col>

              {selectedAmendment.processedDate && (
                <Col span={12}>
                  <Text strong>Processed Date:</Text>
                  <br />
                  <Text>{dayjs(selectedAmendment.processedDate).format('YYYY-MM-DD HH:mm')}</Text>
                </Col>
              )}

              {selectedAmendment.processedBy && (
                <Col span={12}>
                  <Text strong>Processed By:</Text>
                  <br />
                  <Text>{selectedAmendment.processedBy}</Text>
                </Col>
              )}

              {selectedAmendment.processingComments && (
                <Col span={24}>
                  <Text strong>Processing Comments:</Text>
                  <br />
                  <Text>{selectedAmendment.processingComments}</Text>
                </Col>
              )}
            </Row>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default AmendmentManagement;

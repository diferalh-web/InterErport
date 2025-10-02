/**
 * Claim Management Component
 * Implements F6 - Claims functionality with forms and management UI
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
  Progress,
  Steps
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  EyeOutlined,
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { Claim, ClaimFormData, GuaranteeContract, ClaimStatus } from '../types/guarantee';
import { apiService } from '../services/api';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;
const { Step } = Steps;
const { confirm } = Modal;

interface ClaimManagementProps {
  guaranteeId?: number;
  guarantee?: GuaranteeContract;
}

const ClaimManagement: React.FC<ClaimManagementProps> = ({ 
  guaranteeId: propGuaranteeId,
  guarantee: propGuarantee 
}) => {
  const { id: paramGuaranteeId } = useParams();
  const guaranteeId = propGuaranteeId || (paramGuaranteeId ? parseInt(paramGuaranteeId) : null);
  const { t, formatCurrency, formatDate } = useAppTranslation();
  
  const [claims, setClaims] = useState<Claim[]>([]);
  const [guarantee, setGuarantee] = useState<GuaranteeContract | null>(propGuarantee || null);
  const [loading, setLoading] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isDetailDrawerVisible, setIsDetailDrawerVisible] = useState(false);
  const [selectedClaim, setSelectedClaim] = useState<Claim | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    if (guaranteeId) {
      loadClaims();
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
        message: t('messages.loadError'),
        description: t('messages.loadError')
      });
    }
  };

  const loadClaims = async () => {
    if (!guaranteeId) return;
    setLoading(true);
    try {
      const data = await apiService.getClaims(guaranteeId);
      setClaims(data);
    } catch (error) {
      notification.error({
        message: t('messages.loadError'),
        description: t('messages.loadError')
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateClaim = () => {
    form.resetFields();
    setSelectedClaim(null);
    setIsModalVisible(true);
  };

  const handleEditClaim = (claim: Claim) => {
    setSelectedClaim(claim);
    form.setFieldsValue({
      claimReference: claim.claimReference,
      amount: claim.amount,
      currency: claim.currency,
      claimReason: claim.claimReason,
      beneficiaryContact: claim.beneficiaryContact,
      processingDeadline: claim.processingDeadline ? dayjs(claim.processingDeadline) : null,
      processingNotes: claim.processingNotes,
      requiresSpecialApproval: claim.requiresSpecialApproval,
      documentsSubmitted: claim.documentsSubmitted
    });
    setIsModalVisible(true);
  };

  const handleSubmitClaim = async (values: any) => {
    if (!guaranteeId) return;
    
    try {
      const claimData: ClaimFormData = {
        claimReference: values.claimReference,
        amount: values.amount,
        currency: values.currency,
        claimDate: values.claimDate ? values.claimDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'), // Default to today if not provided
        claimReason: values.claimReason,
        beneficiaryContact: values.beneficiaryContact,
        processingDeadline: values.processingDeadline?.format('YYYY-MM-DD'),
        processingNotes: values.processingNotes,
        requiresSpecialApproval: values.requiresSpecialApproval || false,
        documentsSubmitted: values.documentsSubmitted
      };

      if (selectedClaim) {
        // Update claim (if editing)
        notification.info({
          message: 'Claim Update',
          description: 'Claim update functionality will be available in the next version'
        });
      } else {
        // Create new claim
        await apiService.createClaim(guaranteeId, claimData);
        notification.success({
          message: 'Claim Created',
          description: 'Claim has been successfully created'
        });
        loadClaims();
      }
      
      setIsModalVisible(false);
      form.resetFields();
    } catch (error: any) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : error?.response?.data?.message || error?.response?.data?.error || 'Unknown error occurred';
      notification.error({
        message: 'Error',
        description: selectedClaim 
          ? `Failed to update claim: ${errorMessage}` 
          : `Failed to create claim: ${errorMessage}`
      });
    }
  };

  const handleViewClaim = (claim: Claim) => {
    setSelectedClaim(claim);
    setIsDetailDrawerVisible(true);
  };

  const handleApproveClaim = (claim: Claim) => {
    confirm({
      title: 'Approve Claim',
      icon: <ExclamationCircleOutlined />,
      content: `Are you sure you want to approve claim ${claim.claimReference}?`,
      okText: 'Approve',
      okType: 'primary',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          await apiService.approveClaim(guaranteeId!, claim.id!, 'Admin User');
          notification.success({
            message: 'Claim Approved',
            description: `Claim ${claim.claimReference} has been approved`
          });
          loadClaims();
        } catch (error) {
          notification.error({
            message: 'Approval Failed',
            description: 'Failed to approve claim'
          });
        }
      }
    });
  };

  const handleRejectClaim = (claim: Claim) => {
    Modal.confirm({
      title: 'Reject Claim',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>Are you sure you want to reject claim {claim.claimReference}?</p>
          <Input.TextArea
            rows={3}
            placeholder="Enter rejection reason..."
            onChange={(e) => {
              // Store rejection reason in modal instance
              (Modal as any)._rejectionReason = e.target.value;
            }}
          />
        </div>
      ),
      okText: 'Reject',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        const reason = (Modal as any)._rejectionReason || 'No reason provided';
        try {
          await apiService.rejectClaim(guaranteeId!, claim.id!, 'Admin User', reason);
          notification.success({
            message: 'Claim Rejected',
            description: `Claim ${claim.claimReference} has been rejected`
          });
          loadClaims();
        } catch (error) {
          notification.error({
            message: 'Rejection Failed',
            description: 'Failed to reject claim'
          });
        }
      }
    });
  };

  const handleSettleClaim = (claim: Claim) => {
    Modal.confirm({
      title: 'Settle Claim',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>Are you sure you want to settle claim {claim.claimReference}?</p>
          <Input
            placeholder="Enter payment reference..."
            onChange={(e) => {
              // Store payment reference in modal instance
              (Modal as any)._paymentReference = e.target.value;
            }}
          />
        </div>
      ),
      okText: 'Settle',
      okType: 'primary',
      cancelText: 'Cancel',
      onOk: async () => {
        const paymentRef = (Modal as any)._paymentReference || `PAY-${Date.now()}`;
        try {
          await apiService.settleClaim(guaranteeId!, claim.id!, paymentRef);
          notification.success({
            message: 'Claim Settled',
            description: `Claim ${claim.claimReference} has been settled`
          });
          loadClaims();
        } catch (error) {
          notification.error({
            message: 'Settlement Failed',
            description: 'Failed to settle claim'
          });
        }
      }
    });
  };

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      SUBMITTED: 'blue',
      UNDER_REVIEW: 'orange',
      APPROVED: 'green',
      REJECTED: 'red',
      SETTLED: 'purple',
      PENDING_DOCUMENTS: 'gold'
    };
    return colors[status] || 'default';
  };

  const getClaimProgress = (claim: Claim) => {
    const statusProgress: { [key: string]: number } = {
      SUBMITTED: 20,
      UNDER_REVIEW: 40,
      PENDING_DOCUMENTS: 60,
      APPROVED: 80,
      SETTLED: 100,
      REJECTED: 0
    };
    return statusProgress[claim.status] || 0;
  };

  const columns = [
    {
      title: t('claims.claimReference'),
      dataIndex: 'claimReference',
      key: 'claimReference',
      render: (ref: string) => <Text strong>{ref}</Text>
    },
    {
      title: t('table.columns.amount'),
      key: 'amount',
      render: (_: any, record: Claim) => (
        <Text>{formatCurrency(record.amount || 0, record.currency)}</Text>
      )
    },
    {
      title: t('common.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string, record: Claim) => (
        <div>
          <Tag color={getStatusColor(status)}>{t(`claims.statuses.${status}`)}</Tag>
          <Progress 
            percent={getClaimProgress(record)} 
            size="small" 
            status={status === 'REJECTED' ? 'exception' : 'normal'}
            style={{ marginTop: 4, width: 80 }}
          />
        </div>
      )
    },
    {
      title: t('claims.claimReason'),
      dataIndex: 'claimReason',
      key: 'claimReason',
      ellipsis: true
    },
    {
      title: t('claims.processingDeadline'),
      dataIndex: 'processingDeadline',
      key: 'processingDeadline',
      render: (date: string) => date ? formatDate(date) : '-'
    },
    {
      title: t('claims.requiresSpecialApproval'),
      dataIndex: 'requiresSpecialApproval',
      key: 'requiresSpecialApproval',
      render: (requires: boolean) => requires ? 
        <CheckOutlined style={{ color: 'orange' }} /> : 
        <CloseOutlined style={{ color: 'green' }} />
    },
    {
      title: t('table.columns.actions'),
      key: 'actions',
      render: (_: any, record: Claim) => (
        <Space>
          <Button 
            icon={<EyeOutlined />} 
            size="small" 
            onClick={() => handleViewClaim(record)}
          >
            {t('buttons.view')}
          </Button>
          <Button 
            icon={<EditOutlined />} 
            size="small" 
            onClick={() => handleEditClaim(record)}
          >
            {t('buttons.edit')}
          </Button>
          {record.status === ClaimStatus.SUBMITTED && (
            <>
              <Button 
                type="primary"
                size="small" 
                onClick={() => handleApproveClaim(record)}
              >
                {t('buttons.approve')}
              </Button>
              <Button 
                danger
                size="small" 
                onClick={() => handleRejectClaim(record)}
              >
                {t('buttons.reject')}
              </Button>
            </>
          )}
          {record.status === 'APPROVED' && (
            <Button 
              type="primary"
              size="small" 
              onClick={() => handleSettleClaim(record)}
            >
              Settle
            </Button>
          )}
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
                  {t('claims.title')} {guarantee?.reference}
                </Title>
                <Text type="secondary">
                  {t('claims.claimDetails')}
                </Text>
              </div>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={handleCreateClaim}
              >
                {t('claims.createClaim')}
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
              dataSource={claims}
              rowKey="id"
              loading={loading}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total, range) => t('table.showingItems', { 
                start: range[0], 
                end: range[1], 
                total: total 
              })
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* Create/Edit Claim Modal */}
      <Modal
        title={selectedClaim ? 'Edit Claim' : 'Create Claim'}
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={800}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmitClaim}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="claimReference"
                label="Claim Reference"
                rules={[{ required: true, message: 'Please enter claim reference' }]}
              >
                <Input placeholder="Enter claim reference" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="amount"
                label="Claim Amount"
                rules={[{ required: true, message: 'Please enter amount' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="0.00"
                  min={0.01}
                  precision={2}
                  formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={(value) => value!.replace(/\$\s?|(,*)/g, '') as any}
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="currency"
                label="Currency"
                rules={[{ required: true, message: 'Please select currency' }]}
              >
                <Select placeholder="Select">
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
                name="claimDate"
                label="Claim Date"
                rules={[{ required: true, message: 'Please select claim date' }]}
                initialValue={dayjs()}
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="claimReason"
            label="Claim Reason"
            rules={[{ required: true, message: 'Please enter claim reason' }]}
          >
            <TextArea rows={3} placeholder="Enter detailed reason for the claim" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="beneficiaryContact"
                label="Beneficiary Contact"
                rules={[{ required: true, message: 'Please enter beneficiary contact' }]}
              >
                <Input placeholder="Enter beneficiary contact details" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="processingDeadline"
                label="Processing Deadline"
              >
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="requiresSpecialApproval"
                label="Requires Special Approval"
              >
                <Select placeholder="Select">
                  <Option value={true}>Yes</Option>
                  <Option value={false}>No</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="documentsSubmitted"
                label="Documents Submitted"
              >
                <Select placeholder="Select">
                  <Option value={true}>Yes</Option>
                  <Option value={false}>No</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="processingNotes"
            label="Processing Notes"
          >
            <TextArea rows={2} placeholder="Enter any processing notes" />
          </Form.Item>

          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsModalVisible(false)}>
                Cancel
              </Button>
            </Col>
            <Col>
              <Button type="primary" htmlType="submit">
                {selectedClaim ? 'Update' : 'Create'} Claim
              </Button>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Claim Detail Drawer */}
      <Drawer
        title="Claim Details"
        placement="right"
        width={700}
        open={isDetailDrawerVisible}
        onClose={() => setIsDetailDrawerVisible(false)}
      >
        {selectedClaim && (
          <div>
            {/* Claim Processing Steps */}
            <Card title="Processing Status" style={{ marginBottom: 16 }}>
              <Steps
                current={Math.floor(getClaimProgress(selectedClaim) / 25)}
                status={selectedClaim.status === 'REJECTED' ? 'error' : 'process'}
              >
                <Step title="Submitted" description="Claim submitted" />
                <Step title="Review" description="Under review" />
                <Step title="Approved" description="Claim approved" />
                <Step title="Settled" description="Payment made" />
              </Steps>
            </Card>

            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Text strong>Claim Reference:</Text>
                <br />
                <Text>{selectedClaim.claimReference}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Amount:</Text>
                <br />
                <Text>{selectedClaim.amount?.toLocaleString()} {selectedClaim.currency}</Text>
              </Col>
              
              <Col span={12}>
                <Text strong>Status:</Text>
                <br />
                <Tag color={getStatusColor(selectedClaim.status)}>
                  {selectedClaim.status.replace('_', ' ')}
                </Tag>
              </Col>
              <Col span={12}>
                <Text strong>Processing Deadline:</Text>
                <br />
                <Text>
                  {selectedClaim.processingDeadline ? 
                    dayjs(selectedClaim.processingDeadline).format('YYYY-MM-DD') : 
                    'No deadline set'
                  }
                </Text>
              </Col>

              <Col span={24}>
                <Text strong>Claim Reason:</Text>
                <br />
                <Text>{selectedClaim.claimReason}</Text>
              </Col>

              <Col span={12}>
                <Text strong>Beneficiary Contact:</Text>
                <br />
                <Text>{selectedClaim.beneficiaryContact}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Special Approval:</Text>
                <br />
                <Text>{selectedClaim.requiresSpecialApproval ? 'Required' : 'Not Required'}</Text>
              </Col>

              <Col span={12}>
                <Text strong>Documents Submitted:</Text>
                <br />
                <Text>{selectedClaim.documentsSubmitted ? 'Yes' : 'No'}</Text>
              </Col>

              {selectedClaim.claimDate && (
                <Col span={12}>
                  <Text strong>Claim Date:</Text>
                  <br />
                  <Text>{dayjs(selectedClaim.claimDate).format('YYYY-MM-DD HH:mm')}</Text>
                </Col>
              )}

              {selectedClaim.processingNotes && (
                <Col span={24}>
                  <Text strong>Processing Notes:</Text>
                  <br />
                  <Text>{selectedClaim.processingNotes}</Text>
                </Col>
              )}

              {selectedClaim.approvedDate && (
                <Col span={12}>
                  <Text strong>Approved Date:</Text>
                  <br />
                  <Text>{dayjs(selectedClaim.approvedDate).format('YYYY-MM-DD HH:mm')}</Text>
                </Col>
              )}

              {selectedClaim.approvedBy && (
                <Col span={12}>
                  <Text strong>Approved By:</Text>
                  <br />
                  <Text>{selectedClaim.approvedBy}</Text>
                </Col>
              )}

              {selectedClaim.paymentDate && (
                <Col span={12}>
                  <Text strong>Payment Date:</Text>
                  <br />
                  <Text>{dayjs(selectedClaim.paymentDate).format('YYYY-MM-DD HH:mm')}</Text>
                </Col>
              )}

              {selectedClaim.paymentReference && (
                <Col span={12}>
                  <Text strong>Payment Reference:</Text>
                  <br />
                  <Text>{selectedClaim.paymentReference}</Text>
                </Col>
              )}

              {(selectedClaim.rejectedDate || selectedClaim.rejectionReason) && (
                <>
                  <Divider>Rejection Details</Divider>
                  {selectedClaim.rejectedDate && (
                    <Col span={12}>
                      <Text strong>Rejected Date:</Text>
                      <br />
                      <Text>{dayjs(selectedClaim.rejectedDate).format('YYYY-MM-DD HH:mm')}</Text>
                    </Col>
                  )}
                  {selectedClaim.rejectedBy && (
                    <Col span={12}>
                      <Text strong>Rejected By:</Text>
                      <br />
                      <Text>{selectedClaim.rejectedBy}</Text>
                    </Col>
                  )}
                  {selectedClaim.rejectionReason && (
                    <Col span={24}>
                      <Text strong>Rejection Reason:</Text>
                      <br />
                      <Text>{selectedClaim.rejectionReason}</Text>
                    </Col>
                  )}
                </>
              )}
            </Row>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default ClaimManagement;

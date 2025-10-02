import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Typography, Spin, Result, Button, Space, Row, Col, Descriptions, Tag } from 'antd';
import { EditOutlined, FileTextOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useQuery } from 'react-query';
import dayjs from 'dayjs';

import { apiService } from '../services/api';

const { Title, Text } = Typography;

const GuaranteeDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: guarantee, isLoading, error } = useQuery(
    ['guarantee', id],
    () => apiService.getGuaranteeDetails(Number(id)),
    {
      enabled: !!id,
    }
  );

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <Result
        status="404"
        title="404"
        subTitle="Guarantee not found"
      />
    );
  }

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      DRAFT: 'orange',
      SUBMITTED: 'blue',
      APPROVED: 'green',
      REJECTED: 'red',
      CANCELLED: 'default',
      EXPIRED: 'volcano'
    };
    return colors[status] || 'default';
  };

  const handleNavigateToAmendments = () => {
    navigate(`/guarantees/${id}/amendments`);
  };

  const handleNavigateToClaims = () => {
    navigate(`/guarantees/${id}/claims`);
  };

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
              <div>
                <Title level={2} style={{ margin: 0 }}>
                  {guarantee?.reference}
                </Title>
                <Text type="secondary">Guarantee Contract Details</Text>
              </div>
              <Space>
                <Button 
                  icon={<FileTextOutlined />}
                  onClick={handleNavigateToAmendments}
                >
                  Amendments
                </Button>
                <Button 
                  icon={<ExclamationCircleOutlined />}
                  onClick={handleNavigateToClaims}
                >
                  Claims
                </Button>
                <Button 
                  type="primary" 
                  icon={<EditOutlined />}
                  onClick={() => navigate(`/guarantees/${id}/edit`)}
                  disabled={guarantee?.status !== 'DRAFT'}
                >
                  Edit Guarantee
                </Button>
              </Space>
            </div>

            <Descriptions bordered column={2}>
              <Descriptions.Item label="Reference">
                <Text strong>{guarantee?.reference}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={getStatusColor(guarantee?.status || '')}>
                  {guarantee?.status}
                </Tag>
              </Descriptions.Item>
              
              <Descriptions.Item label="Type">
                <Tag color="blue">{guarantee?.guaranteeType?.replace('_', ' ')}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Amount">
                <Text strong>
                  {guarantee?.amount?.toLocaleString()} {guarantee?.currency}
                </Text>
              </Descriptions.Item>

              <Descriptions.Item label="Issue Date">
                {guarantee?.issueDate ? dayjs(guarantee.issueDate).format('YYYY-MM-DD') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Expiry Date">
                {guarantee?.expiryDate ? dayjs(guarantee.expiryDate).format('YYYY-MM-DD') : '-'}
              </Descriptions.Item>

              <Descriptions.Item label="Applicant ID">
                {guarantee?.applicantId}
              </Descriptions.Item>
              <Descriptions.Item label="Beneficiary">
                {guarantee?.beneficiaryName}
              </Descriptions.Item>

              <Descriptions.Item label="Domestic">
                {guarantee?.isDomestic ? 'Yes' : 'No'}
              </Descriptions.Item>
              <Descriptions.Item label="Language">
                {guarantee?.language}
              </Descriptions.Item>

              {guarantee?.baseAmount && guarantee?.baseAmount !== guarantee?.amount && (
                <>
                  <Descriptions.Item label="Base Amount">
                    {guarantee.baseAmount.toLocaleString()} USD
                  </Descriptions.Item>
                  <Descriptions.Item label="Exchange Rate">
                    {guarantee.exchangeRate}
                  </Descriptions.Item>
                </>
              )}

              {guarantee?.purpose && (
                <Descriptions.Item label="Purpose" span={2}>
                  {guarantee.purpose}
                </Descriptions.Item>
              )}

              {guarantee?.undertakingText && (
                <Descriptions.Item label="Undertaking Text" span={2}>
                  <Text style={{ whiteSpace: 'pre-wrap' }}>
                    {guarantee.undertakingText}
                  </Text>
                </Descriptions.Item>
              )}

              {guarantee?.conditions && (
                <Descriptions.Item label="Conditions" span={2}>
                  {guarantee.conditions}
                </Descriptions.Item>
              )}

              {guarantee?.createdDate && (
                <Descriptions.Item label="Created">
                  {dayjs(guarantee.createdDate).format('YYYY-MM-DD HH:mm')}
                </Descriptions.Item>
              )}

              {guarantee?.lastModifiedDate && (
                <Descriptions.Item label="Last Updated">
                  {dayjs(guarantee.lastModifiedDate).format('YYYY-MM-DD HH:mm')}
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default GuaranteeDetail;

import React, { useState } from 'react';
import { Card, Typography, Table, Button, Tag, message, Modal, Form, Input, Select, InputNumber, Switch, Row, Col } from 'antd';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { PlusOutlined } from '@ant-design/icons';

import { apiService } from '../services/api';
import { Client } from '../types/guarantee';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { Option } = Select;

const ClientManagement: React.FC = () => {
  const { t, formatCurrency } = useAppTranslation();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: clientsData, isLoading } = useQuery(
    'clients-management',
    () => apiService.getClients(0, 100),
    {
      onError: () => {
        message.error(t('messages.loadError'));
      },
    }
  );

  const createClientMutation = useMutation(
    (clientData: Partial<Client>) => apiService.createClient(clientData),
    {
      onSuccess: () => {
        message.success(t('messages.createSuccess'));
        queryClient.invalidateQueries('clients-management');
        setIsModalVisible(false);
        form.resetFields();
      },
      onError: (error: any) => {
        const errorMessage = error instanceof Error 
          ? error.message 
          : error?.response?.data?.message || t('messages.serverError');
        message.error(t('messages.createError') + ': ' + errorMessage);
      },
    }
  );

  const handleSubmit = (values: any) => {
    const clientData = {
      ...values,
      isActive: values.isActive ?? true,
    };
    createClientMutation.mutate(clientData);
  };

  const countryOptions = [
    'US', 'CA', 'GB', 'DE', 'FR', 'IT', 'ES', 'NL', 'BE', 'CH',
    'AU', 'JP', 'CN', 'IN', 'BR', 'MX', 'ZA', 'EG', 'NG', 'KE'
  ];

  const riskRatingOptions = ['LOW', 'MEDIUM', 'HIGH'];
  const currencyOptions = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD', 'CNY', 'INR', 'BRL'];

  const columns = [
    {
      title: t('clients.clientCode'),
      dataIndex: 'clientCode',
      key: 'clientCode',
    },
    {
      title: t('table.columns.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('clients.countryCode'),
      dataIndex: 'countryCode',
      key: 'countryCode',
    },
    {
      title: t('clients.riskRating'),
      dataIndex: 'riskRating',
      key: 'riskRating',
      render: (rating: string) => {
        if (!rating) return '-';
        const colors = {
          'LOW': 'green',
          'MEDIUM': 'orange',
          'HIGH': 'red',
        };
        return <Tag color={colors[rating as keyof typeof colors] || 'default'}>{rating}</Tag>;
      },
    },
    {
      title: t('common.status'),
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive: boolean) => (
        <Tag color={isActive ? 'green' : 'red'}>
          {isActive ? t('common.active') : t('common.inactive')}
        </Tag>
      ),
    },
    {
      title: t('clients.creditLimit'),
      dataIndex: 'creditLimit',
      key: 'creditLimit',
      render: (limit: number, record: Client) => {
        if (!limit) return '-';
        return formatCurrency(limit, record.creditCurrency);
      },
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2}>{t('clients.title')}</Title>
          <Text type="secondary">{t('clients.clientDetails')}</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalVisible(true)}>
          {t('clients.createClient')}
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={clientsData?.content || []}
          loading={isLoading}
          rowKey="id"
          expandable={{
            expandedRowRender: (record: Client) => (
              <div style={{ margin: 0 }}>
                <p><strong>{t('clients.address')}:</strong> {record.address || t('common.noData')}</p>
                <p><strong>{t('table.columns.email')}:</strong> {record.email || t('common.noData')}</p>
                <p><strong>{t('table.columns.phone')}:</strong> {record.phone || t('common.noData')}</p>
                <p><strong>{t('clients.entityType')}:</strong> {record.entityType || t('common.noData')}</p>
                <p><strong>{t('clients.taxId')}:</strong> {record.taxId || t('common.noData')}</p>
                {record.notes && <p><strong>{t('common.notes')}:</strong> {record.notes}</p>}
              </div>
            ),
            rowExpandable: () => true,
          }}
        />
      </Card>

      <Modal
        title={t('clients.createClient')}
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={800}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            isActive: true,
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="clientCode"
                label={t('clients.clientCode')}
                rules={[{ required: true, message: t('validation.required') }]}
              >
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label={t('clients.clientName')}
                rules={[{ required: true, message: t('validation.required') }]}
              >
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="email" label={t('table.columns.email')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="phone" label={t('table.columns.phone')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="address" label={t('clients.address')}>
            <Input placeholder={t('forms.placeholders.enterText')} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="city" label={t('clients.city')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="countryCode" label={t('clients.countryCode')}>
                <Select placeholder={t('forms.placeholders.selectOption')} showSearch>
                  {countryOptions.map(country => (
                    <Option key={country} value={country}>{country}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="postalCode" label={t('clients.postalCode')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="taxId" label={t('clients.taxId')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="entityType" label={t('clients.entityType')}>
                <Select placeholder={t('forms.placeholders.selectOption')}>
                  <Option value="CORPORATION">{t('clients.entityTypes.CORPORATION')}</Option>
                  <Option value="LLC">{t('clients.entityTypes.LLC')}</Option>
                  <Option value="PARTNERSHIP">{t('clients.entityTypes.PARTNERSHIP')}</Option>
                  <Option value="INDIVIDUAL">{t('clients.entityTypes.INDIVIDUAL')}</Option>
                  <Option value="GOVERNMENT">{t('clients.entityTypes.GOVERNMENT')}</Option>
                  <Option value="NGO">{t('clients.entityTypes.NGO')}</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="riskRating" label={t('clients.riskRating')}>
                <Select placeholder={t('forms.placeholders.selectOption')}>
                  {riskRatingOptions.map(rating => (
                    <Option key={rating} value={rating}>{rating}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditLimit" label={t('clients.creditLimit')}>
                <InputNumber
                  placeholder={t('forms.placeholders.enterAmount')}
                  style={{ width: '100%' }}
                  min={0}
                  formatter={(value) => value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="creditCurrency" label={t('clients.creditCurrency')}>
                <Select placeholder={t('forms.placeholders.selectOption')}>
                  {currencyOptions.map(currency => (
                    <Option key={currency} value={currency}>{currency}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="industryCode" label={t('clients.industryCode')}>
                <Input placeholder={t('forms.placeholders.enterText')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="isActive" label={t('clients.isActive')} valuePropName="checked">
                <Switch checkedChildren={t('common.active')} unCheckedChildren={t('common.inactive')} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="notes" label={t('common.notes')}>
            <Input.TextArea
              rows={3}
              placeholder={t('forms.placeholders.enterText')}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button onClick={() => setIsModalVisible(false)} style={{ marginRight: 8 }}>
              {t('buttons.cancel')}
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={createClientMutation.isLoading}
            >
              {t('buttons.create')}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ClientManagement;

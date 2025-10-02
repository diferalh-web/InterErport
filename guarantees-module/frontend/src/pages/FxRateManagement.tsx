import React, { useState } from 'react';
import { Card, Typography, Table, Button, message, Modal, Form, Input, Select, DatePicker, InputNumber } from 'antd';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

import { apiService } from '../services/api';
import { FxRate, FxRateProvider } from '../types/guarantee';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { Option } = Select;

const FxRateManagement: React.FC = () => {
  const { t, formatDate } = useAppTranslation();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: fxRates, isLoading } = useQuery(
    'fx-rates',
    () => apiService.getFxRates(),
    {
      onError: () => {
        message.error(t('messages.loadError'));
      },
    }
  );

  const createRateMutation = useMutation(
    (rateData: Partial<FxRate>) => apiService.createManualRate(rateData as any),
    {
      onSuccess: () => {
        message.success(t('messages.createSuccess'));
        queryClient.invalidateQueries('fx-rates');
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
    const rateData = {
      baseCurrency: values.baseCurrency,
      targetCurrency: values.targetCurrency,
      rate: values.rate,
      buyingRate: values.buyingRate,
      sellingRate: values.sellingRate,
      effectiveDate: values.effectiveDate.format('YYYY-MM-DD'),
      provider: FxRateProvider.MANUAL,
      isActive: true,
      notes: values.notes,
    };
    createRateMutation.mutate(rateData);
  };

  const currencyOptions = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'CAD', 'AUD', 'CNY', 'INR', 'BRL', 'MXN', 'ZAR'];

  const columns = [
    {
      title: t('fxRates.baseCurrency'),
      dataIndex: 'baseCurrency',
      key: 'baseCurrency',
    },
    {
      title: t('fxRates.targetCurrency'),
      dataIndex: 'targetCurrency',
      key: 'targetCurrency',
    },
    {
      title: t('fxRates.rate'),
      dataIndex: 'rate',
      key: 'rate',
      render: (rate: number) => rate.toFixed(6),
    },
    {
      title: t('fxRates.provider'),
      dataIndex: 'provider',
      key: 'provider',
    },
    {
      title: t('fxRates.effectiveDate'),
      dataIndex: 'effectiveDate',
      key: 'effectiveDate',
      render: (date: string) => date ? formatDate(date) : '-'
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2}>{t('fxRates.title')}</Title>
          <Text type="secondary">{t('fxRates.manageDescription')}</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalVisible(true)}>
          {t('fxRates.addManualRate')}
        </Button>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={fxRates || []}
          loading={isLoading}
          rowKey="id"
        />
      </Card>

      <Modal
        title={t('fxRates.addManualRate')}
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            effectiveDate: dayjs(),
          }}
        >
          <Form.Item
            name="baseCurrency"
            label={t('fxRates.baseCurrency')}
            rules={[{ required: true, message: t('validation.required') }]}
          >
            <Select placeholder={t('forms.placeholders.selectOption')} showSearch>
              {currencyOptions.map(currency => (
                <Option key={currency} value={currency}>{currency}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="targetCurrency"
            label={t('fxRates.targetCurrency')}
            rules={[{ required: true, message: t('validation.required') }]}
          >
            <Select placeholder={t('forms.placeholders.selectOption')} showSearch>
              {currencyOptions.map(currency => (
                <Option key={currency} value={currency}>{currency}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="rate"
            label={t('fxRates.rate')}
            rules={[
              { required: true, message: t('validation.required') },
              { type: 'number', min: 0.000001, message: t('validation.positiveNumber') }
            ]}
          >
            <InputNumber
              placeholder={t('forms.placeholders.enterRate')}
              style={{ width: '100%' }}
              precision={6}
              min={0.000001}
            />
          </Form.Item>

          <Form.Item
            name="buyingRate"
            label={t('fxRates.buyingRate')}
            rules={[{ type: 'number', min: 0.000001, message: t('validation.positiveNumber') }]}
          >
            <InputNumber
              placeholder={t('forms.placeholders.enterRate')}
              style={{ width: '100%' }}
              precision={6}
              min={0.000001}
            />
          </Form.Item>

          <Form.Item
            name="sellingRate"
            label={t('fxRates.sellingRate')}
            rules={[{ type: 'number', min: 0.000001, message: t('validation.positiveNumber') }]}
          >
            <InputNumber
              placeholder={t('forms.placeholders.enterRate')}
              style={{ width: '100%' }}
              precision={6}
              min={0.000001}
            />
          </Form.Item>

          <Form.Item
            name="effectiveDate"
            label={t('fxRates.effectiveDate')}
            rules={[{ required: true, message: t('validation.required') }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

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
              loading={createRateMutation.isLoading}
            >
              {t('buttons.create')}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FxRateManagement;

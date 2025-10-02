import React, { useState } from 'react';
import {
  Table,
  Card,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Typography,
  Row,
  Col,
  Dropdown,
  Modal,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  CloseOutlined,
  SendOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';

import { apiService } from '../services/api';
import {
  GuaranteeContract,
  GuaranteeStatus,
  GuaranteeType,
  SearchFilters,
} from '../types/guarantee';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;

const GuaranteeList: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { t, formatCurrency, formatDate } = useAppTranslation();

  // State for filters and pagination
  const [filters, setFilters] = useState<SearchFilters>({
    reference: searchParams.get('reference') || '',
    status: (searchParams.get('status') as GuaranteeStatus) || undefined,
    guaranteeType: (searchParams.get('guaranteeType') as GuaranteeType) || undefined,
    currency: searchParams.get('currency') || '',
  });

  const [pagination, setPagination] = useState({
    current: parseInt(searchParams.get('page') || '1'),
    pageSize: parseInt(searchParams.get('size') || '20'),
  });

  // Fetch guarantees with current filters
  const {
    data: guaranteesData,
    isLoading,
    refetch,
  } = useQuery(
    ['guarantees', filters, pagination.current, pagination.pageSize],
    () =>
      apiService.searchGuarantees(
        filters,
        pagination.current - 1, // Backend uses 0-based indexing
        pagination.pageSize,
        'createdDate,desc'
      ),
    {
      keepPreviousData: true,
    }
  );

  // Mutations for guarantee actions
  const submitMutation = useMutation(apiService.submitGuarantee, {
    onSuccess: () => {
      message.success(t('messages.updateSuccess'));
      queryClient.invalidateQueries('guarantees');
    },
    onError: (error: any) => {
      const errorMessage = error instanceof Error 
        ? error.message 
        : error?.response?.data?.message || error?.response?.data?.error || t('common.error');
      message.error(`${t('messages.updateError')}: ${errorMessage}`);
    },
  });

  const approveMutation = useMutation(
    ({ id, approvedBy }: { id: number; approvedBy: string }) =>
      apiService.approveGuarantee(id, approvedBy),
    {
      onSuccess: () => {
        message.success(t('messages.updateSuccess'));
        queryClient.invalidateQueries('guarantees');
      },
      onError: (error: any) => {
        const errorMessage = error instanceof Error 
          ? error.message 
          : error?.response?.data?.message || error?.response?.data?.error || t('common.error');
        message.error(`${t('messages.updateError')}: ${errorMessage}`);
      },
    }
  );

  const rejectMutation = useMutation(
    ({ id, rejectedBy, reason }: { id: number; rejectedBy: string; reason: string }) =>
      apiService.rejectGuarantee(id, rejectedBy, reason),
    {
      onSuccess: () => {
        message.success(t('messages.updateSuccess'));
        queryClient.invalidateQueries('guarantees');
      },
      onError: (error: any) => {
        const errorMessage = error instanceof Error 
          ? error.message 
          : error?.response?.data?.message || error?.response?.data?.error || t('common.error');
        message.error(`${t('messages.updateError')}: ${errorMessage}`);
      },
    }
  );

  const cancelMutation = useMutation(
    ({ id, cancelledBy, reason }: { id: number; cancelledBy: string; reason: string }) =>
      apiService.cancelGuarantee(id, cancelledBy, reason),
    {
      onSuccess: () => {
        message.success(t('messages.updateSuccess'));
        queryClient.invalidateQueries('guarantees');
      },
      onError: (error: any) => {
        const errorMessage = error instanceof Error 
          ? error.message 
          : error?.response?.data?.message || error?.response?.data?.error || t('common.error');
        message.error(`${t('messages.updateError')}: ${errorMessage}`);
      },
    }
  );

  // Handle filter changes
  const handleFilterChange = (newFilters: Partial<SearchFilters>) => {
    const updatedFilters = { ...filters, ...newFilters };
    setFilters(updatedFilters);
    setPagination({ ...pagination, current: 1 });

    // Update URL search params
    const params = new URLSearchParams();
    Object.entries(updatedFilters).forEach(([key, value]) => {
      if (value) params.set(key, value.toString());
    });
    params.set('page', '1');
    params.set('size', pagination.pageSize.toString());
    setSearchParams(params);
  };

  // Handle pagination change
  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    const newPagination = {
      current: paginationConfig.current || 1,
      pageSize: paginationConfig.pageSize || 20,
    };
    setPagination(newPagination);

    // Update URL
    const params = new URLSearchParams(searchParams);
    params.set('page', newPagination.current.toString());
    params.set('size', newPagination.pageSize.toString());
    setSearchParams(params);
  };

  // Handle guarantee actions
  const handleSubmit = (id: number) => {
    submitMutation.mutate(id);
  };

  const handleApprove = (id: number) => {
    approveMutation.mutate({ id, approvedBy: 'Admin User' });
  };

  const handleReject = (id: number, reason: string) => {
    rejectMutation.mutate({ id, rejectedBy: 'Admin User', reason });
  };

  const handleCancel = (id: number, reason: string) => {
    cancelMutation.mutate({ id, cancelledBy: 'Admin User', reason });
  };

  // Show reject/cancel modal
  const showRejectModal = (id: number, action: 'reject' | 'cancel') => {
    Modal.confirm({
      title: `${t(`buttons.${action}`)} ${t('guarantees.title')}`,
      content: (
        <div>
          <p>{t('amendments.reason')}:</p>
          <Input.TextArea
            id="reason-input"
            rows={3}
            placeholder={t('forms.placeholders.enterText')}
          />
        </div>
      ),
      okText: t(`buttons.${action}`),
      cancelText: t('buttons.cancel'),
      onOk: () => {
        const reason = (document.getElementById('reason-input') as HTMLTextAreaElement)?.value;
        if (!reason?.trim()) {
          message.error(t('validation.required'));
          return;
        }
        if (action === 'reject') {
          handleReject(id, reason);
        } else {
          handleCancel(id, reason);
        }
      },
    });
  };

  // Table columns
  const columns: ColumnsType<GuaranteeContract> = [
    {
      title: t('table.columns.reference'),
      dataIndex: 'reference',
      key: 'reference',
      width: 150,
      fixed: 'left',
      render: (text: string, record: GuaranteeContract) => (
        <a onClick={() => navigate(`/guarantees/${record.id}`)}>{text}</a>
      ),
    },
    {
      title: t('table.columns.type'),
      dataIndex: 'guaranteeType',
      key: 'guaranteeType',
      width: 120,
      render: (type: GuaranteeType) => t(`guarantees.guaranteeTypes.${type}`),
    },
    {
      title: t('table.columns.status'),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: GuaranteeStatus) => {
        const colors = {
          [GuaranteeStatus.DRAFT]: 'default',
          [GuaranteeStatus.SUBMITTED]: 'processing',
          [GuaranteeStatus.APPROVED]: 'success',
          [GuaranteeStatus.REJECTED]: 'error',
          [GuaranteeStatus.CANCELLED]: 'warning',
          [GuaranteeStatus.EXPIRED]: 'error',
          [GuaranteeStatus.SETTLED]: 'default',
        };
        return <Tag color={colors[status]}>{t(`guarantees.statuses.${status}`)}</Tag>;
      },
    },
    {
      title: t('table.columns.beneficiary'),
      dataIndex: 'beneficiaryName',
      key: 'beneficiaryName',
      width: 200,
      ellipsis: true,
    },
    {
      title: t('table.columns.amount'),
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (amount: number, record: GuaranteeContract) => (
        <Text strong>{formatCurrency(amount, record.currency)}</Text>
      ),
    },
    {
      title: t('table.columns.issueDate'),
      dataIndex: 'issueDate',
      key: 'issueDate',
      width: 100,
      render: (date: string) => formatDate(date),
    },
    {
      title: t('table.columns.expiryDate'),
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      width: 100,
      render: (date: string) => {
        const daysUntilExpiry = dayjs(date).diff(dayjs(), 'days');
        const isExpiring = daysUntilExpiry <= 30;
        return (
          <Text style={{ color: isExpiring ? 'red' : 'inherit' }}>
            {formatDate(date)}
          </Text>
        );
      },
    },
    {
      title: t('table.columns.actions'),
      key: 'actions',
      width: 100,
      fixed: 'right',
      render: (_, record: GuaranteeContract) => {
        const canEdit = record.status === GuaranteeStatus.DRAFT || record.status === GuaranteeStatus.REJECTED;
        const canSubmit = record.status === GuaranteeStatus.DRAFT;
        const canApprove = record.status === GuaranteeStatus.SUBMITTED;
        const canReject = record.status === GuaranteeStatus.SUBMITTED;
        const canCancel = [GuaranteeStatus.DRAFT, GuaranteeStatus.SUBMITTED, GuaranteeStatus.APPROVED].includes(record.status);

        const menuItems = [
          ...(canEdit ? [{
            key: 'edit',
            label: t('buttons.edit'),
            icon: <EditOutlined />,
            onClick: () => navigate(`/guarantees/${record.id}/edit`),
          }] : []),
          ...(canSubmit ? [{
            key: 'submit',
            label: t('buttons.submit'),
            icon: <SendOutlined />,
            onClick: () => handleSubmit(record.id!),
          }] : []),
          ...(canApprove ? [{
            key: 'approve',
            label: t('buttons.approve'),
            icon: <CheckOutlined />,
            onClick: () => handleApprove(record.id!),
          }] : []),
          ...(canReject ? [{
            key: 'reject',
            label: t('buttons.reject'),
            icon: <CloseOutlined />,
            onClick: () => showRejectModal(record.id!, 'reject'),
          }] : []),
          ...(canCancel ? [{
            key: 'cancel',
            label: t('buttons.cancel'),
            icon: <DeleteOutlined />,
            onClick: () => showRejectModal(record.id!, 'cancel'),
            danger: true,
          }] : []),
        ];

        return (
          <Dropdown
            menu={{ items: menuItems }}
            trigger={['click']}
          >
            <Button type="text" icon={<MoreOutlined />} />
          </Dropdown>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2}>{t('guarantees.title')}</Title>
          <Text type="secondary">{t('guarantees.guaranteeList')}</Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate('/guarantees/new')}
        >
          {t('guarantees.createNew')}
        </Button>
      </div>

      {/* Filters */}
      <Card style={{ marginBottom: '24px' }}>
        <Row gutter={16}>
          <Col span={6}>
            <Input
              placeholder={t('common.search')}
              value={filters.reference}
              onChange={(e) => handleFilterChange({ reference: e.target.value })}
              prefix={<SearchOutlined />}
            />
          </Col>
          <Col span={4}>
            <Select
              placeholder={t('common.status')}
              value={filters.status}
              onChange={(value) => handleFilterChange({ status: value })}
              allowClear
              style={{ width: '100%' }}
            >
              {Object.keys(GuaranteeStatus).map((key) => (
                <Select.Option key={key} value={key}>
                  {t(`guarantees.statuses.${key}`)}
                </Select.Option>
              ))}
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder={t('guarantees.guaranteeType')}
              value={filters.guaranteeType}
              onChange={(value) => handleFilterChange({ guaranteeType: value })}
              allowClear
              style={{ width: '100%' }}
            >
              {Object.keys(GuaranteeType).map((key) => (
                <Select.Option key={key} value={key}>
                  {t(`guarantees.guaranteeTypes.${key}`)}
                </Select.Option>
              ))}
            </Select>
          </Col>
          <Col span={4}>
            <Input
              placeholder={t('guarantees.currency')}
              value={filters.currency}
              onChange={(e) => handleFilterChange({ currency: e.target.value })}
            />
          </Col>
          <Col span={6}>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => refetch()}
                loading={isLoading}
              >
                {t('common.refresh')}
              </Button>
              <Button
                onClick={() => {
                  setFilters({});
                  setPagination({ current: 1, pageSize: 20 });
                  setSearchParams(new URLSearchParams());
                }}
              >
                {t('buttons.clear')}
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={guaranteesData?.content || []}
          loading={isLoading}
          rowKey="id"
          scroll={{ x: 1000 }}
          locale={{
            emptyText: t('table.noData'),
          }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: guaranteesData?.totalElements || 0,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              t('table.showingItems', { 
                start: range[0], 
                end: range[1], 
                total: total 
              }),
          }}
          onChange={handleTableChange}
        />
      </Card>
    </div>
  );
};

export default GuaranteeList;

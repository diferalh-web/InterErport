import React from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Typography, Alert, Spin, Select } from 'antd';
import { useQuery } from 'react-query';
import { Column, Line, Pie } from '@ant-design/charts';
import { useTranslation } from 'react-i18next';
import {
  FileTextOutlined,
  DollarCircleOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import numeral from 'numeral';

import { apiService } from '../services/api';
import { GuaranteeContract, GuaranteeStatus, GuaranteeStatusLabels } from '../types/guarantee';

dayjs.extend(relativeTime);

const { Title, Text } = Typography;
const { Option } = Select;


const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [chartPeriod, setChartPeriod] = React.useState<number>(12);
  
  // Simple chart period change handler
  const handleChartPeriodChange = React.useCallback((value: number) => {
    if (typeof value === 'number' && [6, 12, 24].includes(value)) {
      setChartPeriod(value);
    }
  }, []);

  // Fetch enhanced dashboard data
  const { data: dashboardSummary, isLoading: summaryLoading } = useQuery(
    'dashboard-summary',
    () => apiService.getDashboardSummary(),
    {
      refetchInterval: 30000, // Refresh every 30 seconds
    }
  );

  const { data: monthlyStats, isLoading: monthlyLoading } = useQuery(
    ['monthly-stats', chartPeriod],
    () => apiService.getMonthlyStatistics(chartPeriod),
    {
      refetchInterval: 60000, // Refresh every minute
    }
  );

  const { data: currencyMetrics, isLoading: currencyLoading } = useQuery(
    'currency-metrics',
    () => apiService.getMetricsByCurrency(),
    {
      refetchInterval: 300000, // Refresh every 5 minutes
    }
  );

  const { data: expiringGuarantees, isLoading: expiringLoading } = useQuery(
    'expiring-guarantees',
    () => apiService.getExpiringGuarantees(30),
    {
      refetchInterval: 60000, // Refresh every minute
    }
  );

  // Format currency function
  const formatCurrency = (amount: number, currency = 'USD') => {
    return numeral(amount).format('0,0.00') + ' ' + currency;
  };

  // Prepare chart data with complete safety
  const monthlyChartData = React.useMemo(() => {
    if (!monthlyStats?.monthlyData || !Array.isArray(monthlyStats.monthlyData)) {
      return [];
    }
    
    try {
      return monthlyStats.monthlyData
        .filter(month => {
          return month && 
                 typeof month === 'object' && 
                 month.monthLabel && 
                 typeof month.monthLabel === 'string' &&
                 month.guarantees && 
                 month.claims && 
                 month.amendments;
        })
        .map(month => {
      return {
            month: String(month.monthLabel).trim(),
            guarantees: Math.max(0, parseInt(String(month.guarantees.count)) || 0),
            claims: Math.max(0, parseInt(String(month.claims.count)) || 0),
            amendments: Math.max(0, parseInt(String(month.amendments.count)) || 0),
            guaranteeAmount: Math.max(0, parseFloat(String(month.guarantees.amount)) || 0),
            claimAmount: Math.max(0, parseFloat(String(month.claims.amount)) || 0),
          };
        });
    } catch (error) {
      console.error('Error processing monthly chart data:', error);
      return [];
    }
  }, [monthlyStats, chartPeriod]); // Added chartPeriod dependency

  const monthlyAmountChartData = React.useMemo(() => {
    if (!monthlyStats?.monthlyData || !Array.isArray(monthlyStats.monthlyData)) {
      return [];
    }
    
    try {
      const safeData = [];
      const validMonths = monthlyStats.monthlyData.filter(month => {
        return month && 
               typeof month === 'object' && 
               month.monthLabel && 
               typeof month.monthLabel === 'string' &&
               month.guarantees && 
               month.claims;
      });
      
      for (const month of validMonths) {
        const monthLabel = String(month.monthLabel).trim();
        const guaranteeAmount = Math.max(0, parseFloat(String(month.guarantees?.amount)) || 0);
        const claimAmount = Math.max(0, parseFloat(String(month.claims?.amount)) || 0);
        
        safeData.push({
          month: monthLabel,
          type: 'Guarantees',
          amount: guaranteeAmount
        });
        safeData.push({
          month: monthLabel,
          type: 'Claims',
          amount: claimAmount
        });
      }
      
      return safeData;
    } catch (error) {
      console.error('Error processing monthly amount data:', error);
      return [];
    }
  }, [monthlyStats, chartPeriod]);

  const currencyPieData = React.useMemo(() => {
    if (!currencyMetrics?.guaranteesByCurrency || !Array.isArray(currencyMetrics.guaranteesByCurrency)) {
      return [];
    }
    
    try {
      return currencyMetrics.guaranteesByCurrency
        .filter(item => {
          return item && 
                 typeof item === 'object' && 
                 item.currency && 
                 typeof item.currency === 'string' &&
                 typeof item.amount !== 'undefined' &&
                 typeof item.count !== 'undefined';
        })
        .map(item => {
    return {
            type: String(item.currency).trim(),
            value: Math.max(0, parseFloat(String(item.amount)) || 0),
            count: Math.max(0, parseInt(String(item.count)) || 0),
          };
        })
        .filter(item => item.value > 0); // Only include currencies with actual amounts
    } catch (error) {
      console.error('Error processing currency pie data:', error);
      return [];
    }
  }, [currencyMetrics]);

  // Memoized chart data for monthly counts - completely safe
  const monthlyCountData = React.useMemo(() => {
    if (!Array.isArray(monthlyChartData) || monthlyChartData.length === 0) {
      return [];
    }
    
    try {
      const safeData = [];
      
      for (const item of monthlyChartData) {
        if (!item || typeof item !== 'object' || !item.month) continue;
        
        const monthLabel = String(item.month).trim();
        const guarantees = Math.max(0, parseInt(String(item.guarantees)) || 0);
        const claims = Math.max(0, parseInt(String(item.claims)) || 0);
        const amendments = Math.max(0, parseInt(String(item.amendments)) || 0);
        
        safeData.push({
          month: monthLabel,
          type: 'Guarantees',
          value: guarantees
        });
        safeData.push({
          month: monthLabel,
          type: 'Claims',
          value: claims
        });
        safeData.push({
          month: monthLabel,
          type: 'Amendments',
          value: amendments
        });
      }
      
      return safeData;
    } catch (error) {
      console.error('Error processing monthly count data:', error);
      return [];
    }
  }, [monthlyChartData, chartPeriod]);

  // Ultra-simple chart configuration
  const monthlyCountConfig = React.useMemo(() => {
    return {
      data: Array.isArray(monthlyCountData) ? monthlyCountData : [],
      xField: 'month',
      yField: 'value',
      seriesField: 'type',
      isGroup: true
    };
  }, [monthlyCountData]);

  const monthlyAmountConfig = React.useMemo(() => {
    return {
      data: Array.isArray(monthlyAmountChartData) ? monthlyAmountChartData : [],
      xField: 'month',
      yField: 'amount',
      seriesField: 'type'
    };
  }, [monthlyAmountChartData]);

  const currencyPieConfig = React.useMemo(() => {
    return {
      data: Array.isArray(currencyPieData) ? currencyPieData : [],
      angleField: 'value',
      colorField: 'type'
    };
  }, [currencyPieData]);

  // Table columns for expiring guarantees
  const expiringColumns = [
    {
      title: 'Reference',
      dataIndex: 'reference',
      key: 'reference',
      render: (text: string, record: GuaranteeContract) => (
        <a 
          href={`#/guarantees/${record.id}`}
          onClick={(e) => {
            e.preventDefault();
            navigate(`/guarantees/${record.id}`);
          }}
        >
          {text}
        </a>
      ),
    },
    {
      title: 'Beneficiary',
      dataIndex: 'beneficiaryName',
      key: 'beneficiaryName',
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number, record: GuaranteeContract) => (
        <Text strong>{record.currency} {numeral(amount).format('0,0.00')}</Text>
      ),
    },
    {
      title: 'Expiry Date',
      dataIndex: 'expiryDate',
      key: 'expiryDate',
      render: (date: string) => {
        const expiryDate = dayjs(date);
        const daysUntilExpiry = expiryDate.diff(dayjs(), 'days');
        
        return (
          <div>
            <div>{expiryDate.format('MMM DD, YYYY')}</div>
            <Text type={daysUntilExpiry <= 7 ? 'danger' : 'warning'}>
              {daysUntilExpiry > 0 ? `${daysUntilExpiry} days left` : 'Expired'}
          </Text>
          </div>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: GuaranteeStatus) => {
        const color = status === GuaranteeStatus.APPROVED ? 'green' : 'orange';
        return <Tag color={color}>{GuaranteeStatusLabels[status]}</Tag>;
      },
    },
  ];

  if (summaryLoading || monthlyLoading || currencyLoading) {
    return (
      <div style={{ padding: '50px', textAlign: 'center' }}>
        <Spin size="large" />
        <div style={{ marginTop: '16px' }}>
          <Text>{t('common.loading')}</Text>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <Title level={2}>{t('dashboard.title')}</Title>
        <Text type="secondary">
          {t('dashboard.welcome')}
        </Text>
      </div>

      {/* Summary Metrics Cards */}
      <Row gutter={[24, 24]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title={t('dashboard.totalGuarantees')}
              value={dashboardSummary?.guarantees?.total || 0}
              prefix={<FileTextOutlined style={{ color: '#1890ff' }} />}
              suffix={
                <div style={{ fontSize: '12px', color: '#999' }}>
                  {t('common.active')}: {dashboardSummary?.guarantees?.active || 0}
                </div>
              }
            />
            <div style={{ marginTop: '8px' }}>
              <Text strong style={{ color: '#1890ff' }}>
                {formatCurrency(dashboardSummary?.guarantees?.totalAmount || 0, 'USD')}
              </Text>
            </div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title={t('dashboard.totalClaims')}
              value={dashboardSummary?.claims?.total || 0}
              prefix={<ExclamationCircleOutlined style={{ color: '#52c41a' }} />}
            />
            <div style={{ marginTop: '8px' }}>
              <Text strong style={{ color: '#52c41a' }}>
                {formatCurrency(dashboardSummary?.claims?.totalAmount || 0, 'USD')}
              </Text>
            </div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title={t('dashboard.totalAmendments')}
              value={dashboardSummary?.amendments?.total || 0}
              prefix={<EditOutlined style={{ color: '#fa8c16' }} />}
            />
            <div style={{ marginTop: '8px' }}>
              <Text type="secondary">{t('amendments.title')}</Text>
            </div>
          </Card>
        </Col>
        
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title={t('dashboard.expiringSoon')}
              value={expiringGuarantees?.length || 0}
              prefix={<WarningOutlined style={{ color: '#ff4d4f' }} />}
              suffix="next 30 days"
            />
            <div style={{ marginTop: '8px' }}>
              <Text type="danger">{t('common.warning')}</Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Charts Section */}
      <Row gutter={[24, 24]} style={{ marginBottom: '24px' }}>
        <Col xs={24} lg={16}>
          <Card 
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span><RiseOutlined /> {t('dashboard.statistics')}</span>
                <Select
                  value={chartPeriod}
                  onChange={handleChartPeriodChange}
                  style={{ width: 150 }}
                >
                  <Option value={6}>{t('dashboard.chartPeriods.last6Months')}</Option>
                  <Option value={12}>{t('dashboard.chartPeriods.lastYear')}</Option>
                  <Option value={24}>24 {t('common.months', 'Months')}</Option>
                </Select>
              </div>
            }
          >
            <div style={{ height: 300 }}>
              {monthlyLoading ? (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Spin size="large" />
                </div>
              ) : monthlyCountData && monthlyCountData.length > 0 ? (
                <Column {...monthlyCountConfig} />
              ) : (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Text type="secondary">No data available for chart</Text>
                </div>
              )}
            </div>
          </Card>
                  </Col>

        <Col xs={24} lg={8}>
          <Card title={<span><DollarCircleOutlined /> {t('guarantees.currency')}</span>}>
            <div style={{ height: 300 }}>
              {currencyLoading ? (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Spin size="large" />
                          </div>
              ) : currencyPieData && currencyPieData.length > 0 ? (
                <Pie {...currencyPieConfig} />
              ) : (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Text type="secondary">No currency data available</Text>
                    </div>
              )}
            </div>
            </Card>
          </Col>
        </Row>

      {/* Monthly Amount Chart */}
      <Row gutter={[24, 24]} style={{ marginBottom: '24px' }}>
        <Col span={24}>
          <Card title={<span><DollarCircleOutlined /> Monthly Amount Trends</span>}>
            <div style={{ height: 300 }}>
              {monthlyLoading ? (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Spin size="large" />
                </div>
              ) : monthlyAmountChartData && monthlyAmountChartData.length > 0 ? (
                <Line {...monthlyAmountConfig} />
              ) : (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                  <Text type="secondary">No amount data available for chart</Text>
                </div>
              )}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Expiring Guarantees Table */}
      <Row gutter={[24, 24]}>
        <Col span={24}>
          <Card
            title={<span><WarningOutlined /> Guarantees Expiring Soon</span>}
            extra={
              <Text type="secondary">
                Next 30 days
              </Text>
            }
          >
            {expiringLoading ? (
              <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              </div>
            ) : (
              <>
                {expiringGuarantees && expiringGuarantees.length > 0 ? (
              <Table
                    dataSource={expiringGuarantees}
                    columns={expiringColumns}
                rowKey="id"
                    pagination={{ pageSize: 5, showSizeChanger: false }}
                    size="middle"
                  />
                ) : (
                  <Alert
                    message="No Expiring Guarantees"
                    description="All guarantees have sufficient time before expiry."
                    type="success"
                    showIcon
                    icon={<CheckCircleOutlined />}
                  />
                )}
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
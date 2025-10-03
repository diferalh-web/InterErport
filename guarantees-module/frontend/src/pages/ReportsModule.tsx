/**
 * Reports Module Component
 * Implements F9 - Reports functionality with CSV/PDF/Excel exports
 */

import React, { useState } from 'react';
import {
  Card,
  Button,
  Form,
  Select,
  DatePicker,
  Space,
  Typography,
  Row,
  Col,
  Divider,
  notification,
  Table,
  Tag,
  Progress,
  Modal,
  Input,
  InputNumber,
  Checkbox,
  Tabs
} from 'antd';
import {
  FileExcelOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  DownloadOutlined,
  ScheduleOutlined,
  EyeOutlined,
  DeleteOutlined,
  PlayCircleOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { apiService } from '../services/api';
import { useAppTranslation } from '../i18n/utils';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

interface ReportRequest {
  id: string;
  name: string;
  type: string;
  format: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
  downloadUrl?: string;
  parameters: any;
  progress: number;
}

interface ScheduledReport {
  id: string;
  name: string;
  type: string;
  format: string;
  schedule: string;
  active: boolean;
  lastRun?: string;
  nextRun: string;
  parameters: any;
}

const ReportsModule: React.FC = () => {
  const { t, formatDate } = useAppTranslation();
  const [form] = Form.useForm();
  const [scheduleForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [reportRequests, setReportRequests] = useState<ReportRequest[]>([]);
  const [scheduledReports, setScheduledReports] = useState<ScheduledReport[]>([]);
  const [isScheduleModalVisible, setIsScheduleModalVisible] = useState(false);
  const [selectedReportType, setSelectedReportType] = useState<string>('');

  // Mock data for demonstration
  React.useEffect(() => {
    // Mock report requests
    setReportRequests([
      {
        id: '1',
        name: 'Active Guarantees Report',
        type: 'ACTIVE_TRANSACTIONS',
        format: 'CSV',
        status: 'COMPLETED',
        createdAt: dayjs().subtract(1, 'hour').toISOString(),
        completedAt: dayjs().subtract(30, 'minutes').toISOString(),
        downloadUrl: '/reports/download/1',
        parameters: { dateRange: ['2024-01-01', '2024-12-31'] },
        progress: 100
      },
      {
        id: '2',
        name: 'Commission Report',
        type: 'COMMISSION_REPORT',
        format: 'PDF',
        status: 'PROCESSING',
        createdAt: dayjs().subtract(15, 'minutes').toISOString(),
        parameters: { dateRange: ['2024-09-01', '2024-09-30'] },
        progress: 65
      },
      {
        id: '3',
        name: 'Audit Report',
        type: 'AUDIT_REPORT',
        format: 'EXCEL',
        status: 'FAILED',
        createdAt: dayjs().subtract(2, 'hours').toISOString(),
        parameters: { dateRange: ['2024-08-01', '2024-08-31'] },
        progress: 0
      }
    ]);

    // Mock scheduled reports
    setScheduledReports([
      {
        id: '1',
        name: 'Weekly Active Guarantees',
        type: 'ACTIVE_TRANSACTIONS',
        format: 'CSV',
        schedule: 'Weekly (Mondays)',
        active: true,
        lastRun: dayjs().subtract(2, 'days').toISOString(),
        nextRun: dayjs().add(5, 'days').toISOString(),
        parameters: { includeExpired: false }
      },
      {
        id: '2',
        name: 'Monthly Commission Report',
        type: 'COMMISSION_REPORT',
        format: 'PDF',
        schedule: 'Monthly (1st day)',
        active: true,
        lastRun: dayjs().subtract(30, 'days').toISOString(),
        nextRun: dayjs().add(1, 'month').startOf('month').toISOString(),
        parameters: { includeProjections: true }
      }
    ]);
  }, []);

  const handleGenerateReport = async (values: any) => {
    setLoading(true);
    try {
      const startDate = values.dateRange ? values.dateRange[0].format('YYYY-MM-DD') : '';
      const endDate = values.dateRange ? values.dateRange[1].format('YYYY-MM-DD') : '';
      
      let blob: Blob;
      let filename: string;
      
      // Generate actual report based on type
      switch (values.reportType) {
        case 'COMMISSION_REPORT':
          blob = await apiService.downloadCommissionReport(
            startDate, 
            endDate, 
            values.format, 
            values.includeProjections || false
          );
          filename = `commission-report-${startDate}-to-${endDate}.${values.format.toLowerCase()}`;
          break;
        case 'ACTIVE_TRANSACTIONS':
          blob = await apiService.downloadActiveTransactionsReport(
            startDate, 
            endDate, 
            values.format,
            values.includeDrafts || false,
            values.includeExpired || false
          );
          filename = `active-transactions-${startDate}-to-${endDate}.${values.format.toLowerCase()}`;
          break;
        case 'AUDIT_REPORT':
          blob = await apiService.downloadAuditReport(startDate, endDate, values.format);
          filename = `audit-report-${startDate}-to-${endDate}.${values.format.toLowerCase()}`;
          break;
        case 'EXPIRY_ALERT':
          const daysAhead = values.daysAhead || 30;
          blob = await apiService.downloadExpiryAlertReport(daysAhead, values.format);
          filename = `expiry-alert-report-${daysAhead}-days.${values.format.toLowerCase()}`;
          break;
        default:
          throw new Error('Unsupported report type');
      }
      
      // Download the file
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      notification.success({
        message: t('messages.createSuccess'),
        description: t('reports.reportGenerated')
      });
      
      form.resetFields();
      
    } catch (error) {
      const errorMessage = error instanceof Error 
        ? error.message 
        : (error as any)?.response?.data?.message || 'Unknown error occurred';
      
      notification.error({
        message: t('messages.createError'),
        description: t('reports.reportError') + ': ' + errorMessage
      });
    } finally {
      setLoading(false);
    }
  };

  const handleScheduleReport = async (values: any) => {
    try {
      const newScheduledReport: ScheduledReport = {
        id: Date.now().toString(),
        name: values.name,
        type: values.reportType,
        format: values.format,
        schedule: values.schedule,
        active: true,
        nextRun: dayjs().add(1, values.frequency === 'daily' ? 'day' : values.frequency === 'weekly' ? 'week' : 'month').toISOString(),
        parameters: values
      };

      setScheduledReports(prev => [newScheduledReport, ...prev]);
      
      notification.success({
        message: t('reports.reportScheduled'),
        description: t('reports.reportScheduledSuccess')
      });
      
      setIsScheduleModalVisible(false);
      scheduleForm.resetFields();
      
    } catch (error) {
      notification.error({
        message: t('reports.schedulingFailed'),
        description: t('reports.schedulingError')
      });
    }
  };

  const getReportName = (type: string) => {
    return t(`reports.reportTypes.${type}`) || t('reports.customReport');
  };

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      PENDING: 'orange',
      PROCESSING: 'blue',
      COMPLETED: 'green',
      FAILED: 'red'
    };
    return colors[status] || 'default';
  };

  const handleDownloadReport = (reportId: string) => {
    // Mock download functionality
    notification.info({
      message: t('reports.downloadStarted'),
      description: t('reports.downloadDescription')
    });
  };

  const handleViewReportDetails = (reportId: string) => {
    // Mock view details functionality
    notification.info({
      message: 'Report Details',
      description: `Viewing details for report ${reportId}`
    });
  };

  const handleDeleteReport = (reportId: string) => {
    Modal.confirm({
      title: 'Delete Report',
      content: 'Are you sure you want to delete this report?',
      okText: t('common.yes'),
      cancelText: t('common.no'),
      onOk: () => {
        // Mock delete functionality
        notification.success({
          message: 'Report Deleted',
          description: 'The report has been deleted successfully'
        });
      }
    });
  };

  const reportRequestColumns = [
    {
      title: t('reports.reportName'),
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: ReportRequest) => (
        <div>
          <Text strong>{name}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {record.type} • {record.format}
          </Text>
        </div>
      )
    },
    {
      title: t('reports.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string, record: ReportRequest) => (
        <div>
          <Tag color={getStatusColor(status)}>{t(`reports.statuses.${status}`)}</Tag>
          {status === 'PROCESSING' && (
            <Progress 
              percent={record.progress} 
              size="small" 
              style={{ marginTop: 4, width: 100 }}
            />
          )}
        </div>
      )
    },
    {
      title: t('reports.tableColumns.created'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')
    },
    {
      title: t('reports.tableColumns.completed'),
      dataIndex: 'completedAt',
      key: 'completedAt',
      render: (date: string) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: t('reports.tableColumns.actions'),
      key: 'actions',
      render: (_: any, record: ReportRequest) => (
        <Space>
          {record.status === 'COMPLETED' && (
            <Button 
              icon={<DownloadOutlined />}
              size="small"
              type="primary"
              onClick={() => handleDownloadReport(record.id)}
            >
              {t('reports.buttons.download')}
            </Button>
          )}
          <Button 
            icon={<EyeOutlined />}
            size="small"
            onClick={() => handleViewReportDetails(record.id)}
          >
            {t('reports.buttons.details')}
          </Button>
          <Button 
            icon={<DeleteOutlined />}
            size="small"
            danger
            onClick={() => handleDeleteReport(record.id)}
          >
            {t('reports.buttons.delete')}
          </Button>
        </Space>
      )
    }
  ];

  const scheduledReportColumns = [
    {
      title: t('reports.tableColumns.reportName'),
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: ScheduledReport) => (
        <div>
          <Text strong>{name}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {record.type} • {record.format}
          </Text>
        </div>
      )
    },
    {
      title: t('reports.tableColumns.schedule'),
      dataIndex: 'schedule',
      key: 'schedule'
    },
    {
      title: t('reports.tableColumns.status'),
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag color={active ? 'green' : 'red'}>
          {active ? t('reports.statusValues.active') : t('reports.statusValues.inactive')}
        </Tag>
      )
    },
    {
      title: t('reports.tableColumns.lastRun'),
      dataIndex: 'lastRun',
      key: 'lastRun',
      render: (date: string) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: t('reports.tableColumns.nextRun'),
      dataIndex: 'nextRun',
      key: 'nextRun',
      render: (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')
    },
    {
      title: t('reports.tableColumns.actions'),
      key: 'actions',
      render: (_: any, record: ScheduledReport) => (
        <Space>
          <Button 
            icon={<PlayCircleOutlined />}
            size="small"
            type="primary"
          >
            {t('reports.buttons.runNow')}
          </Button>
          <Button 
            icon={<EyeOutlined />}
            size="small"
          >
            {t('reports.buttons.edit')}
          </Button>
          <Button 
            icon={<DeleteOutlined />}
            size="small"
            danger
          >
            {t('reports.buttons.delete')}
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>{t('reports.title')}</Title>
      <Text type="secondary">{t('reports.description')}</Text>
      
      <Tabs defaultActiveKey="generate" style={{ marginTop: 16 }}>
        <TabPane tab={t('reports.reportGeneration')} key="generate">
          <Row gutter={[16, 16]}>
            <Col span={8}>
              <Card title={t('reports.generateReport')} style={{ height: 'fit-content' }}>
                <Form
                  form={form}
                  layout="vertical"
                  onFinish={handleGenerateReport}
                >
                  <Form.Item
                    name="reportType"
                    label={t('reports.reportType')}
                    rules={[{ required: true, message: t('forms.validation.required') }]}
                  >
                    <Select 
                      placeholder={t('forms.placeholders.selectOption')}
                      onChange={(value) => setSelectedReportType(value)}
                    >
                      <Option value="ACTIVE_TRANSACTIONS">{t('reports.reportTypes.ACTIVE_TRANSACTIONS')}</Option>
                      <Option value="COMMISSION_REPORT">{t('reports.reportTypes.COMMISSION_REPORT')}</Option>
                      <Option value="EXPIRING_GUARANTEES">{t('reports.reportTypes.EXPIRING_GUARANTEES')}</Option>
                      <Option value="CLAIMS_ANALYSIS">{t('reports.reportTypes.CLAIMS_ANALYSIS')}</Option>
                      <Option value="FINANCIAL_SUMMARY">{t('reports.reportTypes.FINANCIAL_SUMMARY')}</Option>
                      <Option value="AUDIT_TRAIL">{t('reports.reportTypes.AUDIT_TRAIL')}</Option>
                    </Select>
                  </Form.Item>

                  <Form.Item
                    name="format"
                    label={t('reports.format')}
                    rules={[{ required: true, message: t('forms.validation.required') }]}
                  >
                    <Select placeholder={t('forms.placeholders.selectOption')}>
                      <Option value="CSV">
                        <FileTextOutlined /> {t('reports.formats.CSV')}
                      </Option>
                      <Option value="PDF">
                        <FilePdfOutlined /> {t('reports.formats.PDF')}
                      </Option>
                      <Option value="EXCEL">
                        <FileExcelOutlined /> {t('reports.formats.EXCEL')}
                      </Option>
                    </Select>
                  </Form.Item>

                  {selectedReportType === 'EXPIRY_ALERT' && (
                  <Form.Item
                    name="daysAhead"
                    label={t('reports.formLabels.daysAhead')}
                    rules={[{ required: true, message: t('reports.validation.enterDaysAhead') }]}
                    initialValue={30}
                  >
                    <InputNumber
                      min={1}
                      max={365}
                      placeholder={t('reports.placeholders.enterDaysAhead')}
                      style={{ width: '100%' }}
                      addonAfter={t('reports.addOns.days')}
                    />
                    </Form.Item>
                  )}

                  {selectedReportType !== 'EXPIRY_ALERT' && (
                  <Form.Item
                    name="dateRange"
                    label={t('reports.formLabels.dateRange')}
                    rules={[{ required: true, message: t('reports.validation.selectDateRange') }]}
                  >
                      <RangePicker style={{ width: '100%' }} />
                    </Form.Item>
                  )}

                  <Form.Item
                    name="includeDrafts"
                    valuePropName="checked"
                  >
                    <Checkbox>{t('reports.formLabels.includeDraftGuarantees')}</Checkbox>
                  </Form.Item>

                  <Form.Item
                    name="includeExpired"
                    valuePropName="checked"
                  >
                    <Checkbox>{t('reports.formLabels.includeExpiredGuarantees')}</Checkbox>
                  </Form.Item>

                  <Form.Item
                    name="digitalSignature"
                    valuePropName="checked"
                  >
                    <Checkbox>{t('reports.formLabels.includeDigitalSignature')}</Checkbox>
                  </Form.Item>

                  <Button 
                    type="primary" 
                    htmlType="submit" 
                    loading={loading}
                    block
                  >
                    {t('reports.buttons.generateReport')}
                  </Button>
                </Form>

                <Divider />

                <Button 
                  icon={<ScheduleOutlined />}
                  onClick={() => setIsScheduleModalVisible(true)}
                  block
                >
                  {t('reports.buttons.scheduleAutomaticReport')}
                </Button>
              </Card>
            </Col>

            <Col span={16}>
              <Card title={t('reports.cardTitles.recentReportRequests')}>
                <Table 
                  columns={reportRequestColumns}
                  dataSource={reportRequests}
                  rowKey="id"
                  pagination={{ pageSize: 5 }}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab={t('reports.tabs.scheduledReports')} key="scheduled">
          <Card title={t('reports.cardTitles.scheduledReports')}>
            <div style={{ marginBottom: 16 }}>
              <Button 
                type="primary" 
                icon={<ScheduleOutlined />}
                onClick={() => setIsScheduleModalVisible(true)}
              >
                {t('reports.buttons.scheduleNewReport')}
              </Button>
            </div>
            
            <Table 
              columns={scheduledReportColumns}
              dataSource={scheduledReports}
              rowKey="id"
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </TabPane>
      </Tabs>

      {/* Schedule Report Modal */}
      <Modal
        title={t('reports.modalTitles.scheduleAutomaticReport')}
        open={isScheduleModalVisible}
        onCancel={() => setIsScheduleModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          form={scheduleForm}
          layout="vertical"
          onFinish={handleScheduleReport}
        >
          <Form.Item
            name="name"
            label={t('reports.formLabels.reportName')}
            rules={[{ required: true, message: t('reports.validation.enterReportName') }]}
          >
            <Input placeholder={t('reports.placeholders.enterReportName')} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="reportType"
                label={t('reports.formLabels.reportType')}
                rules={[{ required: true, message: t('reports.validation.selectReportType') }]}
              >
                <Select placeholder={t('reports.placeholders.selectType')}>
                  <Option value="ACTIVE_TRANSACTIONS">{t('reports.reportTypes.ACTIVE_TRANSACTIONS')}</Option>
                  <Option value="COMMISSION_REPORT">{t('reports.reportTypes.COMMISSION_REPORT')}</Option>
                  <Option value="AUDIT_REPORT">{t('reports.reportTypes.AUDIT_TRAIL')}</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="format"
                label={t('reports.formLabels.format')}
                rules={[{ required: true, message: t('reports.validation.selectFormat') }]}
              >
                <Select placeholder={t('reports.placeholders.selectFormat')}>
                  <Option value="CSV">{t('reports.formats.CSV')}</Option>
                  <Option value="PDF">{t('reports.formats.PDF')}</Option>
                  <Option value="EXCEL">{t('reports.formats.EXCEL')}</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="frequency"
            label={t('reports.formLabels.scheduleFrequency')}
            rules={[{ required: true, message: t('reports.validation.selectFrequency') }]}
          >
            <Select placeholder={t('reports.placeholders.selectFrequency')}>
              <Option value="daily">{t('reports.scheduleOptions.DAILY')}</Option>
              <Option value="weekly">{t('reports.scheduleOptions.WEEKLY')}</Option>
              <Option value="monthly">{t('reports.scheduleOptions.MONTHLY')}</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="recipients"
            label={t('reports.formLabels.emailRecipients')}
          >
            <TextArea 
              rows={2} 
              placeholder={t('reports.placeholders.enterEmailAddresses')}
            />
          </Form.Item>

          <Row justify="end" gutter={8}>
            <Col>
              <Button onClick={() => setIsScheduleModalVisible(false)}>
                {t('reports.buttons.cancel')}
              </Button>
            </Col>
            <Col>
              <Button type="primary" htmlType="submit">
                {t('reports.buttons.scheduleReport')}
              </Button>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default ReportsModule;

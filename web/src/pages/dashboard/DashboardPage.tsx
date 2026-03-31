import { Typography, Spin, Row, Col, Card, Table, Tag, Alert } from 'antd';
import { useQuery } from '@tanstack/react-query';
import api from '@/shared/api/axiosInstance';
import { useAuth } from '@/shared/hooks/useAuth';
import StatCards from '@/features/dashboard/components/StatCards';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const recentColumns = [
  { title: 'Mã NV', dataIndex: 'employeeCode', key: 'code' },
  { title: 'Loại', dataIndex: 'checkType', key: 'type',
    render: (t: string) => t === 'CHECK_IN' ? <Tag color="green">Vào</Tag> : <Tag color="blue">Ra</Tag> },
  { title: 'Thời gian', dataIndex: 'checkTime', key: 'time',
    render: (t: string) => dayjs(t).format('HH:mm:ss') },
  { title: 'Phương thức', dataIndex: 'verificationMethod', key: 'method',
    render: (m: string) => <Tag>{m}</Tag> },
  { title: 'Trạng thái', dataIndex: 'status', key: 'status',
    render: (s: string) => {
      const color = s === 'VALID' ? 'green' : s === 'SUSPICIOUS' ? 'red' : 'orange';
      return <Tag color={color}>{s}</Tag>;
    }
  },
];

export default function DashboardPage() {
  const { user } = useAuth();

  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['dashboard-metrics'],
    queryFn: () => api.get('/dashboard/metrics').then((r) => r.data.data),
    refetchInterval: 30000,
  });

  const { data: recentActivity } = useQuery({
    queryKey: ['attendance-today'],
    queryFn: () => api.get('/attendance/today').then((r) => r.data.data).catch(() => []),
  });

  const isManager = ['SYSTEM_ADMIN', 'CEO', 'DIVISION_DIRECTOR', 'REGION_DIRECTOR', 'DEPT_HEAD', 'DEPUTY_HEAD', 'UNIT_HEAD'].includes(user?.role || '');

  if (metricsLoading) return <div style={{ textAlign: 'center', padding: 50 }}><Spin size="large" /></div>;

  return (
    <div>
      <Title level={4}>Tổng quan — {dayjs().format('DD/MM/YYYY')}</Title>
      <Text type="secondary">Xin chào, {user?.fullName}! Vai trò: {user?.role}</Text>

      <div style={{ marginTop: 20 }}>
        <StatCards metrics={metrics || { presentToday: 0, lateToday: 0, onLeave: 0, pendingApprovals: 0 }} />
      </div>

      {isManager && (
        <Row gutter={16} style={{ marginTop: 24 }}>
          <Col span={16}>
            <Card title="Hoạt động chấm công gần đây">
              <Table
                columns={recentColumns}
                dataSource={recentActivity || []}
                rowKey="id"
                size="small"
                pagination={{ pageSize: 10 }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card title="Thông tin nhanh">
              <div style={{ marginBottom: 12 }}>
                <Text strong>Tỷ lệ chấm công:</Text>
                <Title level={3} style={{ margin: 0, color: '#3f8600' }}>
                  {metrics?.attendanceRate || 0}%
                </Title>
              </div>
              <div style={{ marginBottom: 12 }}>
                <Text strong>Tỷ lệ đúng giờ:</Text>
                <Title level={3} style={{ margin: 0, color: '#1677ff' }}>
                  {metrics?.onTimeRate || 0}%
                </Title>
              </div>
              {(metrics?.suspiciousRecords || 0) > 0 && (
                <Alert
                  message={`${metrics.suspiciousRecords} bản ghi đáng ngờ`}
                  type="warning"
                  showIcon
                  style={{ marginTop: 12 }}
                />
              )}
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}

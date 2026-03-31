import { Typography, Table, Tag, DatePicker, Space, Button } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/shared/api/axiosInstance';
import dayjs, { Dayjs } from 'dayjs';
import { DownloadOutlined } from '@ant-design/icons';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const columns = [
  { title: 'Ngày', dataIndex: 'checkTime', key: 'date',
    render: (t: string) => dayjs(t).format('DD/MM/YYYY') },
  { title: 'Loại', dataIndex: 'checkType', key: 'type',
    render: (t: string) => t === 'CHECK_IN' ? <Tag color="green">Vào</Tag> : <Tag color="blue">Ra</Tag> },
  { title: 'Giờ', dataIndex: 'checkTime', key: 'time',
    render: (t: string) => dayjs(t).format('HH:mm:ss') },
  { title: 'Địa điểm', dataIndex: 'locationId', key: 'location' },
  { title: 'WiFi BSSID', dataIndex: 'wifiBssid', key: 'bssid' },
  { title: 'Phương thức', dataIndex: 'verificationMethod', key: 'method',
    render: (m: string) => <Tag>{m}</Tag> },
  { title: 'Trạng thái', dataIndex: 'status', key: 'status',
    render: (s: string) => {
      const colors: Record<string, string> = { VALID: 'green', SUSPICIOUS: 'red', REJECTED: 'volcano', OFFLINE_SYNCED: 'orange' };
      return <Tag color={colors[s] || 'default'}>{s}</Tag>;
    }
  },
  { title: 'Fraud Score', dataIndex: 'fraudScore', key: 'fraud',
    render: (s: number) => <span style={{ color: s >= 70 ? 'red' : s >= 30 ? 'orange' : 'green' }}>{s}</span> },
];

export default function HistoryPage() {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().startOf('month'), dayjs()
  ]);

  const { data, isLoading } = useQuery({
    queryKey: ['attendance-history', dateRange[0].format('YYYY-MM-DD'), dateRange[1].format('YYYY-MM-DD')],
    queryFn: () => api.get('/attendance/history', {
      params: { from: dateRange[0].format('YYYY-MM-DD'), to: dateRange[1].format('YYYY-MM-DD') }
    }).then((r) => r.data.data),
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}>Lịch sử chấm công</Title>
        <Space>
          <RangePicker
            value={dateRange}
            onChange={(dates) => dates && setDateRange(dates as [Dayjs, Dayjs])}
          />
          <Button icon={<DownloadOutlined />}>Xuất Excel</Button>
        </Space>
      </div>
      <Table columns={columns} dataSource={data || []} loading={isLoading} rowKey="id" />
    </div>
  );
}

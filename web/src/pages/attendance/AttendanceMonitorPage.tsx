import { Table, Tag, Typography, DatePicker } from 'antd';
import { useQuery } from '@tanstack/react-query';
import api from '@/shared/api/axiosInstance';
import dayjs from 'dayjs';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const columns = [
  { title: 'Mã NV', dataIndex: 'employeeCode', key: 'employeeCode' },
  { title: 'Loại', dataIndex: 'checkType', key: 'checkType',
    render: (t: string) => t === 'CHECK_IN' ? 'Vào' : 'Ra' },
  { title: 'Thời gian', dataIndex: 'checkTime', key: 'checkTime',
    render: (t: string) => dayjs(t).format('DD/MM/YYYY HH:mm') },
  { title: 'Phương thức', dataIndex: 'verificationMethod', key: 'method' },
  { title: 'Trạng thái', dataIndex: 'status', key: 'status',
    render: (s: string) => {
      const color = s === 'VALID' ? 'green' : s === 'SUSPICIOUS' ? 'red' : 'orange';
      return <Tag color={color}>{s}</Tag>;
    }
  },
  { title: 'Fraud Score', dataIndex: 'fraudScore', key: 'fraudScore' },
];

export default function AttendanceMonitorPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['attendance-today'],
    queryFn: () => api.get('/attendance/today').then((r) => r.data.data),
  });

  return (
    <div>
      <Title level={4}>Giám sát chấm công</Title>
      <RangePicker style={{ marginBottom: 16 }} />
      <Table columns={columns} dataSource={data || []} loading={isLoading} rowKey="id" />
    </div>
  );
}

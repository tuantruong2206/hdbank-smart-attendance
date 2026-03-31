import { Typography, Table, Tag, Button } from 'antd';

const { Title } = Typography;

const columns = [
  { title: 'Loại nghỉ', dataIndex: 'leaveType', key: 'leaveType' },
  { title: 'Từ ngày', dataIndex: 'startDate', key: 'startDate' },
  { title: 'Đến ngày', dataIndex: 'endDate', key: 'endDate' },
  { title: 'Lý do', dataIndex: 'reason', key: 'reason' },
  { title: 'Trạng thái', dataIndex: 'status', key: 'status',
    render: (s: string) => {
      const colors: Record<string, string> = { PENDING: 'gold', APPROVED: 'green', REJECTED: 'red' };
      return <Tag color={colors[s] || 'default'}>{s}</Tag>;
    }
  },
];

export default function LeaveRequestListPage() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}>Nghỉ phép</Title>
        <Button type="primary">Tạo đơn mới</Button>
      </div>
      <Table columns={columns} dataSource={[]} rowKey="id" />
    </div>
  );
}

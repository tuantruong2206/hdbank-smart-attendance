import { Card, Spin, Table, Tag } from 'antd';
import { SolutionOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { TeamMemberStatus } from '../../types';
import dayjs from 'dayjs';

interface Props {
  query: UseQueryResult<TeamMemberStatus[]>;
}

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PRESENT: { label: 'Co mat', color: 'green' },
  ABSENT: { label: 'Vang', color: 'red' },
  LATE: { label: 'Tre', color: 'orange' },
  ON_LEAVE: { label: 'Nghi phep', color: 'blue' },
};

const columns = [
  { title: 'Ma NV', dataIndex: 'employeeCode', key: 'code', width: 90 },
  { title: 'Ho ten', dataIndex: 'fullName', key: 'name', ellipsis: true },
  {
    title: 'Trang thai',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    render: (s: string) => (
      <Tag color={STATUS_MAP[s]?.color || 'default'}>{STATUS_MAP[s]?.label || s}</Tag>
    ),
  },
  {
    title: 'Gio vao',
    dataIndex: 'checkInTime',
    key: 'time',
    width: 80,
    render: (t: string | null) => (t ? dayjs(t).format('HH:mm') : '--:--'),
  },
];

export default function TeamPulseTable({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Trang thai nhom" size="small" extra={<SolutionOutlined />}>
      {isLoading ? (
        <Spin size="small" />
      ) : (
        <Table
          columns={columns}
          dataSource={data || []}
          rowKey="employeeCode"
          size="small"
          pagination={{ pageSize: 5, size: 'small' }}
          scroll={{ x: 360 }}
        />
      )}
    </Card>
  );
}

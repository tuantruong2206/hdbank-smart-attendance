import { Card, Spin, Table } from 'antd';
import { FileSearchOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { AuditLogEntry } from '../../types';
import dayjs from 'dayjs';

interface Props {
  query: UseQueryResult<AuditLogEntry[]>;
}

const columns = [
  { title: 'Hanh dong', dataIndex: 'action', key: 'action', ellipsis: true },
  { title: 'Nguoi thuc hien', dataIndex: 'performedBy', key: 'by', ellipsis: true },
  { title: 'Doi tuong', dataIndex: 'target', key: 'target', ellipsis: true },
  {
    title: 'Thoi gian',
    dataIndex: 'timestamp',
    key: 'time',
    width: 130,
    render: (t: string) => dayjs(t).format('HH:mm DD/MM/YYYY'),
  },
];

export default function RecentAuditLog({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Nhat ky he thong gan day" size="small" extra={<FileSearchOutlined />}>
      {isLoading ? (
        <Spin size="small" />
      ) : (
        <Table
          columns={columns}
          dataSource={data || []}
          rowKey="id"
          size="small"
          pagination={false}
          scroll={{ x: 500 }}
        />
      )}
    </Card>
  );
}

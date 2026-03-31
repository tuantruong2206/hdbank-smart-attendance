import { Card, Spin, Statistic, Space } from 'antd';
import { AuditOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { PendingApprovalsCountData } from '../../types';

interface Props {
  query: UseQueryResult<PendingApprovalsCountData>;
}

export default function PendingApprovalsCount({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Cho duyet" size="small" extra={<AuditOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Statistic
            value={data.total}
            suffix="yeu cau"
            valueStyle={{ color: data.total > 0 ? '#faad14' : '#3f8600' }}
          />
          <div style={{ fontSize: 12, color: '#888' }}>
            Nghi phep: {data.leaveApprovals} | Bang cong: {data.timesheetApprovals}
          </div>
        </Space>
      )}
    </Card>
  );
}

import { Card, Statistic, Spin } from 'antd';
import { FileSearchOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { PendingLeaveRequestsData } from '../../types';

interface Props {
  query: UseQueryResult<PendingLeaveRequestsData>;
}

export default function PendingLeaveRequests({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Don phep cho duyet" size="small" extra={<FileSearchOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Statistic
          value={data.count}
          suffix="don"
          valueStyle={{ color: data.count > 0 ? '#faad14' : '#3f8600' }}
        />
      )}
    </Card>
  );
}

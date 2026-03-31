import { Card, Statistic, Spin } from 'antd';
import { FieldTimeOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LateCountData } from '../../types';

interface Props {
  query: UseQueryResult<LateCountData>;
}

export default function LateCount({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="So lan di tre" size="small" extra={<FieldTimeOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Statistic
          value={data.lateCount}
          suffix="lan"
          valueStyle={{ color: data.lateCount > 0 ? '#cf1322' : '#3f8600' }}
        />
      )}
    </Card>
  );
}

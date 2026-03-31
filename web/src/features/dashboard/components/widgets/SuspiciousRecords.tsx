import { Card, Spin, Statistic } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { SuspiciousRecordsData } from '../../types';

interface Props {
  query: UseQueryResult<SuspiciousRecordsData>;
}

export default function SuspiciousRecords({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Ban ghi dang ngo tuan nay" size="small" extra={<WarningOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Statistic
          value={data.count}
          suffix="ban ghi"
          valueStyle={{ color: data.count > 0 ? '#cf1322' : '#3f8600' }}
        />
      )}
    </Card>
  );
}

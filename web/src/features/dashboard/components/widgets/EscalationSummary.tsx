import { Card, Spin, Statistic, Space, Tag } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { EscalationSummaryData } from '../../types';

interface Props {
  query: UseQueryResult<EscalationSummaryData>;
}

export default function EscalationSummary({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Leo thang dang xu ly" size="small" extra={<ExclamationCircleOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <>
          <Statistic
            value={data.total}
            suffix="vu"
            valueStyle={{ color: data.total > 0 ? '#cf1322' : '#3f8600' }}
          />
          <Space style={{ marginTop: 8 }}>
            <Tag color="green">Cap 1: {data.level1}</Tag>
            <Tag color="orange">Cap 2: {data.level2}</Tag>
            <Tag color="red">Cap 3: {data.level3}</Tag>
          </Space>
        </>
      )}
    </Card>
  );
}

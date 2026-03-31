import { Card, Spin, Typography, Tag } from 'antd';
import { SafetyOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LateGraceQuotaData } from '../../types';

const { Title } = Typography;

interface Props {
  query: UseQueryResult<LateGraceQuotaData>;
}

export default function LateGraceQuota({ query }: Props) {
  const { data, isLoading } = query;

  const remaining = data ? data.total - data.used : 0;
  const color = remaining === 0 ? 'red' : remaining === 1 ? 'orange' : 'green';
  const label = remaining === 0 ? 'Da het' : remaining === 1 ? 'Con 1 lan' : `Con ${remaining} lan`;

  return (
    <Card title="Tre co phep" size="small" extra={<SafetyOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <div style={{ textAlign: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>
            {data.used}/{data.total}
          </Title>
          <Tag color={color} style={{ marginTop: 8, fontSize: 13 }}>
            {label}
          </Tag>
        </div>
      )}
    </Card>
  );
}

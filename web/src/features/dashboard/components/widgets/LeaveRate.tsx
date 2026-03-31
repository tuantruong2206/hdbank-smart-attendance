import { Card, Spin, Progress, Typography } from 'antd';
import { FileTextOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LeaveRateData } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<LeaveRateData>;
}

export default function LeaveRate({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Ty le nghi phep to chuc" size="small" extra={<FileTextOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <div style={{ textAlign: 'center' }}>
          <Progress
            type="circle"
            percent={Math.round(data.rate)}
            size={90}
            strokeColor={data.rate > 10 ? '#faad14' : '#3f8600'}
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">ty le nghi phep</Text>
          </div>
        </div>
      )}
    </Card>
  );
}

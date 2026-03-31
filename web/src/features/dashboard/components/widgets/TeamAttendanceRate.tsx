import { Card, Spin, Progress, Typography } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { TeamAttendanceRateData } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<TeamAttendanceRateData>;
}

export default function TeamAttendanceRate({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Ty le co mat nhom" size="small" extra={<TeamOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <div style={{ textAlign: 'center' }}>
          <Progress
            type="dashboard"
            percent={Math.round(data.rate)}
            size={100}
            strokeColor={data.rate >= 90 ? '#3f8600' : data.rate >= 70 ? '#faad14' : '#cf1322'}
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">{data.presentCount}/{data.totalCount} nhan vien</Text>
          </div>
        </div>
      )}
    </Card>
  );
}

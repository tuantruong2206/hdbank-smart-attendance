import { Card, Spin, Statistic, Typography } from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { TeamOTSummaryData } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<TeamOTSummaryData>;
}

export default function TeamOTSummary({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Tang ca nhom" size="small" extra={<ThunderboltOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <>
          <Statistic value={data.totalHours} suffix="gio" precision={1} valueStyle={{ color: '#1677ff' }} />
          <Text type="secondary" style={{ fontSize: 12 }}>{data.employeeCount} nhan vien tang ca</Text>
        </>
      )}
    </Card>
  );
}

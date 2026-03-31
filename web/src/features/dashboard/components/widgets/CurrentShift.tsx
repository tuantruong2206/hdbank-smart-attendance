import { Card, Spin, Typography, Space } from 'antd';
import { ScheduleOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { CurrentShiftData } from '../../types';

const { Text, Title } = Typography;

interface Props {
  query: UseQueryResult<CurrentShiftData>;
}

export default function CurrentShift({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Ca lam viec hom nay" size="small" extra={<ScheduleOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Space direction="vertical">
          <Title level={5} style={{ margin: 0 }}>{data.shiftName}</Title>
          <Text type="secondary">{data.startTime} - {data.endTime}</Text>
        </Space>
      )}
    </Card>
  );
}

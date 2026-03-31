import { Card, Tag, Typography, Space, Spin } from 'antd';
import { ClockCircleOutlined, LoginOutlined, LogoutOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { TodayStatusData } from '../../types';
import dayjs from 'dayjs';

const { Text } = Typography;

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PRESENT: { label: 'Co mat', color: 'green' },
  ABSENT: { label: 'Vang mat', color: 'red' },
  LATE: { label: 'Di tre', color: 'orange' },
  ON_LEAVE: { label: 'Nghi phep', color: 'blue' },
  NOT_CHECKED_IN: { label: 'Chua cham cong', color: 'default' },
};

interface Props {
  query: UseQueryResult<TodayStatusData>;
}

export default function TodayStatus({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Trang thai hom nay" size="small" extra={<ClockCircleOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Tag color={STATUS_MAP[data.status]?.color || 'default'} style={{ fontSize: 14 }}>
            {STATUS_MAP[data.status]?.label || data.status}
          </Tag>
          <Space>
            <LoginOutlined />
            <Text>Vao: {data.checkInTime ? dayjs(data.checkInTime).format('HH:mm') : '--:--'}</Text>
          </Space>
          <Space>
            <LogoutOutlined />
            <Text>Ra: {data.checkOutTime ? dayjs(data.checkOutTime).format('HH:mm') : '--:--'}</Text>
          </Space>
        </Space>
      )}
    </Card>
  );
}

import { Card, Spin, List, Badge, Typography } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { NotificationItem } from '../../types';
import dayjs from 'dayjs';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<NotificationItem[]>;
}

export default function RecentNotifications({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Thong bao gan day" size="small" extra={<BellOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <List
          size="small"
          dataSource={data.slice(0, 5)}
          renderItem={(item) => (
            <List.Item>
              <div style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Badge dot={!item.read} offset={[4, 0]}>
                    <Text strong style={{ fontSize: 13 }}>{item.title}</Text>
                  </Badge>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    {dayjs(item.createdAt).format('HH:mm DD/MM')}
                  </Text>
                </div>
                <Text type="secondary" style={{ fontSize: 12 }}>{item.message}</Text>
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}

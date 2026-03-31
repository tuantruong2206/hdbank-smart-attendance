import { Typography, List, Badge, Tag, Empty } from 'antd';
import { BellOutlined, MailOutlined, MessageOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import api from '@/shared/api/axiosInstance';
import dayjs from 'dayjs';

const { Title } = Typography;

const typeIcons: Record<string, any> = {
  PUSH: <BellOutlined />,
  EMAIL: <MailOutlined />,
  SMS: <MessageOutlined />,
};

export default function NotificationCenterPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => api.get('/notifications/me').then((r) => r.data.data).catch(() => []),
  });

  return (
    <div>
      <Title level={4}>Trung tâm thông báo</Title>
      {(!data || data.length === 0) && !isLoading ? (
        <Empty description="Không có thông báo" />
      ) : (
        <List
          loading={isLoading}
          itemLayout="horizontal"
          dataSource={data || []}
          renderItem={(item: any) => (
            <List.Item>
              <List.Item.Meta
                avatar={<Badge dot>{typeIcons[item.type] || <BellOutlined />}</Badge>}
                title={<>{item.title} <Tag>{item.type}</Tag></>}
                description={
                  <div>
                    <div>{item.body}</div>
                    <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>
                      {dayjs(item.createdAt).format('DD/MM/YYYY HH:mm')}
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  );
}

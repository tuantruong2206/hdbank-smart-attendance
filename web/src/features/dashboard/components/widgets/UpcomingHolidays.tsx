import { Card, Spin, List, Typography } from 'antd';
import { GiftOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { HolidayItem } from '../../types';
import dayjs from 'dayjs';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<HolidayItem[]>;
}

export default function UpcomingHolidays({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Ngay le sap toi" size="small" extra={<GiftOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <List
          size="small"
          dataSource={data.slice(0, 3)}
          renderItem={(item) => (
            <List.Item>
              <Text>{item.name}</Text>
              <Text type="secondary">{dayjs(item.date).format('DD/MM/YYYY')}</Text>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}

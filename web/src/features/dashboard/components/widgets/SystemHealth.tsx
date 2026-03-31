import { Card, Spin, List, Badge, Typography } from 'antd';
import { CloudServerOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ServiceStatusItem } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<ServiceStatusItem[]>;
}

export default function SystemHealth({ query }: Props) {
  const { data, isLoading } = query;

  const upCount = data?.filter((s) => s.status === 'UP').length ?? 0;
  const totalCount = data?.length ?? 0;

  return (
    <Card
      title="Trang thai he thong"
      size="small"
      extra={<CloudServerOutlined />}
    >
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <>
          <div style={{ marginBottom: 8 }}>
            <Text type="secondary">{upCount}/{totalCount} dich vu hoat dong</Text>
          </div>
          <List
            size="small"
            dataSource={data}
            renderItem={(item) => (
              <List.Item>
                <Badge
                  status={item.status === 'UP' ? 'success' : 'error'}
                  text={item.name}
                />
              </List.Item>
            )}
          />
        </>
      )}
    </Card>
  );
}

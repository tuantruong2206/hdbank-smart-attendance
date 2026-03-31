import { Card, Spin, List, Typography } from 'antd';
import { UserDeleteOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { AbsentEmployeeItem } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<AbsentEmployeeItem[]>;
}

export default function AbsentList({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Vang mat khong phep" size="small" extra={<UserDeleteOutlined />}>
      {isLoading ? (
        <Spin size="small" />
      ) : !data || data.length === 0 ? (
        <Text type="secondary">Khong co</Text>
      ) : (
        <List
          size="small"
          dataSource={data}
          renderItem={(item) => (
            <List.Item>
              <div>
                <Text strong>{item.fullName}</Text>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {item.employeeCode} - {item.department}
                </Text>
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}

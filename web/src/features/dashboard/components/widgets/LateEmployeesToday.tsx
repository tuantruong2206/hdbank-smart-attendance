import { Card, Spin, List, Typography, Tag } from 'antd';
import { FieldTimeOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LateEmployeeItem } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<LateEmployeeItem[]>;
}

export default function LateEmployeesToday({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Nhan vien di tre hom nay" size="small" extra={<FieldTimeOutlined />}>
      {isLoading ? (
        <Spin size="small" />
      ) : !data || data.length === 0 ? (
        <Text type="secondary">Khong co nhan vien di tre</Text>
      ) : (
        <List
          size="small"
          dataSource={data}
          renderItem={(item) => (
            <List.Item>
              <Text>{item.fullName} ({item.employeeCode})</Text>
              <Tag color="orange">Tre {item.minutesLate} phut</Tag>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}

import { Card, Spin, Space, Typography, Progress } from 'antd';
import { FileTextOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LeaveBalanceData } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<LeaveBalanceData>;
}

export default function LeaveBalance({ query }: Props) {
  const { data, isLoading } = query;

  const items = data
    ? [
        { label: 'Phep nam', remaining: data.annual, total: data.annualTotal },
        { label: 'Nghi om', remaining: data.sick, total: data.sickTotal },
        { label: 'Nghi rieng', remaining: data.personal, total: data.personalTotal },
      ]
    : [];

  return (
    <Card title="So du ngay phep" size="small" extra={<FileTextOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          {items.map((item) => (
            <div key={item.label}>
              <Text>{item.label}: {item.remaining}/{item.total}</Text>
              <Progress
                percent={Math.round((item.remaining / item.total) * 100)}
                size="small"
                strokeColor={item.remaining <= 2 ? '#cf1322' : '#1677ff'}
              />
            </div>
          ))}
        </Space>
      )}
    </Card>
  );
}

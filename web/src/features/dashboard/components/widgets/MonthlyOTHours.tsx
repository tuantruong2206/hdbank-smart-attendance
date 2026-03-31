import { Card, Statistic, Spin } from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { MonthlyOTHoursData } from '../../types';

interface Props {
  query: UseQueryResult<MonthlyOTHoursData>;
}

export default function MonthlyOTHours({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Gio tang ca thang nay" size="small" extra={<ThunderboltOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Statistic value={data.otHours} suffix="gio" precision={1} valueStyle={{ color: '#1677ff' }} />
      )}
    </Card>
  );
}

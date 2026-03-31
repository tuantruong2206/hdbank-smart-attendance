import { Card, Spin, Statistic } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { PendingConfigChangesData } from '../../types';

interface Props {
  query: UseQueryResult<PendingConfigChangesData>;
}

export default function PendingConfigChanges({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Cau hinh cho duyet" size="small" extra={<SettingOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Statistic
          value={data.count}
          suffix="thay doi"
          valueStyle={{ color: data.count > 0 ? '#faad14' : '#3f8600' }}
        />
      )}
    </Card>
  );
}

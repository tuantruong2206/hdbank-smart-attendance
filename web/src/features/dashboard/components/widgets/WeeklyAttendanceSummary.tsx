import { Card, Spin, Tag, Space } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { WeeklyDayStatus } from '../../types';

interface Props {
  query: UseQueryResult<WeeklyDayStatus[]>;
}

const STATUS_COLOR: Record<string, string> = {
  PRESENT: 'green',
  ABSENT: 'red',
  LATE: 'orange',
  LEAVE: 'blue',
  WEEKEND: 'default',
  NOT_YET: 'default',
};

const STATUS_LABEL: Record<string, string> = {
  PRESENT: 'Co mat',
  ABSENT: 'Vang',
  LATE: 'Tre',
  LEAVE: 'Phep',
  WEEKEND: 'Nghi',
  NOT_YET: '-',
};

export default function WeeklyAttendanceSummary({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Tong hop tuan nay" size="small" extra={<BarChartOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <Space wrap>
          {data.map((d) => (
            <div key={d.day} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 12, marginBottom: 4 }}>{d.day}</div>
              <Tag color={STATUS_COLOR[d.status] || 'default'}>
                {STATUS_LABEL[d.status] || d.status}
              </Tag>
            </div>
          ))}
        </Space>
      )}
    </Card>
  );
}

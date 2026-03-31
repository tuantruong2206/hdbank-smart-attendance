import { Card, Progress, Spin, Typography } from 'antd';
import { CalendarOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { PersonalAttendanceCountData } from '../../types';

const { Text } = Typography;

interface Props {
  query: UseQueryResult<PersonalAttendanceCountData>;
}

export default function PersonalAttendanceCount({ query }: Props) {
  const { data, isLoading } = query;
  const percent = data ? Math.round((data.workedDays / data.totalWorkDays) * 100) : 0;

  return (
    <Card title="Ngay cong thang nay" size="small" extra={<CalendarOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <>
          <Progress
            type="circle"
            percent={percent}
            size={80}
            format={() => `${data.workedDays}/${data.totalWorkDays}`}
          />
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">ngay</Text>
          </div>
        </>
      )}
    </Card>
  );
}

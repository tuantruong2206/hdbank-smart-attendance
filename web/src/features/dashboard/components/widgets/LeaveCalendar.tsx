import { Card, Spin, Table, Tag } from 'antd';
import { CalendarOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { LeaveCalendarItem } from '../../types';
import dayjs from 'dayjs';

interface Props {
  query: UseQueryResult<LeaveCalendarItem[]>;
}

export default function LeaveCalendar({ query }: Props) {
  const { data, isLoading } = query;

  const columns = [
    { title: 'Ho ten', dataIndex: 'fullName', key: 'name', ellipsis: true },
    {
      title: 'Loai',
      dataIndex: 'leaveType',
      key: 'type',
      width: 80,
      render: (t: string) => <Tag>{t}</Tag>,
    },
    {
      title: 'Ngay nghi',
      dataIndex: 'dates',
      key: 'dates',
      render: (dates: string[]) => dates.map((d) => dayjs(d).format('DD/MM')).join(', '),
    },
  ];

  return (
    <Card title="Lich nghi phep tuan nay" size="small" extra={<CalendarOutlined />}>
      {isLoading ? (
        <Spin size="small" />
      ) : (
        <Table
          columns={columns}
          dataSource={data || []}
          rowKey="employeeCode"
          size="small"
          pagination={false}
          scroll={{ x: 300 }}
        />
      )}
    </Card>
  );
}

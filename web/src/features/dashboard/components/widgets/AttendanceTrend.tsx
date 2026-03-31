import { Card, Spin } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { AttendanceTrendPoint } from '../../types';
import dayjs from 'dayjs';

interface Props {
  query: UseQueryResult<AttendanceTrendPoint[]>;
}

export default function AttendanceTrend({ query }: Props) {
  const { data, isLoading } = query;

  const option = {
    tooltip: { trigger: 'axis' as const },
    xAxis: {
      type: 'category' as const,
      data: (data || []).map((d) => dayjs(d.date).format('DD/MM')),
      axisLabel: { fontSize: 10 },
    },
    yAxis: { type: 'value' as const, min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: 'Ty le cham cong',
        type: 'line',
        data: (data || []).map((d) => d.rate),
        smooth: true,
        areaStyle: { opacity: 0.2, color: '#1677ff' },
        lineStyle: { color: '#1677ff' },
        itemStyle: { color: '#1677ff' },
      },
    ],
    grid: { top: 10, right: 10, bottom: 20, left: 40 },
  };

  return (
    <Card title="Xu huong cham cong 30 ngay" size="small" extra={<LineChartOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <ReactECharts option={option} style={{ height: 250 }} />
      )}
    </Card>
  );
}

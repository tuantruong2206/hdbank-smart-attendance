import { Card, Spin } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { OrgAttendanceRateData } from '../../types';

interface Props {
  query: UseQueryResult<OrgAttendanceRateData>;
}

export default function OrgAttendanceRate({ query }: Props) {
  const { data, isLoading } = query;

  const option = {
    series: [
      {
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        pointer: { show: true, length: '60%' },
        axisLine: {
          lineStyle: {
            width: 20,
            color: [
              [0.6, '#cf1322'],
              [0.8, '#faad14'],
              [1, '#3f8600'],
            ],
          },
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: {
          fontSize: 24,
          offsetCenter: [0, '70%'],
          formatter: '{value}%',
        },
        data: [{ value: data?.rate ?? 0, name: 'Ty le co mat' }],
      },
    ],
  };

  return (
    <Card title="Ty le cham cong toan to chuc" size="small" extra={<GlobalOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <ReactECharts option={option} style={{ height: 220 }} />
      )}
    </Card>
  );
}

import { Card, Spin } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { BranchComparisonItem } from '../../types';

interface Props {
  query: UseQueryResult<BranchComparisonItem[]>;
}

export default function BranchComparison({ query }: Props) {
  const { data, isLoading } = query;

  const option = {
    tooltip: { trigger: 'axis' as const },
    xAxis: {
      type: 'category' as const,
      data: (data || []).map((d) => d.branchName),
      axisLabel: { rotate: 30, fontSize: 10 },
    },
    yAxis: { type: 'value' as const, min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        type: 'bar',
        data: (data || []).map((d) => ({
          value: d.rate,
          itemStyle: {
            color: d.rate >= 90 ? '#3f8600' : d.rate >= 70 ? '#faad14' : '#cf1322',
          },
        })),
        barMaxWidth: 40,
      },
    ],
    grid: { top: 10, right: 10, bottom: 50, left: 40 },
  };

  return (
    <Card title="So sanh chi nhanh" size="small" extra={<BarChartOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <ReactECharts option={option} style={{ height: 250 }} />
      )}
    </Card>
  );
}

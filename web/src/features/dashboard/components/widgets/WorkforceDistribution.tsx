import { Card, Spin } from 'antd';
import { PieChartOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import type { UseQueryResult } from '@tanstack/react-query';
import type { WorkforceDistributionItem } from '../../types';

interface Props {
  query: UseQueryResult<WorkforceDistributionItem[]>;
}

export default function WorkforceDistribution({ query }: Props) {
  const { data, isLoading } = query;

  const option = {
    tooltip: { trigger: 'item' as const, formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' as const, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '42%'],
        label: { show: false },
        data: (data || []).map((d) => ({ name: d.type, value: d.count })),
      },
    ],
  };

  return (
    <Card title="Phan bo nhan su" size="small" extra={<PieChartOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <ReactECharts option={option} style={{ height: 250 }} />
      )}
    </Card>
  );
}

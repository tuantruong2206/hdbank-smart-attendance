import { Card, Spin, Statistic, Row, Col } from 'antd';
import { AlertOutlined } from '@ant-design/icons';
import type { UseQueryResult } from '@tanstack/react-query';
import type { AnomalySummaryData } from '../../types';

interface Props {
  query: UseQueryResult<AnomalySummaryData>;
}

export default function AnomalySummary({ query }: Props) {
  const { data, isLoading } = query;

  return (
    <Card title="Bat thuong phat hien" size="small" extra={<AlertOutlined />}>
      {isLoading || !data ? (
        <Spin size="small" />
      ) : (
        <>
          <Statistic
            value={data.total}
            suffix="truong hop"
            valueStyle={{ color: data.total > 0 ? '#cf1322' : '#3f8600', marginBottom: 12 }}
          />
          <Row gutter={8}>
            <Col span={12}>
              <div style={{ fontSize: 12, color: '#888' }}>Buddy punching: {data.buddyPunching}</div>
              <div style={{ fontSize: 12, color: '#888' }}>Vi tri: {data.location}</div>
            </Col>
            <Col span={12}>
              <div style={{ fontSize: 12, color: '#888' }}>Thoi gian: {data.time}</div>
              <div style={{ fontSize: 12, color: '#888' }}>Thiet bi: {data.device}</div>
            </Col>
          </Row>
        </>
      )}
    </Card>
  );
}

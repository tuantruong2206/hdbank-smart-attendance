import { Row, Col, Card, Statistic } from 'antd';
import { TeamOutlined, ClockCircleOutlined, FileTextOutlined, WarningOutlined } from '@ant-design/icons';

interface Props {
  metrics: {
    presentToday: number;
    lateToday: number;
    onLeave: number;
    pendingApprovals: number;
  };
}

export default function StatCards({ metrics }: Props) {
  return (
    <Row gutter={16}>
      <Col span={6}>
        <Card>
          <Statistic
            title="Có mặt hôm nay"
            value={metrics.presentToday}
            prefix={<TeamOutlined />}
            valueStyle={{ color: '#3f8600' }}
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="Đi trễ"
            value={metrics.lateToday}
            prefix={<ClockCircleOutlined />}
            valueStyle={{ color: '#cf1322' }}
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="Nghỉ phép"
            value={metrics.onLeave}
            prefix={<FileTextOutlined />}
            valueStyle={{ color: '#1677ff' }}
          />
        </Card>
      </Col>
      <Col span={6}>
        <Card>
          <Statistic
            title="Chờ duyệt"
            value={metrics.pendingApprovals}
            prefix={<WarningOutlined />}
            valueStyle={{ color: '#faad14' }}
          />
        </Card>
      </Col>
    </Row>
  );
}

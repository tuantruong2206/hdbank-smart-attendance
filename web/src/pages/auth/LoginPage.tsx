import { Card, Typography } from 'antd';
import LoginForm from '@/features/auth/components/LoginForm';

const { Title } = Typography;

export default function LoginPage() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5' }}>
      <Card style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3}>Smart Attendance</Title>
          <p style={{ color: '#666' }}>Hệ thống Chấm công Thông minh - HDBank</p>
        </div>
        <LoginForm />
      </Card>
    </div>
  );
}

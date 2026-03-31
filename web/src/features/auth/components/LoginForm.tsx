import { Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import api from '@/shared/api/axiosInstance';
import { useAuth } from '@/shared/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

interface LoginValues {
  email: string;
  password: string;
}

export default function LoginForm() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: (values: LoginValues) =>
      api.post('/auth/login', values).then((res) => res.data.data),
    onSuccess: (data) => {
      if (data.requires2FA) {
        message.info('Vui lòng nhập mã xác thực 2FA');
        return;
      }
      const info = data.employeeInfo || {};
      login(data.accessToken, {
        id: info.id || '',
        email: info.email || '',
        fullName: info.fullName || 'User',
        role: info.role || 'EMPLOYEE',
        employeeCode: info.employeeCode || '',
      });
      navigate('/dashboard');
    },
    onError: () => {
      message.error('Email hoặc mật khẩu không đúng');
    },
  });

  return (
    <Form onFinish={(values) => mutation.mutate(values)} size="large">
      <Form.Item name="email" rules={[{ required: true, message: 'Vui lòng nhập email' }]}>
        <Input prefix={<UserOutlined />} placeholder="Email" />
      </Form.Item>
      <Form.Item name="password" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
        <Input.Password prefix={<LockOutlined />} placeholder="Mật khẩu" />
      </Form.Item>
      <Form.Item>
        <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
          Đăng nhập
        </Button>
      </Form.Item>
    </Form>
  );
}

import { useState } from 'react';
import { Layout, Menu, Avatar, Dropdown, Button, theme } from 'antd';
import {
  DashboardOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  TeamOutlined,
  EnvironmentOutlined,
  BellOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/shared/hooks/useAuth';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Tổng quan' },
  {
    key: 'attendance-group',
    icon: <ClockCircleOutlined />,
    label: 'Chấm công',
    children: [
      { key: '/attendance', label: 'Giám sát' },
      { key: '/attendance/history', label: 'Lịch sử' },
    ],
  },
  { key: '/leaves', icon: <FileTextOutlined />, label: 'Nghỉ phép' },
  { key: '/notifications', icon: <BellOutlined />, label: 'Thông báo' },
  {
    key: 'admin',
    icon: <TeamOutlined />,
    label: 'Quản trị',
    children: [
      { key: '/admin/users', label: 'Nhân viên' },
      { key: '/admin/organizations', label: 'Tổ chức' },
      { key: '/admin/locations', label: 'Địa điểm' },
    ],
  },
];

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { token: { colorBgContainer } } = theme.useToken();

  const userMenu = {
    items: [
      { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: logout },
    ],
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: collapsed ? 14 : 18, fontWeight: 'bold' }}>
          {collapsed ? 'SA' : 'Smart Attendance'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: '0 24px', background: colorBgContainer, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <Dropdown menu={userMenu}>
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar icon={<UserOutlined />} />
              <span>{user?.fullName || 'User'}</span>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: colorBgContainer, borderRadius: 8, minHeight: 360 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

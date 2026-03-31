import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuth } from '@/shared/hooks/useAuth';
import MainLayout from '@/app/layouts/MainLayout';
import LoginPage from '@/pages/auth/LoginPage';

const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'));
const AttendanceMonitorPage = lazy(() => import('@/pages/attendance/AttendanceMonitorPage'));
const HistoryPage = lazy(() => import('@/pages/attendance/HistoryPage'));
const LeaveRequestListPage = lazy(() => import('@/pages/leave/LeaveRequestListPage'));
const UserManagementPage = lazy(() => import('@/pages/admin/UserManagementPage'));
const OrgStructurePage = lazy(() => import('@/pages/admin/OrgStructurePage'));
const LocationConfigPage = lazy(() => import('@/pages/admin/LocationConfigPage'));
const NotificationCenterPage = lazy(() => import('@/pages/notification/NotificationCenterPage'));

const Loading = () => <div style={{ textAlign: 'center', padding: 50 }}><Spin size="large" /></div>;

function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="attendance" element={<AttendanceMonitorPage />} />
          <Route path="attendance/history" element={<HistoryPage />} />
          <Route path="leaves" element={<LeaveRequestListPage />} />
          <Route path="admin/users" element={<UserManagementPage />} />
          <Route path="admin/organizations" element={<OrgStructurePage />} />
          <Route path="admin/locations" element={<LocationConfigPage />} />
          <Route path="notifications" element={<NotificationCenterPage />} />
        </Route>
        <Route path="/login" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
}

export default App;

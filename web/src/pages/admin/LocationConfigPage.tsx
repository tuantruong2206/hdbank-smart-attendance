import { Typography, Table, Button, Modal, Form, Input, InputNumber, Space, Tag } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { PlusOutlined, WifiOutlined } from '@ant-design/icons';
import api from '@/shared/api/axiosInstance';

const { Title } = Typography;

const columns = [
  { title: 'Tên', dataIndex: 'name', key: 'name' },
  { title: 'Địa chỉ', dataIndex: 'address', key: 'address' },
  { title: 'Tòa nhà', dataIndex: 'building', key: 'building' },
  { title: 'Tầng', dataIndex: 'floor', key: 'floor' },
  { title: 'GPS', key: 'gps',
    render: (_: any, r: any) => r.gpsLatitude ? `${r.gpsLatitude}, ${r.gpsLongitude}` : '-' },
  { title: 'Bán kính (m)', dataIndex: 'geofenceRadiusMeters', key: 'radius' },
  { title: 'Trạng thái', dataIndex: 'isActive', key: 'active',
    render: (a: boolean) => <Tag color={a ? 'green' : 'red'}>{a ? 'Hoạt động' : 'Ngưng'}</Tag> },
];

export default function LocationConfigPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['locations'],
    queryFn: () => api.get('/admin/locations').then((r) => r.data.data).catch(() => []),
  });

  const createMutation = useMutation({
    mutationFn: (values: any) => api.post('/admin/locations', values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['locations'] }); setModalOpen(false); },
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}>Quản lý địa điểm</Title>
        <Space>
          <Button icon={<WifiOutlined />}>WiFi Survey</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>Thêm địa điểm</Button>
        </Space>
      </div>
      <Table columns={columns} dataSource={data || []} loading={isLoading} rowKey="id" />
      <Modal title="Thêm địa điểm" open={modalOpen} onCancel={() => setModalOpen(false)} onOk={() => form.submit()}>
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)}>
          <Form.Item name="name" label="Tên" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="address" label="Địa chỉ"><Input /></Form.Item>
          <Form.Item name="building" label="Tòa nhà"><Input /></Form.Item>
          <Form.Item name="floor" label="Tầng"><InputNumber /></Form.Item>
          <Form.Item name="gpsLatitude" label="GPS Latitude"><InputNumber step={0.0001} /></Form.Item>
          <Form.Item name="gpsLongitude" label="GPS Longitude"><InputNumber step={0.0001} /></Form.Item>
          <Form.Item name="geofenceRadiusMeters" label="Bán kính geofence (m)" initialValue={200}><InputNumber /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

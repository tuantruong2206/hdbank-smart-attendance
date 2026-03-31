import { Typography, Table, Button, Input } from 'antd';
import { SearchOutlined, UploadOutlined } from '@ant-design/icons';

const { Title } = Typography;

const columns = [
  { title: 'Mã NV', dataIndex: 'employeeCode', key: 'code' },
  { title: 'Họ tên', dataIndex: 'fullName', key: 'name' },
  { title: 'Email', dataIndex: 'email', key: 'email' },
  { title: 'Phòng ban', dataIndex: 'organizationId', key: 'org' },
  { title: 'Vai trò', dataIndex: 'role', key: 'role' },
  { title: 'Loại NV', dataIndex: 'employeeType', key: 'type' },
];

export default function UserManagementPage() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}>Quản lý nhân viên</Title>
        <div style={{ display: 'flex', gap: 8 }}>
          <Input placeholder="Tìm kiếm..." prefix={<SearchOutlined />} />
          <Button icon={<UploadOutlined />}>Import Excel</Button>
          <Button type="primary">Thêm nhân viên</Button>
        </div>
      </div>
      <Table columns={columns} dataSource={[]} rowKey="id" />
    </div>
  );
}

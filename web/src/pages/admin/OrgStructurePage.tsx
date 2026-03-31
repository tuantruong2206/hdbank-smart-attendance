import { Typography, Tree } from 'antd';
import { useQuery } from '@tanstack/react-query';
import api from '@/shared/api/axiosInstance';

const { Title } = Typography;

export default function OrgStructurePage() {
  const { data } = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get('/admin/organizations/tree').then((r) => r.data.data),
  });

  const treeData = (data || []).map((org: any) => ({
    key: org.id,
    title: `${org.name} (${org.code})`,
    children: [],
  }));

  return (
    <div>
      <Title level={4}>Cơ cấu tổ chức</Title>
      <Tree treeData={treeData} defaultExpandAll />
    </div>
  );
}

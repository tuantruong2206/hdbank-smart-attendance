import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  TextInput,
  Alert,
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  Platform,
} from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import api from '../../../shared/api/axiosInstance';

// --- Types ---

interface LeaveBalance {
  annualTotal: number;
  annualUsed: number;
  annualRemaining: number;
  sickUsed: number;
  personalUsed: number;
}

interface LeaveRequest {
  id: string;
  type: 'ANNUAL' | 'SICK' | 'PERSONAL';
  startDate: string;
  endDate: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  createdAt: string;
  approverName: string | null;
}

type LeaveType = 'ANNUAL' | 'SICK' | 'PERSONAL';

const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  ANNUAL: 'Phep nam',
  SICK: 'Nghi om',
  PERSONAL: 'Viec rieng',
};

const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  PENDING: { label: 'Cho duyet', color: '#faad14', bg: '#fff7e6' },
  APPROVED: { label: 'Da duyet', color: '#52c41a', bg: '#f6ffed' },
  REJECTED: { label: 'Tu choi', color: '#ff4d4f', bg: '#fff2f0' },
  CANCELLED: { label: 'Da huy', color: '#999', bg: '#f5f5f5' },
};

// --- Component ---

export default function LeaveScreen() {
  const [activeTab, setActiveTab] = useState<'list' | 'create'>('list');

  return (
    <View style={styles.container}>
      {/* Tab Header */}
      <View style={styles.tabBar}>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'list' && styles.tabActive]}
          onPress={() => setActiveTab('list')}
        >
          <Text
            style={[
              styles.tabText,
              activeTab === 'list' && styles.tabTextActive,
            ]}
          >
            Don cua toi
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'create' && styles.tabActive]}
          onPress={() => setActiveTab('create')}
        >
          <Text
            style={[
              styles.tabText,
              activeTab === 'create' && styles.tabTextActive,
            ]}
          >
            Tao don moi
          </Text>
        </TouchableOpacity>
      </View>

      {activeTab === 'list' ? <MyLeaveRequests /> : <CreateLeaveRequest onCreated={() => setActiveTab('list')} />}
    </View>
  );
}

// --- Tab 1: My Leave Requests ---

function MyLeaveRequests() {
  const balanceQuery = useQuery<LeaveBalance>({
    queryKey: ['leaves', 'balance'],
    queryFn: () => api.get('/leaves/balance').then((r) => r.data.data),
  });

  const listQuery = useQuery<LeaveRequest[]>({
    queryKey: ['leaves', 'my'],
    queryFn: () => api.get('/leaves/my').then((r) => r.data.data),
  });

  const onRefresh = useCallback(() => {
    balanceQuery.refetch();
    listQuery.refetch();
  }, [balanceQuery, listQuery]);

  const balance = balanceQuery.data;

  const renderHeader = () => (
    <View>
      {/* Balance Card */}
      <View style={styles.balanceCard}>
        <Text style={styles.balanceTitle}>So du nghi phep</Text>
        <View style={styles.balanceRow}>
          <View style={styles.balanceItem}>
            <Text style={styles.balanceValue}>
              {balance?.annualRemaining ?? '--'}
            </Text>
            <Text style={styles.balanceLabel}>Phep nam con</Text>
          </View>
          <View style={styles.balanceDivider} />
          <View style={styles.balanceItem}>
            <Text style={styles.balanceValue}>
              {balance?.sickUsed ?? '--'}
            </Text>
            <Text style={styles.balanceLabel}>Nghi om da dung</Text>
          </View>
          <View style={styles.balanceDivider} />
          <View style={styles.balanceItem}>
            <Text style={styles.balanceValue}>
              {balance?.personalUsed ?? '--'}
            </Text>
            <Text style={styles.balanceLabel}>Viec rieng da dung</Text>
          </View>
        </View>
      </View>

      <Text style={styles.sectionTitle}>Lich su don nghi phep</Text>
    </View>
  );

  const renderItem = ({ item }: { item: LeaveRequest }) => {
    const config = STATUS_CONFIG[item.status] ?? STATUS_CONFIG.PENDING;

    return (
      <View style={styles.requestCard}>
        <View style={styles.requestHeader}>
          <View style={[styles.typeBadge]}>
            <Text style={styles.typeBadgeText}>
              {LEAVE_TYPE_LABELS[item.type] ?? item.type}
            </Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: config.bg }]}>
            <Text style={[styles.statusBadgeText, { color: config.color }]}>
              {config.label}
            </Text>
          </View>
        </View>
        <Text style={styles.requestDates}>
          {dayjs(item.startDate).format('DD/MM/YYYY')} -{' '}
          {dayjs(item.endDate).format('DD/MM/YYYY')}
        </Text>
        <Text style={styles.requestReason} numberOfLines={2}>
          {item.reason}
        </Text>
        {item.approverName && (
          <Text style={styles.approverText}>
            Nguoi duyet: {item.approverName}
          </Text>
        )}
      </View>
    );
  };

  return (
    <FlatList
      data={listQuery.data ?? []}
      keyExtractor={(item) => item.id}
      renderItem={renderItem}
      ListHeaderComponent={renderHeader}
      refreshControl={
        <RefreshControl
          refreshing={
            (listQuery.isFetching && !listQuery.isLoading) ||
            (balanceQuery.isFetching && !balanceQuery.isLoading)
          }
          onRefresh={onRefresh}
        />
      }
      ListEmptyComponent={
        listQuery.isLoading ? (
          <ActivityIndicator
            size="large"
            color="#1677ff"
            style={{ marginTop: 40 }}
          />
        ) : (
          <Text style={styles.emptyText}>Chua co don nghi phep nao.</Text>
        )
      }
      contentContainerStyle={styles.listContent}
    />
  );
}

// --- Tab 2: Create Leave Request ---

function CreateLeaveRequest({ onCreated }: { onCreated: () => void }) {
  const queryClient = useQueryClient();
  const [leaveType, setLeaveType] = useState<LeaveType>('ANNUAL');
  const [startDate, setStartDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [endDate, setEndDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [reason, setReason] = useState('');

  const balanceQuery = useQuery<LeaveBalance>({
    queryKey: ['leaves', 'balance'],
    queryFn: () => api.get('/leaves/balance').then((r) => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.post('/leaves', {
        type: leaveType,
        startDate,
        endDate,
        reason,
      }),
    onSuccess: () => {
      Alert.alert('Thanh cong', 'Don nghi phep da duoc tao.');
      queryClient.invalidateQueries({ queryKey: ['leaves'] });
      setReason('');
      onCreated();
    },
    onError: (e: any) =>
      Alert.alert(
        'Loi',
        e.response?.data?.message ?? 'Khong the tao don. Vui long thu lai.',
      ),
  });

  const handleSubmit = () => {
    if (!reason.trim()) {
      Alert.alert('Thieu thong tin', 'Vui long nhap ly do nghi phep.');
      return;
    }
    if (dayjs(endDate).isBefore(dayjs(startDate))) {
      Alert.alert('Loi ngay', 'Ngay ket thuc phai sau ngay bat dau.');
      return;
    }
    createMutation.mutate();
  };

  return (
    <ScrollView style={styles.formContainer} keyboardShouldPersistTaps="handled">
      {/* Leave Balance Summary */}
      {balanceQuery.data && (
        <View style={styles.balanceMini}>
          <Text style={styles.balanceMiniText}>
            Phep nam con: {balanceQuery.data.annualRemaining}/
            {balanceQuery.data.annualTotal} ngay
          </Text>
        </View>
      )}

      {/* Leave Type Picker */}
      <Text style={styles.formLabel}>Loai nghi phep</Text>
      <View style={styles.typeRow}>
        {(['ANNUAL', 'SICK', 'PERSONAL'] as LeaveType[]).map((type) => (
          <TouchableOpacity
            key={type}
            style={[
              styles.typeOption,
              leaveType === type && styles.typeOptionActive,
            ]}
            onPress={() => setLeaveType(type)}
          >
            <Text
              style={[
                styles.typeOptionText,
                leaveType === type && styles.typeOptionTextActive,
              ]}
            >
              {LEAVE_TYPE_LABELS[type]}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Date Inputs */}
      <Text style={styles.formLabel}>Ngay bat dau (YYYY-MM-DD)</Text>
      <TextInput
        style={styles.formInput}
        value={startDate}
        onChangeText={setStartDate}
        placeholder="2026-03-30"
        keyboardType={Platform.OS === 'ios' ? 'default' : 'default'}
      />

      <Text style={styles.formLabel}>Ngay ket thuc (YYYY-MM-DD)</Text>
      <TextInput
        style={styles.formInput}
        value={endDate}
        onChangeText={setEndDate}
        placeholder="2026-03-30"
      />

      {/* Reason */}
      <Text style={styles.formLabel}>Ly do</Text>
      <TextInput
        style={[styles.formInput, styles.formTextArea]}
        value={reason}
        onChangeText={setReason}
        placeholder="Nhap ly do nghi phep..."
        multiline
        numberOfLines={4}
        textAlignVertical="top"
      />

      {/* Submit */}
      <TouchableOpacity
        style={[
          styles.submitButton,
          createMutation.isPending && styles.submitButtonDisabled,
        ]}
        onPress={handleSubmit}
        disabled={createMutation.isPending}
      >
        {createMutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.submitButtonText}>Gui don nghi phep</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

// --- Styles ---

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  tab: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabActive: { borderBottomColor: '#1677ff' },
  tabText: { fontSize: 14, fontWeight: '600', color: '#999' },
  tabTextActive: { color: '#1677ff' },

  // Balance
  balanceCard: {
    backgroundColor: '#e6f7ff',
    borderRadius: 12,
    padding: 20,
    margin: 16,
    marginBottom: 8,
  },
  balanceTitle: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    marginBottom: 12,
  },
  balanceRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  balanceItem: { alignItems: 'center' },
  balanceValue: { fontSize: 28, fontWeight: 'bold', color: '#1677ff' },
  balanceLabel: { fontSize: 11, color: '#666', marginTop: 2 },
  balanceDivider: { width: 1, height: 40, backgroundColor: '#b5d8f7' },

  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginHorizontal: 16,
    marginTop: 16,
    marginBottom: 8,
  },

  // Request Card
  requestCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginHorizontal: 16,
    marginBottom: 8,
  },
  requestHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  typeBadge: {
    backgroundColor: '#f0f5ff',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
  },
  typeBadgeText: { fontSize: 12, fontWeight: '600', color: '#1677ff' },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
  },
  statusBadgeText: { fontSize: 12, fontWeight: '600' },
  requestDates: { fontSize: 14, fontWeight: '500', marginBottom: 4 },
  requestReason: { fontSize: 13, color: '#666' },
  approverText: { fontSize: 12, color: '#999', marginTop: 4 },

  listContent: { paddingBottom: 20 },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    marginTop: 40,
    fontSize: 14,
  },

  // Create Form
  formContainer: { padding: 16 },
  balanceMini: {
    backgroundColor: '#e6f7ff',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  balanceMiniText: { color: '#1677ff', fontWeight: '600', textAlign: 'center' },
  formLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 6,
    marginTop: 12,
  },
  formInput: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 14,
    borderWidth: 1,
    borderColor: '#ddd',
    fontSize: 14,
  },
  formTextArea: {
    height: 100,
  },
  typeRow: {
    flexDirection: 'row',
    gap: 8,
  },
  typeOption: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  typeOptionActive: {
    borderColor: '#1677ff',
    backgroundColor: '#e6f7ff',
  },
  typeOptionText: { fontSize: 13, color: '#666' },
  typeOptionTextActive: { color: '#1677ff', fontWeight: '600' },
  submitButton: {
    backgroundColor: '#1677ff',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 24,
    marginBottom: 40,
  },
  submitButtonDisabled: { backgroundColor: '#a0c4ff' },
  submitButtonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});

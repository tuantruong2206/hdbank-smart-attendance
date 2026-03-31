import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ScrollView,
  Switch,
  ActivityIndicator,
} from 'react-native';
import { useMutation } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';
import { useAuth } from '../../../shared/hooks/useAuth';

const ROLE_LABELS: Record<string, string> = {
  SYSTEM_ADMIN: 'Quan tri he thong',
  CEO: 'Tong Giam doc',
  DIVISION_DIRECTOR: 'Giam doc Khoi/Vung',
  DEPT_HEAD: 'Truong phong',
  DEPUTY_HEAD: 'Pho phong',
  UNIT_HEAD: 'Truong bo phan',
  EMPLOYEE: 'Nhan vien',
};

const EMPLOYEE_TYPE_LABELS: Record<string, string> = {
  NGHIEP_VU: 'Nghiep vu (co dinh)',
  IT_KY_THUAT: 'IT/Ky thuat (da diem)',
};

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const [is2FAEnabled, setIs2FAEnabled] = useState(false);

  const setup2FAMutation = useMutation({
    mutationFn: () => api.post('/auth/setup-2fa'),
    onSuccess: () => {
      setIs2FAEnabled(true);
      Alert.alert(
        'Thanh cong',
        'Xac thuc 2 yeu to da duoc kich hoat. Vui long kiem tra email de lay ma TOTP.',
      );
    },
    onError: (e: any) =>
      Alert.alert(
        'Loi',
        e.response?.data?.message ?? 'Khong the thiet lap 2FA.',
      ),
  });

  const disable2FAMutation = useMutation({
    mutationFn: () => api.post('/auth/disable-2fa'),
    onSuccess: () => {
      setIs2FAEnabled(false);
      Alert.alert('Thanh cong', 'Xac thuc 2 yeu to da duoc tat.');
    },
    onError: (e: any) =>
      Alert.alert(
        'Loi',
        e.response?.data?.message ?? 'Khong the tat 2FA.',
      ),
  });

  const toggle2FA = (value: boolean) => {
    if (value) {
      Alert.alert(
        'Kich hoat 2FA',
        'Ban co muon kich hoat xac thuc 2 yeu to?',
        [
          { text: 'Huy', style: 'cancel' },
          { text: 'Dong y', onPress: () => setup2FAMutation.mutate() },
        ],
      );
    } else {
      Alert.alert(
        'Tat 2FA',
        'Ban co chac chan muon tat xac thuc 2 yeu to?',
        [
          { text: 'Huy', style: 'cancel' },
          { text: 'Dong y', onPress: () => disable2FAMutation.mutate() },
        ],
      );
    }
  };

  const handleLogout = () => {
    Alert.alert('Dang xuat', 'Ban co chac chan muon dang xuat?', [
      { text: 'Huy', style: 'cancel' },
      { text: 'Dang xuat', style: 'destructive', onPress: logout },
    ]);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Avatar */}
      <View style={styles.avatarContainer}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {user?.fullName?.charAt(0) || 'U'}
          </Text>
        </View>
        <Text style={styles.name}>{user?.fullName || 'Nhan vien'}</Text>
        <Text style={styles.position}>{user?.position || ''}</Text>
      </View>

      {/* Info Card */}
      <View style={styles.infoCard}>
        <InfoRow label="Ma nhan vien" value={user?.employeeCode || '--'} />
        <InfoRow label="Email" value={user?.email || '--'} />
        <InfoRow
          label="Vai tro"
          value={ROLE_LABELS[user?.role ?? ''] ?? user?.role ?? '--'}
        />
        <InfoRow
          label="Loai nhan vien"
          value={
            EMPLOYEE_TYPE_LABELS[user?.employeeType ?? ''] ??
            user?.employeeType ??
            '--'
          }
        />
        <InfoRow label="Phong/Ban" value={user?.departmentName || '--'} />
        <InfoRow label="Chi nhanh" value={user?.branchName || '--'} />
      </View>

      {/* 2FA Toggle */}
      <View style={styles.settingCard}>
        <Text style={styles.settingTitle}>Bao mat</Text>
        <View style={styles.settingRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.settingLabel}>Xac thuc 2 yeu to (2FA)</Text>
            <Text style={styles.settingDesc}>
              Bao ve tai khoan voi ma TOTP qua email
            </Text>
          </View>
          {setup2FAMutation.isPending || disable2FAMutation.isPending ? (
            <ActivityIndicator color="#1677ff" />
          ) : (
            <Switch
              value={is2FAEnabled}
              onValueChange={toggle2FA}
              trackColor={{ false: '#ddd', true: '#1677ff' }}
              thumbColor="#fff"
            />
          )}
        </View>
      </View>

      {/* App Info */}
      <View style={styles.settingCard}>
        <Text style={styles.settingTitle}>Thong tin ung dung</Text>
        <InfoRow label="Phien ban" value="1.0.0" />
        <InfoRow label="He thong" value="Smart Attendance - HDBank" />
      </View>

      {/* Logout */}
      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutText}>Dang xuat</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.infoRow}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { paddingBottom: 40 },
  avatarContainer: { alignItems: 'center', paddingTop: 40, paddingBottom: 24 },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#1677ff',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  avatarText: { fontSize: 32, fontWeight: 'bold', color: '#fff' },
  name: { fontSize: 20, fontWeight: 'bold' },
  position: { fontSize: 14, color: '#666', marginTop: 2 },

  infoCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    marginHorizontal: 16,
    marginBottom: 16,
    padding: 16,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 0.5,
    borderBottomColor: '#f0f0f0',
  },
  infoLabel: { fontSize: 14, color: '#666' },
  infoValue: { fontSize: 14, fontWeight: '500', color: '#333', maxWidth: '60%', textAlign: 'right' },

  settingCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    marginHorizontal: 16,
    marginBottom: 16,
    padding: 16,
  },
  settingTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  settingLabel: { fontSize: 14, fontWeight: '500' },
  settingDesc: { fontSize: 12, color: '#999', marginTop: 2 },

  logoutButton: {
    backgroundColor: '#ff4d4f',
    borderRadius: 8,
    padding: 16,
    marginHorizontal: 16,
    marginTop: 8,
    alignItems: 'center',
  },
  logoutText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});

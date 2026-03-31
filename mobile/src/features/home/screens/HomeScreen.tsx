import React, { useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  ActivityIndicator,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../shared/hooks/useAuth';
import api from '../../../shared/api/axiosInstance';

interface PersonalMetrics {
  workDays: number;
  lateCount: number;
  leaveBalance: number;
}

interface TodayAttendance {
  checkInTime: string | null;
  checkOutTime: string | null;
  status: 'NOT_CHECKED_IN' | 'CHECKED_IN' | 'CHECKED_OUT' | 'LATE' | 'ON_LEAVE';
  shiftName: string;
  shiftStart: string;
  shiftEnd: string;
}

interface LateGraceQuota {
  used: number;
  total: number;
  remaining: number;
}

const formatTime = (isoTime: string | null): string => {
  if (!isoTime) return '--:--';
  try {
    const d = new Date(isoTime);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
  } catch {
    return '--:--';
  }
};

export default function HomeScreen() {
  const { user } = useAuth();

  // Fetch personal metrics by combining multiple APIs
  const metricsQuery = useQuery<PersonalMetrics>({
    queryKey: ['home', 'personal-metrics'],
    queryFn: async () => {
      const now = new Date();
      const year = now.getFullYear();
      const month = now.getMonth() + 1;
      const monthStart = `${year}-${String(month).padStart(2, '0')}-01`;
      const today = now.toISOString().split('T')[0];

      const [historyRes, leaveRes] = await Promise.all([
        api.get('/attendance/history', { params: { from: monthStart, to: today } }).then(r => r.data.data).catch(() => []),
        api.get('/leaves/balance').then(r => r.data.data).catch(() => []),
      ]);

      const records = Array.isArray(historyRes) ? historyRes : [];
      // Count unique work days (days with CHECK_IN)
      const workDaySet = new Set<string>();
      let lateCount = 0;
      for (const r of records) {
        if (r.checkType === 'CHECK_IN' && r.checkTime) {
          const day = r.checkTime.substring(0, 10);
          workDaySet.add(day);
          // Simple late detection: check-in after 08:15 (shift start 08:00 + 15min grace)
          const hour = new Date(r.checkTime).getHours();
          const min = new Date(r.checkTime).getMinutes();
          if (hour > 8 || (hour === 8 && min > 15)) {
            lateCount++;
          }
        }
      }

      // Find annual leave balance
      const annualLeave = Array.isArray(leaveRes)
        ? leaveRes.find((b: any) => b.leaveType === 'ANNUAL' || b.leave_type === 'ANNUAL')
        : null;
      const leaveBalance = annualLeave
        ? (annualLeave.remainingDays ?? annualLeave.remaining_days ?? (annualLeave.totalDays ?? annualLeave.total_days ?? 12) - (annualLeave.usedDays ?? annualLeave.used_days ?? 0))
        : 12;

      return { workDays: workDaySet.size, lateCount, leaveBalance };
    },
  });

  // Fetch today's attendance
  const todayQuery = useQuery<TodayAttendance>({
    queryKey: ['attendance', 'today'],
    queryFn: async () => {
      const [todayRes, shiftRes] = await Promise.all([
        api.get('/attendance/today').then(r => r.data.data).catch(() => []),
        api.get('/attendance/current-shift').then(r => r.data.data).catch(() => null),
      ]);
      const records = Array.isArray(todayRes) ? todayRes : [];
      // Sort by time descending to get latest
      const sorted = [...records].sort((a: any, b: any) =>
        new Date(b.checkTime).getTime() - new Date(a.checkTime).getTime()
      );
      const checkIn = sorted.find((r: any) => r.checkType === 'CHECK_IN');
      const checkOut = sorted.find((r: any) => r.checkType === 'CHECK_OUT');
      let status: TodayAttendance['status'] = 'NOT_CHECKED_IN';
      if (checkOut) status = 'CHECKED_OUT';
      else if (checkIn) status = 'CHECKED_IN';
      return {
        checkInTime: checkIn?.checkTime ?? null,
        checkOutTime: checkOut?.checkTime ?? null,
        status,
        shiftName: shiftRes?.shiftName ?? 'Ca sang',
        shiftStart: shiftRes?.shiftStart ?? '08:00',
        shiftEnd: shiftRes?.shiftEnd ?? '17:00',
      };
    },
  });

  // Fetch late grace quota
  const graceQuery = useQuery<LateGraceQuota>({
    queryKey: ['attendance', 'late-grace'],
    queryFn: () => api.get('/attendance/late-grace-quota').then(r => r.data.data),
  });

  const isLoading = metricsQuery.isLoading || todayQuery.isLoading || graceQuery.isLoading;

  const onRefresh = useCallback(() => {
    metricsQuery.refetch();
    todayQuery.refetch();
    graceQuery.refetch();
  }, [metricsQuery, todayQuery, graceQuery]);

  const isRefreshing = metricsQuery.isFetching || todayQuery.isFetching || graceQuery.isFetching;

  const metrics = metricsQuery.data;
  const today = todayQuery.data;
  const grace = graceQuery.data;

  const gracePercentage = grace ? (grace.used / grace.total) * 100 : 0;
  const graceColor =
    grace && grace.remaining <= 0 ? '#ff4d4f'
    : grace && grace.remaining === 1 ? '#faad14'
    : '#52c41a';

  const statusLabel = (status?: string) => {
    switch (status) {
      case 'CHECKED_IN': return 'Da cham cong vao';
      case 'CHECKED_OUT': return 'Da cham cong ra';
      case 'LATE': return 'Di tre';
      case 'ON_LEAVE': return 'Nghi phep';
      default: return 'Chua cham cong';
    }
  };

  const statusColor = (status?: string) => {
    switch (status) {
      case 'CHECKED_IN': return { bg: '#e6f7ff', text: '#1677ff' };
      case 'CHECKED_OUT': return { bg: '#f6ffed', text: '#52c41a' };
      case 'LATE': return { bg: '#fff7e6', text: '#faad14' };
      default: return { bg: '#f0f0f0', text: '#999' };
    }
  };

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chao buoi sang';
    if (hour < 18) return 'Chao buoi chieu';
    return 'Chao buoi toi';
  };

  const handleApplyLateGrace = () => {
    if (!grace || grace.remaining <= 0) {
      Alert.alert('Het quota', 'Ban da su dung het luot tre co phep trong thang nay.');
      return;
    }
    Alert.alert(
      'Xac nhan',
      `Ban muon su dung 1 luot tre co phep?\n\nCon lai: ${grace.remaining - 1}/${grace.total} luot`,
      [
        { text: 'Huy', style: 'cancel' },
        {
          text: 'Dong y',
          onPress: async () => {
            try {
              await api.post('/attendance/apply-late-grace');
              Alert.alert('Thanh cong', 'Da ap dung tre co phep cho hom nay.');
              graceQuery.refetch();
            } catch (e: any) {
              Alert.alert('Loi', e.response?.data?.message ?? 'Khong the ap dung. Vui long thu lai.');
            }
          },
        },
      ],
    );
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#1677ff" />
        <Text style={styles.loadingText}>Dang tai du lieu...</Text>
      </View>
    );
  }

  const sc = statusColor(today?.status);

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} />}
    >
      <Text style={styles.greeting}>
        {getGreeting()}, {user?.fullName || 'Nhan vien'}!
      </Text>

      {/* Today Status */}
      <View style={styles.todayCard}>
        <View style={styles.todayHeader}>
          <Text style={styles.todayTitle}>Hom nay</Text>
          <View style={[styles.statusBadge, { backgroundColor: sc.bg }]}>
            <Text style={[styles.statusBadgeText, { color: sc.text }]}>
              {statusLabel(today?.status)}
            </Text>
          </View>
        </View>
        {today?.shiftName && (
          <Text style={styles.shiftInfo}>
            {today.shiftName}: {today.shiftStart} - {today.shiftEnd}
          </Text>
        )}
        {today?.checkInTime && (
          <Text style={styles.timeInfo}>Vao: {formatTime(today.checkInTime)}</Text>
        )}
        {today?.checkOutTime && (
          <Text style={styles.timeInfo}>Ra: {formatTime(today.checkOutTime)}</Text>
        )}
      </View>

      {/* Quick Stats */}
      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: '#e6f7ff' }]}>
          <Text style={styles.statValue}>{metrics?.workDays ?? '--'}</Text>
          <Text style={styles.statLabel}>Ngay cong</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#fff7e6' }]}>
          <Text style={[styles.statValue, (metrics?.lateCount ?? 0) > 0 && { color: '#faad14' }]}>
            {metrics?.lateCount ?? '--'}
          </Text>
          <Text style={styles.statLabel}>Lan tre</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#f6ffed' }]}>
          <Text style={styles.statValue}>{metrics?.leaveBalance ?? '--'}</Text>
          <Text style={styles.statLabel}>Ngay phep con</Text>
        </View>
      </View>

      {/* Late Grace Quota */}
      <View style={styles.graceCard}>
        <View style={styles.graceHeader}>
          <Text style={styles.graceTitle}>Tre co phep thang nay</Text>
          {grace && grace.remaining > 0 && (
            <TouchableOpacity style={styles.applyButton} onPress={handleApplyLateGrace}>
              <Text style={styles.applyButtonText}>Ap dung</Text>
            </TouchableOpacity>
          )}
        </View>
        <View style={styles.graceProgress}>
          <View
            style={[styles.graceFill, {
              width: `${Math.min(gracePercentage, 100)}%`,
              backgroundColor: graceColor,
            }]}
          />
        </View>
        <Text style={[styles.graceText, { color: graceColor }]}>
          {grace
            ? `Da dung ${grace.used}/${grace.total} lan. Con ${grace.remaining} lan.`
            : 'Dang tai...'}
        </Text>
        {grace && grace.remaining === 0 && (
          <Text style={styles.graceWarning}>
            Ban da het luot tre co phep. Lan tre tiep theo se tinh la tre khong phep.
          </Text>
        )}
        {grace && grace.remaining === 1 && (
          <Text style={[styles.graceWarning, { color: '#faad14' }]}>
            Chi con 1 luot tre co phep. Hay chu y gio vao lam!
          </Text>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f5f5f5' },
  loadingText: { marginTop: 12, color: '#666', fontSize: 14 },
  greeting: { fontSize: 22, fontWeight: 'bold', marginBottom: 20 },
  todayCard: { backgroundColor: '#fff', borderRadius: 12, padding: 20, marginBottom: 20, elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 3 },
  todayHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  todayTitle: { fontSize: 18, fontWeight: 'bold' },
  statusBadge: { paddingHorizontal: 12, paddingVertical: 4, borderRadius: 12 },
  statusBadgeText: { fontSize: 12, fontWeight: '600' },
  shiftInfo: { fontSize: 14, color: '#666', marginBottom: 4 },
  timeInfo: { fontSize: 16, color: '#333', fontWeight: '500', marginTop: 2 },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  statCard: { flex: 1, borderRadius: 12, padding: 16, alignItems: 'center' },
  statValue: { fontSize: 24, fontWeight: 'bold' },
  statLabel: { fontSize: 12, color: '#666', marginTop: 4 },
  graceCard: { backgroundColor: '#fff', borderRadius: 12, padding: 20, elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 3, marginBottom: 20 },
  graceHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  graceTitle: { fontSize: 16, fontWeight: 'bold' },
  applyButton: { backgroundColor: '#1677ff', paddingHorizontal: 16, paddingVertical: 6, borderRadius: 8 },
  applyButtonText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  graceProgress: { height: 8, backgroundColor: '#f0f0f0', borderRadius: 4, marginBottom: 8 },
  graceFill: { height: '100%', borderRadius: 4 },
  graceText: { fontSize: 13 },
  graceWarning: { fontSize: 12, color: '#ff4d4f', marginTop: 8, fontStyle: 'italic' },
});

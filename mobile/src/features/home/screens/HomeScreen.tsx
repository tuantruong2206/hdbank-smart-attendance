import React, { useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../shared/hooks/useAuth';
import api from '../../../shared/api/axiosInstance';

interface DashboardMetrics {
  workDays: number;
  lateCount: number;
  leaveBalance: number;
  overtimeHours: number;
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

export default function HomeScreen() {
  const { user } = useAuth();

  const metricsQuery = useQuery<DashboardMetrics>({
    queryKey: ['dashboard', 'metrics'],
    queryFn: () => api.get('/dashboard/metrics').then((r) => r.data.data),
  });

  const todayQuery = useQuery<TodayAttendance>({
    queryKey: ['attendance', 'today'],
    queryFn: async () => {
      const [todayRes, shiftRes] = await Promise.all([
        api.get('/attendance/today').then((r) => r.data.data).catch(() => []),
        api.get('/attendance/current-shift').then((r) => r.data.data).catch(() => null),
      ]);
      const records = Array.isArray(todayRes) ? todayRes : [];
      const checkIn = records.find((r: any) => r.checkType === 'CHECK_IN');
      const checkOut = records.find((r: any) => r.checkType === 'CHECK_OUT');
      let status: TodayAttendance['status'] = 'NOT_CHECKED_IN';
      if (checkOut) status = 'CHECKED_OUT';
      else if (checkIn) status = 'CHECKED_IN';
      return {
        checkInTime: checkIn?.checkTime ?? null,
        checkOutTime: checkOut?.checkTime ?? null,
        status,
        shiftName: shiftRes?.shiftName ?? 'Ca sáng',
        shiftStart: shiftRes?.shiftStart ?? '08:00',
        shiftEnd: shiftRes?.shiftEnd ?? '17:00',
      };
    },
  });

  const graceQuery = useQuery<LateGraceQuota>({
    queryKey: ['attendance', 'late-grace'],
    queryFn: () => api.get('/attendance/late-grace-quota').then((r) => r.data.data),
  });

  const isLoading =
    metricsQuery.isLoading || todayQuery.isLoading || graceQuery.isLoading;

  const onRefresh = useCallback(() => {
    metricsQuery.refetch();
    todayQuery.refetch();
    graceQuery.refetch();
  }, [metricsQuery, todayQuery, graceQuery]);

  const isRefreshing =
    metricsQuery.isFetching || todayQuery.isFetching || graceQuery.isFetching;

  const metrics = metricsQuery.data;
  const today = todayQuery.data;
  const grace = graceQuery.data;

  const gracePercentage = grace ? (grace.used / grace.total) * 100 : 0;
  const graceColor =
    grace && grace.remaining <= 0
      ? '#ff4d4f'
      : grace && grace.remaining === 1
        ? '#faad14'
        : '#52c41a';

  const statusLabel = (status?: string) => {
    switch (status) {
      case 'CHECKED_IN':
        return 'Da cham cong vao';
      case 'CHECKED_OUT':
        return 'Da cham cong ra';
      case 'LATE':
        return 'Di tre';
      case 'ON_LEAVE':
        return 'Nghi phep';
      default:
        return 'Chua cham cong';
    }
  };

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chao buoi sang';
    if (hour < 18) return 'Chao buoi chieu';
    return 'Chao buoi toi';
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#1677ff" />
        <Text style={styles.loadingText}>Dang tai du lieu...</Text>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} />
      }
    >
      <Text style={styles.greeting}>
        {getGreeting()}, {user?.fullName || 'Nhan vien'}!
      </Text>

      {/* Today Status */}
      <View style={styles.todayCard}>
        <View style={styles.todayHeader}>
          <Text style={styles.todayTitle}>Hom nay</Text>
          <View
            style={[
              styles.statusBadge,
              {
                backgroundColor:
                  today?.status === 'CHECKED_OUT'
                    ? '#f6ffed'
                    : today?.status === 'LATE'
                      ? '#fff7e6'
                      : '#e6f7ff',
              },
            ]}
          >
            <Text
              style={[
                styles.statusBadgeText,
                {
                  color:
                    today?.status === 'CHECKED_OUT'
                      ? '#52c41a'
                      : today?.status === 'LATE'
                        ? '#faad14'
                        : '#1677ff',
                },
              ]}
            >
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
          <Text style={styles.timeInfo}>Vao: {today.checkInTime}</Text>
        )}
        {today?.checkOutTime && (
          <Text style={styles.timeInfo}>Ra: {today.checkOutTime}</Text>
        )}
      </View>

      {/* Quick Stats */}
      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: '#e6f7ff' }]}>
          <Text style={styles.statValue}>{metrics?.workDays ?? '--'}</Text>
          <Text style={styles.statLabel}>Ngay cong</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#fff7e6' }]}>
          <Text style={styles.statValue}>{metrics?.lateCount ?? '--'}</Text>
          <Text style={styles.statLabel}>Lan tre</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#f6ffed' }]}>
          <Text style={styles.statValue}>{metrics?.leaveBalance ?? '--'}</Text>
          <Text style={styles.statLabel}>Ngay phep con</Text>
        </View>
      </View>

      {/* Late Grace Quota */}
      <View style={styles.graceCard}>
        <Text style={styles.graceTitle}>Tre co phep thang nay</Text>
        <View style={styles.graceProgress}>
          <View
            style={[
              styles.graceFill,
              {
                width: `${Math.min(gracePercentage, 100)}%`,
                backgroundColor: graceColor,
              },
            ]}
          />
        </View>
        <Text style={[styles.graceText, { color: graceColor }]}>
          {grace
            ? `Da dung ${grace.used}/${grace.total} lan. Con ${grace.remaining} lan.`
            : 'Dang tai...'}
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  loadingText: { marginTop: 12, color: '#666', fontSize: 14 },
  greeting: { fontSize: 22, fontWeight: 'bold', marginBottom: 20 },
  todayCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  todayHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  todayTitle: { fontSize: 18, fontWeight: 'bold' },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusBadgeText: { fontSize: 12, fontWeight: '600' },
  shiftInfo: { fontSize: 14, color: '#666', marginBottom: 4 },
  timeInfo: { fontSize: 14, color: '#333' },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  statCard: {
    flex: 1,
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
  },
  statValue: { fontSize: 24, fontWeight: 'bold' },
  statLabel: { fontSize: 12, color: '#666', marginTop: 4 },
  graceCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    marginBottom: 20,
  },
  graceTitle: { fontSize: 16, fontWeight: 'bold', marginBottom: 12 },
  graceProgress: {
    height: 8,
    backgroundColor: '#f0f0f0',
    borderRadius: 4,
    marginBottom: 8,
  },
  graceFill: { height: '100%', borderRadius: 4 },
  graceText: { fontSize: 13 },
});

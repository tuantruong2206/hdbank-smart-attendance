import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import api from '../../../shared/api/axiosInstance';

interface AttendanceRecord {
  id: string;
  date: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  status: 'VALID' | 'LATE' | 'EARLY_LEAVE' | 'ABSENT' | 'SUSPICIOUS' | 'ON_LEAVE';
  fraudScore: number | null;
  shiftName: string;
  lateMinutes: number | null;
}

interface HistoryResponse {
  records: AttendanceRecord[];
  totalElements: number;
  totalPages: number;
}

const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
  VALID: { label: 'Hop le', color: '#52c41a', bg: '#f6ffed' },
  LATE: { label: 'Di tre', color: '#faad14', bg: '#fff7e6' },
  EARLY_LEAVE: { label: 'Ve som', color: '#fa8c16', bg: '#fff7e6' },
  ABSENT: { label: 'Vang mat', color: '#ff4d4f', bg: '#fff2f0' },
  SUSPICIOUS: { label: 'Nghi ngo', color: '#ff4d4f', bg: '#fff2f0' },
  ON_LEAVE: { label: 'Nghi phep', color: '#1677ff', bg: '#e6f7ff' },
};

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

export default function HistoryScreen() {
  const now = dayjs();
  const [selectedYear, setSelectedYear] = useState(now.year());
  const [selectedMonth, setSelectedMonth] = useState(now.month() + 1);

  const startDate = dayjs(`${selectedYear}-${String(selectedMonth).padStart(2, '0')}-01`);
  const endDate = startDate.endOf('month');

  const historyQuery = useQuery<HistoryResponse>({
    queryKey: ['attendance', 'history', selectedYear, selectedMonth],
    queryFn: () =>
      api
        .get('/attendance/history', {
          params: {
            startDate: startDate.format('YYYY-MM-DD'),
            endDate: endDate.format('YYYY-MM-DD'),
            page: 0,
            size: 50,
          },
        })
        .then((r) => r.data.data),
  });

  const onRefresh = useCallback(() => {
    historyQuery.refetch();
  }, [historyQuery]);

  const goToPrevMonth = () => {
    if (selectedMonth === 1) {
      setSelectedMonth(12);
      setSelectedYear((y) => y - 1);
    } else {
      setSelectedMonth((m) => m - 1);
    }
  };

  const goToNextMonth = () => {
    if (selectedMonth === 12) {
      setSelectedMonth(1);
      setSelectedYear((y) => y + 1);
    } else {
      setSelectedMonth((m) => m + 1);
    }
  };

  const canGoNext =
    selectedYear < now.year() ||
    (selectedYear === now.year() && selectedMonth < now.month() + 1);

  const renderItem = ({ item }: { item: AttendanceRecord }) => {
    const config = STATUS_CONFIG[item.status] ?? STATUS_CONFIG.VALID;

    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.date}>
            {dayjs(item.date).format('DD/MM/YYYY')}
          </Text>
          <View style={[styles.statusBadge, { backgroundColor: config.bg }]}>
            <Text style={[styles.statusText, { color: config.color }]}>
              {config.label}
            </Text>
          </View>
        </View>

        <View style={styles.row}>
          <Text style={styles.timeLabel}>Vao:</Text>
          <Text style={styles.timeValue}>{item.checkInTime ?? '--:--'}</Text>
          <Text style={styles.timeLabel}>Ra:</Text>
          <Text style={styles.timeValue}>{item.checkOutTime ?? '--:--'}</Text>
        </View>

        {item.shiftName && (
          <Text style={styles.shiftText}>Ca: {item.shiftName}</Text>
        )}

        {item.lateMinutes != null && item.lateMinutes > 0 && (
          <Text style={styles.lateText}>Tre {item.lateMinutes} phut</Text>
        )}

        {item.status === 'SUSPICIOUS' && item.fraudScore != null && (
          <View style={styles.fraudContainer}>
            <Text style={styles.fraudText}>
              Diem nghi ngo: {item.fraudScore}/100
            </Text>
            <View style={styles.fraudBar}>
              <View
                style={[
                  styles.fraudFill,
                  {
                    width: `${item.fraudScore}%`,
                    backgroundColor:
                      item.fraudScore >= 90
                        ? '#ff4d4f'
                        : item.fraudScore >= 70
                          ? '#faad14'
                          : '#52c41a',
                  },
                ]}
              />
            </View>
          </View>
        )}
      </View>
    );
  };

  return (
    <View style={styles.container}>
      {/* Month Picker */}
      <View style={styles.monthPicker}>
        <TouchableOpacity onPress={goToPrevMonth} style={styles.monthArrow}>
          <Text style={styles.monthArrowText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={styles.monthLabel}>
          Thang {selectedMonth}/{selectedYear}
        </Text>
        <TouchableOpacity
          onPress={goToNextMonth}
          style={[styles.monthArrow, !canGoNext && styles.monthArrowDisabled]}
          disabled={!canGoNext}
        >
          <Text
            style={[
              styles.monthArrowText,
              !canGoNext && { color: '#ccc' },
            ]}
          >
            {'>'}
          </Text>
        </TouchableOpacity>
      </View>

      {historyQuery.isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#1677ff" />
        </View>
      ) : (
        <FlatList
          data={historyQuery.data?.records ?? []}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          refreshControl={
            <RefreshControl
              refreshing={historyQuery.isFetching && !historyQuery.isLoading}
              onRefresh={onRefresh}
            />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>
                Khong co du lieu cham cong trong thang nay.
              </Text>
            </View>
          }
          contentContainerStyle={
            (historyQuery.data?.records?.length ?? 0) === 0
              ? styles.emptyList
              : undefined
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  monthPicker: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  monthArrow: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  monthArrowDisabled: { opacity: 0.3 },
  monthArrowText: { fontSize: 20, fontWeight: 'bold', color: '#1677ff' },
  monthLabel: { fontSize: 16, fontWeight: '600', minWidth: 140, textAlign: 'center' },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginHorizontal: 16,
    marginTop: 8,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  date: { fontSize: 16, fontWeight: 'bold' },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
  },
  statusText: { fontSize: 12, fontWeight: '600' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  timeLabel: { fontSize: 13, color: '#999' },
  timeValue: { fontSize: 14, fontWeight: '500', marginRight: 12 },
  shiftText: { fontSize: 12, color: '#999', marginTop: 4 },
  lateText: { fontSize: 12, color: '#faad14', marginTop: 4, fontWeight: '500' },
  fraudContainer: { marginTop: 8 },
  fraudText: { fontSize: 12, color: '#ff4d4f', fontWeight: '600', marginBottom: 4 },
  fraudBar: {
    height: 4,
    backgroundColor: '#f0f0f0',
    borderRadius: 2,
  },
  fraudFill: { height: '100%', borderRadius: 2 },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  emptyList: { flexGrow: 1 },
  emptyText: { color: '#999', fontSize: 14 },
});

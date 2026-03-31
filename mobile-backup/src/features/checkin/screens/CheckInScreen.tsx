import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { useMutation } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';

export default function CheckInScreen() {
  const checkInMutation = useMutation({
    mutationFn: () => api.post('/attendance/check-in', {
      employeeCode: 'NV001',
      employeeType: 'NGHIEP_VU',
      bssidSignals: [{ bssid: 'AA:BB:CC:DD:EE:04', rssi: -45 }],
      gpsLatitude: 10.7731,
      gpsLongitude: 106.7030,
      gpsAccuracy: 10,
      deviceId: 'mobile-001',
    }).then((r) => r.data.data),
    onSuccess: (data) => Alert.alert('Thành công', data.message),
    onError: (e: any) => Alert.alert('Lỗi', e.response?.data?.message || 'Có lỗi xảy ra'),
  });

  const checkOutMutation = useMutation({
    mutationFn: () => api.post('/attendance/check-out', {
      employeeCode: 'NV001',
      employeeType: 'NGHIEP_VU',
      bssidSignals: [{ bssid: 'AA:BB:CC:DD:EE:04', rssi: -45 }],
      deviceId: 'mobile-001',
    }).then((r) => r.data.data),
    onSuccess: (data) => Alert.alert('Thành công', data.message),
    onError: (e: any) => Alert.alert('Lỗi', e.response?.data?.message || 'Có lỗi xảy ra'),
  });

  return (
    <View style={styles.container}>
      <View style={styles.statusCard}>
        <Text style={styles.statusTitle}>Trạng thái hôm nay</Text>
        <Text style={styles.statusText}>Ca sáng: 08:00 - 17:00</Text>
        <Text style={styles.lateGrace}>Trễ có phép còn: 1/4 lần</Text>
      </View>

      <TouchableOpacity
        style={[styles.checkButton, styles.checkIn]}
        onPress={() => checkInMutation.mutate()}
      >
        <Text style={styles.checkButtonText}>Chấm công VÀO</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.checkButton, styles.checkOut]}
        onPress={() => checkOutMutation.mutate()}
      >
        <Text style={styles.checkButtonText}>Chấm công RA</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: '#f5f5f5' },
  statusCard: { backgroundColor: '#fff', borderRadius: 12, padding: 20, marginBottom: 24, elevation: 2 },
  statusTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 8 },
  statusText: { fontSize: 14, color: '#666', marginBottom: 4 },
  lateGrace: { fontSize: 14, color: '#faad14', fontWeight: '600' },
  checkButton: { borderRadius: 12, padding: 20, alignItems: 'center', marginBottom: 16 },
  checkIn: { backgroundColor: '#52c41a' },
  checkOut: { backgroundColor: '#1677ff' },
  checkButtonText: { color: '#fff', fontSize: 20, fontWeight: 'bold' },
});

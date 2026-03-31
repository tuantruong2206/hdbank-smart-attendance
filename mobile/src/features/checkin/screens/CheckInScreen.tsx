import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useMutation, useQuery } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';
import { useAuth } from '../../../shared/hooks/useAuth';

// NOTE: expo-location must be installed via `npx expo install expo-location`
// If not installed yet, the app will fall back to default coordinates.
let Location: typeof import('expo-location') | null = null;
try {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  Location = require('expo-location');
} catch {
  // expo-location not installed -- will use fallback coordinates
}

interface LocationCoords {
  latitude: number;
  longitude: number;
  accuracy: number;
}

interface ShiftInfo {
  shiftName: string;
  shiftStart: string;
  shiftEnd: string;
}

interface LateGraceQuota {
  used: number;
  total: number;
  remaining: number;
}

interface CheckResult {
  message: string;
  status: string;
  recordId?: string;
}

// Mock BSSID: WiFi scanning is not available in Expo managed workflow.
// In production, use a native module or bare workflow to access WiFi scan APIs.
const MOCK_BSSID_SIGNALS = [
  { bssid: 'AA:BB:CC:DD:EE:04', rssi: -45 },
];

export default function CheckInScreen() {
  const { user } = useAuth();
  const [coords, setCoords] = useState<LocationCoords | null>(null);
  const [locationLoading, setLocationLoading] = useState(true);
  const [locationError, setLocationError] = useState<string | null>(null);

  // Fetch current shift info
  const shiftQuery = useQuery<ShiftInfo>({
    queryKey: ['attendance', 'current-shift'],
    queryFn: () => api.get('/attendance/current-shift').then((r) => r.data.data),
  });

  // Fetch late grace quota
  const graceQuery = useQuery<LateGraceQuota>({
    queryKey: ['attendance', 'late-grace'],
    queryFn: () =>
      api.get('/attendance/late-grace-quota').then((r) => r.data.data),
  });

  useEffect(() => {
    (async () => {
      if (!Location) {
        // Fallback when expo-location is not installed
        setCoords({ latitude: 10.7731, longitude: 106.703, accuracy: 15 });
        setLocationLoading(false);
        setLocationError(
          'expo-location chua duoc cai dat. Dang dung toa do mac dinh.',
        );
        return;
      }

      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== 'granted') {
          setLocationError('Ung dung can quyen truy cap vi tri de cham cong.');
          setLocationLoading(false);
          return;
        }

        const loc = await Location.getCurrentPositionAsync({
          accuracy: Location.Accuracy.High,
        });
        setCoords({
          latitude: loc.coords.latitude,
          longitude: loc.coords.longitude,
          accuracy: loc.coords.accuracy ?? 15,
        });
      } catch {
        setCoords({ latitude: 10.7731, longitude: 106.703, accuracy: 15 });
        setLocationError('Khong the lay vi tri. Dang dung toa do mac dinh.');
      } finally {
        setLocationLoading(false);
      }
    })();
  }, []);

  const buildPayload = () => ({
    employeeCode: user?.employeeCode ?? '',
    employeeType: user?.employeeType ?? 'NGHIEP_VU',
    bssidSignals: MOCK_BSSID_SIGNALS,
    gpsLatitude: coords?.latitude ?? 10.7731,
    gpsLongitude: coords?.longitude ?? 106.703,
    gpsAccuracy: coords?.accuracy ?? 15,
    deviceId: `mobile-${user?.id ?? 'unknown'}`,
  });

  const checkInMutation = useMutation<CheckResult>({
    mutationFn: () =>
      api.post('/attendance/check-in', buildPayload()).then((r) => r.data.data),
    onSuccess: (data) =>
      Alert.alert('Thanh cong', data.message ?? 'Cham cong vao thanh cong!'),
    onError: (e: any) =>
      Alert.alert(
        'Loi',
        e.response?.data?.message ?? 'Khong the cham cong vao. Vui long thu lai.',
      ),
  });

  const checkOutMutation = useMutation<CheckResult>({
    mutationFn: () =>
      api
        .post('/attendance/check-out', buildPayload())
        .then((r) => r.data.data),
    onSuccess: (data) =>
      Alert.alert('Thanh cong', data.message ?? 'Cham cong ra thanh cong!'),
    onError: (e: any) =>
      Alert.alert(
        'Loi',
        e.response?.data?.message ?? 'Khong the cham cong ra. Vui long thu lai.',
      ),
  });

  const shift = shiftQuery.data;
  const grace = graceQuery.data;
  const graceColor =
    grace && grace.remaining <= 0
      ? '#ff4d4f'
      : grace && grace.remaining === 1
        ? '#faad14'
        : '#52c41a';

  const isPending = checkInMutation.isPending || checkOutMutation.isPending;

  return (
    <View style={styles.container}>
      {/* Shift & Status Info */}
      <View style={styles.statusCard}>
        <Text style={styles.statusTitle}>Trang thai hom nay</Text>
        {shift ? (
          <Text style={styles.statusText}>
            {shift.shiftName}: {shift.shiftStart} - {shift.shiftEnd}
          </Text>
        ) : (
          <Text style={styles.statusText}>Dang tai ca lam viec...</Text>
        )}

        {/* Late Grace Quota */}
        {grace && (
          <View style={styles.graceContainer}>
            <Text style={[styles.lateGrace, { color: graceColor }]}>
              Tre co phep con: {grace.remaining}/{grace.total} lan
            </Text>
            <View style={styles.graceProgress}>
              <View
                style={[
                  styles.graceFill,
                  {
                    width: `${Math.min((grace.used / grace.total) * 100, 100)}%`,
                    backgroundColor: graceColor,
                  },
                ]}
              />
            </View>
          </View>
        )}
      </View>

      {/* Location Status */}
      {locationLoading ? (
        <View style={styles.locationCard}>
          <ActivityIndicator size="small" color="#1677ff" />
          <Text style={styles.locationText}>Dang lay vi tri...</Text>
        </View>
      ) : locationError ? (
        <View style={[styles.locationCard, { backgroundColor: '#fff7e6' }]}>
          <Text style={styles.locationWarning}>{locationError}</Text>
        </View>
      ) : coords ? (
        <View style={styles.locationCard}>
          <Text style={styles.locationText}>
            Vi tri: {coords.latitude.toFixed(4)}, {coords.longitude.toFixed(4)}{' '}
            (+-{coords.accuracy.toFixed(0)}m)
          </Text>
        </View>
      ) : null}

      {/* WiFi Mock Notice */}
      <View style={[styles.locationCard, { backgroundColor: '#f0f5ff' }]}>
        <Text style={styles.locationText}>
          WiFi BSSID: Mock data (Expo khong ho tro WiFi scan)
        </Text>
      </View>

      {/* Check-in / Check-out Buttons */}
      <TouchableOpacity
        style={[styles.checkButton, styles.checkIn]}
        onPress={() => checkInMutation.mutate()}
        disabled={isPending || locationLoading}
      >
        {checkInMutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.checkButtonText}>Cham cong VAO</Text>
        )}
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.checkButton, styles.checkOut]}
        onPress={() => checkOutMutation.mutate()}
        disabled={isPending || locationLoading}
      >
        {checkOutMutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.checkButtonText}>Cham cong RA</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, backgroundColor: '#f5f5f5' },
  statusCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginBottom: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  statusTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 8 },
  statusText: { fontSize: 14, color: '#666', marginBottom: 4 },
  graceContainer: { marginTop: 12 },
  lateGrace: { fontSize: 14, fontWeight: '600', marginBottom: 8 },
  graceProgress: {
    height: 6,
    backgroundColor: '#f0f0f0',
    borderRadius: 3,
  },
  graceFill: { height: '100%', borderRadius: 3 },
  locationCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  locationText: { fontSize: 12, color: '#666', flex: 1 },
  locationWarning: { fontSize: 12, color: '#faad14', flex: 1 },
  checkButton: {
    borderRadius: 12,
    padding: 20,
    alignItems: 'center',
    marginBottom: 16,
  },
  checkIn: { backgroundColor: '#52c41a' },
  checkOut: { backgroundColor: '#1677ff' },
  checkButtonText: { color: '#fff', fontSize: 20, fontWeight: 'bold' },
});

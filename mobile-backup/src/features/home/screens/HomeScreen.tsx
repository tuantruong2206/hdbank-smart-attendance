import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useAuth } from '../../../shared/hooks/useAuth';

export default function HomeScreen() {
  const { user } = useAuth();

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.greeting}>Xin chào, {user?.fullName || 'Nhân viên'}!</Text>

      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: '#e6f7ff' }]}>
          <Text style={styles.statValue}>22</Text>
          <Text style={styles.statLabel}>Ngày công</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#fff7e6' }]}>
          <Text style={styles.statValue}>3</Text>
          <Text style={styles.statLabel}>Lần trễ</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: '#f6ffed' }]}>
          <Text style={styles.statValue}>11</Text>
          <Text style={styles.statLabel}>Ngày phép còn</Text>
        </View>
      </View>

      <View style={styles.graceCard}>
        <Text style={styles.graceTitle}>Trễ có phép tháng này</Text>
        <View style={styles.graceProgress}>
          <View style={[styles.graceFill, { width: '75%', backgroundColor: '#faad14' }]} />
        </View>
        <Text style={styles.graceText}>Đã dùng 3/4 lần. Còn 1 lần.</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  greeting: { fontSize: 22, fontWeight: 'bold', marginBottom: 20 },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  statCard: { flex: 1, borderRadius: 12, padding: 16, alignItems: 'center' },
  statValue: { fontSize: 24, fontWeight: 'bold' },
  statLabel: { fontSize: 12, color: '#666', marginTop: 4 },
  graceCard: { backgroundColor: '#fff', borderRadius: 12, padding: 20, elevation: 2 },
  graceTitle: { fontSize: 16, fontWeight: 'bold', marginBottom: 12 },
  graceProgress: { height: 8, backgroundColor: '#f0f0f0', borderRadius: 4, marginBottom: 8 },
  graceFill: { height: '100%', borderRadius: 4 },
  graceText: { fontSize: 13, color: '#faad14' },
});

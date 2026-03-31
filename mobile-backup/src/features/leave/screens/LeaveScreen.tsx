import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';

export default function LeaveScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Nghỉ phép</Text>
      <View style={styles.balanceCard}>
        <Text style={styles.balanceLabel}>Phép năm còn lại</Text>
        <Text style={styles.balanceValue}>11 ngày</Text>
      </View>
      <TouchableOpacity style={styles.button}>
        <Text style={styles.buttonText}>Tạo đơn nghỉ phép</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  title: { fontSize: 20, fontWeight: 'bold', marginBottom: 16 },
  balanceCard: { backgroundColor: '#e6f7ff', borderRadius: 12, padding: 20, alignItems: 'center', marginBottom: 20 },
  balanceLabel: { fontSize: 14, color: '#666' },
  balanceValue: { fontSize: 32, fontWeight: 'bold', color: '#1677ff' },
  button: { backgroundColor: '#1677ff', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});

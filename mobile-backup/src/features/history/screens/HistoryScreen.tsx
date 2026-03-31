import React from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';

const mockData = [
  { id: '1', date: '28/03/2026', checkIn: '07:55', checkOut: '17:05', status: 'VALID' },
  { id: '2', date: '27/03/2026', checkIn: '08:10', checkOut: '17:30', status: 'VALID' },
  { id: '3', date: '26/03/2026', checkIn: '08:20', checkOut: '17:00', status: 'SUSPICIOUS' },
];

export default function HistoryScreen() {
  return (
    <View style={styles.container}>
      <FlatList
        data={mockData}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Text style={styles.date}>{item.date}</Text>
            <View style={styles.row}>
              <Text>Vào: {item.checkIn}</Text>
              <Text>Ra: {item.checkOut}</Text>
              <Text style={[styles.status, item.status === 'SUSPICIOUS' && { color: 'red' }]}>
                {item.status}
              </Text>
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  card: { backgroundColor: '#fff', borderRadius: 8, padding: 16, marginBottom: 8 },
  date: { fontSize: 16, fontWeight: 'bold', marginBottom: 4 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  status: { fontWeight: '600', color: 'green' },
});

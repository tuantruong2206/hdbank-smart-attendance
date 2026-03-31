import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useAuth } from '../../../shared/hooks/useAuth';

export default function ProfileScreen() {
  const { user, logout } = useAuth();

  return (
    <View style={styles.container}>
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>{user?.fullName?.charAt(0) || 'U'}</Text>
      </View>
      <Text style={styles.name}>{user?.fullName || 'Nhân viên'}</Text>
      <Text style={styles.role}>{user?.role || 'EMPLOYEE'}</Text>
      <TouchableOpacity style={styles.logoutButton} onPress={logout}>
        <Text style={styles.logoutText}>Đăng xuất</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, alignItems: 'center', paddingTop: 60, backgroundColor: '#f5f5f5' },
  avatar: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#1677ff', justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
  avatarText: { fontSize: 32, fontWeight: 'bold', color: '#fff' },
  name: { fontSize: 20, fontWeight: 'bold', marginBottom: 4 },
  role: { fontSize: 14, color: '#666', marginBottom: 40 },
  logoutButton: { backgroundColor: '#ff4d4f', borderRadius: 8, padding: 16, width: '100%', alignItems: 'center' },
  logoutText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});

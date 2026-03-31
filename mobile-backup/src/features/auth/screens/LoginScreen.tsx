import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { useMutation } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';
import { useAuth } from '../../../shared/hooks/useAuth';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();

  const mutation = useMutation({
    mutationFn: () => api.post('/auth/login', { email, password }).then((r) => r.data.data),
    onSuccess: (data) => {
      if (data.requires2FA) {
        Alert.alert('2FA', 'Vui lòng nhập mã xác thực');
        return;
      }
      login(data.accessToken, {
        id: '', email, fullName: 'User', role: 'EMPLOYEE', employeeCode: '',
      });
    },
    onError: () => Alert.alert('Lỗi', 'Email hoặc mật khẩu không đúng'),
  });

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Smart Attendance</Text>
      <Text style={styles.subtitle}>HDBank</Text>
      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
      />
      <TextInput
        style={styles.input}
        placeholder="Mật khẩu"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />
      <TouchableOpacity style={styles.button} onPress={() => mutation.mutate()} disabled={mutation.isPending}>
        {mutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Đăng nhập</Text>
        )}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#f5f5f5' },
  title: { fontSize: 28, fontWeight: 'bold', textAlign: 'center', color: '#1677ff' },
  subtitle: { fontSize: 16, textAlign: 'center', color: '#666', marginBottom: 40 },
  input: { backgroundColor: '#fff', borderRadius: 8, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#ddd' },
  button: { backgroundColor: '#1677ff', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});

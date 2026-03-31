import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useMutation } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';
import { useAuth, User } from '../../../shared/hooks/useAuth';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  requires2FA?: boolean;
  employeeInfo?: {
    id: string;
    email: string;
    fullName: string;
    role: string;
    employeeCode: string;
    employeeType: 'NGHIEP_VU' | 'IT_KY_THUAT';
    departmentName: string;
    branchName: string;
    position: string;
  };
}

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();

  const mutation = useMutation({
    mutationFn: () =>
      api
        .post<{ data: LoginResponse }>('/auth/login', { email, password })
        .then((r) => r.data.data),
    onSuccess: (data) => {
      if (data.requires2FA) {
        Alert.alert('2FA', 'Vui long nhap ma xac thuc');
        return;
      }

      const info = data.employeeInfo;
      const user: User = {
        id: info?.id ?? '',
        email: info?.email ?? email,
        fullName: info?.fullName ?? email.split('@')[0],
        role: info?.role ?? 'EMPLOYEE',
        employeeCode: info?.employeeCode ?? '',
        employeeType: info?.employeeType ?? 'NGHIEP_VU',
        departmentName: info?.departmentName ?? '',
        branchName: info?.branchName ?? '',
        position: info?.position ?? '',
      };

      login(data.accessToken, data.refreshToken ?? '', user);
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ?? 'Email hoac mat khau khong dung';
      Alert.alert('Loi', message);
    },
  });

  const canSubmit = email.trim().length > 0 && password.length > 0;

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.logoContainer}>
        <View style={styles.logoCircle}>
          <Text style={styles.logoText}>SA</Text>
        </View>
        <Text style={styles.title}>Smart Attendance</Text>
        <Text style={styles.subtitle}>HDBank</Text>
      </View>

      <TextInput
        style={styles.input}
        placeholder="Email"
        placeholderTextColor="#999"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        autoCorrect={false}
      />
      <TextInput
        style={styles.input}
        placeholder="Mat khau"
        placeholderTextColor="#999"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />
      <TouchableOpacity
        style={[styles.button, !canSubmit && styles.buttonDisabled]}
        onPress={() => mutation.mutate()}
        disabled={mutation.isPending || !canSubmit}
      >
        {mutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Dang nhap</Text>
        )}
      </TouchableOpacity>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f5f5f5',
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 40,
  },
  logoCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#1677ff',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  logoText: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    color: '#1677ff',
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    color: '#666',
  },
  input: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#ddd',
    fontSize: 16,
  },
  button: {
    backgroundColor: '#1677ff',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonDisabled: {
    backgroundColor: '#a0c4ff',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

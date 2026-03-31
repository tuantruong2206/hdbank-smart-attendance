import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import { useAuth } from '../shared/hooks/useAuth';
import AuthStack from './navigation/AuthStack';
import MainTab from './navigation/MainTab';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

export default function App() {
  const { isAuthenticated, isHydrated } = useAuth();

  // Wait for zustand to rehydrate from AsyncStorage before rendering nav
  if (!isHydrated) {
    return (
      <View style={styles.splash}>
        <ActivityIndicator size="large" color="#1677ff" />
      </View>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <NavigationContainer>
        <StatusBar style="auto" />
        {isAuthenticated ? <MainTab /> : <AuthStack />}
      </NavigationContainer>
    </QueryClientProvider>
  );
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
});

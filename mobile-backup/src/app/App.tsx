import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from '../shared/hooks/useAuth';
import AuthStack from './navigation/AuthStack';
import MainTab from './navigation/MainTab';

const queryClient = new QueryClient();

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <QueryClientProvider client={queryClient}>
      <NavigationContainer>
        {isAuthenticated ? <MainTab /> : <AuthStack />}
      </NavigationContainer>
    </QueryClientProvider>
  );
}

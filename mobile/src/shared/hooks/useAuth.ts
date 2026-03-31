import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  employeeCode: string;
  employeeType: 'NGHIEP_VU' | 'IT_KY_THUAT';
  departmentName: string;
  branchName: string;
  position: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  login: (token: string, refreshToken: string, user: User) => void;
  logout: () => void;
  setToken: (token: string) => void;
  setHydrated: (hydrated: boolean) => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      isHydrated: false,
      login: (token, refreshToken, user) =>
        set({ token, refreshToken, user, isAuthenticated: true }),
      logout: () =>
        set({
          token: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
        }),
      setToken: (token) => set({ token }),
      setHydrated: (hydrated) => set({ isHydrated: hydrated }),
    }),
    {
      name: 'smart-attendance-auth',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => () => {
        useAuth.getState().setHydrated(true);
      },
    },
  ),
);

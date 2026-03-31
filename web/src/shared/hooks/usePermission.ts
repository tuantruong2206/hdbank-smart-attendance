import { create } from 'zustand';
import { useAuth } from './useAuth';
import api from '@/shared/api/axiosInstance';

interface Permission {
  role: string;
  action: string;
  resource: string;
  scope: string;
}

interface PermissionState {
  permissions: Permission[];
  isLoaded: boolean;
  isLoading: boolean;
  error: string | null;
  fetchPermissions: () => Promise<void>;
  canAccess: (action: string, resource: string) => boolean;
  getScope: (action: string, resource: string) => string | null;
  clear: () => void;
}

export const usePermission = create<PermissionState>()((set, get) => ({
  permissions: [],
  isLoaded: false,
  isLoading: false,
  error: null,

  fetchPermissions: async () => {
    const user = useAuth.getState().user;
    if (!user?.role) return;

    if (get().isLoading) return;

    set({ isLoading: true, error: null });

    try {
      const response = await api.get<{
        data: Permission[];
      }>(`/auth/permissions?role=${user.role}`);
      set({
        permissions: response.data.data,
        isLoaded: true,
        isLoading: false,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch permissions';
      set({ error: message, isLoading: false });
    }
  },

  canAccess: (action: string, resource: string): boolean => {
    const { permissions, isLoaded } = get();
    if (!isLoaded) return false;
    return permissions.some(
      (p) => p.action === action && p.resource === resource
    );
  },

  getScope: (action: string, resource: string): string | null => {
    const { permissions, isLoaded } = get();
    if (!isLoaded) return null;
    const perm = permissions.find(
      (p) => p.action === action && p.resource === resource
    );
    return perm?.scope ?? null;
  },

  clear: () => set({ permissions: [], isLoaded: false, isLoading: false, error: null }),
}));

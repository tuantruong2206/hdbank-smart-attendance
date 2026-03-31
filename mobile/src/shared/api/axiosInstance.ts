import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { Platform } from 'react-native';
import { useAuth } from '../hooks/useAuth';

// For physical device: use your Mac's local IP (same WiFi network required)
// For emulator: Android uses 10.0.2.2, iOS Simulator uses localhost
import Constants from 'expo-constants';

const getBaseURL = () => {
  // Physical device (Expo Go) — use LAN IP
  if (!__DEV__) return 'http://10.8.48.247:8080/api/v1';

  const isDevice = Constants.executionEnvironment === 'storeClient' ||
    (Constants.expoConfig?.hostUri && !Constants.expoConfig.hostUri.includes('localhost'));

  if (isDevice || Platform.OS === 'web') {
    // Physical device via Expo Go — use Mac's LAN IP
    return 'http://10.8.48.247:8080/api/v1';
  }
  // Emulator
  return Platform.OS === 'android'
    ? 'http://10.0.2.2:8080/api/v1'
    : 'http://localhost:8080/api/v1';
};

const baseURL = getBaseURL();

const api = axios.create({ baseURL, timeout: 15_000 });

// Track whether a token refresh is in progress to avoid concurrent refreshes
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

// Request interceptor: attach JWT
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuth.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 with token refresh
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // If 401 and we haven't retried yet, attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      const { refreshToken } = useAuth.getState();

      // No refresh token available -- force logout
      if (!refreshToken) {
        useAuth.getState().logout();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // Another refresh is in flight -- queue this request
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const response = await axios.post(`${baseURL}/auth/refresh`, {
          refreshToken,
        });
        const newToken: string = response.data.data.accessToken;
        const newRefreshToken: string =
          response.data.data.refreshToken || refreshToken;

        useAuth.getState().setToken(newToken);
        if (newRefreshToken !== refreshToken) {
          useAuth.getState().login(
            newToken,
            newRefreshToken,
            useAuth.getState().user!,
          );
        }

        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuth.getState().logout();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default api;

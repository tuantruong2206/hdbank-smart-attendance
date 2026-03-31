import axios from 'axios';
import { Platform } from 'react-native';
import { useAuth } from '../hooks/useAuth';

const baseURL = Platform.OS === 'android'
  ? 'http://10.0.2.2:8080/api/v1'
  : 'http://localhost:8080/api/v1';

const api = axios.create({ baseURL, timeout: 10000 });

api.interceptors.request.use((config) => {
  const token = useAuth.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuth.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default api;

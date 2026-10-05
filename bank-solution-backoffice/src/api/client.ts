import axios from 'axios';
import {redirectToLogin} from '@/auth/session';

const API_BASE_URL = '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
      if (error.response?.status === 401) {
          redirectToLogin();
      }
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

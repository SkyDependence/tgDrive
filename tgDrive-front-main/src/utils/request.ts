import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { callGlobalClearUserInfo } from '@/store/user';

// 请求防抖映射
const pendingRequests = new Map<string, AbortController>();

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000 // 30秒超时
});

// 请求拦截器
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  
  // 生成请求唯一标识
  const requestKey = `${config.method}_${config.url}_${JSON.stringify(config.data || {})}`;
  
  // 取消相同的待处理请求
  if (pendingRequests.has(requestKey)) {
    const controller = pendingRequests.get(requestKey);
    controller?.abort();
  }
  
  // 创建新的AbortController
  const controller = new AbortController();
  config.signal = controller.signal;
  pendingRequests.set(requestKey, controller);
  
  return config;
}, (error) => {
  return Promise.reject(error);
});

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 请求完成后从待处理列表中移除
    const config = response.config;
    const requestKey = `${config.method}_${config.url}_${JSON.stringify(config.data || {})}`;
    pendingRequests.delete(requestKey);
    return response;
  },
  (error) => {
    // 请求失败后从待处理列表中移除
    if (error.config) {
      const config = error.config;
      const requestKey = `${config.method}_${config.url}_${JSON.stringify(config.data || {})}`;
      pendingRequests.delete(requestKey);
    }
    
    if (error.response && error.response.status === 401) {
      callGlobalClearUserInfo();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default service;
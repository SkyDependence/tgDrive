import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { callGlobalClearUserInfo } from '@/store/user';
import router from '@/routers'; // 导入 router 实例

// 请求防抖映射
const pendingRequests = new Map<string, AbortController>();

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000 // 30秒超时
});

// 请求拦截器
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token');
  const expireAtRaw = localStorage.getItem('tokenExpireAt');

  if (token) {
    const expireAt = expireAtRaw ? Number(expireAtRaw) : null;
    if (expireAt !== null && !Number.isNaN(expireAt) && Date.now() >= expireAt) {
      callGlobalClearUserInfo();
      ElMessage.warning('登录状态已过期，请重新登录');
      router.push({
        path: '/login',
        query: {
          redirect: router.currentRoute.value.fullPath
        }
      });
      return Promise.reject(new Error('登录状态已过期，请重新登录'));
    }

    config.headers['Authorization'] = `Bearer ${token}`;
  }
  
  // 生成请求唯一标识
  const requestKey = `${config.method}_${config.url}_${JSON.stringify(config.data || {})}`;
  
  // 取消相同地待处理请求
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
      // 使用 router 进行跳转，并携带 redirect 参数
      router.push({
        path: '/login',
        query: {
          redirect: router.currentRoute.value.fullPath
        }
      });
    }
    return Promise.reject(error);
  }
);

export default service;

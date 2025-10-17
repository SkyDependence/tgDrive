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

const redirectToLogin = () => {
  const currentRoute = router.currentRoute.value;

  const extractRedirect = (value: unknown): string => {
    if (Array.isArray(value)) {
      return value[0] ?? '/';
    }
    if (typeof value === 'string' && value.length > 0) {
      return value;
    }
    return '/';
  };

  if (currentRoute.path === '/login') {
    const rawRedirect = currentRoute.query.redirect;
    if (rawRedirect) {
      const existing = extractRedirect(rawRedirect);
      const currentValue = Array.isArray(rawRedirect) ? rawRedirect[0] : rawRedirect;
      if (currentValue !== existing) {
        router.replace({ path: '/login', query: { redirect: existing } });
      }
    }
    return;
  }

  const { redirect, ...restQuery } = currentRoute.query;
  const searchParams = new URLSearchParams();

  Object.entries(restQuery).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach(item => {
        if (item != null) {
          searchParams.append(key, String(item));
        }
      });
    } else if (value != null) {
      searchParams.append(key, String(value));
    }
  });

  const target = (() => {
    const queryString = searchParams.toString();
    if (queryString) {
      return `${currentRoute.path}?${queryString}`;
    }
    return currentRoute.path || '/';
  })();

  const loginLocation = `/login${target ? `?redirect=${encodeURIComponent(target)}` : ''}`;

  router.push({
    path: '/login',
    query: {
      redirect: target
    }
  }).catch(() => {
    // ignore redundant navigation warnings
  });

  // 确保页面实际跳转，避免出现地址栏变化但视图未刷新的情况
  window.location.replace(loginLocation);
};

// 请求拦截器
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token');
  const expireAtRaw = localStorage.getItem('tokenExpireAt');
  const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData;

  if (token) {
    const expireAt = expireAtRaw ? Number(expireAtRaw) : null;
    if (expireAt !== null && !Number.isNaN(expireAt) && Date.now() >= expireAt) {
      callGlobalClearUserInfo();
      ElMessage.warning('登录状态已过期，请重新登录');
      redirectToLogin();
      return Promise.reject(new Error('登录状态已过期，请重新登录'));
    }

    config.headers['Authorization'] = `Bearer ${token}`;
  }
  
  if (!isFormData) {
    const requestKey = `${config.method}_${config.url}_${JSON.stringify(config.data || {})}`;

    if (pendingRequests.has(requestKey)) {
      const controller = pendingRequests.get(requestKey);
      controller?.abort();
    }

    const controller = new AbortController();
    config.signal = controller.signal;
    pendingRequests.set(requestKey, controller);
    (config as InternalAxiosRequestConfig & { __requestKey?: string }).__requestKey = requestKey;
  }
  
  return config;
}, (error) => {
  return Promise.reject(error);
});

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 请求完成后从待处理列表中移除
    const config = response.config as InternalAxiosRequestConfig & { __requestKey?: string };
    if (config.__requestKey) {
      pendingRequests.delete(config.__requestKey);
      delete config.__requestKey;
    }
    return response;
  },
  (error) => {
    // 请求失败后从待处理列表中移除
    const config = error.config as (InternalAxiosRequestConfig & { __requestKey?: string }) | undefined;
    if (config && config.__requestKey) {
      pendingRequests.delete(config.__requestKey);
      delete config.__requestKey;
    }
    
    if (error.response && error.response.status === 401) {
      callGlobalClearUserInfo();
      // 使用 router 进行跳转，并携带 redirect 参数
      redirectToLogin();
    }
    return Promise.reject(error);
  }
);

export default service;

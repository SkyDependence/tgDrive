import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';

// 使用懒加载导入组件
const Upload = () => import('../views/UploadPage.vue');
const Home = () => import('../views/Home.vue');
const FileList = () => import('../views/FileList.vue');
const Login = () => import('../views/LoginPage.vue');
const Register = () => import('../views/registerpage.vue');

const AboutPage = () => import('../views/AboutPage.vue');
const Layout = () => import('@/components/Layout.vue');
const AdminLayout = () => import('@/components/AdminLayout.vue');
const ChangePassword = () => import('../views/ChangePassword.vue');
const BackupPage = () => import('../views/BackupPage.vue');
const BotKeepAlivePage = () => import('../views/BotKeepAlivePage.vue');
const WebDavConfigPage = () => import('../views/WebDavConfigPage.vue');
const UserManagement = () => import('../views/UserManagement.vue');
const UserAgreementPage = () => import('@/views/UserAgreementPage.vue')
const PrivacyPolicyPage = () => import('@/views/PrivacyPolicyPage.vue')


interface RouteMeta extends Record<string | number | symbol, unknown> {
  requiresAuth?: boolean;
  requiredRole?: 'admin' | 'visitor' | 'user';
}

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        component: Upload,
        meta: {
          requiresAuth: true,
          requiredRole: 'visitor'
        } as RouteMeta
      },
      {
        path: 'login',
        component: Login,
      },
      {
        path: 'about',
        component: AboutPage,
      },
      {
        path: 'agreement',
        component: UserAgreementPage,
      },
      {
        path: 'privacy',
        component: PrivacyPolicyPage,
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    meta: {
      requiresAuth: true,
      requiredRole: 'user'
    } as RouteMeta,
    children: [
      {
        path: 'home',
        component: FileList,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'upload',
        component: Upload,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'changePassword',
        component: ChangePassword,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      }
    ]
  },
  {
    path: '/',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      requiredRole: 'admin'
    } as RouteMeta,
    children: [
      {
        path: 'home',
        component: Home,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'fileList',
        component: FileList,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'changePassword',
        component: ChangePassword,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'backup',
        component: BackupPage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'bot-keep-alive',
        component: BotKeepAlivePage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'webdav-config',
        component: WebDavConfigPage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'user-management',
        component: UserManagement,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },

    ]
  },
  { 
    path: '/login', 
    component: Login 
  },
  { 
    path: '/register', 
    component: Register,
    beforeEnter: async (to, from, next) => {
      try {
        const response = await request.get('/setting/registration-status');
        if (response.data.code === 1 && response.data.data.isRegistrationAllowed) {
          next();
        } else {
          ElMessage.warning('管理员已关闭注册功能');
          next('/login');
        }
      } catch (error) {
        console.error('获取注册状态失败:', error);
        ElMessage.error('无法获取注册状态，请稍后再试');
        next('/login');
      }
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role');
  
  // 检查是否登录
  if (to.meta.requiresAuth && !token) {
    next('/login');
    return;
  }

  // 检查角色权限
  if (to.meta.requiredRole) {
    if (userRole === 'admin') {
      next();
    } else if (userRole === 'visitor' && to.meta.requiredRole === 'visitor') {
      next();
    } else if (userRole === 'user' && to.meta.requiredRole === 'user') {
      next();
    } else {
      // 根据角色重定向到对应的首页
      if (userRole === 'admin') {
        next('/home');
      } else if (userRole === 'user') {
        next('/user/home');
      } else {
        next('/');
      }
    }
    return;
  }

  next();
});

export default router;

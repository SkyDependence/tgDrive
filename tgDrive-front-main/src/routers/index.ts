import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

// 使用懒加载导入组件
const Upload = () => import('../views/UploadPage.vue');
const Home = () => import('../views/Home.vue');
const FileList = () => import('../views/FileList.vue');
const Login = () => import('../views/LoginPage.vue');

const AboutPage = () => import('../views/AboutPage.vue');
const Layout = () => import('@/components/Layout.vue');
const AdminLayout = () => import('@/components/AdminLayout.vue');
const ChangePassword = () => import('../views/ChangePassword.vue');
const BackupPage = () => import('../views/BackupPage.vue');
const BotKeepAlivePage = () => import('../views/BotKeepAlivePage.vue');
const WebDavConfigPage = () => import('../views/WebDavConfigPage.vue');


interface RouteMeta extends Record<string | number | symbol, unknown> {
  requiresAuth?: boolean;
  requiredRole?: 'admin' | 'visitor';
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

    ]
  },
  { 
    path: '/login', 
    component: Login 
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
    } else {
      next('/'); // 无权限用户重定向到首页
    }
    return;
  }

  next();
});

export default router;

import { createApp } from 'vue';
import type { App as AppType } from 'vue';
import App from './App.vue';
import router from './routers';
// 按需导入Element Plus样式
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import './assets/theme.css';
// 按需导入Element Plus图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const app: AppType = createApp(App);

// 注册Element Plus图标（仅注册常用图标以减少包大小）
const commonIcons = [
  'Upload', 'UploadFilled', 'Download', 'Delete', 'Edit', 'View', 
  'Search', 'Refresh', 'Setting', 'User', 'Lock', 'Unlock',
  'Document', 'Folder', 'FolderOpened', 'Picture', 'VideoPlay',
  'Close', 'Check', 'Warning', 'InfoFilled', 'SuccessFilled',
  'CircleClose', 'ArrowLeft', 'ArrowRight', 'More', 'Plus',
  'Minus', 'Star', 'StarFilled', 'Share', 'Link', 'CopyDocument',
  'Tickets', 'Files', 'Monitor', 'Connection'
];

for (const iconName of commonIcons) {
  if (ElementPlusIconsVue[iconName]) {
    app.component(iconName, ElementPlusIconsVue[iconName]);
  }
}

// Global message function
const showMessage = (message: string, type: 'success' | 'warning' | 'info' | 'error' = 'info'): void => {
  ElMessage({
    message,
    type,
    duration: 2000,
    zIndex: 20000
  });
};

// Element Plus已通过unplugin-vue-components自动按需导入
// 无需手动配置全量导入

// Use router
app.use(router);

// Mount the app
app.mount('#app');

// Export app instance for testing
export default app;

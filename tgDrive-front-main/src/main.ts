import { createApp } from 'vue';
import type { App as AppType } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './routers';
// 按需导入Element Plus样式
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import './assets/theme.css';
// 按需具名导入实际用到的 Element Plus 图标（避免 import * 引入整个图标库）
import {
  Upload, UploadFilled, Download, Delete, Edit, View,
  Search, Refresh, Setting, User, Lock, Unlock,
  Document, Folder, FolderOpened, Picture, VideoPlay,
  Close, Check, Warning, InfoFilled, SuccessFilled,
  CircleClose, ArrowLeft, ArrowRight, More, Plus,
  Minus, Star, StarFilled, Share, Link, CopyDocument,
  Tickets, Files, Monitor, Connection,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const app: AppType = createApp(App);
const pinia = createPinia();

// 全局注册图标组件（供模板以 <Upload /> 等标签使用）
const commonIcons: Record<string, unknown> = {
  Upload, UploadFilled, Download, Delete, Edit, View,
  Search, Refresh, Setting, User, Lock, Unlock,
  Document, Folder, FolderOpened, Picture, VideoPlay,
  Close, Check, Warning, InfoFilled, SuccessFilled,
  CircleClose, ArrowLeft, ArrowRight, More, Plus,
  Minus, Star, StarFilled, Share, Link, CopyDocument,
  Tickets, Files, Monitor, Connection,
};

for (const [name, comp] of Object.entries(commonIcons)) {
  app.component(name, comp as never);
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

// Use Pinia
app.use(pinia);

// Use router
app.use(router);

// Mount the app
app.mount('#app');

// Export app instance for testing
export default app;

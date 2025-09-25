<template>
  <div class="page-container">
    <el-row :gutter="20">
      <!-- Left Column: Upload and Progress -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <el-icon><UploadFilled /></el-icon>
              <span>文件上传</span>
            </div>
          </template>

          <!-- Upload Zone -->
          <el-upload
            ref="uploadRef"
            drag
            multiple
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="selectedFiles"
            class="upload-dragger"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处, 或 <em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持多文件上传，支持 Ctrl+V 粘贴文件。
              </div>
            </template>
          </el-upload>

          <!-- Upload Button -->
          <div class="upload-actions">
            <el-button
              type="primary"
              @click="handleUpload"
              :disabled="isUploading || selectedFiles.length === 0"
              :loading="isUploading"
              size="large"
              :icon="Upload"
            >
              {{ isUploading ? `正在上传 (${uploadCompletedCount}/${selectedFiles.length})` : '开始上传' }}
            </el-button>
          </div>

          <!-- Progress Section -->
          <el-collapse-transition>
            <div v-if="uploadProgress.length > 0" class="progress-section">
              <UploadProgressItem
                v-for="item in uploadProgress"
                :key="item.uid"
                :item="item"
              />
            </div>
          </el-collapse-transition>
        </el-card>
      </el-col>

      <!-- Right Column: Uploaded Files -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <el-icon><Tickets /></el-icon>
              <span>本次上传结果</span>
              <el-button text type="primary" @click="goToFileList">查看全部</el-button>
            </div>
          </template>
          
          <div v-if="uploadedFiles.length === 0" class="empty-state">
            <el-empty description="暂无上传成功的文件" />
          </div>

          <div v-else class="uploaded-files-list">
            <div v-for="file in uploadedFiles" :key="file.fileId" class="uploaded-file-item">
              <div class="file-details">
                <el-icon><Document /></el-icon>
                <span class="uploaded-file-name">{{ file.fileName }}</span>
              </div>
              <div class="file-actions">
                <el-tooltip content="复制 Markdown 格式" placement="top">
                  <el-button text circle :icon="Link" @click="copyMarkdown(file)" />
                </el-tooltip>
                <el-tooltip content="复制下载链接" placement="top">
                  <el-button text circle :icon="Paperclip" @click="copyLink(file)" />
                </el-tooltip>
                <el-tooltip content="打开/下载文件" placement="top">
                  <el-button text circle :icon="View" @click="openLink(file.downloadLink)" />
                </el-tooltip>
              </div>
            </div>
          </div>
          
          <div v-if="uploadedFiles.length > 0" class="batch-actions">
            <div class="batch-button-group">
              <el-button @click="batchCopyMarkdown" :disabled="uploadedFiles.length === 0" size="small" plain>批量复制 (MD)</el-button>
              <el-button @click="batchCopyLinks" :disabled="uploadedFiles.length === 0" size="small" plain>批量复制 (链接)</el-button>
            </div>
          </div>
          
          <!-- 批量操作按钮移到这里 -->

        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, UploadFile, UploadFiles, UploadInstance } from 'element-plus';
import { UploadFilled, Upload, Document, Link, Tickets, Paperclip, View } from '@element-plus/icons-vue';
import UploadProgressItem from '@/components/UploadProgressItem.vue';
import request from '@/utils/request';

// --- Interfaces ---
interface UploadedFile {
  fileName: string;
  downloadLink: string;
  fileId: string;
}

interface ProgressItem {
  uid: number;
  name: string;
  total: number;
  client: {
    percentage: number;
    loaded: number;
    status: 'uploading' | 'success' | 'exception';
  };
  server: {
    percentage: number;
    currentChunk: number;
    totalChunks: number;
    status: 'waiting' | 'uploading' | 'success' | 'exception';
  };
}

// --- Component State ---
const router = useRouter();
const uploadRef = ref<UploadInstance>();
const selectedFiles = ref<UploadFile[]>([]);
const uploadedFiles = ref<UploadedFile[]>([]);
const isUploading = ref(false);
const uploadProgress = ref<ProgressItem[]>([]);
const websocket = ref<WebSocket | null>(null);
const manualClosedSockets = new WeakSet<WebSocket>();
const reconnectTimer = ref<number | null>(null);
const heartbeatTimer = ref<number | null>(null);
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 10;
const reconnectDelay = ref(1000); // 初始重连延迟 1 秒
const isPageVisible = ref(true);
const CONCURRENCY_LIMIT = 3;

const uploadCompletedCount = computed(() =>
  uploadProgress.value.filter(p => p.server.status === 'success').length
);

// --- Methods ---
const handleFileChange = (_file: UploadFile, fileList: UploadFiles) => {
  selectedFiles.value = fileList;
};

const handleFileRemove = (_file: UploadFile, fileList: UploadFiles) => {
  selectedFiles.value = fileList;
};

// Corrected: Using a sequential for...of loop for robust, one-by-one uploads.
const handleUpload = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件');
    return;
  }

  isUploading.value = true;
  uploadedFiles.value = [];
  uploadProgress.value = selectedFiles.value.map(f => reactive({
    uid: f.uid,
    name: f.name,
    total: f.size || 0,
    client: { percentage: 0, loaded: 0, status: 'uploading' },
    server: { percentage: 0, currentChunk: 0, totalChunks: 0, status: 'waiting' },
  }));

  const queue = [...selectedFiles.value];

  const runNext = async (): Promise<void> => {
    const nextFile = queue.shift();
    if (!nextFile) return;

    const progressItem = uploadProgress.value.find(p => p.uid === nextFile.uid);
    if (!progressItem) {
      await runNext();
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', nextFile.raw as File);

      const response = await request.post('/upload', formData, {
        timeout: 21600000,
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            progressItem.client.loaded = progressEvent.loaded;
            progressItem.client.percentage = (progressEvent.loaded / progressEvent.total) * 100;
          }
        }
      });

      const { code, msg, data } = response.data;
      if (code === 1) {
        progressItem.client.status = 'success';
        uploadedFiles.value.push(data);
      } else {
        throw new Error(msg || '上传响应错误');
      }
    } catch (error: any) {
      if (error?.message === '登录状态已过期，请重新登录') {
        progressItem.client.status = 'exception';
        throw error;
      }
      progressItem.client.status = 'exception';
      ElMessage.error(`${nextFile.name} 上传失败: ${error.message}`);
    } finally {
      if (queue.length > 0) {
        await runNext();
      }
    }
  };

  const workerCount = Math.min(CONCURRENCY_LIMIT, queue.length);
  const workers = Array.from({ length: workerCount }, () => runNext());
  const results = await Promise.allSettled(workers);

  const hadAuthError = results.some((result) => result.status === 'rejected' && result.reason?.message === '登录状态已过期，请重新登录');

  isUploading.value = false;
  selectedFiles.value = [];
  uploadRef.value?.clearFiles();

  if (hadAuthError) {
    return;
  }
};

const connectWebSocket = () => {
  // 清理之前的连接
  if (websocket.value) {
    manualClosedSockets.add(websocket.value);
    websocket.value.close();
  }

  // 清理定时器
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value);
    reconnectTimer.value = null;
  }
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value);
    heartbeatTimer.value = null;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const wsUrl = `${protocol}://${window.location.host}/ws/upload-progress`;

  try {
    websocket.value = new WebSocket(wsUrl);

    websocket.value.onopen = () => {
      console.log('WebSocket 连接已建立');
      reconnectAttempts.value = 0;
      reconnectDelay.value = 1000; // 重置重连延迟
      startHeartbeat(); // 开始心跳
    };

    websocket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        // 如果是心跳响应，忽略
        if (data.type === 'pong') {
          return;
        }

        const progressItem = uploadProgress.value.find(p => p.name === data.fileName);
        if (!progressItem) return;

        if (data.type === 'upload_progress') {
          progressItem.server.status = 'uploading';
          const totalChunks = data.total_chunks || data.totalChunks;
          const currentChunk = data.current_chunk || data.currentChunk;
          if (totalChunks !== undefined) progressItem.server.totalChunks = totalChunks;
          if (currentChunk !== undefined) progressItem.server.currentChunk = currentChunk;
          if (data.percentage !== undefined) progressItem.server.percentage = data.percentage;
        } else if (data.type === 'upload_complete') {
          progressItem.server.status = 'success';
          progressItem.server.percentage = 100;
        } else if (data.type === 'upload_error') {
          progressItem.server.status = 'exception';
          ElMessage.error(`${data.fileName} 传输到Telegram失败: ${data.error}`);
        }
      } catch (error) {
        console.error('WebSocket message parse error:', error);
      }
    };

    websocket.value.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    websocket.value.onclose = (event) => {
      console.log('WebSocket 连接已关闭', event);
      stopHeartbeat();

      const closedSocket = (event?.target || null) as WebSocket | null;
      if (closedSocket && manualClosedSockets.has(closedSocket)) {
        manualClosedSockets.delete(closedSocket);
        return;
      }

      // 只在页面可见且未达到最大重连次数时进行重连
      if (isPageVisible.value && reconnectAttempts.value < maxReconnectAttempts) {
        reconnectAttempts.value++;
        console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`);

        reconnectTimer.value = window.setTimeout(() => {
          connectWebSocket();
        }, reconnectDelay.value);

        // 使用指数退避策略，最大延迟 30 秒
        reconnectDelay.value = Math.min(reconnectDelay.value * 2, 30000);
      }
    };
  } catch (error) {
    console.error('创建 WebSocket 连接失败:', error);
    // 尝试重连
    if (isPageVisible.value && reconnectAttempts.value < maxReconnectAttempts) {
      reconnectAttempts.value++;
      reconnectTimer.value = window.setTimeout(() => {
        connectWebSocket();
      }, reconnectDelay.value);
    }
  }
};

// 心跳机制
const startHeartbeat = () => {
  stopHeartbeat();
  heartbeatTimer.value = window.setInterval(() => {
    if (websocket.value && websocket.value.readyState === WebSocket.OPEN) {
      websocket.value.send(JSON.stringify({ type: 'ping' }));
    }
  }, 30000); // 每 30 秒发送一次心跳
};

const stopHeartbeat = () => {
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value);
    heartbeatTimer.value = null;
  }
};

// --- Utility and Lifecycle ---
const goToFileList = () => router.push('/fileList');
const copyToClipboard = (text: string, message: string) => {
  navigator.clipboard.writeText(text).then(() => ElMessage.success(message));
};
const copyMarkdown = (file: UploadedFile) => copyToClipboard(`![${file.fileName}](${file.downloadLink})`, 'Markdown 格式已复制');
const copyLink = (file: UploadedFile) => copyToClipboard(file.downloadLink, '下载链接已复制');
const openLink = (url: string) => window.open(url, '_blank');

const batchCopyMarkdown = () => {
  const text = uploadedFiles.value.map(f => `![${f.fileName}](${f.downloadLink})`).join('\n');
  copyToClipboard(text, `已批量复制 ${uploadedFiles.value.length} 个 Markdown 链接`);
};

const batchCopyLinks = () => {
  const text = uploadedFiles.value.map(f => f.downloadLink).join('\n');
  copyToClipboard(text, `已批量复制 ${uploadedFiles.value.length} 个下载链接`);
};

const handlePaste = (event: ClipboardEvent) => {
  const items = event.clipboardData?.items;
  if (!items) return;
  const files: File[] = [];
  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file') {
      const file = items[i].getAsFile();
      if (file) files.push(file);
    }
  }
  if (files.length > 0) {
    const uploadFiles = files.map((file, i) => {
      const uid = Date.now() + i;
      return { name: file.name, size: file.size, uid, raw: Object.assign(file, { uid }), status: 'ready' } as UploadFile;
    });
    const newFiles = uploadFiles.filter(uf => !selectedFiles.value.some(sf => sf.name === uf.name && sf.size === uf.size));
    if (newFiles.length > 0) {
      selectedFiles.value.push(...newFiles);
      ElMessage.success(`已通过粘贴添加 ${newFiles.length} 个文件`);
    }
  }
};

onMounted(() => {
  window.addEventListener('paste', handlePaste);

  // 页面可见性检测
  const handleVisibilityChange = () => {
    isPageVisible.value = !document.hidden;
    if (!document.hidden) {
      console.log('页面显示，检查 WebSocket 连接状态');
      if (!websocket.value || websocket.value.readyState !== WebSocket.OPEN) {
        reconnectAttempts.value = 0;
        reconnectDelay.value = 1000;
        connectWebSocket();
      }
    } else {
      console.log('页面隐藏，保持 WebSocket 连接');
    }
  };

  document.addEventListener('visibilitychange', handleVisibilityChange);

  // 添加窗口焦点事件监听
  const handleFocus = () => {
    if (!websocket.value || websocket.value.readyState !== WebSocket.OPEN) {
      console.log('窗口获得焦点，检查并重连 WebSocket');
      reconnectAttempts.value = 0;
      reconnectDelay.value = 1000;
      connectWebSocket();
    }
  };

  window.addEventListener('focus', handleFocus);

  connectWebSocket();

  // 保存事件监听器以便清理
  (window as any).__visibilityHandler = handleVisibilityChange;
  (window as any).__focusHandler = handleFocus;
});

onBeforeUnmount(() => {
  window.removeEventListener('paste', handlePaste);

  // 清理页面可见性监听
  if ((window as any).__visibilityHandler) {
    document.removeEventListener('visibilitychange', (window as any).__visibilityHandler);
    delete (window as any).__visibilityHandler;
  }

  // 清理焦点事件监听
  if ((window as any).__focusHandler) {
    window.removeEventListener('focus', (window as any).__focusHandler);
    delete (window as any).__focusHandler;
  }

  // 清理定时器
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value);
  }
  stopHeartbeat();

  // 关闭 WebSocket 连接
  if (websocket.value) {
    manualClosedSockets.add(websocket.value);
    websocket.value.close();
  }
});
</script>





<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
}

.content-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

.upload-dragger {
  margin-bottom: 20px;
}

.upload-actions {
  text-align: center;
}

.progress-section {
  margin-top: 20px;
  max-height: 200px;
  overflow-y: auto;
}

.file-progress-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 6px;
  background-color: var(--el-fill-color-lighter);
  box-sizing: border-box;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--el-text-color-primary);
  flex-grow: 1;
}

.file-size-info {
  font-size: 12px;
  color: var(--el-text-color-regular);
  flex-shrink: 0;
}

.unified-progress-bar {
  margin: 4px 0;
}

.progress-info-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
  text-align: center;
  height: 16px;
}

.el-progress {
  width: 100%;
}

.chunk-progress-info {
  margin-top: 8px;
  padding: 8px;
  background-color: var(--el-fill-color-extra-light);
  border-radius: 4px;
  border-left: 3px solid var(--el-color-primary);
}

.chunk-text {
  font-size: 11px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
  font-weight: 500;
}

.telegram-info {
  margin-top: 8px;
}

.telegram-progress-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 200px;
}

.uploaded-files-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.uploaded-file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  background-color: #f9fafb; /* 浅色背景 */
  margin-bottom: 8px;
  transition: background-color 0.3s;
  border: 1px solid var(--el-border-color-lighter);
}

html.dark .uploaded-file-item {
  background-color: var(--el-bg-color-overlay);
}

.uploaded-file-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-color: #7dd3fc;
}

.file-details {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0; /* 允许收缩 */
}

.uploaded-file-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px; /* 限制最大宽度 */
}

.file-actions {
  display: flex;
  gap: 5px;
  flex-shrink: 0; /* 防止被压缩 */
  min-width: 120px; /* 确保按钮区域有足够空间 */
}

.batch-actions {
    margin-top: 20px;
    border-top: 1px solid var(--border-color);
    padding-top: 20px;
    display: flex;
    justify-content: center;
  }

  .batch-button-group {
    display: flex;
    gap: 10px;
    justify-content: center;
    flex-wrap: wrap;
  }

:deep(.el-progress-bar__inner--striped) {
  animation-duration: 2s; /* 减慢流动动画速度，默认为1s */
}

/* 双进度条样式 */
.dual-progress-container {
  margin-top: 10px;
  padding: 15px;
  background: var(--hover-bg-color);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.progress-stage {
  margin-bottom: 15px;
}

.progress-stage:last-child {
  margin-bottom: 0;
}

.stage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stage-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-color);
}

.stage-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.status-waiting {
  background: #f0f0f0;
  color: #666;
}

.status-uploading {
  background: #e6f7ff;
  color: #1890ff;
}

.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.status-error {
  background: #fff2f0;
  color: #ff4d4f;
}

.chunk-info {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-top: 4px;
  text-align: center;
}

/* Responsive styles for UploadPage.vue */
@media (max-width: 767px) { /* Mobile breakpoint */
  .page-container {
    padding: 10px; /* Reduce padding on mobile */
  }

  .card-header {
    flex-wrap: wrap; /* Allow header items to wrap */
    justify-content: space-between; /* Better alignment */
    text-align: left;
    gap: 8px;
  }

  .card-header .el-button {
    margin-top: 0;
    flex-shrink: 0;
  }

  .upload-actions {
    margin-top: 15px;
  }

  .upload-actions .el-button {
    width: 100%; /* Make upload button full width */
    padding: 12px 20px;
    font-size: 16px;
  }

  .file-progress-item {
    padding: 15px;
    margin-bottom: 20px;
    border: 1px solid var(--el-border-color-light);
  }

  .file-name {
    width: 100%;
    max-width: none;
    text-align: left;
    margin-bottom: 8px;
    font-size: 15px;
    font-weight: 600;
  }

  .el-progress {
    width: 100%;
    margin: 8px 0;
  }

  .file-size-info {
    text-align: left;
    margin-top: 8px;
    font-size: 13px;
  }

  .telegram-info {
    margin-top: 12px;
    padding-top: 8px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .telegram-progress-text {
    margin-bottom: 8px;
    font-size: 13px;
  }

  .uploaded-file-item {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 15px;
  }

  .file-details {
    width: 100%;
    justify-content: flex-start;
  }

  .uploaded-file-name {
    font-size: 14px;
    max-width: none;
    flex: 1;
  }

  .file-actions {
    width: 100%;
    justify-content: center;
    gap: 8px;
    margin-top: 8px;
  }

  .file-actions .el-button {
    flex: 1;
    max-width: 80px;
    padding: 8px;
  }

  .batch-actions {
     justify-content: center;
     padding: 15px 0;
   }

   .batch-button-group {
     display: flex;
     flex-direction: row;
     gap: 10px;
     width: 100%;
     max-width: 300px;
     justify-content: center;
   }

   .batch-button-group .el-button {
     flex: 1;
     padding: 10px 8px;
     font-size: 13px;
     white-space: nowrap;
   }
}
</style>

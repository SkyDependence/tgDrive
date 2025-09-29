<template>
  <div class="page-container">
    <div v-if="connectionLost" class="connection-alert">
      <el-alert
        type="error"
        show-icon
        :closable="false"
        title="实时进度连接已断开"
        description="请检查网络状态，然后点击“重新连接”恢复实时进度。"
      />
      <div class="connection-alert-actions">
        <el-button type="primary" size="small" @click="retryWebSocket">重新连接</el-button>
      </div>
    </div>
    <el-row :gutter="20">
      <!-- Left Column: Upload and Progress -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <el-icon><UploadFilled /></el-icon>
              <span>文件上传</span>
              <el-switch
                v-model="enableResumable"
                active-text="断点续传"
                inactive-text="普通上传"
                style="margin-left: auto"
              />
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
            :disabled="isUploading"
            class="upload-dragger"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处, 或 <em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持多文件上传，支持 Ctrl+V 粘贴文件。
                <span v-if="enableResumable" class="resumable-tip">
                  <el-icon><InfoFilled /></el-icon>
                  断点续传模式：支持大文件上传、断网恢复、秒传
                </span>
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
              {{ uploadButtonText }}
            </el-button>
            <el-button
              v-if="isUploading && enableResumable"
              @click="togglePause"
              size="large"
              :icon="isPaused ? VideoPlay : VideoPause"
            >
              {{ isPaused ? '继续' : '暂停' }}
            </el-button>
            <el-button
              v-if="isUploading"
              @click="cancelUpload"
              size="large"
              type="danger"
              :icon="CircleClose"
            >
              取消
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

              <!-- 断点续传额外信息 -->
              <div v-if="enableResumable && resumableInfo" class="resumable-info">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="上传模式">
                    <el-tag size="small" type="success">断点续传</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="任务ID">
                    {{ resumableInfo.taskId }}
                  </el-descriptions-item>
                  <el-descriptions-item label="总分块">
                    {{ resumableInfo.totalChunks }} 块
                  </el-descriptions-item>
                  <el-descriptions-item label="已上传">
                    {{ resumableInfo.uploadedChunks }} 块
                  </el-descriptions-item>
                </el-descriptions>
              </div>
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
                <el-tag v-if="file.isInstant" type="success" size="small" style="margin-left: 8px">秒传</el-tag>
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
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, UploadFile, UploadFiles, UploadInstance } from 'element-plus';
import {
  UploadFilled, Upload, Document, Link, Tickets, Paperclip, View,
  InfoFilled, VideoPlay, VideoPause, CircleClose
} from '@element-plus/icons-vue';
import UploadProgressItem from '@/components/UploadProgressItem.vue';
import request from '@/utils/request';
import { UploadQueueManager } from '@/utils/uploadQueueManager';
import type { UploadOptions } from '@/utils/resumableUploader';

// --- Interfaces ---
interface UploadedFile {
  fileName: string;
  downloadLink: string;
  fileId: string;
  isInstant?: boolean; // 是否秒传
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

interface ResumableInfo {
  taskId: string;
  totalChunks: number;
  uploadedChunks: number;
}

// --- Component State ---
const router = useRouter();
const uploadRef = ref<UploadInstance>();
const selectedFiles = ref<UploadFile[]>([]);
const uploadedFiles = ref<UploadedFile[]>([]);
const isUploading = ref(false);
const uploadProgress = ref<ProgressItem[]>([]);
const totalUploadCount = ref(0);
const websocket = ref<WebSocket | null>(null);
const manualClosedSockets = new WeakSet<WebSocket>();
const reconnectTimer = ref<number | null>(null);
const heartbeatTimer = ref<number | null>(null);
const heartbeatTimeoutTimer = ref<number | null>(null);
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 10;
const reconnectDelay = ref(1000);
const isPageVisible = ref(true);
const CONCURRENCY_LIMIT = 3;
const HEARTBEAT_INTERVAL = 30000;
const HEARTBEAT_TIMEOUT = 15000;
const connectionLost = ref(false);
const reconnectFailureNotified = ref(false);
const RESUMABLE_MIN_SIZE = 10 * 1024 * 1024; // 10MB

// 断点续传相关
const enableResumable = ref(true); // 是否启用断点续传
const isPaused = ref(false);
const resumableInfo = ref<ResumableInfo | null>(null);

const queueManager = new UploadQueueManager({
  maxConcurrent: CONCURRENCY_LIMIT,
  autoStart: false,
  persistQueue: false,
  onQueueUpdate: () => {
    const tasks = queueManager.getTasks();
    const active = tasks.some(task => task.status === 'pending' || task.status === 'uploading');
    isUploading.value = active;
    if (!active) {
      isPaused.value = false;
      resumableInfo.value = null;
      totalUploadCount.value = 0;
    }
  },
  onTaskFailed: (task, error) => {
    const progressItem = taskIdToProgress.get(task.id);
    if (progressItem) {
      progressItem.client.status = 'exception';
      progressItem.server.status = 'exception';
    }
    if (task.file) {
      ElMessage.error(`${task.file.name} 上传失败: ${error.message}`);
    }
  }
});

const taskIdToProgress = new Map<string, ProgressItem>();
const serverTaskState = new Map<string, ResumableInfo>();

const toPercent = (value: number): number => Number(value.toFixed(2));

const uploadCompletedCount = computed(() =>
  uploadProgress.value.filter(p => p.server.status === 'success').length
);

const uploadButtonText = computed(() => {
  if (!isUploading.value) return '开始上传';
  if (isPaused.value) return '已暂停';
  return `正在上传 (${uploadCompletedCount.value}/${Math.max(totalUploadCount.value, 1)})`;
});

// --- Methods ---
const handleFileChange = (_file: UploadFile, fileList: UploadFiles) => {
  if (isUploading.value) {
    ElMessage.warning('当前正在上传，请稍后再添加文件');
    selectedFiles.value = [...selectedFiles.value];
    return;
  }
  selectedFiles.value = fileList;
};

const handleFileRemove = (_file: UploadFile, fileList: UploadFiles) => {
  if (isUploading.value) {
    return;
  }
  selectedFiles.value = fileList;
};

// 切换暂停/继续
const togglePause = () => {
  if (!isUploading.value) {
    return;
  }

  if (isPaused.value) {
    const pausedTasks = queueManager.getTasks().filter(task => task.status === 'paused');
    pausedTasks.forEach(task => queueManager.resumeTask(task.id));
    queueManager.start();
    isPaused.value = false;
    ElMessage.success('已恢复上传');
  } else {
    queueManager.stop();
    isPaused.value = true;
    ElMessage.info('已暂停上传');
  }
};

// 取消上传
const cancelUpload = async () => {
  queueManager.stop();
  await queueManager.cancelAll();
  taskIdToProgress.clear();
  serverTaskState.clear();
  isUploading.value = false;
  isPaused.value = false;
  uploadProgress.value = [];
  selectedFiles.value = [];
  uploadRef.value?.clearFiles();
  resumableInfo.value = null;
  totalUploadCount.value = 0;
  ElMessage.warning('已取消上传');
};

// 使用断点续传上传
const handleResumableUpload = async () => {
  queueManager.stop();
  await queueManager.cancelAll();

  const files = [...selectedFiles.value];

  for (const file of files) {
    if (!file.raw) continue;

    const progressItem = uploadProgress.value.find(p => p.uid === file.uid);
    if (!progressItem) continue;

    progressItem.client.status = 'uploading';
    progressItem.server.status = 'waiting';
    progressItem.client.percentage = 0;
    progressItem.server.percentage = 0;
    progressItem.server.currentChunk = 0;

    let queueTaskId = '';
    let serverTaskId = '';

    const options: UploadOptions = {
      concurrency: CONCURRENCY_LIMIT,
      onProgress: (
        _overallProgress: number,
        _chunkIndex: number,
        totalChunks: number,
        taskId: string,
        stageOneRatio: number = 0
      ) => {
        if (!taskId) return;

        serverTaskId = taskId;

        const rawFile = file.raw as File;
        const state = serverTaskState.get(taskId) || {
          taskId,
          totalChunks,
          uploadedChunks: 0
        };

        state.totalChunks = totalChunks;
        serverTaskState.set(taskId, state);

        const progressRatio = totalChunks > 0
          ? Math.min((state.uploadedChunks + stageOneRatio) / totalChunks, 1)
          : Math.min(stageOneRatio, 1);

        progressItem.client.percentage = toPercent(progressRatio * 100);
        progressItem.client.loaded = rawFile.size * progressRatio;
        progressItem.server.totalChunks = totalChunks;
        progressItem.server.percentage = state.uploadedChunks > 0 && totalChunks > 0
          ? toPercent((state.uploadedChunks / totalChunks) * 100)
          : 0;
        progressItem.server.status = state.uploadedChunks > 0 ? 'uploading' : 'waiting';

        taskIdToProgress.set(queueTaskId, progressItem);

        if (!resumableInfo.value || resumableInfo.value.taskId === taskId) {
          resumableInfo.value = {
            taskId,
            totalChunks,
            uploadedChunks: state.uploadedChunks
          };
        }
      },
      onChunkComplete: (
        _chunkIndex: number,
        totalChunks: number,
        taskId: string,
        completedCount: number
      ) => {
        if (!taskId) return;

        serverTaskId = taskId;

        const rawFile = file.raw as File;
        const state = serverTaskState.get(taskId) || {
          taskId,
          totalChunks,
          uploadedChunks: 0
        };

        state.totalChunks = totalChunks;
        state.uploadedChunks = completedCount;
        serverTaskState.set(taskId, state);

        const progressRatio = totalChunks > 0
          ? Math.min(completedCount / totalChunks, 1)
          : 1;

        progressItem.client.percentage = toPercent(progressRatio * 100);
        progressItem.client.loaded = rawFile.size * progressRatio;
        progressItem.server.totalChunks = totalChunks;
        progressItem.server.currentChunk = completedCount;
        progressItem.server.percentage = toPercent(progressRatio * 100);
        progressItem.server.status = completedCount >= totalChunks ? 'success' : 'uploading';

        if (!resumableInfo.value || resumableInfo.value.taskId === taskId) {
          resumableInfo.value = {
            taskId,
            totalChunks,
            uploadedChunks: completedCount
          };
        }
      },
      onComplete: (data: any) => {
        progressItem.client.status = 'success';
        progressItem.server.status = 'success';
        progressItem.server.percentage = 100;
        progressItem.server.currentChunk = progressItem.server.totalChunks;

        const isInstant = progressItem.server.totalChunks === 0 ||
          progressItem.server.currentChunk === 0;

        uploadedFiles.value.push({
          ...data,
          isInstant
        });

        if (isInstant) {
          ElMessage.success(`${file.name} 秒传成功！`);
        }

        if (serverTaskId) {
          serverTaskState.delete(serverTaskId);
        }
        taskIdToProgress.delete(queueTaskId);
      },
      onError: (error: Error) => {
        progressItem.client.status = 'exception';
        progressItem.server.status = 'exception';
        ElMessage.error(`${file.name} 上传失败: ${error.message}`);

        if (serverTaskId) {
          serverTaskState.delete(serverTaskId);
        }
        taskIdToProgress.delete(queueTaskId);
      }
    };

    queueTaskId = queueManager.addTask(file.raw as File, 5, options);
    taskIdToProgress.set(queueTaskId, progressItem);
  }

  if (queueManager.getTasks().length === 0) {
    isUploading.value = false;
    totalUploadCount.value = 0;
    return;
  }

  queueManager.start();
};

// 使用普通上传（原有逻辑）
const handleNormalUpload = async () => {
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
            progressItem.client.percentage = toPercent((progressEvent.loaded / progressEvent.total) * 100);
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
  await Promise.allSettled(workers);
};

const handleUpload = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件');
    return;
  }

  try {
    await request.get('/upload/permission-check');
  } catch (error: any) {
    const message = error?.response?.data?.msg || error?.message;
    if (message && message !== '登录状态已过期，请重新登录') {
      ElMessage.error(message);
    }
    return;
  }

  isUploading.value = true;
  isPaused.value = false;
  uploadedFiles.value = [];
  resumableInfo.value = null;
  totalUploadCount.value = selectedFiles.value.length;

  uploadProgress.value = selectedFiles.value.map(f => reactive({
    uid: f.uid,
    name: f.name,
    total: f.size || 0,
    client: { percentage: 0, loaded: 0, status: 'uploading' },
    server: { percentage: 0, currentChunk: 0, totalChunks: 0, status: 'waiting' },
  }));

  const smallFiles = enableResumable.value
    ? selectedFiles.value.filter(f => (f.raw?.size ?? f.size ?? 0) <= RESUMABLE_MIN_SIZE)
    : [];

  const shouldUseResumable = enableResumable.value && smallFiles.length === 0;

  if (shouldUseResumable) {
    await handleResumableUpload();
    selectedFiles.value = [];
    uploadRef.value?.clearFiles();
    return;
  }

  if (enableResumable.value && smallFiles.length > 0) {
    const names = smallFiles.map(f => f.name).join('、');
    ElMessage.warning(`以下文件不满足断点续传最小限制(>10MB)，已改用普通上传：${names}`);
  }

  try {
    await handleNormalUpload();
  } finally {
    isUploading.value = false;
    isPaused.value = false;
    selectedFiles.value = [];
    uploadRef.value?.clearFiles();
    resumableInfo.value = null;
    totalUploadCount.value = 0;
  }
};

// WebSocket相关方法保持不变
const connectWebSocket = () => {
  if (websocket.value) {
    manualClosedSockets.add(websocket.value);
    websocket.value.close();
  }

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
      reconnectDelay.value = 1000;
      reconnectFailureNotified.value = false;
      connectionLost.value = false;
      startHeartbeat();
    };

    websocket.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'pong') {
          clearHeartbeatTimeout();
          return;
        }

        const progressItem = uploadProgress.value.find(p => p.name === data.fileName);
        if (!progressItem) return;

        if (data.type === 'upload_progress') {
          const totalChunks = data.total_chunks || data.totalChunks;
          const currentChunk = data.current_chunk || data.currentChunk;

          if (totalChunks !== undefined) {
            progressItem.server.totalChunks = totalChunks;
          }

          if (currentChunk !== undefined) {
            const confirmedChunks = resumableInfo.value && resumableInfo.value.taskId
              ? resumableInfo.value.uploadedChunks
              : 0;
            const safeCurrent = Math.min(currentChunk, confirmedChunks);
            if (safeCurrent > 0) {
              progressItem.server.status = safeCurrent >= (progressItem.server.totalChunks || 0)
                ? 'success'
                : 'uploading';
            }
            progressItem.server.currentChunk = safeCurrent;
          }

          if (data.percentage !== undefined) {
            const confirmedChunks = resumableInfo.value && resumableInfo.value.taskId
              ? resumableInfo.value.uploadedChunks
              : 0;
            const safePercentage = totalChunks
              ? Math.min(data.percentage, (confirmedChunks / totalChunks) * 100)
              : data.percentage;
            progressItem.server.percentage = toPercent(safePercentage);
          }
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

      if (isPageVisible.value && reconnectAttempts.value < maxReconnectAttempts) {
        reconnectAttempts.value++;
        console.log(`尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})...`);

        reconnectTimer.value = window.setTimeout(() => {
          connectWebSocket();
        }, reconnectDelay.value);

        reconnectDelay.value = Math.min(reconnectDelay.value * 2, 30000);
      } else if (isPageVisible.value && !reconnectFailureNotified.value) {
        reconnectFailureNotified.value = true;
        connectionLost.value = true;
        ElMessage.error('实时连接多次尝试失败，请检查网络后点击"重新连接"按钮。');
      }
    };
  } catch (error) {
    console.error('创建 WebSocket 连接失败:', error);
    if (isPageVisible.value && reconnectAttempts.value < maxReconnectAttempts) {
      reconnectAttempts.value++;
      reconnectTimer.value = window.setTimeout(() => {
        connectWebSocket();
      }, reconnectDelay.value);
      reconnectDelay.value = Math.min(reconnectDelay.value * 2, 30000);
    } else if (isPageVisible.value && !reconnectFailureNotified.value) {
      reconnectFailureNotified.value = true;
      connectionLost.value = true;
      ElMessage.error('实时连接多次尝试失败，请检查网络后点击"重新连接"按钮。');
    }
  }
};

const retryWebSocket = () => {
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value);
    reconnectTimer.value = null;
  }
  reconnectAttempts.value = 0;
  reconnectDelay.value = 1000;
  connectionLost.value = false;
  reconnectFailureNotified.value = false;
  connectWebSocket();
};

const clearHeartbeatTimeout = () => {
  if (heartbeatTimeoutTimer.value) {
    clearTimeout(heartbeatTimeoutTimer.value);
    heartbeatTimeoutTimer.value = null;
  }
};

const scheduleHeartbeatTimeout = () => {
  clearHeartbeatTimeout();
  heartbeatTimeoutTimer.value = window.setTimeout(() => {
    console.warn('WebSocket 心跳超时，尝试重新连接');
    if (websocket.value && websocket.value.readyState === WebSocket.OPEN) {
      websocket.value.close();
    }
  }, HEARTBEAT_TIMEOUT);
};

const sendHeartbeat = () => {
  if (websocket.value && websocket.value.readyState === WebSocket.OPEN) {
    websocket.value.send(JSON.stringify({ type: 'ping' }));
    scheduleHeartbeatTimeout();
  } else {
    clearHeartbeatTimeout();
  }
};

const startHeartbeat = () => {
  stopHeartbeat();
  sendHeartbeat();
  heartbeatTimer.value = window.setInterval(() => {
    sendHeartbeat();
  }, HEARTBEAT_INTERVAL);
};

const stopHeartbeat = () => {
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value);
    heartbeatTimer.value = null;
  }
  clearHeartbeatTimeout();
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
  if (isUploading.value) {
    ElMessage.warning('当前正在上传，请稍后再粘贴文件');
    return;
  }
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

  const handleVisibilityChange = () => {
    isPageVisible.value = !document.hidden;
    if (!document.hidden) {
      console.log('页面显示，检查 WebSocket 连接状态');
      if (connectionLost.value) {
        return;
      }
      if (!websocket.value || websocket.value.readyState !== WebSocket.OPEN) {
        connectWebSocket();
      }
    } else {
      console.log('页面隐藏，保持 WebSocket 连接');
    }
  };

  document.addEventListener('visibilitychange', handleVisibilityChange);

  const handleFocus = () => {
    if (connectionLost.value) {
      console.log('窗口获得焦点，但连接已标记为失败，等待用户手动重连');
      return;
    }
    const autoReconnecting = reconnectTimer.value !== null || reconnectAttempts.value > 0;
    if (autoReconnecting) {
      console.log('窗口获得焦点，自动重连进行中，跳过额外检查');
      return;
    }
    if (!websocket.value || websocket.value.readyState !== WebSocket.OPEN) {
      console.log('窗口获得焦点，尝试恢复 WebSocket 连接');
      connectWebSocket();
    }
  };

  window.addEventListener('focus', handleFocus);

  connectWebSocket();

  (window as any).__visibilityHandler = handleVisibilityChange;
  (window as any).__focusHandler = handleFocus;
});

onBeforeUnmount(() => {
  window.removeEventListener('paste', handlePaste);

  if ((window as any).__visibilityHandler) {
    document.removeEventListener('visibilitychange', (window as any).__visibilityHandler);
    delete (window as any).__visibilityHandler;
  }

  if ((window as any).__focusHandler) {
    window.removeEventListener('focus', (window as any).__focusHandler);
    delete (window as any).__focusHandler;
  }

  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value);
  }
  stopHeartbeat();

  if (websocket.value) {
    manualClosedSockets.add(websocket.value);
    websocket.value.close();
  }

  queueManager.stop();
  queueManager.cancelAll().catch(() => {
    // 忽略组件卸载时的取消异常
  });
});
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
}

.connection-alert {
  margin-bottom: 16px;
}

.connection-alert-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
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
  display: flex;
  justify-content: center;
  gap: 10px;
}

.resumable-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 10px;
  color: var(--el-color-success);
  font-weight: 500;
}

.resumable-info {
  margin-top: 20px;
  padding: 15px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.progress-section {
  margin-top: 20px;
  max-height: 400px;
  overflow-y: auto;
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
  background-color: #f9fafb;
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
  min-width: 0;
}

.uploaded-file-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.file-actions {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
  min-width: 120px;
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

@media (max-width: 767px) {
  .page-container {
    padding: 10px;
  }

  .card-header {
    flex-wrap: wrap;
    justify-content: space-between;
    text-align: left;
    gap: 8px;
  }

  .upload-actions {
    margin-top: 15px;
    flex-direction: column;
  }

  .upload-actions .el-button {
    width: 100%;
    padding: 12px 20px;
    font-size: 16px;
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
}
</style>

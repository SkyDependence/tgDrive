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
              <div v-for="file in uploadProgress" :key="file.uid" class="file-progress-item">
                <span class="file-name">{{ file.name }}</span>
                <el-progress
                  :percentage="Math.round(file.percentage * 100) / 100"
                  :status="file.status"
                  :stroke-width="8"
                  striped
                  :striped-flow="file.status !== 'success' && file.status !== 'exception'"
                />
                <div class="file-size-info">
                  {{ formatFileSize(file.loaded) }} / {{ formatFileSize(file.total) }}
                </div>
                <!-- 服务器到Telegram进度 -->
                <div v-if="file.serverToTelegram.status === 'uploading' || file.serverToTelegram.status === 'completed'" class="telegram-info">
                  <div class="telegram-progress-text">
                    <span v-if="file.serverToTelegram.status === 'completed'">✅ 已传输到Telegram</span>
                    <span v-else>
                      传输到Telegram: {{ file.serverToTelegram.currentChunk || 0 }}/{{ file.serverToTelegram.totalChunks || 0 }} 片
                      ({{ Math.round(file.serverToTelegram.percentage * 100) / 100 }}%)
                    </span>
                  </div>
                  <el-progress
                    :percentage="Math.round(file.serverToTelegram.percentage * 100) / 100"
                    :status="file.serverToTelegram.status === 'completed' ? 'success' : file.status"
                    :stroke-width="8"
                    striped
                    :striped-flow="file.serverToTelegram.status !== 'completed' && file.status !== 'exception'"
                  />
                </div>
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
            <el-button @click="batchCopyMarkdown" :disabled="uploadedFiles.length === 0" size="small" plain>批量复制 (MD)</el-button>
            <el-button @click="batchCopyLinks" :disabled="uploadedFiles.length === 0" size="small" plain>批量复制 (链接)</el-button>
          </div>
          
          <!-- 批量操作按钮移到这里 -->

        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { ElMessage, UploadFile, UploadFiles, UploadRawFile, UploadInstance } from 'element-plus';
import { UploadFilled, Upload, Document, Link, Tickets, Paperclip, View } from '@element-plus/icons-vue';

interface UploadedFile {
  fileName: string;
  downloadLink: string;
  fileId: string;
}

interface ProgressItem {
  uid: number;
  name: string;
  percentage: number;
  status: 'success' | 'exception' | undefined;
  loaded: number;
  total: number;
  // 服务器到Telegram的进度
  serverToTelegram: {
    percentage: number;
    currentChunk: number;
    totalChunks: number;
    status: 'waiting' | 'uploading' | 'completed' | 'error';
  };
}

const router = useRouter();
const uploadRef = ref<UploadInstance>();

const selectedFiles = ref<UploadFile[]>([]);
const uploadedFiles = ref<UploadedFile[]>([]);
const isUploading = ref(false);
const uploadProgress = ref<ProgressItem[]>([]);
const websocket = ref<WebSocket | null>(null);

const uploadCompletedCount = computed(() => 
  uploadProgress.value.filter(p => p.status === 'success').length
);



const handleFileChange = (file: UploadFile, fileList: UploadFiles) => {
  selectedFiles.value = fileList;
};

const handleFileRemove = (file: UploadFile, fileList: UploadFiles) => {
  selectedFiles.value = fileList;
};

const handleUpload = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件');
    return;
  }

  isUploading.value = true;
  uploadedFiles.value = []; // Clear previous results
  uploadProgress.value = selectedFiles.value.map(f => ({
    uid: f.uid,
    name: f.name,
    percentage: 0,
    status: undefined,
    loaded: 0,
    total: f.size || 0,
    serverToTelegram: {
      percentage: 0,
      currentChunk: 0,
      totalChunks: 0,
      status: 'waiting' as const
    }
  }));

  for (const file of selectedFiles.value) {
    const progressItem = uploadProgress.value.find(p => p.uid === file.uid);
    if (!progressItem) continue;

    const rawFile = file.raw as File;
    
    try {
      // 使用普通上传
      const formData = new FormData();
      formData.append('file', rawFile);

      const response = await axios.post('/api/upload', formData, {
        timeout: 21600000, // 6小时超时
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            progressItem.loaded = progressEvent.loaded;
            progressItem.total = progressEvent.total;
            const uploadPercentage = Math.round((progressEvent.loaded / progressEvent.total) * 10000) / 100;
            progressItem.percentage = uploadPercentage;
          }
        }
      });

      const { code, msg, data } = response.data;
      if (code === 1) {
        // 上传完成，开始服务器到Telegram阶段
        progressItem.serverToTelegram.status = 'uploading';
        uploadedFiles.value.push(data);
        // 不在这里显示消息，等待WebSocket的upload_complete消息
      } else {
        progressItem.status = 'exception';
        ElMessage.error(`${file.name} 上传失败: ${msg || '未知错误'}`);
      }
    } catch (error: any) {
      progressItem.status = 'exception';
      const errorMsg = error.response?.data?.msg || error.message || '网络错误';
      ElMessage.error(`${file.name} 上传失败: ${errorMsg}`);
    }
  }

  isUploading.value = false;
  selectedFiles.value = [];
  uploadRef.value?.clearFiles();
};

const goToFileList = () => {
  router.push('/fileList');
};

const copyToClipboard = (text: string, message: string) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(message);
  }).catch(err => {
    ElMessage.error('复制失败: ' + err);
  });
};

const copyMarkdown = (file: UploadedFile) => {
  copyToClipboard(`[${file.fileName}](${file.downloadLink})`, 'Markdown 格式已复制');
};

const copyLink = (file: UploadedFile) => {
  copyToClipboard(file.downloadLink, '下载链接已复制');
};

const openLink = (url: string) => {
  window.open(url, '_blank');
};

const batchCopyMarkdown = () => {
  const text = uploadedFiles.value.map(f => `[${f.fileName}](${f.downloadLink})`).join('\n');
  copyToClipboard(text, `已批量复制 ${uploadedFiles.value.length} 个 Markdown 链接`);
};

const batchCopyLinks = () => {
  const text = uploadedFiles.value.map(f => f.downloadLink).join('\n');
  copyToClipboard(text, `已批量复制 ${uploadedFiles.value.length} 个下载链接`);
};

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const handlePaste = (event: ClipboardEvent) => {
  const items = event.clipboardData?.items;
  if (!items) return;

  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file') {
      const file = items[i].getAsFile();
      if (file) {
        const uid = Date.now() + i;
        const uploadFile: UploadFile = {
          name: file.name,
          size: file.size,
          uid: uid,
          raw: Object.assign(file, { uid }) as UploadRawFile,
          status: 'ready',
        };
        
        const isDuplicate = selectedFiles.value.some(f => f.name === uploadFile.name && f.size === uploadFile.size);
        if (!isDuplicate) {
          selectedFiles.value.push(uploadFile);
        }
      }
    }
  }
  if (selectedFiles.value.length > 0) {
    ElMessage.success('已通过粘贴添加文件');
  }
};

const connectWebSocket = () => {
  const wsUrl = `ws://${window.location.host}/ws/upload-progress`;
  websocket.value = new WebSocket(wsUrl);
  
  websocket.value.onmessage = (event) => {
     try {
       const data = JSON.parse(event.data);
       console.log('WebSocket received:', data);
       
       if (data.type === 'upload_progress') {
         const progressItem = uploadProgress.value.find(p => p.name === data.fileName);
         // 更新服务器到Telegram的进度
         if (progressItem && progressItem.serverToTelegram.status !== 'completed') {
           // 确保服务器到Telegram阶段已开始
           if (progressItem.serverToTelegram.status === 'waiting') {
             progressItem.serverToTelegram.status = 'uploading';
           }
           progressItem.serverToTelegram.percentage = Math.round(data.percentage * 100) / 100;
           if (data.currentChunk !== undefined) {
             progressItem.serverToTelegram.currentChunk = data.currentChunk;
           }
           if (data.totalChunks !== undefined) {
             progressItem.serverToTelegram.totalChunks = data.totalChunks;
           }
           // 直接使用Telegram进度作为整体进度
           progressItem.percentage = Math.round(data.percentage * 100) / 100;
           console.log(`更新${data.fileName}的Telegram进度: ${data.percentage}%`);
         }
       } else if (data.type === 'upload_complete') {
         const progressItem = uploadProgress.value.find(p => p.name === data.fileName);
         if (progressItem) {
           // 服务器到Telegram阶段完成
           progressItem.serverToTelegram.status = 'completed';
           progressItem.serverToTelegram.percentage = 100;
           progressItem.status = 'success';
           progressItem.percentage = 100;
           ElMessage.success(`${data.fileName} 完全上传完成`);
         }
       } else if (data.type === 'upload_error') {
         const progressItem = uploadProgress.value.find(p => p.name === data.fileName);
         if (progressItem) {
           progressItem.status = 'exception';
           progressItem.serverToTelegram.status = 'error';
         }
         ElMessage.error(`${data.fileName} 传输到Telegram失败: ${data.error}`);
       }
     } catch (error) {
       console.error('WebSocket message parse error:', error);
     }
   };
  
  websocket.value.onerror = (error) => {
    console.error('WebSocket error:', error);
  };
};

const disconnectWebSocket = () => {
  if (websocket.value) {
    websocket.value.close();
    websocket.value = null;
  }
};

onMounted(() => {
  window.addEventListener('paste', handlePaste);
  connectWebSocket();
});

onBeforeUnmount(() => {
  window.removeEventListener('paste', handlePaste);
  disconnectWebSocket();
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

.file-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--el-text-color-primary);
}

.el-progress {
  width: 100%;
}

.file-size-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
  margin-top: 4px;
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
}

.uploaded-file-name {
  font-size: 14px;
}

.file-actions {
  display: flex;
  gap: 5px;
}

.batch-actions {
  margin-top: 20px;
  border-top: 1px solid var(--border-color);
  padding-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap; /* Allow buttons to wrap on smaller screens */
}

:deep(.el-progress-bar__inner--striped) {
  animation-duration: 2s; /* 减慢流动动画速度，默认为1s */
}

/* 双进度条样式 */
.dual-progress-container {
  margin-top: 10px;
  padding: 15px;
  background: var(--bg-color-light);
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
  color: var(--text-color-light);
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
    justify-content: center; /* Center header items */
    text-align: center;
  }

  .card-header .el-button {
    margin-top: 5px; /* Add some space if button wraps */
  }

  .upload-actions .el-button {
    width: 100%; /* Make upload button full width */
  }

  .file-progress-item {
    flex-direction: column; /* Stack file name and progress bar */
    align-items: flex-start; /* Align items to start */
  }

  .file-name {
    width: 100%; /* Take full width */
    max-width: none; /* Remove max-width constraint */
    text-align: left;
  }

  .el-progress {
    width: 100%; /* Make progress bar full width */
  }

  .uploaded-file-item {
    flex-direction: column; /* Stack file details and buttons */
    align-items: flex-start;
    gap: 8px;
    padding: 10px 12px;
  }

  .file-details {
    width: 100%;
  }

  .uploaded-file-name {
    font-size: 13px;
    word-break: break-all;
  }

  .file-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .file-actions .el-button {
    padding: 6px;
    margin: 0 2px;
  }
}
</style>
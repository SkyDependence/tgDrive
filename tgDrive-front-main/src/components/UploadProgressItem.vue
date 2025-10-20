<template>
  <div class="file-progress-item">
    <div class="progress-header">
      <span class="file-name">{{ item.name }}</span>
      <span class="file-size-info">{{ formatFileSize(item.total) }}</span>
    </div>
    <el-progress
      :percentage="totalPercentage"
      :status="progressStatus"
      :stroke-width="10"
      :striped="isInProgress"
      :striped-flow="isInProgress"
      class="unified-progress-bar"
    />
    <div class="progress-info-text">
      <span>{{ stageText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from 'vue';

// Define the structure of the progress item prop
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

const props = defineProps<{
  item: ProgressItem;
}>();

// --- Computed Properties for a Stable UI ---

const formatPercentage = (value: number) => Number(value.toFixed(2));

const totalPercentage = computed(() => {
  const item = props.item;
  const totalChunks = item.server.totalChunks || 0;
  const completedChunks = Math.min(item.server.currentChunk || 0, totalChunks);

  if (totalChunks > 0) {
    if (item.server.status === 'success') {
      return 100;
    }
    return formatPercentage((completedChunks / totalChunks) * 100);
  }

  // 普通上传（没有分块信息）使用“前50%本地、后50%服务器”策略
  if (item.server.status === 'success') {
    return 100;
  }
  if (item.server.status === 'uploading') {
    return formatPercentage(50 + item.server.percentage * 0.5);
  }
  if (item.client.status === 'success') {
    return 50;
  }
  if (item.client.status === 'uploading') {
    return formatPercentage(item.client.percentage * 0.5);
  }
  if (item.client.status === 'exception') {
    return formatPercentage(item.client.percentage * 0.5);
  }
  if (item.server.status === 'exception') {
    return formatPercentage(50 + item.server.percentage * 0.5);
  }
  return 0;
});

const progressStatus = computed(() => {
  const item = props.item;
  if (item.client.status === 'exception' || item.server.status === 'exception') {
    return 'exception';
  }
  if (item.server.status === 'success') {
    return 'success';
  }
  return '';
});

const isInProgress = computed(() => {
  const item = props.item;
  if (item.client.status === 'exception' || item.server.status === 'exception') return false;
  if (item.server.status === 'success') return false;
  return true;
});

const stageText = computed(() => {
  const item = props.item;
  const totalChunks = item.server.totalChunks || 0;
  const completedChunks = Math.min(item.server.currentChunk || 0, totalChunks);

  if (item.server.status === 'success') {
    return totalChunks > 0
      ? `✅ 上传完成 (${totalChunks}/${totalChunks} 块)`
      : '✅ 上传完成';
  }
  if (item.client.status === 'exception') {
    return `❌ 阶段1失败: 上传到服务器时出错`;
  }
  if (item.server.status === 'exception') {
    return `❌ 阶段2失败: 从服务器传输时出错`;
  }
  if (totalChunks > 0) {
    return `上传进度：${completedChunks}/${totalChunks} 块`;
  }
  if (item.server.status === 'uploading') {
    return `阶段2: 传输到服务器 (${formatPercentage(item.server.percentage)}%)`;
  }
  if (item.client.status === 'success') {
    return '阶段2: 等待服务器处理...';
  }
  if (item.client.status === 'uploading') {
    return `阶段1: 上传到服务器 (${formatFileSize(item.client.loaded)})`;
  }
  return '等待上传';
});

// --- Utility ---
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};
</script>

<style scoped>
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
  color: var(--el-text-color-secondary);
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
</style>

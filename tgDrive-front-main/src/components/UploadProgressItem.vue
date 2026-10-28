<template>
  <div class="file-progress-item">
    <div class="progress-header">
      <span class="file-name">{{ item.name }}</span>
      <span class="file-size-info">{{ formatFileSize(item.total) }}</span>
    </div>
    <el-progress
      :percentage="displayPercentage"
      :status="progressStatus"
      :stroke-width="10"
      :striped="isInProgress"
      :striped-flow="isInProgress"
      :format="formatPercentage"
      class="unified-progress-bar"
    >
      <template #default="{ percentage }">
        <span v-if="progressStatus === 'success'" class="progress-check-icon" aria-label="上传完成">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <circle cx="12" cy="12" r="11" fill="url(#uploadSuccessGrad)" />
            <path d="M7 12.5l3.2 3.2L17 9" fill="none" stroke="#fff" stroke-width="2.2"
                  stroke-linecap="round" stroke-linejoin="round" />
            <defs>
              <linearGradient id="uploadSuccessGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#34d399" />
                <stop offset="100%" stop-color="#059669" />
              </linearGradient>
            </defs>
          </svg>
        </span>
        <span v-else-if="progressStatus === 'exception'" class="progress-percentage is-error">
          {{ percentage }}%
        </span>
        <span v-else class="progress-percentage">{{ percentage }}%</span>
      </template>
    </el-progress>
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

const totalPercentage = computed(() => {
  const item = props.item;
  // The success of the server stage is the definitive final state.
  // This rule must be first to ensure it overrides any other state.
  if (item.server.status === 'success') {
    return 100;
  }
  // If the server is uploading, calculate the second half of the progress.
  if (item.server.status === 'uploading') {
    return 50 + (item.server.percentage * 0.5);
  }
  // If the client has finished and the server is waiting, we are exactly at the halfway mark.
  if (item.client.status === 'success') {
    return 50;
  }
  // If the client is uploading, calculate the first half of the progress.
  if (item.client.status === 'uploading') {
    return item.client.percentage * 0.5;
  }
  // Handle failure cases to show where it stopped.
  if (item.client.status === 'exception') {
    return item.client.percentage * 0.5;
  }
  if (item.server.status === 'exception') {
    return 50 + (item.server.percentage * 0.5);
  }
  return 0;
});

// 进度条 percentage 只接受数值，且需限制在 0~100；取整避免出现超长小数（如 8.377836248437022%）
const displayPercentage = computed(() => {
  const v = totalPercentage.value;
  if (Number.isNaN(v)) return 0;
  return Math.min(100, Math.max(0, Math.round(v)));
});

// 自定义百分比文案（配合 #default 插槽，成功态显示对勾图标而非数字）
const formatPercentage = (percentage: number) => `${percentage}%`;

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
  if (item.server.status === 'success') {
    return '上传完成';
  }
  if (item.client.status === 'exception') {
    return '阶段1失败：上传到服务器时出错';
  }
  if (item.server.status === 'exception') {
    return '阶段2失败：从服务器传输时出错';
  }
  if (item.server.status === 'uploading') {
    return `阶段2: 传输到Telegram (${item.server.currentChunk}/${item.server.totalChunks}块)`;
  }
  if (item.client.status === 'success') {
    return '阶段2: 等待传输...';
  }
  return `阶段1: 上传到服务器 (${formatFileSize(item.client.loaded)})`;
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

.progress-percentage {
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-regular);
}

.progress-percentage.is-error {
  color: var(--el-color-danger);
}

.progress-check-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  animation: check-pop 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.progress-check-icon svg {
  display: block;
  filter: drop-shadow(0 1px 2px rgba(5, 150, 105, 0.35));
}

@keyframes check-pop {
  0% { transform: scale(0.2); opacity: 0; }
  60% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}
</style>

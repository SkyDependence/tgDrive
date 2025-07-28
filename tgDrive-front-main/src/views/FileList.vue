<template>
  <div class="page-container">
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><FolderOpened /></el-icon>
            <span>文件列表</span>
          </div>
          <div class="header-right">
            <el-select 
              v-if="currentRole === 'admin'" 
              v-model="selectedUserId" 
              placeholder="选择用户" 
              clearable 
              filterable
              style="width: 150px; margin-right: 10px;"
              @change="handleUserChange"
              :max-height="200"
            >
              <el-option label="全部用户" :value="null" />
              <el-option 
                v-for="user in userList" 
                :key="user.id" 
                :label="user.username" 
                :value="user.id"
              />
            </el-select>
            <el-input v-model="searchQuery" placeholder="搜索文件名" clearable style="width: 200px; margin-right: 10px;" @keyup.enter="handleSearch" />
            <div class="search-button-group">
              <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
              <el-button type="default" @click="clearSearch" :icon="Refresh">全部</el-button>
            </div>
            <el-button v-if="currentRole === 'admin'" type="primary" @click="openUpdateDialog" :icon="Refresh" class="update-url-btn">更新 URL 前缀</el-button>
          </div>
        </div>
      </template>

      <!-- Desktop Table View -->
      <el-table
        v-if="!isMobile"
        :data="fileList"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        height="calc(100vh - 280px)"
        style="width: 100%;"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip fixed="left">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><Document /></el-icon>
              <span>{{ scope.row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center">
          <template #default="scope">
            {{ formatUploadTime(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column v-if="currentRole === 'admin'" prop="uploader" label="上传者" width="120" align="center">
          <template #default="scope">
            {{ scope.row.uploader || '未知' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="480" align="center" fixed="right">
          <template #default="scope">
            <el-button-group>
              <el-button type="info" size="small" @click="previewFileAction(scope.row)" :icon="View">预览</el-button>
              <el-button type="primary" size="small" @click="copyMarkdown(scope.row)" :icon="Memo">Markdown</el-button>
              <el-button type="success" size="small" @click="copyLink(scope.row)" :icon="Link">链接</el-button>
              <el-button type="warning" size="small" @click="openLink(scope.row.downloadUrl)" :icon="Download">下载</el-button>
              <el-button type="danger" size="small" @click="deleteFile(scope.row)" :icon="Delete">删除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- Mobile List View -->
      <div v-if="isMobile" class="mobile-file-list" ref="mobileListRef">
        <el-empty v-if="fileList.length === 0 && !loading" description="暂无文件" />
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else>
          <div 
            v-for="file in fileList" 
            :key="file.fileId" 
            class="mobile-file-item"
            :class="{ 'is-selected': isSelected(file) }"
            @click="toggleSelection(file)"
          >
            <div class="file-info">
              <el-checkbox 
                :model-value="isSelected(file)" 
                @change="() => toggleSelection(file)" 
                @click.stop
                size="large"
                class="mobile-checkbox"
              />
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name-mobile">{{ file.fileName }}</span>
            </div>
            <div class="file-meta">
              <span>大小: {{ file.size }}</span>
              <span>上传时间: {{ formatUploadTime(file.uploadTime) }}</span>
              <span v-if="currentRole === 'admin'">上传者: {{ file.uploader || '未知' }}</span>
              
            </div>
            <div class="file-actions">
              <el-button-group>
                <el-button type="info" size="small" @click.stop="previewFileAction(file)" :icon="View" circle />
                <el-button type="primary" size="small" @click.stop="copyMarkdown(file)" :icon="Memo" circle />
                <el-button type="success" size="small" @click.stop="copyLink(file)" :icon="Link" circle />
                <el-button type="warning" size="small" @click.stop="openLink(file.downloadUrl)" :icon="Download" circle />
                <el-button type="danger" size="small" @click.stop="deleteFile(file)" :icon="Delete" circle />
              </el-button-group>
            </div>
          </div>
        </div>
      </div>

      <div class="footer-toolbar" :class="{ 'is-mobile': isMobile }">
        <div class="batch-actions">
          <el-button 
            type="primary" 
            @click="batchCopyMarkdown" 
            :disabled="selectedFiles.length === 0"
            :icon="Memo"
            plain
          >
            批量复制 (MD)
          </el-button>
          <el-button 
            type="success" 
            @click="batchCopyLinks" 
            :disabled="selectedFiles.length === 0"
            :icon="Link"
            plain
          >
            批量复制 (链接)
          </el-button>
          <el-button 
            type="danger" 
            @click="batchDeleteFiles" 
            :disabled="selectedFiles.length === 0"
            :icon="Delete"
            plain
          >
            批量删除 ({{ selectedFiles.length }})
          </el-button>
        </div>
        <el-pagination
          v-if="totalItems > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalItems"
          :page-sizes="[10, 20, 50, 100]"
          :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          background
          small
        />
      </div>
    </el-card>

    <el-dialog v-model="isDialogVisible" title="更新 URL 前缀" width="500px">
      <span>
        此操作将根据服务器最新配置，更新所有文件的下载链接前缀。当您更换了域名或IP地址后，可以使用此功能。
      </span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="isDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmUpdate">确认更新</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件预览组件 -->
    <FilePreview
      v-model="showPreview"
      :file-info="previewFile"
      @download="handleDownloadFromPreview"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import request from '../utils/request';
import { ElMessage, ElCheckbox, ElMessageBox } from 'element-plus';
import { FolderOpened, Refresh, Document, Link, Download, Memo, Delete, View, Search } from '@element-plus/icons-vue';
import FilePreview from '../components/FilePreview.vue';

interface FileItem {
  fileName: string;
  downloadUrl: string;
  size: string;
  fileId: string;
  uploadTime: number;
  userId?: number;
  isPublic: boolean;
  uploader?: string;
}

const fileList = ref<FileItem[]>([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const totalItems = ref(0);
const isDialogVisible = ref(false);
const selectedFiles = ref<FileItem[]>([]);
const isMobile = ref(false);
const mobileListRef = ref<HTMLElement | null>(null);
const showPreview = ref(false);
const previewFile = ref<FileItem | null>(null);
const searchQuery = ref('');
const selectedUserId = ref<number | null>(null);
const userList = ref<{id: number, username: string}[]>([]);

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  lastLoginTime?: string;
}

// 获取当前用户信息
const currentRole = computed(() => localStorage.getItem('role') || '');
const currentUserId = computed(() => {
  const userId = localStorage.getItem('userId');
  return userId ? parseInt(userId) : null;
});

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768;
};

const isSelected = (file: FileItem) => {
  return selectedFiles.value.some(selected => selected.fileId === file.fileId);
};

const toggleSelection = (file: FileItem) => {
  if (isSelected(file)) {
    selectedFiles.value = selectedFiles.value.filter(selected => selected.fileId !== file.fileId);
  } else {
    selectedFiles.value.push(file);
  }
};

const fetchFileList = async () => {
  loading.value = true;
  try {
    let url = `/fileList?page=${currentPage.value}&size=${pageSize.value}`;
    if (searchQuery.value) {
      url += `&keyword=${encodeURIComponent(searchQuery.value)}`;
    }
    if (selectedUserId.value) {
      url += `&userId=${selectedUserId.value}`;
    }
    const response = await request.get(url);
    if (response.data?.code === 1) {
      const pageResult = response.data.data;
      fileList.value = pageResult.records || [];
      totalItems.value = pageResult.total || 0;
    } else {
      ElMessage.error(response.data?.msg || '获取文件列表失败');
    }
  } catch (error) {
    ElMessage.error('获取文件列表失败，请检查网络');
  } finally {
    loading.value = false;
  }
};

const handleSelectionChange = (selection: FileItem[]) => {
  selectedFiles.value = selection;
};

const copyToClipboard = (text: string, message: string) => {
  // 优先使用现代的 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      ElMessage.success(message);
    }).catch(err => {
      console.error('Clipboard API failed:', err);
      fallbackCopyTextToClipboard(text, message);
    });
  } else {
    // 降级到传统方法
    fallbackCopyTextToClipboard(text, message);
  }
};

// 传统的复制方法作为降级方案
const fallbackCopyTextToClipboard = (text: string, message: string) => {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  
  // 避免滚动到底部
  textArea.style.top = '0';
  textArea.style.left = '0';
  textArea.style.position = 'fixed';
  textArea.style.opacity = '0';
  
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  
  try {
    const successful = document.execCommand('copy');
    if (successful) {
      ElMessage.success(message);
    } else {
      ElMessage.error('复制失败，请手动复制');
    }
  } catch (err) {
    console.error('Fallback copy failed:', err);
    ElMessage.error('复制失败，请手动复制');
  }
  
  document.body.removeChild(textArea);
};

const batchCopyMarkdown = () => {
  const text = selectedFiles.value.map(f => `[${f.fileName}](${f.downloadUrl})`).join('\n');
  copyToClipboard(text, `已批量复制 ${selectedFiles.value.length} 个 Markdown 链接`);
};

const batchCopyLinks = () => {
  const text = selectedFiles.value.map(f => f.downloadUrl).join('\n');
  copyToClipboard(text, `已批量复制 ${selectedFiles.value.length} 个下载链接`);
};

const batchDeleteFiles = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择要删除的文件');
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？此操作不可撤销。`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );
    
    const deletePromises = selectedFiles.value.map(file => 
      request.delete(`/file/${file.fileId}`)
    );
    
    const results = await Promise.allSettled(deletePromises);
    
    let successCount = 0;
    let failCount = 0;
    
    results.forEach((result, index) => {
      if (result.status === 'fulfilled' && result.value.data.code === 1) {
        successCount++;
      } else {
        failCount++;
        console.error(`删除文件 ${selectedFiles.value[index].fileName} 失败:`, result);
      }
    });
    
    if (successCount > 0) {
      ElMessage.success(`成功删除 ${successCount} 个文件`);
    }
    if (failCount > 0) {
      ElMessage.error(`${failCount} 个文件删除失败`);
    }
    
    // 清空选中列表并重新加载文件列表
    selectedFiles.value = [];
    fetchFileList();
    
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('批量删除文件失败:', error);
      ElMessage.error('批量删除失败: ' + (error.response?.data?.msg || error.message));
    }
  }
};

const formatUploadTime = (timestamp: number) => {
  return new Date(timestamp * 1000).toLocaleString('zh-CN', { hour12: false });
};

const openUpdateDialog = () => {
  isDialogVisible.value = true;
};

const confirmUpdate = async () => {
  isDialogVisible.value = false;
  const loading = ElMessage({ message: '正在更新...', type: 'info', duration: 0 });
  try {
    const response = await request.put('/file-url');
    if (response.data?.code === 1) {
      ElMessage.success('URL 更新成功');
      fetchFileList();
    } else {
      ElMessage.error(response.data?.msg || 'URL 更新失败');
    }
  } catch (error) {
    ElMessage.error('更新失败，请检查网络');
  } finally {
    loading.close();
  }
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  selectedFiles.value = []; // Clear selection on page change
  fetchFileList();
  if (isMobile.value && mobileListRef.value) {
    mobileListRef.value.scrollTop = 0; // Scroll to top on page change
  }
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  selectedFiles.value = []; // Clear selection on size change
  fetchFileList();
};

const copyMarkdown = (row: FileItem) => {
  copyToClipboard(`[${row.fileName}](${row.downloadUrl})`, 'Markdown 格式已复制');
};

const copyLink = (row: FileItem) => {
  copyToClipboard(row.downloadUrl, '下载链接已复制');
};

const openLink = (url: string) => {
  window.open(url, '_blank');
};

const previewFileAction = (file: FileItem) => {
  previewFile.value = file;
  showPreview.value = true;
};

const handleDownloadFromPreview = (file: FileItem) => {
  openLink(file.downloadUrl);
};

const deleteFile = async (file: FileItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.fileName}" 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    );
    
    const response = await request.delete(`/file/${file.fileId}`);
    if (response.data.code === 1) {
      ElMessage.success('文件删除成功');
      // 从选中列表中移除已删除的文件
      selectedFiles.value = selectedFiles.value.filter(selected => selected.fileId !== file.fileId);
      // 重新加载文件列表
      fetchFileList();
    } else {
      ElMessage.error(response.data.msg || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除文件失败:', error);
      ElMessage.error('删除文件失败: ' + (error.response?.data?.message || error.message));
    }
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchFileList();
};

const clearSearch = () => {
  searchQuery.value = '';
  selectedUserId.value = null;
  currentPage.value = 1;
  fetchFileList();
};

// 获取用户列表
const fetchUserList = async () => {
  if (currentRole.value !== 'admin') return;
  
  try {
    const response = await request.get('/auth/admin/users');
    if (response.data?.code === 1) {
      userList.value = response.data.data.map((user: User) => ({
        id: user.id,
        username: user.username
      }));
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
  }
};

// 处理用户筛选变化
const handleUserChange = () => {
  currentPage.value = 1;
  fetchFileList();
};

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
  fetchFileList();
  fetchUserList();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile);
});
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  overflow: hidden; /* Prevent card from overflowing page */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
}

:deep(.el-card__body) {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden; /* Important for layout */
}

.el-table {
  flex-grow: 1;
}

.footer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0; /* Prevent footer from shrinking */
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.search-button-group {
  display: flex;
  gap: 8px;
}

/* 桌面端样式重置 */
@media (min-width: 768px) {
  .card-header {
    flex-direction: row;
    align-items: center;
    gap: 0;
  }

  .card-header .header-right {
    width: auto;
    display: flex;
    flex-direction: row;
    gap: 10px;
    align-items: center;
  }

  .card-header .header-right .el-select {
    width: 150px !important;
    margin-right: 10px !important;
  }

  .card-header .header-right .el-input {
    width: 200px !important;
    margin-right: 10px !important;
  }

  .card-header .header-right .el-button {
    width: auto;
    margin-left: 0 !important;
  }

  .search-button-group {
    width: auto;
    margin-right: 10px;
  }

  .search-button-group .el-button {
    flex: none;
    width: auto;
  }

  .update-url-btn {
    margin-left: 10px !important;
  }
}

/* Mobile List View Styles */
.mobile-file-list {
  padding: 10px;
  flex-grow: 1;
  overflow-y: auto; /* Enable scrolling for the list itself */
}

.mobile-file-item {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: var(--el-box-shadow-light);
  transition: background-color 0.3s, border-color 0.3s;
  cursor: pointer;
}

.mobile-file-item.is-selected {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.mobile-file-item .file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  flex: 1;
  min-width: 0;
}

.mobile-checkbox {
  margin-right: 8px;
}

.file-icon {
  font-size: 1.2em;
}

.mobile-file-item .file-name-mobile {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-file-item .file-meta {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  gap: 5px;
  padding-left: 40px; /* Align with file name */
}

.mobile-file-item .file-actions {
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
  min-width: 200px;
}

/* Responsive styles for FileList.vue */
@media (max-width: 767px) {
  .page-container {
    padding: 10px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .card-header .header-right {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .card-header .header-right .el-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .card-header .header-right .el-input {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .card-header .header-right .el-button {
    width: 100%;
    margin-left: 0 !important;
  }

  /* 移动端搜索按钮组 */
  .search-button-group {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .search-button-group .el-button {
    flex: 1;
  }

  .footer-toolbar.is-mobile {
    flex-direction: column;
    align-items: center;
    gap: 15px;
    padding: 15px 10px;
  }

  .batch-actions {
    width: 100%;
    display: flex;
    gap: 10px;
  }

  .batch-actions .el-button {
    flex: 1;
    min-width: 0; /* Allow button to shrink */
    font-size: 12px; /* Reduce font size on mobile */
    padding: 8px 4px; /* Reduce padding */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .el-pagination {
    width: 100%;
    justify-content: center;
  }

  :deep(.el-pagination .el-pagination__sizes) {
    margin: 0;
  }
}

/* Desktop 表格文件名列 */
.el-table-column[prop="fileName"] {
  .cell {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
}

/* 操作按钮区域 */
.el-table-column[label="操作"] {
  .el-button-group {
    display: flex;
    flex-wrap: nowrap;
    gap: 8px;
  }
}

/* 移动端文件名 */
.file-name-mobile {
  max-width: 60vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.file-actions .el-button-group {
  flex-wrap: wrap;
  row-gap: 8px;
  padding: 4px 0;
}
</style>

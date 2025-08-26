<template>
  <div class="chunk-upload">
    <el-upload
      ref="uploadRef"
      class="upload-demo"
      drag
      :auto-upload="false"
      :on-change="handleFileChange"
      :limit="1"
      :show-file-list="false"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持断点续传，上传中断后可以继续上传
        </div>
      </template>
    </el-upload>

    <!-- 上传进度 -->
    <div v-if="uploading" class="upload-progress">
      <el-progress
        :percentage="uploadProgress"
        :status="uploadStatus"
        :stroke-width="8"
      />
      <div class="progress-info">
        <span>已上传: {{ uploadedSize }} / {{ totalSize }}</span>
        <span>速度: {{ uploadSpeed }}/s</span>
      </div>
    </div>

    <!-- 上传列表 -->
    <div v-if="currentFile" class="file-info">
      <h3>当前文件: {{ currentFile.name }}</h3>
      <div class="chunk-list">
        <div
          v-for="chunk in chunkList"
          :key="chunk.number"
          :class="['chunk-item', chunk.status]"
        >
          <span>分块 {{ chunk.number }}</span>
          <el-icon v-if="chunk.status === 'completed'"><check /></el-icon>
          <el-icon v-else-if="chunk.status === 'uploading'"><loading /></el-icon>
          <el-icon v-else><close /></el-icon>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="currentFile" class="actions">
      <el-button
        v-if="!uploading && !completed"
        type="primary"
        @click="startUpload"
        :loading="checkingChunks"
      >
        {{ checkingChunks ? '检查中...' : (hasExistingChunks ? '继续上传' : '开始上传') }}
      </el-button>
      <el-button
        v-if="uploading"
        type="danger"
        @click="pauseUpload"
      >
        暂停上传
      </el-button>
      <el-button
        v-if="uploading || completed"
        @click="resetUpload"
      >
        重新选择
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Check, Loading, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  // 上传完成后的回调
  onSuccess: {
    type: Function,
    default: () => {}
  }
})

const uploadRef = ref(null)
const currentFile = ref(null)
const uploading = ref(false)
const completed = ref(false)
const checkingChunks = ref(false)
const uploadProgress = ref(0)
const uploadSpeed = ref('0 B')
const uploadedSize = ref('0 B')
const totalSize = ref('0 B')
const uploadStatus = ref('')
const sessionId = ref('')
const chunkList = ref([])
const uploadStartTime = ref(0)
const uploadedBytes = ref(0)

// 分块大小 (10MB)
const CHUNK_SIZE = 10 * 1024 * 1024

// 计算属性
const hasExistingChunks = computed(() => {
  return chunkList.value.some(chunk => chunk.status === 'completed')
})

// 格式化文件大小
const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 生成文件唯一标识
const generateFileIdentifier = async (file) => {
  // 使用文件名、大小和修改时间生成简单的标识
  const identifier = `${file.name}-${file.size}-${file.lastModified}`
  // 简单的哈希函数
  let hash = 0
  for (let i = 0; i < identifier.length; i++) {
    const char = identifier.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // 转换为32位整数
  }
  return Math.abs(hash).toString(36)
}

// 文件选择变化
const handleFileChange = async (file) => {
  if (!file) return
  
  currentFile.value = file.raw
  totalSize.value = formatSize(file.raw.size)
  resetUpload()
  
  // 生成文件标识
  const identifier = await generateFileIdentifier(file.raw)
  sessionId.value = identifier
  
  // 初始化上传会话
  try {
    const response = await request({
      url: '/api/chunk/init',
      method: 'post',
      data: {
        filename: file.raw.name,
        totalSize: file.raw.size
      }
    })
    
    if (response.code === 200) {
      const session = response.data
      sessionId.value = session.identifier
      
      // 初始化分块列表
      const totalChunks = Math.ceil(file.raw.size / CHUNK_SIZE)
      chunkList.value = Array.from({ length: totalChunks }, (_, i) => ({
        number: i + 1,
        status: 'pending'
      }))
      
      // 检查已上传的分块
      await checkExistingChunks()
    }
  } catch (error) {
    ElMessage.error('初始化上传失败')
    console.error('Init upload failed:', error)
  }
}

// 检查已上传的分块
const checkExistingChunks = async () => {
  checkingChunks.value = true
  try {
    const response = await request({
      url: `/api/chunk/${sessionId.value}/chunks`,
      method: 'get'
    })
    
    if (response.code === 200) {
      const uploadedChunks = response.data
      uploadedChunks.forEach(chunkNumber => {
        const chunk = chunkList.value.find(c => c.number === chunkNumber)
        if (chunk) {
          chunk.status = 'completed'
        }
      })
      
      // 计算已上传大小
      uploadedBytes.value = uploadedChunks.length * CHUNK_SIZE
      uploadedSize.value = formatSize(uploadedBytes.value)
      uploadProgress.value = Math.round((uploadedBytes.value / currentFile.value.size) * 100)
    }
  } catch (error) {
    console.error('Check chunks failed:', error)
  } finally {
    checkingChunks.value = false
  }
}

// 开始上传
const startUpload = async () => {
  if (!currentFile.value || uploading.value) return
  
  uploading.value = true
  uploadStatus.value = ''
  uploadStartTime.value = Date.now()
  
  try {
    await uploadChunks()
  } catch (error) {
    ElMessage.error('上传失败')
    console.error('Upload failed:', error)
    uploading.value = false
  }
}

// 上传分块
const uploadChunks = async () => {
  const file = currentFile.value
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  
  // 上传未完成的分块
  for (let i = 0; i < totalChunks; i++) {
    if (!uploading.value) break // 检查是否暂停
    
    const chunkNumber = i + 1
    const chunk = chunkList.value.find(c => c.number === chunkNumber)
    
    if (chunk && chunk.status !== 'completed') {
      chunk.status = 'uploading'
      
      const start = i * CHUNK_SIZE
      const end = Math.min(file.size, start + CHUNK_SIZE)
      const chunkFile = file.slice(start, end)
      
      // 创建 FormData
      const formData = new FormData()
      formData.append('file', chunkFile)
      formData.append('filename', `${file.name}.part${chunkNumber}`)
      
      try {
        await request({
          url: `/api/chunk/${sessionId.value}/${chunkNumber}`,
          method: 'post',
          data: formData,
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          onUploadProgress: (progressEvent) => {
            // 更新上传进度
            uploadedBytes.value = i * CHUNK_SIZE + progressEvent.loaded
            updateProgress()
          }
        })
        
        chunk.status = 'completed'
      } catch (error) {
        chunk.status = 'error'
        throw error
      }
    }
  }
  
  // 所有分块上传完成，执行合并
  if (uploading.value) {
    await completeUpload()
  }
}

// 完成上传
const completeUpload = async () => {
  try {
    const response = await request({
      url: `/api/chunk/${sessionId.value}/complete`,
      method: 'post',
      data: {
        filename: currentFile.value.name,
        totalChunks: chunkList.value.length,
        totalSize: currentFile.value.size
      }
    })
    
    if (response.code === 200) {
      completed.value = true
      uploading.value = false
      uploadStatus.value = 'success'
      ElMessage.success('上传完成')
      props.onSuccess(response.data)
    }
  } catch (error) {
    ElMessage.error('创建记录文件失败')
    throw error
  }
}

// 更新上传进度
const updateProgress = () => {
  const progress = Math.round((uploadedBytes.value / currentFile.value.size) * 100)
  uploadProgress.value = progress
  uploadedSize.value = formatSize(uploadedBytes.value)
  
  // 计算上传速度
  const elapsed = (Date.now() - uploadStartTime.value) / 1000
  const speed = uploadedBytes.value / elapsed
  uploadSpeed.value = formatSize(speed)
}

// 暂停上传
const pauseUpload = () => {
  uploading.value = false
  ElMessage.info('上传已暂停')
}

// 重置上传
const resetUpload = () => {
  uploading.value = false
  completed.value = false
  uploadProgress.value = 0
  uploadSpeed.value = '0 B'
  uploadedSize.value = '0 B'
  uploadedBytes.value = 0
  chunkList.value = []
  
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 组件卸载时清理
onUnmounted(() => {
  if (uploading.value && !completed.value) {
    // 可选：自动取消上传
    // cancelUpload()
  }
})
</script>

<style scoped>
.chunk-upload {
  max-width: 600px;
  margin: 0 auto;
}

.upload-demo {
  margin-bottom: 20px;
}

.upload-progress {
  margin: 20px 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}

.file-info {
  margin: 20px 0;
}

.chunk-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.chunk-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 12px;
}

.chunk-item.pending {
  background-color: #f5f7fa;
}

.chunk-item.uploading {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.chunk-item.completed {
  background-color: #f0f9ff;
  border-color: #67c23a;
}

.chunk-item.error {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
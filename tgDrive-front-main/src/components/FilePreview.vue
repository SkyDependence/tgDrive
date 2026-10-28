<template>
  <el-dialog
    v-model="visible"
    :title="fileInfo?.fileName || '文件预览'"
    width="80%"
    :before-close="handleClose"
    class="file-preview-dialog"
  >
    <div class="preview-container">
      <!-- 图片预览 -->
      <div v-if="isImage" class="image-preview">
        <el-image
          :src="inlinePreviewUrl"
          :alt="fileInfo?.fileName"
          fit="contain"
          class="preview-image"
          :preview-src-list="[inlinePreviewUrl]"
          :initial-index="0"
          preview-teleported
        />
      </div>

      <!-- 视频预览 -->
      <div v-else-if="isVideo" class="video-preview">
        <video
          :src="inlinePreviewUrl"
          controls
          class="preview-video"
          preload="metadata"
        >
          您的浏览器不支持视频播放
        </video>
      </div>

      <!-- 音频预览 -->
      <div v-else-if="isAudio" class="audio-preview">
        <div class="audio-info">
          <el-icon size="48" color="#409EFF"><Headset /></el-icon>
          <h3>{{ fileInfo?.fileName }}</h3>
        </div>
        <audio
          :src="inlinePreviewUrl"
          controls
          class="preview-audio"
          preload="metadata"
        >
          您的浏览器不支持音频播放
        </audio>
      </div>

      <!-- PDF预览 -->
      <div v-else-if="isPdf" class="pdf-preview">
        <div v-if="iframeLoading" class="iframe-loading" v-loading="true" element-loading-text="正在加载 PDF..."></div>
        <iframe
          :src="inlinePreviewUrl"
          class="preview-iframe"
          frameborder="0"
          @load="onIframeLoad"
        >
          您的浏览器不支持PDF预览，请<a :href="previewUrl" target="_blank">点击下载</a>
        </iframe>
      </div>

      <!-- 文本文件预览 -->
      <div v-else-if="isText" class="text-preview">
        <el-scrollbar height="500px">
          <pre class="text-content">{{ textContent }}</pre>
        </el-scrollbar>
      </div>

      <!-- Office文档预览 -->
      <div v-else-if="isOffice" class="office-preview">
        <div class="office-viewer">
          <div v-if="iframeLoading" class="iframe-loading" v-loading="true" element-loading-text="正在加载文档..."></div>
          <iframe
            :src="getOfficePreviewUrl()"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          >
            文档预览加载中...
          </iframe>
        </div>
      </div>

      <!-- 代码文件预览 -->
      <div v-else-if="isCode" class="code-preview">
        <el-scrollbar height="500px">
          <pre><code :class="getCodeLanguage()" v-html="highlightedCode"></code></pre>
        </el-scrollbar>
      </div>

      <!-- 不支持的文件类型 -->
      <div v-else class="unsupported-preview">
        <div class="unsupported-content">
          <el-icon size="64" color="#909399"><Document /></el-icon>
          <h3>暂不支持此文件类型的预览</h3>
          <p>文件名: {{ fileInfo?.fileName }}</p>
          <p>文件大小: {{ formatFileSize(fileInfo?.size) }}</p>
          <el-button type="primary" @click="downloadFile">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" v-loading="loading" element-loading-text="加载中..." class="loading-overlay">
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="downloadFile">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
        <el-button type="primary" @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, Headset } from '@element-plus/icons-vue'

// highlight.js 按需 + 懒加载：只在预览代码文件时才加载核心库与所需语言，
// 避免把 190+ 语言的完整包（约 800KB）打进首屏 vendor。
type Hljs = typeof import('highlight.js/lib/core')['default']
let hljsInstance: Hljs | null = null
const registeredLangs = new Set<string>()

// 语言别名 -> 动态加载器，仅覆盖本组件实际支持的语言
const langLoaders: Record<string, () => Promise<{ default: unknown }>> = {
  javascript: () => import('highlight.js/lib/languages/javascript'),
  typescript: () => import('highlight.js/lib/languages/typescript'),
  xml: () => import('highlight.js/lib/languages/xml'), // html/vue 模板走 xml
  css: () => import('highlight.js/lib/languages/css'),
  scss: () => import('highlight.js/lib/languages/scss'),
  less: () => import('highlight.js/lib/languages/less'),
  java: () => import('highlight.js/lib/languages/java'),
  python: () => import('highlight.js/lib/languages/python'),
  cpp: () => import('highlight.js/lib/languages/cpp'),
  c: () => import('highlight.js/lib/languages/c'),
  go: () => import('highlight.js/lib/languages/go'),
  rust: () => import('highlight.js/lib/languages/rust'),
  php: () => import('highlight.js/lib/languages/php'),
  ruby: () => import('highlight.js/lib/languages/ruby'),
  swift: () => import('highlight.js/lib/languages/swift'),
  kotlin: () => import('highlight.js/lib/languages/kotlin'),
}

async function ensureHljs(lang: string): Promise<Hljs> {
  if (!hljsInstance) {
    const core = await import('highlight.js/lib/core')
    hljsInstance = core.default
    // 高亮主题样式也随组件懒加载
    await import('highlight.js/styles/github.css')
  }
  const loader = langLoaders[lang]
  if (loader && !registeredLangs.has(lang)) {
    const mod = await loader()
    // xml 语言别名映射到 html/vue
    const regName = lang === 'xml' ? 'xml' : lang
    // @ts-expect-error highlight.js 语言模块默认导出为 LanguageFn
    hljsInstance.registerLanguage(regName, mod.default)
    registeredLangs.add(lang)
  }
  return hljsInstance
}

interface FileInfo {
  id: string
  fileName: string
  downloadUrl: string
  size: string
  uploadTime: number
}

interface Props {
  modelValue: boolean
  fileInfo: FileInfo | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'download', fileInfo: FileInfo): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const textContent = ref('')
const highlightedCode = ref('')

// 文件类型判断
const fileExtension = computed(() => {
  if (!props.fileInfo?.fileName) return ''
  return props.fileInfo.fileName.split('.').pop()?.toLowerCase() || ''
})

const isImage = computed(() => {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
  return imageExts.includes(fileExtension.value)
})

const isVideo = computed(() => {
  const videoExts = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv']
  return videoExts.includes(fileExtension.value)
})

const isAudio = computed(() => {
  const audioExts = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'm4a']
  return audioExts.includes(fileExtension.value)
})

const isPdf = computed(() => {
  return fileExtension.value === 'pdf'
})

const isText = computed(() => {
  const textExts = ['txt', 'md', 'json', 'xml', 'csv', 'log']
  return textExts.includes(fileExtension.value)
})

const isOffice = computed(() => {
  const officeExts = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
  return officeExts.includes(fileExtension.value)
})

const isCode = computed(() => {
  const codeExts = ['js', 'ts', 'vue', 'html', 'css', 'scss', 'less', 'java', 'py', 'cpp', 'c', 'go', 'rs', 'php', 'rb', 'swift', 'kt']
  return codeExts.includes(fileExtension.value)
})

const previewUrl = computed(() => {
  return props.fileInfo?.downloadUrl || ''
})

// 预览专用 URL：追加 preview=1，让后端对 PDF/图片/视频/音频/文本返回
// Content-Disposition: inline，浏览器内联渲染而非直接下载
const inlinePreviewUrl = computed(() => {
  const url = props.fileInfo?.downloadUrl || ''
  if (!url) return ''
  return url + (url.includes('?') ? '&' : '?') + 'preview=1'
})

// iframe（PDF / Office）加载状态，用于展示加载动画
const iframeLoading = ref(false)
const onIframeLoad = () => {
  iframeLoading.value = false
}

// 获取代码语言
const getCodeLanguage = () => {
  const langMap: Record<string, string> = {
    'js': 'javascript',
    'ts': 'typescript',
    'vue': 'xml',   // vue 模板用 xml 语法高亮
    'html': 'xml',  // html 由 highlight.js 的 xml 语言处理
    'css': 'css',
    'scss': 'scss',
    'less': 'less',
    'java': 'java',
    'py': 'python',
    'cpp': 'cpp',
    'c': 'c',
    'go': 'go',
    'rs': 'rust',
    'php': 'php',
    'rb': 'ruby',
    'swift': 'swift',
    'kt': 'kotlin'
  }
  return langMap[fileExtension.value] || 'plaintext'
}

// 获取Office文档预览URL
const getOfficePreviewUrl = () => {
  const encodedUrl = encodeURIComponent(previewUrl.value)
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodedUrl}`
}

// 格式化文件大小
const formatFileSize = (size: string | number) => {
  const bytes = typeof size === 'string' ? parseInt(size) : size
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 加载文本内容
const loadTextContent = async () => {
  if (!props.fileInfo || (!isText.value && !isCode.value)) return
  
  loading.value = true
  try {
    const response = await fetch(previewUrl.value)
    const text = await response.text()
    
    if (isCode.value) {
      // 代码高亮（按需懒加载 highlight.js 核心与对应语言）
      const lang = getCodeLanguage()
      try {
        const hl = await ensureHljs(lang)
        const supported = lang !== 'plaintext' && hl.getLanguage(lang)
        highlightedCode.value = supported
          ? hl.highlight(text, { language: lang }).value
          : hl.highlightAuto(text).value
      } catch (e) {
        console.warn('代码高亮加载失败，降级为纯文本', e)
        // 转义后作为纯文本展示，避免 v-html 注入
        highlightedCode.value = text
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
      }
    } else {
      textContent.value = text
    }
  } catch (error) {
    console.error('加载文件内容失败:', error)
    ElMessage.error('加载文件内容失败')
  } finally {
    loading.value = false
  }
}

// 下载文件
const downloadFile = () => {
  if (props.fileInfo) {
    emit('download', props.fileInfo)
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  textContent.value = ''
  highlightedCode.value = ''
}

// 打开/切换预览时的统一初始化：iframe 类预览（PDF/Office）先展示加载动画，
// 文本/代码类则拉取内容
const initPreview = async () => {
  if (!props.fileInfo) return
  await nextTick()
  if (isPdf.value || isOffice.value) {
    iframeLoading.value = true
  }
  if (isText.value || isCode.value) {
    loadTextContent()
  }
}

// 监听文件信息变化
watch(() => props.fileInfo, async (newFileInfo) => {
  if (newFileInfo && visible.value) {
    await initPreview()
  }
}, { immediate: true })

// 监听对话框显示状态
watch(visible, async (newVisible) => {
  if (newVisible && props.fileInfo) {
    await initPreview()
  }
})
</script>

<style scoped>
.file-preview-dialog {
  .preview-container {
    position: relative;
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .image-preview {
    width: 100%;
    text-align: center;
    
    .preview-image {
      max-width: 100%;
      max-height: 600px;
    }
  }

  .video-preview {
    width: 100%;
    text-align: center;
    
    .preview-video {
      max-width: 100%;
      max-height: 600px;
    }
  }

  .audio-preview {
    text-align: center;
    
    .audio-info {
      margin-bottom: 20px;
      
      h3 {
        margin: 10px 0;
        color: #303133;
      }
    }
    
    .preview-audio {
      width: 100%;
      max-width: 500px;
    }
  }

  .pdf-preview,
  .office-preview {
    width: 100%;
    height: 600px;
    position: relative;

    .office-viewer {
      width: 100%;
      height: 100%;
      position: relative;
    }

    .preview-iframe {
      width: 100%;
      height: 100%;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }

    .iframe-loading {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 2;
      background: #fff;
      border-radius: 4px;
    }
  }

  .text-preview,
  .code-preview {
    width: 100%;
    
    .text-content {
      font-family: 'Courier New', monospace;
      font-size: 14px;
      line-height: 1.5;
      color: #303133;
      background: #f5f7fa;
      padding: 16px;
      border-radius: 4px;
      margin: 0;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
    
    code {
      font-family: 'Courier New', monospace;
      font-size: 14px;
      line-height: 1.5;
      background: #f5f7fa;
      padding: 16px;
      border-radius: 4px;
      display: block;
    }
  }

  .unsupported-preview {
    width: 100%;
    
    .unsupported-content {
      text-align: center;
      padding: 40px;
      
      h3 {
        margin: 16px 0;
        color: #303133;
      }
      
      p {
        margin: 8px 0;
        color: #606266;
      }
      
      .el-button {
        margin-top: 20px;
      }
    }
  }

  .loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .dialog-footer {
    text-align: right;
    
    .el-button {
      margin-left: 8px;
    }
  }
}
</style>
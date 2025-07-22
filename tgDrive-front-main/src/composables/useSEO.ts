import { onMounted } from 'vue'
import request from '@/utils/request'

/**
 * SEO管理组合式函数
 * 用于动态更新页面的SEO信息
 */
export function useSEO() {
  /**
   * 加载并应用SEO设置
   */
  const loadSEOSettings = async () => {
    try {
      const response = await request.get('/misc/seo')
      if (response.data.code === 1) {
        const seoSettings = response.data.data
        applySEOSettings(seoSettings)
      }
    } catch (error) {
      console.error('加载SEO设置失败:', error)
    }
  }

  /**
   * 应用SEO设置到页面
   */
  const applySEOSettings = (settings: any) => {
    // 更新页面标题
    if (settings.title) {
      document.title = settings.title
    }

    // 更新或创建meta标签
    updateMetaTag('description', settings.description)
    updateMetaTag('keywords', settings.keywords)
    updateMetaTag('author', settings.author)
    
    // 更新Open Graph标签
    updateMetaTag('og:title', settings.title, 'property')
    updateMetaTag('og:description', settings.description, 'property')
    updateMetaTag('og:image', settings.ogImage, 'property')
    updateMetaTag('og:type', 'website', 'property')
    
    // 更新Twitter Card标签
    updateMetaTag('twitter:card', 'summary_large_image', 'name')
    updateMetaTag('twitter:title', settings.title, 'name')
    updateMetaTag('twitter:description', settings.description, 'name')
    updateMetaTag('twitter:image', settings.ogImage, 'name')
    
    // 更新favicon
    if (settings.favicon) {
      updateFavicon(settings.favicon)
    }
  }

  /**
   * 更新或创建meta标签
   */
  const updateMetaTag = (name: string, content: string, attribute: string = 'name') => {
    if (!content) return
    
    let meta = document.querySelector(`meta[${attribute}="${name}"]`) as HTMLMetaElement
    
    if (meta) {
      meta.content = content
    } else {
      meta = document.createElement('meta')
      meta.setAttribute(attribute, name)
      meta.content = content
      document.head.appendChild(meta)
    }
  }

  /**
   * 更新favicon
   */
  const updateFavicon = (faviconUrl: string) => {
    // 移除现有的favicon链接
    const existingFavicons = document.querySelectorAll('link[rel*="icon"]')
    existingFavicons.forEach(favicon => favicon.remove())
    
    // 创建新的favicon链接
    const link = document.createElement('link')
    link.rel = 'icon'
    link.type = 'image/x-icon'
    link.href = faviconUrl
    document.head.appendChild(link)
    
    // 同时创建apple-touch-icon
    const appleLink = document.createElement('link')
    appleLink.rel = 'apple-touch-icon'
    appleLink.href = faviconUrl
    document.head.appendChild(appleLink)
  }

  /**
   * 手动更新SEO信息
   */
  const updateSEO = (seoData: {
    title?: string
    description?: string
    keywords?: string
    author?: string
    ogImage?: string
    favicon?: string
  }) => {
    applySEOSettings(seoData)
  }

  return {
    loadSEOSettings,
    updateSEO
  }
}

/**
 * 自动加载SEO设置的组合式函数
 * 在组件挂载时自动加载SEO设置
 */
export function useAutoSEO() {
  const { loadSEOSettings, updateSEO } = useSEO()
  
  onMounted(() => {
    loadSEOSettings()
  })
  
  return {
    updateSEO
  }
}
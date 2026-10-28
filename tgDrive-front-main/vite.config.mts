import { defineConfig } from 'vite';
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vue from '@vitejs/plugin-vue';
import { createHtmlPlugin } from 'vite-plugin-html';
import vueTs from '@vitejs/plugin-vue-jsx';

export default defineConfig({
  base: '/',  // 使用绝对根路径，确保任意层级前端路由（如 /user/home）下静态资源都指向根，避免相对路径被解析到子路径导致 MIME 错误白屏
  plugins: [
    vue({
      script: {
        defineModel: true,
        propsDestructure: true
      }
    }),
    createHtmlPlugin({
      inject: {
        data: {
          title: 'TG Drive',
        },
      },
    }),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    })
  ],
  resolve: {
    alias: {
      '@': '/src',
    },
  },
  build: {
    // 目标浏览器，允许更激进的现代语法输出，减小体积
    target: 'es2019',
    // 开启 CSS 代码分割：各懒加载路由的样式随组件按需加载
    cssCodeSplit: true,
    // 代码分割配置
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks(id: string) {
          if (id.includes('node_modules')) {
            // highlight.js 单独成块：仅在预览代码文件时才动态加载，不进首屏
            if (id.includes('highlight.js')) return 'highlight'
            // Vue 全家桶与 Element Plus 强耦合（EP 依赖 vue 响应式 API），
            // 合并为同一 vendor chunk，避免跨 chunk 循环引用导致的
            // "Cannot access 'X' before initialization" (TDZ) 白屏问题。
            return 'vendor'
          }
        },
        // 为静态资源添加hash
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]'
      }
    },
    // 启用压缩
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 生产环境移除console
        drop_debugger: true,
        passes: 2
      }
    },
    // 设置chunk大小警告限制
    chunkSizeWarningLimit: 700
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: 'ws://localhost:8085',
        ws: true,
      },
    },
    port: 3000,
  },
});

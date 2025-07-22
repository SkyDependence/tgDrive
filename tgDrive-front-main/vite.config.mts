import { defineConfig } from 'vite';
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vue from '@vitejs/plugin-vue';
import { createHtmlPlugin } from 'vite-plugin-html';
import vueTs from '@vitejs/plugin-vue-jsx';

export default defineConfig({
  base: './',  // 添加这行，使用相对路径
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
          title: 'ST-TG网盘',
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
    // 代码分割配置
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks: {
          // 将Vue相关库分离
          'vue-vendor': ['vue', 'vue-router'],
          // 将Element Plus分离
          'element-plus': ['element-plus'],
          // 将其他第三方库分离
          'vendor': ['axios', 'highlight.js']
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
        drop_debugger: true
      }
    },
    // 设置chunk大小警告限制
    chunkSizeWarningLimit: 1000
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
    },
    port: 3000,
  },
});

// vite.config.mts
import { defineConfig } from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/vite/dist/node/index.js";
import AutoImport from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/unplugin-vue-components/dist/vite.js";
import { ElementPlusResolver } from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/unplugin-vue-components/dist/resolvers.js";
import vue from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { createHtmlPlugin } from "file:///D:/%E7%BC%96%E7%A8%8B/TG-Drive-ST/tgDrive-front-main/node_modules/vite-plugin-html/dist/index.mjs";
var vite_config_default = defineConfig({
  base: "./",
  // 添加这行，使用相对路径
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
          title: "ST-TG\u7F51\u76D8"
        }
      }
    }),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      "@": "/src"
    }
  },
  build: {
    // 代码分割配置
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks: {
          // 将Vue相关库分离
          "vue-vendor": ["vue", "vue-router"],
          // 将Element Plus分离
          "element-plus": ["element-plus"],
          // 将其他第三方库分离
          "vendor": ["axios", "highlight.js"]
        },
        // 为静态资源添加hash
        chunkFileNames: "assets/js/[name]-[hash].js",
        entryFileNames: "assets/js/[name]-[hash].js",
        assetFileNames: "assets/[ext]/[name]-[hash].[ext]"
      }
    },
    // 启用压缩
    minify: "terser",
    terserOptions: {
      compress: {
        drop_console: true,
        // 生产环境移除console
        drop_debugger: true
      }
    },
    // 设置chunk大小警告限制
    chunkSizeWarningLimit: 1e3
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8085",
        changeOrigin: true,
        secure: false
      }
    },
    port: 3e3
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcubXRzIl0sCiAgInNvdXJjZXNDb250ZW50IjogWyJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiRDpcXFxcXHU3RjE2XHU3QTBCXFxcXFRHLURyaXZlLVNUXFxcXHRnRHJpdmUtZnJvbnQtbWFpblwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRDpcXFxcXHU3RjE2XHU3QTBCXFxcXFRHLURyaXZlLVNUXFxcXHRnRHJpdmUtZnJvbnQtbWFpblxcXFx2aXRlLmNvbmZpZy5tdHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6LyVFNyVCQyU5NiVFNyVBOCU4Qi9URy1Ecml2ZS1TVC90Z0RyaXZlLWZyb250LW1haW4vdml0ZS5jb25maWcubXRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSc7XG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xuaW1wb3J0IENvbXBvbmVudHMgZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvdml0ZSdcbmltcG9ydCB7IEVsZW1lbnRQbHVzUmVzb2x2ZXIgfSBmcm9tICd1bnBsdWdpbi12dWUtY29tcG9uZW50cy9yZXNvbHZlcnMnXG5pbXBvcnQgdnVlIGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZSc7XG5pbXBvcnQgeyBjcmVhdGVIdG1sUGx1Z2luIH0gZnJvbSAndml0ZS1wbHVnaW4taHRtbCc7XG5pbXBvcnQgdnVlVHMgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlLWpzeCc7XG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIGJhc2U6ICcuLycsICAvLyBcdTZERkJcdTUyQTBcdThGRDlcdTg4NENcdUZGMENcdTRGN0ZcdTc1MjhcdTc2RjhcdTVCRjlcdThERUZcdTVGODRcbiAgcGx1Z2luczogW1xuICAgIHZ1ZSh7XG4gICAgICBzY3JpcHQ6IHtcbiAgICAgICAgZGVmaW5lTW9kZWw6IHRydWUsXG4gICAgICAgIHByb3BzRGVzdHJ1Y3R1cmU6IHRydWVcbiAgICAgIH1cbiAgICB9KSxcbiAgICBjcmVhdGVIdG1sUGx1Z2luKHtcbiAgICAgIGluamVjdDoge1xuICAgICAgICBkYXRhOiB7XG4gICAgICAgICAgdGl0bGU6ICdTVC1UR1x1N0Y1MVx1NzZEOCcsXG4gICAgICAgIH0sXG4gICAgICB9LFxuICAgIH0pLFxuICAgIEF1dG9JbXBvcnQoe1xuICAgICAgcmVzb2x2ZXJzOiBbRWxlbWVudFBsdXNSZXNvbHZlcigpXSxcbiAgICB9KSxcbiAgICBDb21wb25lbnRzKHtcbiAgICAgIHJlc29sdmVyczogW0VsZW1lbnRQbHVzUmVzb2x2ZXIoKV0sXG4gICAgfSlcbiAgXSxcbiAgcmVzb2x2ZToge1xuICAgIGFsaWFzOiB7XG4gICAgICAnQCc6ICcvc3JjJyxcbiAgICB9LFxuICB9LFxuICBidWlsZDoge1xuICAgIC8vIFx1NEVFM1x1NzgwMVx1NTIwNlx1NTI3Mlx1OTE0RFx1N0Y2RVxuICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgIG91dHB1dDoge1xuICAgICAgICAvLyBcdTYyNEJcdTUyQThcdTUyMDZcdTUyNzJcdTRFRTNcdTc4MDFcdTU3NTdcbiAgICAgICAgbWFudWFsQ2h1bmtzOiB7XG4gICAgICAgICAgLy8gXHU1QzA2VnVlXHU3NkY4XHU1MTczXHU1RTkzXHU1MjA2XHU3OUJCXG4gICAgICAgICAgJ3Z1ZS12ZW5kb3InOiBbJ3Z1ZScsICd2dWUtcm91dGVyJ10sXG4gICAgICAgICAgLy8gXHU1QzA2RWxlbWVudCBQbHVzXHU1MjA2XHU3OUJCXG4gICAgICAgICAgJ2VsZW1lbnQtcGx1cyc6IFsnZWxlbWVudC1wbHVzJ10sXG4gICAgICAgICAgLy8gXHU1QzA2XHU1MTc2XHU0RUQ2XHU3QjJDXHU0RTA5XHU2NUI5XHU1RTkzXHU1MjA2XHU3OUJCXG4gICAgICAgICAgJ3ZlbmRvcic6IFsnYXhpb3MnLCAnaGlnaGxpZ2h0LmpzJ11cbiAgICAgICAgfSxcbiAgICAgICAgLy8gXHU0RTNBXHU5NzU5XHU2MDAxXHU4RDQ0XHU2RTkwXHU2REZCXHU1MkEwaGFzaFxuICAgICAgICBjaHVua0ZpbGVOYW1lczogJ2Fzc2V0cy9qcy9bbmFtZV0tW2hhc2hdLmpzJyxcbiAgICAgICAgZW50cnlGaWxlTmFtZXM6ICdhc3NldHMvanMvW25hbWVdLVtoYXNoXS5qcycsXG4gICAgICAgIGFzc2V0RmlsZU5hbWVzOiAnYXNzZXRzL1tleHRdL1tuYW1lXS1baGFzaF0uW2V4dF0nXG4gICAgICB9XG4gICAgfSxcbiAgICAvLyBcdTU0MkZcdTc1MjhcdTUzOEJcdTdGMjlcbiAgICBtaW5pZnk6ICd0ZXJzZXInLFxuICAgIHRlcnNlck9wdGlvbnM6IHtcbiAgICAgIGNvbXByZXNzOiB7XG4gICAgICAgIGRyb3BfY29uc29sZTogdHJ1ZSwgLy8gXHU3NTFGXHU0RUE3XHU3M0FGXHU1ODgzXHU3OUZCXHU5NjY0Y29uc29sZVxuICAgICAgICBkcm9wX2RlYnVnZ2VyOiB0cnVlXG4gICAgICB9XG4gICAgfSxcbiAgICAvLyBcdThCQkVcdTdGNkVjaHVua1x1NTkyN1x1NUMwRlx1OEI2Nlx1NTQ0QVx1OTY1MFx1NTIzNlxuICAgIGNodW5rU2l6ZVdhcm5pbmdMaW1pdDogMTAwMFxuICB9LFxuICBzZXJ2ZXI6IHtcbiAgICBwcm94eToge1xuICAgICAgJy9hcGknOiB7XG4gICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4NScsXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZSxcbiAgICAgICAgc2VjdXJlOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgfSxcbiAgICBwb3J0OiAzMDAwLFxuICB9LFxufSk7XG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQXdULFNBQVMsb0JBQW9CO0FBQ3JWLE9BQU8sZ0JBQWdCO0FBQ3ZCLE9BQU8sZ0JBQWdCO0FBQ3ZCLFNBQVMsMkJBQTJCO0FBQ3BDLE9BQU8sU0FBUztBQUNoQixTQUFTLHdCQUF3QjtBQUdqQyxJQUFPLHNCQUFRLGFBQWE7QUFBQSxFQUMxQixNQUFNO0FBQUE7QUFBQSxFQUNOLFNBQVM7QUFBQSxJQUNQLElBQUk7QUFBQSxNQUNGLFFBQVE7QUFBQSxRQUNOLGFBQWE7QUFBQSxRQUNiLGtCQUFrQjtBQUFBLE1BQ3BCO0FBQUEsSUFDRixDQUFDO0FBQUEsSUFDRCxpQkFBaUI7QUFBQSxNQUNmLFFBQVE7QUFBQSxRQUNOLE1BQU07QUFBQSxVQUNKLE9BQU87QUFBQSxRQUNUO0FBQUEsTUFDRjtBQUFBLElBQ0YsQ0FBQztBQUFBLElBQ0QsV0FBVztBQUFBLE1BQ1QsV0FBVyxDQUFDLG9CQUFvQixDQUFDO0FBQUEsSUFDbkMsQ0FBQztBQUFBLElBQ0QsV0FBVztBQUFBLE1BQ1QsV0FBVyxDQUFDLG9CQUFvQixDQUFDO0FBQUEsSUFDbkMsQ0FBQztBQUFBLEVBQ0g7QUFBQSxFQUNBLFNBQVM7QUFBQSxJQUNQLE9BQU87QUFBQSxNQUNMLEtBQUs7QUFBQSxJQUNQO0FBQUEsRUFDRjtBQUFBLEVBQ0EsT0FBTztBQUFBO0FBQUEsSUFFTCxlQUFlO0FBQUEsTUFDYixRQUFRO0FBQUE7QUFBQSxRQUVOLGNBQWM7QUFBQTtBQUFBLFVBRVosY0FBYyxDQUFDLE9BQU8sWUFBWTtBQUFBO0FBQUEsVUFFbEMsZ0JBQWdCLENBQUMsY0FBYztBQUFBO0FBQUEsVUFFL0IsVUFBVSxDQUFDLFNBQVMsY0FBYztBQUFBLFFBQ3BDO0FBQUE7QUFBQSxRQUVBLGdCQUFnQjtBQUFBLFFBQ2hCLGdCQUFnQjtBQUFBLFFBQ2hCLGdCQUFnQjtBQUFBLE1BQ2xCO0FBQUEsSUFDRjtBQUFBO0FBQUEsSUFFQSxRQUFRO0FBQUEsSUFDUixlQUFlO0FBQUEsTUFDYixVQUFVO0FBQUEsUUFDUixjQUFjO0FBQUE7QUFBQSxRQUNkLGVBQWU7QUFBQSxNQUNqQjtBQUFBLElBQ0Y7QUFBQTtBQUFBLElBRUEsdUJBQXVCO0FBQUEsRUFDekI7QUFBQSxFQUNBLFFBQVE7QUFBQSxJQUNOLE9BQU87QUFBQSxNQUNMLFFBQVE7QUFBQSxRQUNOLFFBQVE7QUFBQSxRQUNSLGNBQWM7QUFBQSxRQUNkLFFBQVE7QUFBQSxNQUNWO0FBQUEsSUFDRjtBQUFBLElBQ0EsTUFBTTtBQUFBLEVBQ1I7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=

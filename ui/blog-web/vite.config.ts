import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// @ts-ignore
import postcssPxToViewport from 'postcss-px-to-viewport'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
      vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    postcss: {
      plugins: [
        postcssPxToViewport({
          viewportWidth: 1920,        // 设计稿宽度（移动端设计稿通常是 375px）
          unitPrecision: 6,          // 转换后的小数位数
          viewportUnit: 'vw',        // 转换后的单位
          fontViewportUnit: 'vw',    // 字体转换后的单位
          selectorBlackList: ['.ignore'],  // 忽略转换的类名
          minPixelValue: 1,          // 小于等于 1px 不转换
          mediaQuery: true,          // 媒体查询中的 px 是否转换
          exclude: [/node_modules/], // 排除第三方库
        })
      ]
    }
  }
})

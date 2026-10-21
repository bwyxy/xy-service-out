import { mergeConfig, type UserConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
// @ts-ignore
import viteBaseConfig from "../../configs/vite.base.config.ts";

// https://vite.dev/config/
// 桌宠是纯 Tauri 应用，不参与 qiankun 微前端，因此没有路由前缀与 qiankun 插件
export default (): UserConfig => {
  const config = {
    // 桌面端通过 tauri:// 协议加载资源，必须使用相对路径
    base: './',
    plugins: [vue()],
    server: {
      // 与 y-chat-sub(9001) 隔离，避免两个 dev 服务抢同一端口导致 base 路径错配
      port: 9002,
      strictPort: true,
      host: '0.0.0.0'
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      // 与 src-tauri/tauri.conf.json 的 frontendDist(=../dist) 保持一致
      outDir: 'dist'
    }
  }

  return mergeConfig(viteBaseConfig, config)
}

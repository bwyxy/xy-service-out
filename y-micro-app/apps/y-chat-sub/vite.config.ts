import { mergeConfig, type UserConfig, loadEnv } from 'vite'
import {fileURLToPath, URL} from 'node:url'
import { readFileSync } from 'node:fs'
import vue from '@vitejs/plugin-vue'
import qiankun from 'vite-plugin-qiankun'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// @ts-ignore
import viteBaseConfig from "../../configs/vite.base.config.ts";

/** 前端版本号唯一来源：package.json（发版时与 src-tauri/tauri.conf.json 手工保持一致） */
const pkg = JSON.parse(readFileSync(fileURLToPath(new URL('./package.json', import.meta.url)), 'utf-8'))
const [majorVersion = 0, minorVersion = 0, patchVersion = 0] = String(pkg.version)
    .split('.')
    .map((item: string) => parseInt(item, 10) || 0)

// https://vite.dev/config/
export default ({ mode } :{mode :any}) :UserConfig => {
  const env = loadEnv(mode, process.cwd())
  // Tauri 环境下使用相对路径，桌面应用通过 tauri:// 协议加载资源
  const isTauri = !!process.env.TAURI_ENV_PLATFORM

  const config = {
    base: isTauri ? './' : '/chat',
    define: {
      // 客户端检查更新时上报的版本信息
      __APP_VERSION__: JSON.stringify(pkg.version),
      __APP_VERSION_CODE__: String(majorVersion * 10000 + minorVersion * 100 + patchVersion),
    },
    plugins: [
        vue(),
      qiankun('chat', {
        useDevMode: env.VITE_NODE_ENV === 'development'
      }),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        dts: 'src/utils/auto-imports.d.ts'
      }),
      Components({
        // 组件名称转换
        directoryAsNamespace: false,
        globalNamespaces: [],
        // 自动导入自定义组件
        dirs: ['src/components'],
        // 生成的文件配置
        dts: 'src/utils/components.d.ts',
        extensions: ['vue'],
        deep: true,
        // 包含的文件
        include: [/\.vue$/, /\.vue\?vue/],
        // 排除的文件
        exclude: [/[\\/]node_modules[\\/]/, /[\\/]\.git[\\/]/, /[\\/]\.nuxt[\\/]/],
        resolvers: [
          ElementPlusResolver()
        ],
      })
    ],
    server: {
      port: 9001,
      host: '0.0.0.0',
      cors: true,
      origin: '//localhost:9001',
      proxy: {
        '/api': {
          target: 'http://localhost:9080',
          changeOrigin: true,
          rewrite: (path :any) => path.replace(/^\/api/, '')
        }
      }
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
            @use "@/assets/styles/variables.scss" as *;
            @use "@/assets/styles/mixins.scss" as *;
           `
        }
      }
    },
    build: {
      // Tauri 打包读取的是 src-tauri/tauri.conf.json 的 frontendDist(=../dist)，
      // 必须与这里保持一致，否则会出现「构建产物是新的、装进桌面端的是旧的」
      outDir: isTauri ? 'dist' : 'chat',
    }
  }

  return mergeConfig(viteBaseConfig, config)
}

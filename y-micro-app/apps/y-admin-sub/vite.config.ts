import { mergeConfig, type UserConfig, loadEnv } from 'vite'
import {fileURLToPath, URL} from 'node:url'
import vue from '@vitejs/plugin-vue'
import qiankun from 'vite-plugin-qiankun'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// @ts-ignore
import viteBaseConfig from "../../configs/vite.base.config.ts";

// https://vite.dev/config/
export default ({ mode } :{mode :any}) :UserConfig => {
  const env = loadEnv(mode, process.cwd())

  const config = {
    base: '/',
    plugins: [
        vue(),
        qiankun('admin', {
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
      port: 9002,
      host: '0.0.0.0',
      cors: true,
      origin: '//localhost:9002',
      proxy: {
        '/api': {
          target: 'http://localhost:9090',
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
      outDir: '',
    }
  }

  return mergeConfig(viteBaseConfig, config)
}

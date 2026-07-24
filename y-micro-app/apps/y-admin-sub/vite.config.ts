import { mergeConfig, type UserConfig, loadEnv } from 'vite'
import {fileURLToPath, URL} from 'node:url'
import vue from '@vitejs/plugin-vue'
import qiankun from 'vite-plugin-qiankun'
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
        })
    ],
    server: {
      port: 9002,
      host: '0.0.0.0',
      cors: true,
      origin: '//localhost:9002'
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    }
  }

  return mergeConfig(viteBaseConfig, config)
}

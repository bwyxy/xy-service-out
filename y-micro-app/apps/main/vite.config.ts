import { mergeConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
// @ts-ignore
import viteBaseConfig from "../../configs/vite.base.config.ts";

// https://vite.dev/config/
export default () => {

  const config = {
    base: '/',
    plugins: [
        vue()
    ],
    server: {
      port: 5000,
      cors: true,
      headers: {
        'Access-Control-Allow-Origin': '*'
      },
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    }
  }

  return mergeConfig(viteBaseConfig, config)
}

import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * 仅 H5 端生效：dev server 代理
 *
 * App 端为原生请求，不存在浏览器跨域限制，直接使用 utils/config.js 中的绝对地址。
 * H5 端由浏览器发请求，而 fg-chat（security 链未放行任何跨域来源）会拒绝跨域预检，
 * 故沿用网页端 y-chat-sub 的做法：开发环境通过 dev server 代理转发到后端（服务端转发无跨域）。
 *
 * 请求 /chat-api/chat/login → 代理到 http://localhost:9080/chat/login
 *
 * 注意：HBuilderX 会把工程根目录的 vite.config.js 整体替换掉内置配置
 * （见 @dcloudio/vite-plugin-uni/dist/cli/utils.js 的 resolveConfigFile），
 * 因此必须显式引入 uni 插件，否则 .vue 无法解析。
 */
export default defineConfig({
	plugins: [uni({
		vueOptions: {
			template: {
			  transformAssetUrls: {
				tags: {
				  'wd-img': ['src']
				}
			  }
			}
		  }
	})],
	server: {
		proxy: {
			'/chat-api': {
				target: 'http://localhost:9080',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/chat-api/, ''),
			},
		},
	},
})

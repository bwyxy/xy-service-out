// 首行注入浏览器 API polyfill（App 逻辑层不保证提供 btoa / atob / TextEncoder / TextDecoder）
import './utils/polyfill'

import App from './App'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue({
	...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'

export function createApp() {
	const app = createSSRApp(App)
	app.use(Pinia.createPinia())
	return {
		app,
		// uni-app 要求必须将 Pinia 返回
		Pinia
	}
}
// #endif

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// @ts-ignore
import '@/assets/css/index.scss'
// @ts-ignore
import 'element-plus/dist/index.css'
import { setupStore } from '@/store/index.ts'
import '@wangeditor/editor/dist/css/style.css'

import Vue3StarrySky from 'vue3-starry-sky';
import 'vue3-starry-sky/lib/style.css';
import 'animate.css';

const app = createApp(App)

app.use(Vue3StarrySky)

setupStore(app)

app.use(router).mount('#app')

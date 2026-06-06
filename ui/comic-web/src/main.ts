import { createApp } from 'vue'
import App from './App.vue'
import 'ant-design-vue/dist/reset.css';
import '@/assets/styles/index.scss'

import router from '@/router/index.ts'

const app = createApp(App)
app.use(router).mount('#app')

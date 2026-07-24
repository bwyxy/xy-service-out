import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { registerApps } from '@/qiankun/index.ts'

registerApps()

const app = createApp(App)
app.use(router).mount('#app')

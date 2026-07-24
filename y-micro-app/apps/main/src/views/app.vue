<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { loadMicroApp } from 'qiankun'
import { microApps } from '@/qiankun/modules/registerApp'

const route = useRoute()
const microApp = ref<any>(null)
/** 加载微应用 */
const loadMicroAppFn = () => {
  if (microApp.value) {
    microApp.value.unmount()
  }

  const path = route.path.split('/')[1]
  const app :any = microApps.find((item :any) => item.name === path)

  if (!app) return

  app.container = '#subapp-container'
  app.props = {
    userInfo: null,
    token: '',
    type: '',
    payload: null,
    wsStatus: 0,
    redirectUrl: ''
  }

  console.log(app)
  microApp.value = loadMicroApp(app)
}
/** 卸载微应用 */
const unmountMicroAppFn = () => {
  if (microApp.value) {
    microApp.value.unmount()
  }
}
/** 监听路由变化时加载微应用 */
watch(() => route.path, () => {
  loadMicroAppFn()
}, { deep: true })

onMounted(() => {
  console.log('初始化界面')
  loadMicroAppFn()
})
/** 组件卸载时卸载微应用 */
onBeforeUnmount(() => {
  unmountMicroAppFn()
})
</script>

<template>
  <div id="subapp-container"></div>
</template>

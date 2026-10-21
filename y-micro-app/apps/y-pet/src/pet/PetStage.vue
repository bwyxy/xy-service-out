<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { createSpinePet, type SpinePet } from './useSpinePet'

const props = withDefaults(defineProps<{ fit?: number }>(), { fit: 0.78 })
const emit = defineEmits<{ ready: []; error: [message: string] }>()

/** 模型放在 public/spine 下，按页面基准解析地址，开发与打包后都能命中 */
const ASSET_BASE = new URL('spine/', document.baseURI).href

const host = ref<HTMLDivElement | null>(null)
/** Pixi 对象不能被 Vue 深层代理，否则每帧访问都会带上响应式开销 */
const pet = shallowRef<SpinePet | null>(null)
const loading = ref(true)
const failed = ref('')

function play(name: string, loop: boolean) {
  pet.value?.play(name, loop)
}

onMounted(async () => {
  const element = host.value
  if (!element) return
  try {
    pet.value = await createSpinePet({
      host: element,
      skeletonUrl: `${ASSET_BASE}pet.json`,
      atlasUrl: `${ASSET_BASE}pet.atlas`,
      fit: props.fit,
    })
    emit('ready')
  } catch (error) {
    failed.value = error instanceof Error ? error.message : String(error)
    emit('error', failed.value)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  pet.value?.destroy()
  pet.value = null
})

defineExpose({ play })
</script>

<template>
  <div ref="host" class="pet-stage">
    <p v-if="loading" class="pet-stage__tip">正在加载形象…</p>
    <p v-else-if="failed" class="pet-stage__tip pet-stage__tip--error">
      形象加载失败：{{ failed }}
    </p>
  </div>
</template>

<style scoped>
.pet-stage {
  position: absolute;
  inset: 0;
}

.pet-stage__tip {
  position: absolute;
  top: 50%;
  left: 50%;
  margin: 0;
  padding: 4px 8px;
  border-radius: 8px;
  transform: translate(-50%, -50%);
  background: rgba(24, 28, 33, 0.72);
  color: #e8f5ee;
  font-size: 11px;
  text-align: center;
}

.pet-stage__tip--error {
  max-width: 260px;
  background: rgba(120, 30, 30, 0.82);
  color: #ffd9d9;
}
</style>

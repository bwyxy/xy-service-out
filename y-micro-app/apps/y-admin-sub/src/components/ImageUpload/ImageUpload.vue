<template>
  <div class="image-upload">
    <el-upload
      ref="uploadRef"
      action="#"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :show-file-list="false"
      :multiple="multiple"
      :limit="limit"
      :disabled="disabled || isUploading"
      accept="image/*"
      class="upload-trigger"
    >
      <div
        v-if="modelValueList.length < limit"
        class="upload-card"
        :class="{
          'is-disabled': disabled || isUploading,
          'is-single': !multiple && modelValueList.length >= 1
        }"
      >
        <el-icon class="upload-icon"><Upload /></el-icon>
        <div class="upload-text">{{ uploadText }}</div>
        <div class="upload-tip">{{ acceptTip }}</div>
      </div>
    </el-upload>

    <div v-if="modelValueList.length > 0" class="image-list">
      <div
        v-for="(url, index) in modelValueList"
        :key="url + index"
        class="image-item"
      >
        <el-image
          :src="url"
          fit="cover"
          class="preview-image"
          :preview-src-list="modelValueList"
          :initial-index="index"
          hide-on-click-modal
        />
        <div class="image-actions">
          <el-icon class="action-icon" @click="handlePreview(url)">
            <ZoomIn />
          </el-icon>
          <el-icon class="action-icon" @click="handleRemove(index)">
            <Delete />
          </el-icon>
        </div>
      </div>

      <div v-if="currentUpload.status === 'uploading'" class="image-item uploading-item">
        <div class="uploading-mask">
          <el-icon class="uploading-icon"><Upload /></el-icon>
          <div class="uploading-text">上传中 {{ currentUpload.percentage }}%</div>
          <el-progress
            :percentage="currentUpload.percentage"
            :show-text="false"
            :stroke-width="4"
            class="upload-progress"
          />
        </div>
      </div>
    </div>

    <div v-else-if="currentUpload.status === 'uploading'" class="image-list">
      <div class="image-item uploading-item">
        <div class="uploading-mask">
          <el-icon class="uploading-icon"><Upload /></el-icon>
          <div class="uploading-text">上传中 {{ currentUpload.percentage }}%</div>
          <el-progress
            :percentage="currentUpload.percentage"
            :show-text="false"
            :stroke-width="4"
            class="upload-progress"
          />
        </div>
      </div>
    </div>

    <el-image-viewer
      v-if="previewVisible"
      :url-list="[previewUrl]"
      :initial-index="0"
      hide-on-click-modal
      teleported
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, ZoomIn, Delete } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/upload'
import { validateImage } from '@/utils/file'

interface Props {
  modelValue?: string | string[]
  limit?: number
  maxSize?: number
  multiple?: boolean
  disabled?: boolean
  uploadText?: string
  accept?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  limit: 9,
  maxSize: 2,
  multiple: false,
  disabled: false,
  uploadText: '上传图片',
  accept: 'image/*'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void
  (e: 'success', url: string, file: File): void
  (e: 'error', error: any, file: File): void
  (e: 'remove', index: number, url: string): void
}>()

const uploadRef = ref<any>(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const isUploading = ref(false)

const currentUpload = ref({
  status: 'ready' as 'ready' | 'uploading' | 'success' | 'error',
  percentage: 0
})

const modelValueList = computed<string[]>(() => {
  if (!props.modelValue) return []
  return Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue]
})

const acceptTip = computed(() => {
  return `支持 ${props.multiple ? '多张' : '单张'}上传，单张不超过 ${props.maxSize}MB`
})

const updateModelValue = (urls: string[]) => {
  if (props.multiple) {
    emit('update:modelValue', urls)
  } else {
    emit('update:modelValue', urls.length > 0 ? urls[0] : '')
  }
}

const beforeUpload = (file: File) => {
  const error = validateImage(file, props.maxSize)
  if (error) {
    ElMessage.error(error)
    return false
  }

  const currentCount = modelValueList.value.length
  const willAddCount = props.multiple ? 1 : 1
  if (currentCount + willAddCount > props.limit) {
    ElMessage.warning(`最多只能上传 ${props.limit} 张图片`)
    return false
  }

  return true
}

const handleUpload = async (options: any) => {
  const file: File = options.file
  isUploading.value = true
  currentUpload.value = { status: 'uploading', percentage: 0 }

  try {
    const res = await uploadImage(file, (progressEvent) => {
      const percent = progressEvent.total
        ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
        : 0
      currentUpload.value.percentage = percent
    })

    if (res.code === 200) {
      const url = res.data
      const newUrls = props.multiple ? [...modelValueList.value, url] : [url]
      updateModelValue(newUrls)
      currentUpload.value.status = 'success'
      ElMessage.success('上传成功')
      emit('success', url, file)
    } else {
      throw new Error(res.msg || '上传失败')
    }
  } catch (error: any) {
    currentUpload.value.status = 'error'
    const message = error?.message || '上传失败，请稍后重试'
    ElMessage.error(message)
    emit('error', error, file)
  } finally {
    isUploading.value = false
    setTimeout(() => {
      currentUpload.value = { status: 'ready', percentage: 0 }
    }, 500)
  }
}

const handlePreview = (url: string) => {
  previewUrl.value = url
  previewVisible.value = true
}

const handleRemove = (index: number) => {
  const url = modelValueList.value[index]
  const newUrls = [...modelValueList.value]
  newUrls.splice(index, 1)
  updateModelValue(newUrls)
  emit('remove', index, url)
  ElMessage.success('删除成功')
}
</script>

<style scoped lang="scss">
.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-trigger {
  :deep(.el-upload) {
    display: block;
  }
}

.upload-card {
  width: 120px;
  height: 120px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: var(--el-fill-color-lighter);

  &:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.6;

    &:hover {
      border-color: var(--el-border-color);
      color: inherit;
    }
  }

  &.is-single {
    display: none;
  }
}

.upload-icon {
  font-size: 28px;
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
}

.upload-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  padding: 0 8px;
  text-align: center;
  line-height: 1.2;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
}

.preview-image {
  width: 100%;
  height: 100%;
  display: block;
}

.image-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.3s;

  .image-item:hover & {
    opacity: 1;
  }
}

.action-icon {
  font-size: 20px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.1);
    color: var(--el-color-primary);
  }
}

.uploading-item {
  background-color: var(--el-fill-color-lighter);
}

.uploading-mask {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.uploading-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.uploading-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
}

.upload-progress {
  width: 80%;
}
</style>

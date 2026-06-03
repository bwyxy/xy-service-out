<script setup>
import { Plus, CloseBold } from '@element-plus/icons-vue'
import { updateImage } from '@/api/upload'
import {computed, toRefs} from "vue";

const imageUrl = defineModel('modelValue', {
  type: String,
  default: ''
})

const prop = defineProps({
  multiple: {
    type: Boolean,
    default: false
  }
})
const { multiple } = toRefs(prop)

const handleUpload = async (file) => {
  const formData = new FormData()
  formData.append('file', file.file)
  try {
    const res = await updateImage(formData)

    if (!multiple.value) {
      imageUrl.value = res.data
      return
    }
    if (!imageUrl.value) {
      imageUrl.value = res.data
    } else {
      imageUrl.value += `;${res.data}`
    }
  } catch (err) {
    console.error(err)
  }

}

const removeImage = (index) => {
  let urls = imageUrls.value
  if (urls.length === 1) {
    urls = []
  } else {
    urls.splice(index, 1)
  }
  imageUrl.value = urls.join(';')
}

const imageUrls = computed(() => {
  if (imageUrl.value) {
    return imageUrl.value.split(';')
  }
  return []
})
</script>

<template>
  <div class="upload-image">
    <div v-for="(url, index) in imageUrls" :key="index" class="upload-image-item">
      <el-image :src="url" lazy fit="cover" style="max-width: 100%; max-height: 100%;" />
      <div class="image-close" @click="removeImage(index)">
        <el-icon><CloseBold /></el-icon>
      </div>
    </div>
    <el-upload
        class="image-uploader"
        :show-file-list="false"
        action="#"
        accept="image/*"
        :http-request="(file) => handleUpload(file)"
    >
      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
    </el-upload>
  </div>
</template>

<style scoped lang="scss">
.image-uploader {
  border: 1px dashed #ccc;
  background-color: #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  margin-bottom: 12px;
}

.image-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 48px;
  color: #ccc;
  width: 124px;
  height: 124px;
  text-align: center;
}

.upload-image-item {
  width: 124px;
  height: 124px;
  border: 1px dashed #ccc;
  background-color: #f0f0f0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  margin-bottom: 12px;
  position: relative;
}

.image-close {
  width: 30px;
  height: 30px;
  background: #1ab394;
  border-radius: 50%;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: -8px;
  right: -8px;
}

.upload-image {
  display: flex;
  flex-wrap: wrap;
}
</style>

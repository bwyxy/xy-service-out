<script setup lang="ts">
import { reactive, ref} from "vue";
import { storeToRefs } from 'pinia'
import { VueCropper } from 'cropper-next-vue'
import 'cropper-next-vue/style.css'
import useUserStore from "@/store/user.ts";
import {RefreshLeft, RefreshRight, Upload, ZoomIn, ZoomOut} from "@element-plus/icons-vue";
import {ElMessage, UploadRequestOptions} from "element-plus";
import { uploadAvatar } from '@/api/upload.ts'

const MAX_SIZE = 2 * 1024 * 1024

const userStore = useUserStore()
const { userInfo } = storeToRefs(useUserStore())
const isUploadAvatar = ref<boolean>(false)
const cropperRefs = ref()

const options = reactive({
  img: userInfo.value.avatar || new URL('@/assets/images/logo.jpg', import.meta.url).toString(),  //裁剪图片的地址
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  outputType:"png",
  filename: 'avatar',
  visible: false,
  maxScale: 1,
  scale: .1,
})

/** 上传预处理 */
function beforeUpload(file :File) {
  if (file.type.indexOf("image/") == -1) {
    ElMessage.error("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。")
  } else {
    if (file.size >= MAX_SIZE) {
      ElMessage.error("图片太大了,请换一张")
      return
    }
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      options.img = typeof reader.result === 'string' ? reader.result : ''
      options.filename = file.name
    }
  }
}

/** 覆盖默认上传行为 */
function requestUpload(_options: UploadRequestOptions) :any  {}

/** 提交上传 */
const isLoading = ref(false)
const handleSubmit = async () => {
  if (!cropperRefs.value) return;

  const blob = await cropperRefs.value.getCropBlob()

  if (!blob) {
    console.error('获取裁剪数据失败')
    return
  }
  if (isLoading.value) return;
  isLoading.value = true
  const formData = new FormData()
  const file = new File([blob], 'cropped-image.jpg', { type: 'image/jpeg' })
  formData.append('file', file)
  if (file.size > MAX_SIZE) {
    ElMessage.error("上传失败: 图片不能超过2MB")
    isLoading.value = false
    return
  }

  const response = await uploadAvatar(formData)

  isLoading.value = false
  handleClose()
  if (response.code === 200) {
    ElMessage.success("更新头像成功")
    await userStore.setUserInfo()
  }
}

const handleRotate = (type :('left' | 'right')) => {
  if (!cropperRefs.value) return;

  if (type === 'left') {
    cropperRefs.value.rotateLeft()
  } else {
    cropperRefs.value.rotateRight()
  }
}

const handleScale = (type :('big' | 'small')) => {
  options.scale = type === 'big' ? 1 : -1
  cropperRefs.value.changeScale(options.scale)
}

const handleClose = () => {
  isUploadAvatar.value = false
  options.img = userInfo.value.avatar || new URL('@/assets/images/logo.jpg', import.meta.url).toString()
}
</script>

<template>
  <div class="upload-avatar" @click="isUploadAvatar = true">
    <slot></slot>
  </div>

  <el-dialog
      v-model="isUploadAvatar"
      title="编辑头像"
      :append-to-body="false"
      class="upload-avatar-dialog"
      @opened="options.visible = true"
      :before-close="handleClose"
  >
    <div class="cropper-avatar">
      <vue-cropper
          ref="cropperRefs"
          :img="options.img"
          :info="true"
          :autoCrop="options.autoCrop"
          :autoCropWidth="options.autoCropWidth"
          :autoCropHeight="options.autoCropHeight"
          :fixedBox="options.fixedBox"
          :outputType="options.outputType"
          v-if="options.visible"
      />
      <div class="option-row">
        <div class="option-col" @click="handleRotate('right')">
          <el-icon><RefreshRight /></el-icon>
          <span>向右旋转</span>
        </div>
        <div class="option-col" @click="handleRotate('left')">
          <el-icon><RefreshLeft /></el-icon>
          <span>向左旋转</span>
        </div>
        <div class="option-col" @click="handleScale('big')">
          <el-icon><ZoomIn /></el-icon>
          <span>放大</span>
        </div>
        <div class="option-col" @click="handleScale('small')">
          <el-icon><ZoomOut /></el-icon>
          <span>缩小</span>
        </div>
        <el-upload
            action="#"
            :http-request="requestUpload"
            :show-file-list="false"
            :before-upload="beforeUpload"
        >
          <div class="option-col">
            <el-icon><Upload /></el-icon>
            <span>上传</span>
          </div>
        </el-upload>
      </div>

      <div class="option-row">
        <el-button
            class="upload-btn"
            type="primary"
            v-loading="isLoading"
            @click="handleSubmit"
        >
          提交
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
:deep(.vue-cropper) {
  width: 100% !important;
  height: size(400) !important;
}

.cropper-avatar {
  width: 100%;
}

.option-row {
  @include flexCenter;
  margin-top: size(12);
  .option-col {
    width: size(60);
    height: size(60);
    font-size: size(24);
    @include flexCenter;
    flex-direction: column;
    color: #1f1f1f;
    margin-right: size(13);
    cursor: pointer;
    span {
      font-size: size(12);
      color: #8c8c8c;
      margin-top: size(6);
    }
    &:hover {
      background: rgba(0, 0, 0, .06);
    }
  }
}

.upload-btn {
  font-size: size(16);
}
</style>
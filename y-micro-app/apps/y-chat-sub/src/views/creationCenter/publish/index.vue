<script setup lang="ts">
import {reactive, ref } from "vue";
import {CaretBottom, Upload} from '@element-plus/icons-vue'
import { createPost } from '@/api/forum.ts'
import { uploadImage } from '@/api/upload.ts'
import { ElMessage, ElLoading } from 'element-plus'

const submitLoading = ref(false)

const params = reactive({
  title: '',
  content: '',
  coverImage: '',
  visibility: 1
})

const visibilitys = reactive([
  {id: 1, name: '公开'},
  {id: 2, name: '好友可见'},
  {id: 3, name: '私密'}
])
const emit = defineEmits(['switchTab'])

const beforeCoverUpload = async (file: File) => {
  const loading = ElLoading.service({
    target: '.publish-upload',
    text: '上传中...'
  })
  try {
    const response = await uploadImage(file)
    // 业务失败的错误提示已由请求层统一弹出，此处只处理成功分支
    if (response.code === 200) {
      params.coverImage = response.data
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
  } finally {
    loading.close()
  }
  return false
}

const handleSubmit = async () => {
  if (!params.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (params.title.trim().length < 5) {
    ElMessage.warning('标题长度至少 5 个字符')
    return
  }
  if (!params.content.trim()) {
    ElMessage.warning('请输入正文')
    return
  }

  submitLoading.value = true
  try {
    const response = await createPost({
      title: params.title.trim(),
      content: params.content,
      coverImage: params.coverImage,
      visibility: params.visibility
    })

    // 业务失败的错误提示已由请求层统一弹出，此处只处理成功分支
    if (response.code === 200) {
      params.title = ''
      params.content = ''
      params.coverImage = ''
      params.visibility = 1
      emit('switchTab', 'me')
      ElMessage.success('提交成功')
    }
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div class="publish">
    <div class="publish-lt">
      <el-upload
          action="#"
          class="publish-upload"
          :show-file-list="false"
          :before-upload="beforeCoverUpload"
      >
        <div class="option-col" v-if="!params.coverImage">
          <el-icon><Upload /></el-icon>
          <span>上传封面</span>
        </div>
        <el-image
            v-else
            class="cover-preview"
            :src="params.coverImage"
            fit="cover"
        />
      </el-upload>
    </div>
    <div class="publish-rt">

      <div class="publish-label">标题</div>
      <div class="title-box">
        <el-input v-model="params.title" placeholder="请输入标题" maxlength="100" show-word-limit />
      </div>

      <div class="publish-label">内容</div>
      <div class="content-box">
        <el-input
            class="content-box-textarea"
            v-model="params.content"
            type="textarea"
            placeholder="请输入内容"
            maxlength="50000"
            show-word-limit
        />
      </div>

      <div class="publish-option">
        <el-select
            v-model="params.visibility"
            :suffix-icon="CaretBottom"
        >
          <el-option
              v-for="item in visibilitys"
              :key="item.id"
              :value="item.id"
              :label="item.name"
          />
        </el-select>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.publish {
  width: 100%;
  height: 100%;
  display: flex;
  box-sizing: border-box;
  padding: size(32);
  &-lt {
    flex: 1;
    box-sizing: border-box;
    padding-right: size(32);
    display: flex;
    flex-direction: column;
  }
  &-rt {
    width: 320px;
    box-sizing: border-box;
    padding-left: size(16);
    display: flex;
    flex-direction: column;
  }
}

.publish-label {
  font-size: 16px;
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 2px;
  margin-top: 14px;
  &:first-child {
    margin-top: 0;
  }
}

.board-box,
.title-box {
  margin: 14px 0;
  :deep(.el-select) {
    width: 100%;
  }
}

.publish-option {
  display: flex;
  :deep(.el-select) {
    width: 110px;
    .el-select__wrapper {
      padding-right: 6px;
    }
    .el-icon {
      color: var(--theme-color) !important;
      font-size: 24px;
    }
  }
  .el-button {
    flex: 1;
    margin-left: 12px;
  }
}

.content-box {
  flex: 1;
  margin: 14px 0;
  :deep(.el-textarea) {
    width: 100%;
    height: 100%;
    .el-textarea__inner {
      height: 100%;
    }
  }
}

.publish-upload {
  flex: 1;
  background: rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
  border: 1.3px solid rgba(0, 0, 0, .1);
  border-radius: 13px;
  overflow: hidden;
  :deep(.el-upload) {
    width: 100%;
    height: 100%;
  }
  .option-col {
    width: 100%;
    height: 100%;
    @include flexCenter;
    flex-direction: column;
    .el-icon {
      font-size: 64px;
      color: #8c8c8c;
    }
    span {
      font-size: 14px;
      letter-spacing: 2px;
    }
  }
  .cover-preview {
    width: 100%;
    height: 100%;
  }
}
</style>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getUsableDispatchPlatforms, submitDispatch } from '@/api/blog/dispatch'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  articleIds: number[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const loading = ref(false)
const submitting = ref(false)
const platforms = ref<any[]>([])
const selectedCodes = ref<string[]>([])
const ignoreDispatched = ref(true)
const result = ref<any>(null)

const loadPlatforms = async () => {
  loading.value = true
  try {
    const res = await getUsableDispatchPlatforms()
    if (res.code === 200) {
      platforms.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

watch(visible, (value) => {
  if (!value) return
  result.value = null
  selectedCodes.value = []
  ignoreDispatched.value = true
  loadPlatforms()
})

const handleSubmit = async () => {
  if (!props.articleIds || props.articleIds.length === 0) {
    ElMessage.warning('请先选择待分发的文章')
    return
  }
  if (selectedCodes.value.length === 0) {
    ElMessage.warning('请至少选择一个目标平台')
    return
  }
  submitting.value = true
  try {
    const res = await submitDispatch({
      articleIds: props.articleIds,
      platformCodes: selectedCodes.value,
      ignoreDispatched: ignoreDispatched.value
    })
    if (res.code === 200) {
      result.value = res.data
      if (res.data?.createdCount > 0) {
        ElMessage.success(`已创建 ${res.data.createdCount} 个分发任务，正在后台执行`)
      } else {
        ElMessage.warning('没有创建任何分发任务，请查看跳过原因')
      }
      emit('success')
    } else {
      ElMessage.error(res.msg || '分发提交失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="分发到第三方平台" width="560px" append-to-body>
    <div v-loading="loading" class="dispatch-body">
      <el-alert
        :title="`已选择 ${articleIds?.length || 0} 篇文章，任务将在后台异步执行`"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="dispatch-platform">
        <div class="dispatch-label">目标平台</div>
        <el-empty v-if="!loading && platforms.length === 0" description="暂无可用平台，请先在分发平台配置中配置并启用" :image-size="80" />
        <el-checkbox-group v-else v-model="selectedCodes">
          <el-checkbox
            v-for="item in platforms"
            :key="item.platformCode"
            :value="item.platformCode"
            :disabled="!item.adapterSupported"
            class="dispatch-platform-item"
          >
            <div class="dispatch-platform-name">
              <span>{{ item.platformName }}</span>
              <el-tag v-if="item.credentialConfigured" size="small" type="success" effect="plain">已配置凭证</el-tag>
              <el-tag v-else size="small" type="warning" effect="plain">未配置凭证</el-tag>
              <el-tag v-if="!item.adapterSupported" size="small" type="danger" effect="plain">无适配器</el-tag>
            </div>
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <el-form label-width="120px" class="dispatch-form">
        <el-form-item label="忽略已分发">
          <el-switch v-model="ignoreDispatched" />
          <span class="dispatch-tip">开启后，已成功分发过的文章不会再重复提交到同一平台</span>
        </el-form-item>
      </el-form>

      <div v-if="result" class="dispatch-result">
        <el-divider content-position="left">提交结果</el-divider>
        <p>新建任务：{{ result.createdCount }} 个，跳过：{{ result.skippedCount }} 个</p>
        <ul v-if="result.skippedReasons?.length" class="dispatch-skipped">
          <li v-for="(reason, index) in result.skippedReasons" :key="index">{{ reason }}</li>
        </ul>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关 闭</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">开始分发</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dispatch-body {
  min-height: 120px;
}

.dispatch-platform {
  margin-top: 16px;
}

.dispatch-label {
  margin-bottom: 8px;
  font-weight: bold;
}

.dispatch-platform-item {
  display: block;
  height: auto;
  margin-bottom: 6px;
}

.dispatch-platform-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dispatch-form {
  margin-top: 12px;
}

.dispatch-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.dispatch-result {
  p {
    margin: 8px 0;
  }
}

.dispatch-skipped {
  max-height: 160px;
  overflow-y: auto;
  padding-left: 20px;
  color: #e6a23c;

  li {
    margin-bottom: 4px;
    line-height: 1.5;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>

<script setup lang="ts">
import { ref, watch, toRefs } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { findAnnouncements, publishAnnouncement, deleteAnnouncement } from '@/api/group.ts'

const isShow = defineModel('modelValue', { type: Boolean })

const props = defineProps<{
  groupId: string | number
  isAdmin: boolean
}>()
const { groupId, isAdmin } = toRefs(props)

const announcements = ref<any[]>([])
const loading = ref(false)
const isPublishing = ref(false)
const form = ref<{ title: string; content: string; isPinned: boolean }>({
  title: '',
  content: '',
  isPinned: false,
})

/** 加载群公告列表 */
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const result = await findAnnouncements(groupId.value)
    if (result.code === 200) {
      announcements.value = result.data || []
    }
  } finally {
    loading.value = false
  }
}

/** 发布群公告 */
const handlePublish = async () => {
  const content = form.value.content.trim()
  if (!content) {
    ElMessage.warning('请输入公告内容')
    return
  }

  isPublishing.value = true
  try {
    const result = await publishAnnouncement({
      groupId: groupId.value,
      title: form.value.title.trim(),
      content,
      isPinned: form.value.isPinned ? 1 : 0,
    })

    if (result.code === 200) {
      ElMessage.success('发布成功')
      form.value = { title: '', content: '', isPinned: false }
      await loadAnnouncements()
    }
  } finally {
    isPublishing.value = false
  }
}

/** 删除群公告 */
const handleDelete = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' })
  } catch {
    return
  }

  const result = await deleteAnnouncement(groupId.value, item.id)
  if (result.code === 200) {
    ElMessage.success('删除成功')
    await loadAnnouncements()
  }
}

watch(isShow, (val) => {
  if (val) {
    loadAnnouncements()
  }
})
</script>

<template>
  <el-dialog v-model="isShow" :align-center="true" width="420px" title="群公告">
    <div v-loading="loading" class="announcement-box">
      <div v-if="announcements.length === 0 && !loading" class="announcement-empty">
        暂无群公告
      </div>

      <div v-for="item in announcements" :key="item.id" class="announcement-item">
        <div class="announcement-item-header">
          <span class="announcement-item-title">
            <el-tag v-if="item.isPinned === 1" size="small" type="danger" effect="plain">置顶</el-tag>
            {{ item.title || '群公告' }}
          </span>
          <div class="announcement-item-right">
            <span class="announcement-item-time">{{ item.createTime }}</span>
            <el-icon v-if="isAdmin" class="announcement-item-del" @click="handleDelete(item)">
              <Delete />
            </el-icon>
          </div>
        </div>
        <div class="announcement-item-content">{{ item.content }}</div>
      </div>

      <template v-if="isAdmin">
        <el-divider content-position="left">发布公告</el-divider>
        <el-input v-model="form.title" maxlength="100" placeholder="公告标题（选填）" />
        <el-input
            v-model="form.content"
            class="announcement-form-content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入公告内容"
        />
        <el-checkbox v-model="form.isPinned" class="announcement-form-pinned">置顶该公告</el-checkbox>
      </template>
    </div>

    <template #footer>
      <el-button @click="isShow = false">关闭</el-button>
      <el-button v-if="isAdmin" type="primary" :loading="isPublishing" @click="handlePublish">发布</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.announcement-box {
  max-height: 60vh;
  overflow-y: auto;
}

.announcement-empty {
  text-align: center;
  color: #8c8c8c;
  padding: size(24) 0;
}

.announcement-item {
  padding: size(12) 0;
  border-bottom: 1px solid #f0f0f0;

  &-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &-title {
    display: flex;
    align-items: center;
    gap: size(6);
    font-size: size(14);
    font-weight: bold;
    color: #1f1f1f;
  }

  &-right {
    display: flex;
    align-items: center;
    gap: size(8);
  }

  &-time {
    font-size: size(12);
    color: #8c8c8c;
  }

  &-del {
    cursor: pointer;
    color: #e00f00;
  }

  &-content {
    margin-top: size(6);
    font-size: size(13);
    color: #4a4a4a;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.announcement-form-content {
  margin-top: size(12);
}

.announcement-form-pinned {
  margin-top: size(12);
}
</style>

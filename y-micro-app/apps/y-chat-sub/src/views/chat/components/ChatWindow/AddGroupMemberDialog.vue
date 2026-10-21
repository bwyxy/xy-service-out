<script setup lang="ts">
import { computed, ref, toRefs, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addGroupMembers } from '@/api/group.ts'

const isShow = defineModel('modelValue', { type: Boolean })

const props = defineProps<{
  groupId: string | number
  /** 群内现有成员，用于从好友列表中排除 */
  members: GroupMember[]
}>()
const { groupId, members } = toRefs(props)

const emit = defineEmits<{
  (e: 'success'): void
}>()

const selectedFriends = ref<any[]>([])
const submitting = ref<boolean>(false)

/** 已在群内的成员 Id */
const excludeUids = computed(() => members.value.map((item) => item.uid))

watch(isShow, (val) => {
  if (val) {
    selectedFriends.value = []
  }
})

/** 添加所选好友入群 */
const handleSubmit = async () => {
  if (selectedFriends.value.length === 0) {
    ElMessage.warning('请选择要添加的成员')
    return
  }

  submitting.value = true
  try {
    const result = await addGroupMembers({
      groupId: groupId.value,
      memberIds: selectedFriends.value.map((item) => item.friendId),
    })

    // 业务失败的错误提示已由请求层统一弹出，此处只处理成功分支
    if (result.code === 200) {
      ElMessage.success('添加成功')
      isShow.value = false
      emit('success')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="isShow" :align-center="true" width="420px" title="添加群成员" destroy-on-close>
    <div class="add-group-member-box">
      <span class="add-group-member-tip">选择联系人（已在群内的好友不会展示）</span>
      <friend-list
          :is-selected="true"
          :exclude-uids="excludeUids"
          @select-friend="(list :any[]) => selectedFriends = list"
      />
    </div>

    <template #footer>
      <el-button @click="isShow = false">取消</el-button>
      <el-button
          type="primary"
          :loading="submitting"
          :disabled="selectedFriends.length === 0"
          @click="handleSubmit"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.add-group-member-tip {
  display: block;
  font-size: size(12);
  color: #8c8c8c;
  margin-bottom: size(8);
}

.add-group-member-box {
  height: 360px;
  overflow-y: auto;
  box-sizing: border-box;

  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }
}
</style>

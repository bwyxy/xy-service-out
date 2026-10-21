<script setup lang="ts">
import { computed, ref, watch, toRefs } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storeToRefs } from 'pinia'
import useUserStore from '@/store/user.ts'
import { muteMember, removeGroupMember, updateGroupNickname } from '@/api/group.ts'
import { resolveMemberName } from '@/utils/chat.ts'

const isShow = defineModel('modelValue', { type: Boolean })

const props = defineProps<{
  groupId: string | number
  member: GroupMember | null
  isAdmin: boolean
  /** 是否可移出该成员（群主/管理员可操作，群主不可被移出） */
  canRemove: boolean
}>()
const { groupId, member, isAdmin, canRemove } = toRefs(props)

const emit = defineEmits<{
  (e: 'success'): void
}>()

const { userInfo } = storeToRefs(useUserStore())

const nickname = ref<string>('')
const muteMinutes = ref<number>(60)
const submitting = ref<boolean>(false)

/** 是否为当前登录用户 */
const isSelf = computed(() => String(member.value?.uid) === String(userInfo.value.uid))
/** 是否为群主 */
const isOwner = computed(() => member.value?.role === 2)
/** 是否可被禁言（管理员操作、非群主、非自己） */
const canMute = computed(() => isAdmin.value && !isOwner.value && !isSelf.value)

const muteOptions = [
  { label: '10 分钟', value: 10 },
  { label: '1 小时', value: 60 },
  { label: '1 天', value: 1440 },
  { label: '7 天', value: 10080 },
]

watch(isShow, (val) => {
  if (val) {
    nickname.value = member.value?.groupNickname || ''
    muteMinutes.value = 60
  }
})

/** 修改我的群昵称 */
const handleSaveNickname = async () => {
  const name = nickname.value.trim()
  if (!name) {
    ElMessage.warning('请输入群昵称')
    return
  }

  submitting.value = true
  try {
    const result = await updateGroupNickname({ groupId: groupId.value, groupNickname: name })
    if (result.code === 200) {
      ElMessage.success('修改成功')
      isShow.value = false
      emit('success')
    }
  } finally {
    submitting.value = false
  }
}

/** 禁言/解除禁言，minutes 小于等于 0 表示解除 */
const handleMute = async (minutes: number) => {
  submitting.value = true
  try {
    const result = await muteMember({
      groupId: groupId.value,
      uid: member.value?.uid,
      muteMinutes: minutes,
    })
    if (result.code === 200) {
      ElMessage.success(minutes > 0 ? '已禁言' : '已解除禁言')
      isShow.value = false
      emit('success')
    }
  } finally {
    submitting.value = false
  }
}

/** 移出群聊 */
const handleRemove = async () => {
  const name = resolveMemberName(member.value, '该成员')

  try {
    await ElMessageBox.confirm(`确定将「${name}」移出群聊吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '移出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    const result = await removeGroupMember(groupId.value, member.value?.uid ?? '')

    // 业务失败的错误提示已由请求层统一弹出，此处只处理成功分支
    if (result.code === 200) {
      ElMessage.success('已移出群聊')
      isShow.value = false
      emit('success')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="isShow" :align-center="true" width="360px" title="成员管理">
    <div class="member-action-box">
      <div class="member-action-name">
        <span>{{ resolveMemberName(member, '') }}</span>
      </div>

      <!-- 修改我的群昵称 -->
      <div v-if="isSelf" class="member-action-section">
        <div class="member-action-label">我的群昵称</div>
        <el-input v-model="nickname" maxlength="50" placeholder="请输入群昵称" />
        <el-button
            class="member-action-save"
            type="primary"
            :loading="submitting"
            @click="handleSaveNickname"
        >
          保存
        </el-button>
      </div>

      <!-- 管理员禁言 -->
      <div v-if="canMute" class="member-action-section">
        <div class="member-action-label">禁言时长</div>
        <el-select v-model="muteMinutes" style="width: 100%">
          <el-option
              v-for="item in muteOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
        <div class="member-action-btns">
          <el-button type="danger" :loading="submitting" @click="handleMute(muteMinutes)">禁言</el-button>
          <el-button :loading="submitting" @click="handleMute(0)">解除禁言</el-button>
        </div>
      </div>

      <!-- 群主/管理员移除成员 -->
      <div v-if="canRemove" class="member-action-section">
        <el-button
            type="danger"
            plain
            style="width: 100%"
            :loading="submitting"
            @click="handleRemove"
        >
          移出群聊
        </el-button>
      </div>
    </div>

    <template #footer>
      <el-button @click="isShow = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.member-action-name {
  font-size: size(16);
  font-weight: bold;
  color: #1f1f1f;
  text-align: center;
  margin-bottom: size(16);
}

.member-action-section {
  margin-bottom: size(16);
}

.member-action-label {
  font-size: size(13);
  color: #8c8c8c;
  margin-bottom: size(8);
}

.member-action-save {
  margin-top: size(12);
  width: 100%;
}

.member-action-btns {
  display: flex;
  margin-top: size(12);
  gap: size(12);

  .el-button {
    flex: 1;
    margin-left: 0;
  }
}
</style>

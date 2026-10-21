<script setup lang="ts">
import {computed, ref, toRefs} from "vue";
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import {ArrowDown, ArrowRight, ArrowUp, Plus, Search} from "@element-plus/icons-vue";
import useUserStore from "@/store/user.ts";
import useChatStore from "@/store/chat.ts";
import { editGroup, dissolveGroup } from "@/api/group.ts";
import { resolveMemberName } from "@/utils/chat.ts";
import type { SearchMessageItem } from "@/api/chat.ts";
import EditGroupName from "@/views/chat/components/ChatWindow/EditGroupName.vue";
import GroupAnnouncement from "@/views/chat/components/ChatWindow/GroupAnnouncement.vue";
import MemberActionDialog from "@/views/chat/components/ChatWindow/MemberActionDialog.vue";
import AddGroupMemberDialog from "@/views/chat/components/ChatWindow/AddGroupMemberDialog.vue";
import SearchMessageDialog from "@/views/chat/components/ChatWindow/SearchMessageDialog.vue";

const { userInfo } = storeToRefs(useUserStore())
const chatStore = useChatStore()

const chatDrawerRefs = ref<HTMLElement>()
const isShow = defineModel('modelValue', {
  type: Boolean
})

const props = defineProps<{
  members: GroupMember[],
  info: any
}>()
const { members, info } = toRefs(props)

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'locate', item: SearchMessageItem): void
  (e: 'dissolved'): void
}>()

const isExpandMember = ref<boolean>(false)
const isShowAnnouncement = ref<boolean>(false)
const isShowMemberAction = ref<boolean>(false)
const isShowAddMember = ref<boolean>(false)
const isShowSearch = ref<boolean>(false)
const selectedMember = ref<GroupMember | null>(null)

/** 当前登录用户在群内的成员信息 */
const currentMember = computed(() => {
  return members.value.find(item => String(item.uid) === String(userInfo.value.uid))
})

/** 当前登录用户是否为群主/管理员 */
const isAdmin = computed(() => (currentMember.value?.role ?? 0) > 0)

/** 当前登录用户是否为群主 */
const isOwner = computed(() => currentMember.value?.role === 2)

/** 解散群聊（仅群主可操作） */
const handleDissolveGroup = async () => {
  if (!isOwner.value) {
    ElMessage.warning('只有群主才能解散群聊')
    return
  }

  try {
    await ElMessageBox.confirm('解散后将无法恢复，确定解散该群聊吗？', '提示', {
      type: 'warning',
      confirmButtonText: '解散',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  const result = await dissolveGroup(info.value.groupId)

  // 业务失败的错误提示已由请求层统一弹出，此处只处理成功分支
  if (result.code === 200) {
    ElMessage.success('群聊已解散')
    emit('dissolved')
  }
}

/** 删除聊天记录：仅本机清除，对方仍可查看 */
const handleClearRecord = async () => {
  const contact = chatStore.activeContact

  if (!contact) {
    return
  }

  try {
    await ElMessageBox.confirm(
        '删除后，该聊天的聊天记录仅在本机清除，对方仍可查看。',
        '删除聊天记录',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  const success = await chatStore.clearMessages(contact)

  if (success) {
    ElMessage.success('聊天记录已删除')
  } else {
    ElMessage.error('删除失败，请重试')
  }
}

/** 点击成员，打开成员管理弹窗 */
const handleMemberClick = (item: GroupMember) => {
  selectedMember.value = item
  isShowMemberAction.value = true
}

/** 打开添加群成员弹窗 */
const handleAddMember = () => {
  isShowAddMember.value = true
}

/** 是否可移出成员：群主/管理员可操作，群主与自己不可被移出，管理员之间不能互相移出 */
const canRemoveMember = computed(() => {
  const target = selectedMember.value

  if (!target || !isAdmin.value) {
    return false
  }
  if (String(target.uid) === String(userInfo.value.uid) || target.role === 2) {
    return false
  }
  // 管理员只能移出普通成员
  return isOwner.value || target.role === 0
})

/** 打开「我的群昵称」弹窗 */
const handleEditMyNickname = () => {
  if (!currentMember.value) {
    ElMessage.warning('您不在该群聊中')
    return
  }
  selectedMember.value = currentMember.value
  isShowMemberAction.value = true
}

/** 切换全员禁言 */
const handleToggleBan = async (val: string | number | boolean) => {
  const enabled = val === true
  const result = await editGroup({ groupId: info.value.groupId, isBan: enabled ? 1 : 0 })

  if (result.code === 200) {
    ElMessage.success(enabled ? '已开启全员禁言' : '已关闭全员禁言')
    emit('refresh')
  }
}

/** 编辑群名/公告/禁言成功后通知父组件刷新群信息 */
const handleRefresh = () => {
  emit('refresh')
}

/** 当前会话是否置顶 */
const isTop = computed(() => !!chatStore.activeContact?.top)

/** 置顶/取消置顶当前会话 */
const handleToggleTop = async (val: string | number | boolean) => {
  const contact = chatStore.activeContact

  if (!contact) {
    return
  }

  const success = await chatStore.setContactTop(contact, val === true)

  if (!success) {
    ElMessage.error('操作失败，请重试')
  }
}

const slotMembers = computed(() => {
  if (isExpandMember.value) {
    return members.value.sort((a, b) => b.role - a.role)
  } else {
    const list = members.value.sort((a, b) => b.role - a.role)
    return list.slice(0, 7)
  }
})
</script>

<template>
  <div
      ref="chatDrawerRefs"
      v-show="isShow"
      :class="['chat-drawer-box', { 'show-drawer': isShow }]"
  >
    <div class="group-info">
      <el-image class="group-avatar" :src="info.avatar">
        <template #error>
          <img class="group-avatar" src="@/assets/images/default_group_avatar.png" alt="avatar" />
        </template>
      </el-image>
      <div class="group-info-rt">
        <div style="flex: 1;display: flex">
          <span>
            {{ info.name }}
          </span>
          <!-- 编辑群聊名称 -->
          <edit-group-name
              :group-id="info.groupId"
              :name="info.name"
              @success="handleRefresh"
          />
        </div>
        <span>最大群人数: {{ info.maxMember }}</span>
      </div>
    </div>
    <!-- 搜索群成员 -->
    <el-input placeholder="搜索群成员" :prefix-icon="Search" />
    <!--  成员列表  -->
    <div :class="['member-list', {'member-list_auto_height': isExpandMember}]">
      <div
          v-for="(item, index) in slotMembers"
          :key="index"
          class="member-item"
          @click="handleMemberClick(item)"
      >
        <div class="member-avatar">
          <el-image class="member-avatar-image" :src="item.avatar">
            <template #error>
              <img class="member-avatar-image" src="@/assets/images/logo.jpg" alt="avatar" />
            </template>
          </el-image>
        </div>
        <div class="member-name">
          <span>{{ resolveMemberName(item, '') }}</span>
        </div>
      </div>
      <div v-if="slotMembers.length < info.maxMember" class="add-member" @click="handleAddMember">
        <div class="add-member-content">
          <el-icon><Plus /></el-icon>
        </div>
      </div>
    </div>
    <div v-if="members.length > 7" class="expand-member" @click="isExpandMember = !isExpandMember">
      <span>{{ isExpandMember ? '收起' : '展开更多' }}</span>
      <el-icon v-if="isExpandMember"><ArrowUp /></el-icon>
      <el-icon v-else><ArrowDown /></el-icon>
    </div>
    <div class="group-cell" @click="isShowSearch = true">
      <span>查找聊天内容</span>
      <el-icon><ArrowRight /></el-icon>
    </div>
    <div class="group-cell" @click="isShowAnnouncement = true">
      <span>群公告</span>
      <el-icon><ArrowRight /></el-icon>
    </div>
    <div class="group-cell" @click="handleEditMyNickname">
      <span>我的群昵称</span>
      <el-icon><ArrowRight /></el-icon>
    </div>
    <div v-if="isAdmin" class="group-cell">
      <span>全员禁言</span>
      <el-switch :model-value="info.isBan === 1" @change="handleToggleBan" />
    </div>
    <div class="group-cell">
      <span>置顶聊天</span>
      <el-switch :model-value="isTop" @change="handleToggleTop" />
    </div>
    <div class="group-cell" @click="handleClearRecord">
      <span style="color: #2f54eb;text-align: center">删除聊天记录</span>
    </div>
    <div class="group-cell" style="border-top: none" @click="handleDissolveGroup">
      <span style="color: #e00f00;text-align: center">解散群聊</span>
    </div>

    <!-- 查找聊天内容 -->
    <search-message-dialog
        v-model="isShowSearch"
        :members="members"
        @locate="emit('locate', $event)"
    />

    <!-- 群公告 -->
    <group-announcement
        v-model="isShowAnnouncement"
        :group-id="info.groupId"
        :is-admin="isAdmin"
    />

    <!-- 添加群成员 -->
    <add-group-member-dialog
        v-model="isShowAddMember"
        :group-id="info.groupId"
        :members="members"
        @success="handleRefresh"
    />

    <!-- 成员管理：群昵称 / 禁言 / 移出群聊 -->
    <member-action-dialog
        v-model="isShowMemberAction"
        :group-id="info.groupId"
        :member="selectedMember"
        :is-admin="isAdmin"
        :can-remove="canRemoveMember"
        @success="handleRefresh"
    />
  </div>
</template>

<style scoped lang="scss">
.chat-drawer-box {
  width: 0;
  height: 100%;
  background: #fff;
  border-left: 1px solid rgba(0, 0, 0, .06);
  transition: 1s;
  box-sizing: border-box;
  padding: 0 size(24);
  right: 0;
  overflow: hidden;
  overflow-y: scroll;
}

.group-top-title {
  height: size(40);
  font-size: size(16);
  border-bottom: 1px solid #f0f0f0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  span {
    flex: 1;
    color: #1f1f1f;
    font-weight: bold;
    letter-spacing: 1px;
  }
}

.group-info {
  display: flex;
  margin: size(14) 0 size(24);
  .group-avatar {
    width: size(70);
    height: size(70);
    border-radius: 50px;
    flex-shrink: 0;
  }

  &-rt {
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    padding: size(4) 0;
    margin-left: size(12);
    span {
      font-size: size(16);
      letter-spacing: 1px;
      line-height: size(16);
      color: #1f1f1f;
      font-weight: bold;
      @include textEllipsis(2);
    }
    span:nth-child(2) {
      font-size: size(12);
      color: #8c8c8c;
    }
  }
}

.member-list {
  height: size(140);
  display: flex;
  flex-wrap: wrap;
  overflow: hidden;
  margin-top: size(13);

  &_auto_height {
    height: auto;
  }

  &::-webkit-scrollbar {
    appearance: none;
    height: 0;
  }
}

.group-cell {
  height: size(60);
  box-sizing: border-box;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  font-size: size(16);
  cursor: pointer;
  span {
    flex: 1;
    color: #1f1f1f;
    letter-spacing: 1px;
  }
}

.member-item {
  width: size(70);
  height: size(70);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.add-member {
  width: size(70);
  height: size(70);
  flex-shrink: 0;
  cursor: pointer;
  @include flexCenter;
  &-content {
    width: size(50);
    height: size(50);
    background: #f0f0f0;
    border-radius: 50px;
    border: 1px solid #d9d9d9;
    box-sizing: border-box;
    color: #8c8c8c;
    font-size: size(20);
    @include flexCenter;
  }
}

.expand-member {
  height: size(30);
  font-size: size(14);
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: size(24);
}

.member-avatar {
  width: size(50);
  height: size(50);
  position: relative;
  &-image {
    width: 100%;
    height: 100%;
    border-radius: 50px;
  }
}

.member-role {
  width: 100%;
  height: size(14);
  background: rgba(246, 255, 237, 1);
  border-radius: 0 0 6px 6px;
  @include flexCenter;
  position: absolute;
  left: 0;
  bottom: 0;
  span {
    font-size: size(10);
    font-weight: bold;
    color: #1f1f1f;
    letter-spacing: 1px;
  }
}

.member-name {
  span {
    color: #8c8c8c;
    font-size: size(10);
    @include textEllipsis(1);
  }
}

.show-drawer {
  width: size(360);
  padding: 0 size(24);
  position: relative;
}
</style>
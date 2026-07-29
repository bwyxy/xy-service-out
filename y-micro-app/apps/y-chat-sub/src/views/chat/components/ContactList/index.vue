<script setup lang="ts">
import { ref } from 'vue'
import useChatStore from '@/store/chat'
import { Search, Plus } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const showAddDialog = ref(false)
const addUid = ref('')
const addName = ref('')

const handleAddContact = () => {
  if (addUid.value.trim()) {
    chatStore.addContact(addUid.value.trim(), addName.value.trim() || addUid.value.trim())
    addUid.value = ''
    addName.value = ''
    showAddDialog.value = false
  }
}

const handleSelectContact = (contact: any) => {
  chatStore.selectContact(contact)
}
</script>

<template>
  <div class="contact-list">
    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-input
        v-model="addUid"
        placeholder="搜索"
        :prefix-icon="Search"
        size="small"
        @keyup.enter="handleAddContact"
      />
      <el-button :icon="Plus" circle size="small" @click="showAddDialog = true" />
    </div>

    <!-- 添加好友弹窗 -->
    <el-dialog v-model="showAddDialog" title="添加好友" width="300px">
      <el-input v-model="addUid" placeholder="用户ID" style="margin-bottom: 10px" />
      <el-input v-model="addName" placeholder="备注名（可选）" />
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddContact">添加</el-button>
      </template>
    </el-dialog>

    <!-- 联系人列表 -->
    <div class="contact-items">
      <div
        v-for="contact in chatStore.contacts"
        :key="contact.uid + contact.channelType"
        class="contact-item"
        :class="{ active: chatStore.activeContact?.uid === contact.uid }"
        @click="handleSelectContact(contact)"
      >
        <div class="avatar">{{ contact.name.charAt(0).toUpperCase() }}</div>
        <div class="info">
          <div class="name">{{ contact.name }}</div>
          <div class="type-tag">{{ contact.type === 'group' ? '群聊' : '好友' }}</div>
        </div>
      </div>

      <div v-if="chatStore.contacts.length === 0" class="empty">
        暂无联系人，请添加好友
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.contact-list {
  width: 260px;
  height: 100%;
  background: #fff;
  border-right: 1px solid $border-color-1;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid $border-color-1;
}

.contact-items {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f5f5;
  }

  &.active {
    background: #e8f4ff;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #faad14;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: bold;
    flex-shrink: 0;
  }

  .info {
    flex: 1;
    min-width: 0;

    .name {
      font-size: 14px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .type-tag {
      font-size: 12px;
      color: #999;
    }
  }
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}
</style>
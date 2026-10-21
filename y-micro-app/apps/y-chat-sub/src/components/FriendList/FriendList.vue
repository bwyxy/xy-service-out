<script setup lang="ts">
import {onMounted, ref, toRefs} from "vue";
import { getFriendList } from '@/api/friend.ts'
import {Check} from "@element-plus/icons-vue";

const props = defineProps<{
  isSelected?: boolean;
  /** 需要排除的用户 Id（如已在群内的成员），按好友用户 Id 匹配 */
  excludeUids?: Array<string | number>;
}>()

const { isSelected, excludeUids } = toRefs(props)

const emit = defineEmits<{
  (e :'select-friend', row :any) :void
}>()

const searchVal = ref<string>("")
const friends = ref<any[]>([])
const selectedFriends = ref<any[]>([])

/** 获取好友列表 */
const fetchFriends = async () => {
  const response = await getFriendList()

  if (response.code === 200) {
    const excludeSet = new Set((excludeUids.value || []).map((uid) => String(uid)))

    friends.value = (response.data || []).filter(
        (item :any) => !excludeSet.has(String(item.friendId))
    )
  }
}

const handleFriend = (row :any) => {
  if (isSelected.value) {
    if (isSelectedFriend(row.id)) {
      const index = selectedFriends.value.findIndex(f => f.id === row.id)

      selectedFriends.value.splice(index, 1)
    } else {
      selectedFriends.value.push(row)
    }
    emit('select-friend', selectedFriends.value)
  }
}

/** 是否选中 */
const isSelectedFriend = (id :number) => {
  if (selectedFriends.value.find(f => f.id === id)) {
    return true
  } else {
    return false
  }
}

onMounted(() => {
  fetchFriends()
})
</script>

<template>
  <div class="friend-list">
    <el-input v-model="searchVal" size="small" placeholder="搜索好友" />

    <div
        v-for="item in friends"
        :key="item.id"
        class="friend-item"
        @click="handleFriend(item)"
    >
      <div
          v-if="isSelected"
          :class="['friend-selected', { 'friend-selected_active': isSelectedFriend(item.id) }]">
        <el-icon><Check /></el-icon>
      </div>
      <img v-if="item.avatar" class="user-head" :src="item.avatar" alt="avatar" />
      <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
      <span>{{ item.nickname || '' }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.friend-list {
  width: 100%;
  box-sizing: border-box;
  padding: 16px;
}

.friend-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 14px 0;

  .friend-selected {
    width: 16px;
    height: 16px;
    border: 1px solid #dbdbdb;
    border-radius: 50%;
    box-sizing: border-box;
    font-size: 12px;
    color: #fff;
    margin-right: 6px;
    background: #fff;
    @include flexCenter;
    &_active {
      border: none;
      background: $theme-color;
    }
  }

  .user-head {
    width: 32px;
    height: 32px;
    border-radius: 4px;
    box-sizing: border-box;
    border: 1px solid #dbdbdb;
  }
  span {
    flex: 1;
    font-size: 12px;
    color: #1f1f1f;
    letter-spacing: 1px;
    margin-left: 4px;
    @include textEllipsis(2);
  }
}
</style>
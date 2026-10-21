<script setup lang="ts">
import { getFriendList, getReceivedRequests } from '@/api/friend.ts'
import { getUserInfo } from '@/api/user.ts'
import {computed, onMounted, ref} from "vue";
import {useRouter} from 'vue-router'
import {ChatRound, Phone, Plus, VideoCamera} from "@element-plus/icons-vue";
import useCallStore from '@/store/call.ts'
import useUserStore from '@/store/user.ts'
import useChatStore from '@/store/chat.ts'
import FriendRequestDialog from '@/views/chat/components/SearchFun/FriendRequestDialog.vue'

const friends = ref<any[]>([])
const friendInfo = ref<any>(null)
const requestDialogVisible = ref(false)
const pendingCount = ref(0)
const callStore = useCallStore()
const userStore = useUserStore()
const chatStore = useChatStore()
const router = useRouter()

/** 获取好友列表 */
const fetchFriends = async () => {
  const response = await getFriendList()

  if (response.code === 200) {
    friends.value = response.data
  }
}

/** 汉字首字母边界字符（按字母顺序排列） */
const LETTER_BOUNDARIES = '啊芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝'
/** 与边界字符一一对应的字母（不含 I/U/V） */
const BOUNDARY_LETTERS = 'ABCDEFGHJKLMNOPQRSTWXYZ'

/** 获取昵称首字母，非汉字/字母统一归入 # */
const getFirstLetter = (name: string): string => {
  const first = (name || '').trim().charAt(0)
  if (!first) {
    return '#'
  }
  if (/[a-zA-Z]/.test(first)) {
    return first.toUpperCase()
  }
  if (!/[\u4e00-\u9fa5]/.test(first)) {
    return '#'
  }
  for (let i = LETTER_BOUNDARIES.length - 1; i >= 0; i--) {
    if (first.localeCompare(LETTER_BOUNDARIES.charAt(i), 'zh-CN') >= 0) {
      return BOUNDARY_LETTERS.charAt(i)
    }
  }
  return '#'
}

/** 按首字母分组的好友列表，组内按拼音排序，# 组置底 */
const groupedFriends = computed(() => {
  const map = new Map<string, any[]>()
  friends.value.forEach((friend) => {
    const letter = getFirstLetter(friend.nickname || '')
    if (!map.has(letter)) {
      map.set(letter, [])
    }
    map.get(letter)!.push(friend)
  })
  return Array.from(map.entries())
      .map(([letter, list]) => {
        const sorted = list.slice().sort((a, b) =>
            (a.nickname || '').localeCompare(b.nickname || '', 'zh-CN'))
        return {letter, list: sorted}
      })
      .sort((a, b) => {
        if (a.letter === '#') return 1
        if (b.letter === '#') return -1
        return a.letter < b.letter ? -1 : 1
      })
})

/** 索引条字母（仅展示实际存在的分组） */
const visibleLetters = computed(() => groupedFriends.value.map((group) => group.letter))

const listRef = ref<HTMLElement>()
const activeLetter = ref('')

/** 点击字母索引定位到对应分组 */
const scrollToLetter = (letter: string) => {
  activeLetter.value = letter
  listRef.value?.querySelector(`[data-letter="${letter}"]`)?.scrollIntoView({block: 'start'})
}

/** 获取待处理好友申请数量（用于「新的朋友」红点） */
const fetchPendingCount = async () => {
  const response = await getReceivedRequests(0)

  if (response.code === 200) {
    pendingCount.value = (response.data || []).length
  }
}

/** 打开「新的朋友」 */
const openRequests = () => {
  requestDialogVisible.value = true
}

/** 好友申请处理完成后刷新好友列表与红点 */
const handleRequestRefresh = () => {
  fetchFriends()
  fetchPendingCount()
}

/** 获取联系人详细信息 */
const getInfo = async (uId: string) => {
  const response = await getUserInfo(Number(uId))

  if (response.code === 200) {
    friendInfo.value = response.data
  }
}

/** 发起语音通话 */
const handleAudioCall = () => {
  if (!friendInfo.value) return
  callStore.startCall({
    mode: 'p2p',
    peer: {
      uid: String(friendInfo.value.uid ?? friendInfo.value.friendId),
      nickname: friendInfo.value.nickname || '',
      avatar: friendInfo.value.avatar || '',
    },
  }, 1)
}

/** 发起视频通话 */
const handleVideoCall = () => {
  if (!friendInfo.value) return
  callStore.startCall({
    mode: 'p2p',
    peer: {
      uid: String(friendInfo.value.uid ?? friendInfo.value.friendId),
      nickname: friendInfo.value.nickname || '',
      avatar: friendInfo.value.avatar || '',
    },
  }, 2)
}

/** 向好友发送消息：选中该会话并跳转到聊天界面 */
const handleSendMessage = () => {
  if (!friendInfo.value) return

  const uid = String(friendInfo.value.uid ?? friendInfo.value.friendId)
  const nickname = friendInfo.value.nickname || uid
  const avatar = friendInfo.value.avatar || ''

  // 确保会话出现在左侧消息列表
  chatStore.addContact(uid, nickname, avatar)

  // 设置当前会话并跳转到聊天页（chat 页据 currentChannel 渲染 ChatWindow）
  userStore.currentChannel = { channelId: uid, type: 1 }
  router.push('/chat')

  // 选中该会话，加载历史消息
  const contact = chatStore.contacts.find((c) => c.uid === uid && c.channelType === 1)
  if (contact) {
    chatStore.selectContact(contact)
  }
}

onMounted(() => {
  fetchFriends()
  fetchPendingCount()
})
</script>

<template>
  <div class="friend">
    <div class="friend-sidebar">
      <div class="friend-item new-friend" @click="openRequests">
        <div class="new-friend-icon">
          <el-icon><Plus /></el-icon>
        </div>
        <span>新的朋友</span>
        <el-badge v-if="pendingCount > 0" :value="pendingCount" class="new-friend-badge" />
      </div>
<!--      <div class="friend-tiler">-->
<!--        <span>联系人列表</span>-->
<!--      </div>-->
      <div ref="listRef" class="friend-list">
        <template v-for="group in groupedFriends" :key="group.letter">
          <div class="group-title" :data-letter="group.letter">{{ group.letter }}</div>
          <div
              v-for="item in group.list"
              :key="item.id"
              class="friend-item"
              @click="getInfo(item.friendId)"
          >
            <img v-if="item.avatar" class="user-head" :src="item.avatar" alt="avatar" />
            <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
            <span>{{ item.nickname || '' }}</span>
          </div>
        </template>
        <div v-if="friends.length === 0" class="empty-text">暂无好友</div>
      </div>
      <div v-if="visibleLetters.length" class="letter-bar">
        <div
            v-for="letter in visibleLetters"
            :key="letter"
            class="letter-item"
            :class="{ active: activeLetter === letter }"
            @click="scrollToLetter(letter)"
        >{{ letter }}</div>
      </div>
    </div>

    <div class="friend-main">
      <div v-if="friendInfo" class="friend-main-content">
        <div class="user-box">
          <img v-if="friendInfo.avatar" class="user-head" :src="friendInfo.avatar" alt="avatar" />
          <img v-else class="user-head" src="@/assets/images/logo.jpg" alt="avatar" />
          <div class="user-box-info">
            <span>{{ friendInfo.nickname }}</span>
            <span>{{ friendInfo.sex === '1' ? '男' : friendInfo.sex === '2' ? '女' : '未知' }}</span>
          </div>
        </div>

        <div class="user-chat">
          <div class="user-chat-item" @click="handleSendMessage">
            <el-icon><ChatRound /></el-icon>
            <span>发消息</span>
          </div>
          <div class="user-chat-item" @click="handleAudioCall">
            <el-icon><Phone /></el-icon>
            <span>语音聊天</span>
          </div>
          <div class="user-chat-item" @click="handleVideoCall">
            <el-icon><VideoCamera /></el-icon>
            <span>视频聊天</span>
          </div>
        </div>
      </div>
    </div>

    <friend-request-dialog v-model:visible="requestDialogVisible" @refresh="handleRequestRefresh" />
  </div>
</template>

<style scoped lang="scss">
.friend {
  width: 100%;
  height: 100%;
  display: flex;
}

.friend-sidebar {
  position: relative;
  width: size(320);
  height: 100%;
  background-color: #fafafa;
  border-right: 1px solid rgba(0, 0, 0, .06);
  box-sizing: border-box;
  padding: size(24) size(24) 0;
  display: flex;
  flex-direction: column;
}

.friend-tiler {
  flex-shrink: 0;
  font-size: size(20);
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 1px;
}

.friend-list {
  flex: 1;
  overflow-y: auto;
  &::-webkit-scrollbar {
    appearance: none;
    width: 0;
  }

  .empty-text {
    text-align: center;
    color: #999;
    padding: size(24) 0;
    font-size: size(14);
  }

  .group-title {
    position: sticky;
    top: 0;
    z-index: 1;
    background: #fafafa;
    font-size: size(14);
    font-weight: 600;
    color: #888;
    letter-spacing: 1px;
    padding: size(8) 0 size(4);
  }

  .friend-item {
    display: flex;
    align-items: center;
    padding: size(16) 0;
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    cursor: pointer;

    span {
      font-size: size(16);
      font-weight: bold;
      letter-spacing: 1px;
      @include textEllipsis(1);
    }
  }
}

.new-friend {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-bottom: size(20);
  .new-friend-icon {
    width: size(60);
    height: size(60);
    border-radius: size(8);
    margin-right: size(13);
    background: $theme-color;
    color: #fff;
    @include flexCenter;

    .el-icon {
      font-size: size(30);
    }
  }

  span {
    font-size: size(20);
    font-weight: 500;
    letter-spacing: 1px;
    color: #1f1f1f;
  }

  .new-friend-badge {
    margin-left: size(12);
  }
}

.letter-bar {
  position: absolute;
  right: size(4);
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;

  .letter-item {
    width: size(18);
    line-height: size(18);
    text-align: center;
    font-size: size(12);
    color: #888;
    cursor: pointer;
    border-radius: 50%;

    &:hover,
    &.active {
      color: #fff;
      background: $theme-color;
    }
  }
}

.user-head {
  width: size(50);
  height: size(50);
  border-radius: size(8);
  margin-right: size(13);
}

.friend-main {
  flex: 1;
  @include flexCenter;
  &-content {
    width: size(480);
  }
  .user-box {
    display: flex;
    align-items: center;
    padding-bottom: size(24);
    border-bottom: 1px solid rgba(0, 0, 0, .06);
    .user-box-info {
      margin-left: size(8);
      display: flex;
      flex-direction: column;
      span:nth-child(1) {
        font-size: size(22);
        color: #1f1f1f;
        font-weight: 700;
        letter-spacing: 1px;
        @include textEllipsis(1);
      }
      span:nth-child(2) {
        font-size: size(16);
        color: #888;
        margin-top: size(8);
      }
    }
  }
}

.user-chat {
  display: flex;
  margin-top: size(13);
  &-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: size(24) 0;
    .el-icon {
      font-size: size(36);
      color: $theme-color;
    }
    span {
      font-size: size(16);
      color: #888;
      letter-spacing: 1px;
      margin-top: size(8);
    }
    &:hover {
      cursor: pointer;
      background: rgba(0, 0, 0, .02);
    }
  }
}
</style>
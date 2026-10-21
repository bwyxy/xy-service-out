<script setup lang="ts">
import { computed, onUnmounted, ref, toRefs, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { Search } from '@element-plus/icons-vue'
import useChatStore from '@/store/chat.ts'
import useUserStore from '@/store/user.ts'
import { MessageType } from '@/services/im'
import { searchMessages } from '@/api/chat.ts'
import type { SearchMessageItem, SearchMessageParams } from '@/api/chat.ts'
import { formatMessageTime } from '@/utils/date.ts'
import { escapeHtml, escapeRegExp } from '@/utils/html.ts'
import { resolveMemberName } from '@/utils/chat.ts'

const isShow = defineModel('modelValue', { type: Boolean })

const props = defineProps<{
  members?: GroupMember[]
}>()
const { members } = toRefs(props)

const emit = defineEmits<{
  (e: 'locate', item: SearchMessageItem): void
}>()

/** 查找类型：全部（文本关键字）/ 图片 / 文件 / 链接 / 日期 */
type SearchMode = 'all' | 'image' | 'file' | 'link' | 'date'

const MODE_OPTIONS: Array<{ value: SearchMode; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'image', label: '图片' },
  { value: 'file', label: '文件' },
  { value: 'link', label: '链接' },
  { value: 'date', label: '日期' },
]

/** 图片 / 文件消息类型（后端与前端常量并存，做兼容） */
const IMAGE_TYPES = [MessageType.IMAGE]
const FILE_TYPES = [MessageType.FILE, 4]
/** 日期查找展示当天所有内容消息 */
const DATE_TYPES = [MessageType.TEXT, MessageType.IMAGE, MessageType.FILE, 4]

const chatStore = useChatStore()
const { userInfo } = storeToRefs(useUserStore())

/** 防抖定时器 */
let searchTimer: ReturnType<typeof setTimeout> | null = null
/** 最近一次发起搜索的标识，用于丢弃过期结果 */
let pendingToken = ''

const mode = ref<SearchMode>('all')
const keyword = ref('')
const date = ref('')
const loading = ref(false)
const results = ref<SearchMessageItem[]>([])
/** 是否已执行过搜索（用于区分初始空态与「未找到」） */
const searched = ref(false)

/** 当前会话信息 */
const activeContact = computed(() => chatStore.activeContact)

/** 解析发送者展示名 */
const resolveSenderName = (fromUid: string): string => {
  if (String(fromUid) === String(userInfo.value?.uid)) {
    return '我'
  }

  const member = members?.value?.find((m) => String(m.uid) === String(fromUid))
  if (member) {
    return resolveMemberName(member, fromUid)
  }

  return activeContact.value?.name || fromUid
}

/** 关键字高亮后的 HTML */
const highlight = (content: string, kw: string): string => {
  const escaped = escapeHtml(content || '')
  const trimmed = kw.trim()
  if (!trimmed) return escaped

  const reg = new RegExp(escapeRegExp(escapeHtml(trimmed)), 'gi')
  return escaped.replace(reg, (m) => `<em class="search-hl">${m}</em>`)
}

/** 是否为图片消息 */
const isImageItem = (item: SearchMessageItem): boolean =>
  item.type === MessageType.IMAGE && !!item.content

/** 是否为文件消息 */
const isFileItem = (item: SearchMessageItem): boolean =>
  item.type === MessageType.FILE || item.type === 4

/** 文件展示名：取 URL 最后一段 */
const fileLabel = (content: string): string => {
  if (!content) return '文件'
  const path = content.split('?')[0].split('#')[0]
  const name = path.split('/').pop()
  return name || content
}

/** 格式化消息时间（搜索结果用数字日期） */
const formatTime = (timestamp: number): string =>
  formatMessageTime(timestamp, { numericDate: true })

/** 当天的起止毫秒时间戳 */
const dayRange = (value: string): { startTime: number; endTime: number } | null => {
  if (!value) return null
  const [y, m, d] = value.split('-').map(Number)
  if (!y || !m || !d) return null
  const start = new Date(y, m - 1, d, 0, 0, 0, 0).getTime()
  const end = new Date(y, m - 1, d, 23, 59, 59, 999).getTime()
  return { startTime: start, endTime: end }
}

/** 组装当前模式的搜索参数 */
const buildParams = (): SearchMessageParams | null => {
  const contact = activeContact.value
  if (!contact) return null

  const base: SearchMessageParams = {
    channelId: contact.uid,
    channelType: contact.channelType,
    limit: 50,
  }

  if (mode.value === 'all') {
    return { ...base, keyword: keyword.value.trim(), types: [MessageType.TEXT] }
  }

  if (mode.value === 'image') {
    return { ...base, types: IMAGE_TYPES }
  }

  if (mode.value === 'file') {
    return { ...base, types: FILE_TYPES }
  }

  if (mode.value === 'link') {
    return { ...base, types: [MessageType.TEXT], linkOnly: true }
  }

  // 日期查找
  const range = dayRange(date.value)
  if (!range) return null
  return { ...base, types: DATE_TYPES, ...range }
}

/** 执行搜索 */
const runSearch = async () => {
  const params = buildParams()
  if (!params) {
    results.value = []
    searched.value = false
    return
  }

  // 全部模式下必须输入关键字
  if (mode.value === 'all' && !params.keyword) {
    results.value = []
    searched.value = false
    return
  }

  const token = `${mode.value}|${params.keyword || ''}|${date.value}`
  pendingToken = token
  loading.value = true

  try {
    const res = await searchMessages(params)

    // 条件已变化，丢弃过期结果
    if (pendingToken !== token) return

    results.value = res.code === 200 ? res.data || [] : []
    searched.value = true
  } catch (error) {
    if (pendingToken === token) {
      results.value = []
      searched.value = true
    }
    console.error('[SearchMessage] 搜索失败:', error)
  } finally {
    if (pendingToken === token) {
      loading.value = false
    }
  }
}

/** 输入防抖（仅「全部」模式按关键字搜索） */
const handleInput = () => {
  if (searchTimer) clearTimeout(searchTimer)

  if (mode.value !== 'all') return

  if (!keyword.value.trim()) {
    results.value = []
    searched.value = false
    loading.value = false
    pendingToken = ''
    return
  }

  searchTimer = setTimeout(runSearch, 400)
}

/** 切换查找类型 */
const handleModeChange = (value: SearchMode) => {
  if (mode.value === value) return
  if (searchTimer) clearTimeout(searchTimer)

  mode.value = value
  results.value = []
  searched.value = false

  // 日期模式需先选择日期
  if (value === 'date') return

  runSearch()
}

/** 选择日期后立即查找 */
const handleDateChange = () => {
  runSearch()
}

/** 点击结果项：通知父组件定位并关闭弹窗 */
const handleSelect = (item: SearchMessageItem) => {
  emit('locate', item)
  isShow.value = false
}

/** 空态文案 */
const emptyTip = computed(() => {
  if (mode.value === 'date' && !date.value) return '请选择日期'
  if (mode.value === 'all') return '输入关键字查找聊天内容'
  return '未找到相关聊天内容'
})

/** 关闭时重置状态 */
watch(isShow, (val) => {
  if (!val) {
    if (searchTimer) clearTimeout(searchTimer)
    mode.value = 'all'
    keyword.value = ''
    date.value = ''
    results.value = []
    searched.value = false
    loading.value = false
    pendingToken = ''
  }
})

// 卸载时清理防抖定时器，避免组件销毁后回调仍触发搜索
onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})
</script>

<template>
  <el-dialog
      v-model="isShow"
      title="查找聊天内容"
      width="440px"
      append-to-body
      class="search-message-dialog"
  >
    <el-input
        v-model="keyword"
        placeholder="搜索聊天记录"
        :prefix-icon="Search"
        clearable
        @input="handleInput"
    />

    <!-- 查找类型 -->
    <div class="search-modes">
      <span
          v-for="opt in MODE_OPTIONS"
          :key="opt.value"
          :class="['search-mode', { 'search-mode_active': mode === opt.value }]"
          @click="handleModeChange(opt.value)"
      >
        {{ opt.label }}
      </span>
    </div>

    <!-- 按日期查找 -->
    <div v-if="mode === 'date'" class="search-date">
      <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          :clearable="false"
          style="width: 100%"
          @change="handleDateChange"
      />
    </div>

    <div class="search-result">
      <div v-if="loading" class="search-tip">正在搜索...</div>

      <template v-else-if="results.length > 0">
        <div
            v-for="(item, index) in results"
            :key="`${item.messageSeq}_${index}`"
            class="search-item"
            @click="handleSelect(item)"
        >
          <div class="search-item__head">
            <span class="search-item__sender">{{ resolveSenderName(item.fromUid) }}</span>
            <span class="search-item__time">{{ formatTime(item.timestamp) }}</span>
          </div>

          <!-- 图片：展示缩略图 -->
          <div v-if="isImageItem(item)" class="search-item__image">
            <el-image :src="item.content" fit="cover" :preview-src-list="[item.content]" />
          </div>

          <!-- 文件：展示文件名 -->
          <div v-else-if="isFileItem(item)" class="search-item__file">
            <span>[文件] {{ fileLabel(item.content) }}</span>
          </div>

          <!-- 文本 / 链接：展示高亮内容 -->
          <div
              v-else
              class="search-item__content"
              v-html="highlight(item.content, mode === 'all' ? keyword : '')"
          ></div>
        </div>
      </template>

      <div v-else-if="searched" class="search-tip">未找到相关聊天记录</div>
      <div v-else class="search-tip">{{ emptyTip }}</div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.search-modes {
  display: flex;
  flex-wrap: wrap;
  gap: size(8);
  margin-top: size(12);
}

.search-mode {
  padding: size(4) size(12);
  border-radius: size(14);
  background: #f2f3f5;
  color: #595959;
  font-size: size(12);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #e9ebee;
  }

  &_active {
    background: #07c160;
    color: #fff;
  }
}

.search-date {
  margin-top: size(10);
}

.search-result {
  margin-top: size(12);
  max-height: size(360);
  overflow-y: auto;
}

.search-tip {
  text-align: center;
  color: #8c8c8c;
  font-size: size(12);
  padding: size(30) 0;
}

.search-item {
  padding: size(10) size(8);
  border-radius: size(6);
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f6f8;
  }

  &__head {
    display: flex;
    align-items: center;
    margin-bottom: size(4);
  }

  &__sender {
    flex: 1;
    font-size: size(12);
    color: #1f1f1f;
    font-weight: bold;
    @include textEllipsis(1);
  }

  &__time {
    font-size: size(11);
    color: #bfbfbf;
    flex-shrink: 0;
    margin-left: size(8);
  }

  &__content {
    font-size: size(12);
    color: #595959;
    line-height: 1.5;
    word-break: break-all;
    @include textEllipsis(2);
  }

  &__image {
    :deep(.el-image) {
      width: size(80);
      height: size(80);
      border-radius: size(4);
      overflow: hidden;
    }
  }

  &__file {
    font-size: size(12);
    color: #2f54eb;
    word-break: break-all;
    @include textEllipsis(1);
  }
}

:deep(.search-hl) {
  font-style: normal;
  color: #f5222d;
}
</style>

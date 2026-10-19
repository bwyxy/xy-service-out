<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, toRefs } from 'vue'
import { storeToRefs } from 'pinia'
import { Picture, Pointer, ChatRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'
import { compressImage, isOversize } from '@/utils/image'
import useUserStore from '@/store/user.ts'
import { MENTION_ALL_UID } from '@/store/chat.ts'
import type { MentionInfo } from '@/store/chat.ts'

/** @成员选择器候选项 */
interface MentionCandidate {
  uid: string
  name: string
  avatar?: string
}

const props = defineProps<{
  disabled?: boolean,
  isSystemUser: boolean,
  /** 群成员，用于 @提醒 */
  members?: GroupMember[],
  /** 是否启用 @提醒（仅群聊） */
  enableMention?: boolean,
}>()

const emit = defineEmits<{
  /** 发送文本消息（mentions 为本次消息 @ 到的成员） */
  (e: 'send-text', text: string, mentions?: MentionInfo[]): void
  /** 发送图片消息 */
  (e: 'send-image', url: string): void
}>()
const { isSystemUser } = toRefs(props)
const { userInfo } = storeToRefs(useUserStore())

const inputRef = ref<HTMLDivElement | null>(null)
const sendingImage = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const emojiVisible = ref(false)
const emojiRef = ref<HTMLDivElement | null>(null)

/** 本次消息 @ 到的成员 */
const mentions = ref<MentionInfo[]>([])
/** @成员选择器是否可见 */
const mentionVisible = ref(false)
/** @成员搜索关键词（@ 之后输入的内容） */
const mentionQuery = ref('')
/** @成员选择器当前高亮项 */
const mentionActiveIndex = ref(0)

/** 是否允许 @提醒 */
const mentionEnabled = computed(() => !!props.enableMention && !props.disabled)

/** 候补成员：@所有人 + 按关键词过滤后的群成员（排除自己） */
const mentionCandidates = computed<MentionCandidate[]>(() => {
  if (!mentionEnabled.value) return []

  const keyword = mentionQuery.value.trim().toLowerCase()
  const selfUid = String(userInfo.value?.uid ?? '')

  const members: MentionCandidate[] = (props.members || [])
    .filter((m) => String(m.uid) !== selfUid)
    .filter((m) => {
      if (!keyword) return true
      const displayName = (m.groupNickname || m.nickname || '').toLowerCase()
      return displayName.includes(keyword) || String(m.uid).includes(keyword)
    })
    .map((m) => ({
      uid: String(m.uid),
      name: m.groupNickname || m.nickname,
      avatar: m.avatar,
    }))

  // 「@所有人」置顶，命中关键词「所有人 / all」时展示
  const allLabel = '所有人'
  const allMatched = !keyword || allLabel.includes(keyword) || 'all'.includes(keyword)
  const allOption: MentionCandidate[] = allMatched
    ? [{ uid: MENTION_ALL_UID, name: allLabel }]
    : []

  return [...allOption, ...members].slice(0, 20)
})

/** 是否有文本内容 */
const hasText = computed(() => {
  return !!inputRef.value && inputRef.value.innerText.trim().length > 0
})

/** 常用 Emoji 列表 */
const emojis = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃',
  '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙',
  '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔',
  '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
  '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮',
  '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓',
  '🧐', '😕', '😟', '🙁', '☹️', '😮', '😯', '😲', '😳', '🥺',
  '😦', '😧', '😨', '😰', '😥', '😢', '😭', '😱', '😖', '😣',
  '😞', '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬', '😈',
  '👍', '👎', '👏', '🙌', '🤝', '🤞', '✌️', '🤟', '🤘', '👌',
]

/** 保持光标在最后 */
const keepCursorAtEnd = () => {
  const el = inputRef.value
  if (!el) return

  const selection = window.getSelection()
  if (!selection) return

  const range = document.createRange()
  range.selectNodeContents(el)
  range.collapse(false)
  selection.removeAllRanges()
  selection.addRange(range)
}

/** 聚焦 */
const focus = () => {
  inputRef.value?.focus()
  keepCursorAtEnd()
}

/** 清空 */
const clear = () => {
  if (inputRef.value) {
    inputRef.value.innerHTML = ''
  }
  mentions.value = []
  mentionVisible.value = false
  mentionQuery.value = ''
}

/** 回填内容（用于撤回后重新编辑） */
const setContent = (text: string) => {
  if (!inputRef.value) return
  inputRef.value.innerText = text
  mentions.value = []
  focus()
}

/** 插入 Emoji */
const insertEmoji = (emoji: string) => {
  const el = inputRef.value
  if (!el || props.disabled) return

  el.focus()
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    el.innerText += emoji
    return
  }

  const range = selection.getRangeAt(0)
  range.deleteContents()
  const textNode = document.createTextNode(emoji)
  range.insertNode(textNode)
  range.setStartAfter(textNode)
  range.setEndAfter(textNode)
  selection.removeAllRanges()
  selection.addRange(range)
}

/** 取光标之前（不含光标）的纯文本 */
const getTextBeforeCaret = (): string => {
  const el = inputRef.value
  if (!el) return ''

  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) return ''

  const range = selection.getRangeAt(0)
  if (!el.contains(range.startContainer)) return ''

  const preRange = document.createRange()
  preRange.selectNodeContents(el)
  preRange.setEnd(range.startContainer, range.startOffset)

  return preRange.toString()
}

/** 输入内容变化：检测是否正在输入 @提醒 */
const handleInput = () => {
  if (!mentionEnabled.value) {
    mentionVisible.value = false
    return
  }

  // 匹配光标前以 @ 开头、且不含空白/@ 的片段，作为搜索关键词
  const match = /@([^\s@]*)$/.exec(getTextBeforeCaret())

  if (match) {
    mentionQuery.value = match[1]
    mentionActiveIndex.value = 0
    mentionVisible.value = true
  } else {
    mentionVisible.value = false
  }
}

/** 选中成员，把「@关键词」替换为「@昵称 」并记录提醒 */
const insertMention = (member: MentionCandidate) => {
  const el = inputRef.value
  if (!el) return

  el.focus()
  const selection = window.getSelection()

  if (selection && selection.rangeCount > 0) {
    const self = selection as Selection & {
      modify?: (alter: string, direction: string, granularity: string) => void
    }

    // 依次向前选中「@关键词」这几个字符后整体删除
    const chars = mentionQuery.value.length + 1
    for (let i = 0; i < chars; i++) {
      self.modify?.('extend', 'backward', 'character')
    }
    selection.deleteFromDocument()

    const textNode = document.createTextNode(`@${member.name} `)
    const range = selection.getRangeAt(0)
    range.insertNode(textNode)
    range.setStartAfter(textNode)
    range.setEndAfter(textNode)
    selection.removeAllRanges()
    selection.addRange(range)

    if (!mentions.value.some((m) => m.uid === member.uid)) {
      mentions.value.push({ uid: member.uid, name: member.name })
    }
  }

  mentionVisible.value = false
  mentionQuery.value = ''
}

/** 处理图片上传 */
const handleImageFile = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  if (isOversize(file)) {
    ElMessage.warning('图片大小不能超过 2MB，将尝试压缩')
  }

  sendingImage.value = true
  try {
    const compressed = await compressImage(file)

    if (isOversize(compressed)) {
      ElMessage.error('图片超过 2MB，请更换更小的图片')
      return
    }

    const res = await uploadImage(compressed)
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '图片上传失败')
      return
    }

    emit('send-image', res.data)
  } catch (error) {
    console.error('[RichInput] 图片上传失败:', error)
    ElMessage.error('图片上传失败')
  } finally {
    sendingImage.value = false
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

/** 点击上传按钮 */
const onUploadClick = () => {
  if (props.disabled || sendingImage.value) return
  fileInputRef.value?.click()
}

/** 选择文件 */
const onFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    handleImageFile(file)
  }
}

/** 粘贴图片 */
const onPaste = (e: ClipboardEvent) => {
  if (props.disabled || sendingImage.value) return

  if (isSystemUser.value) return

  const items = e.clipboardData?.items
  if (!items) return

  let hasImage = false
  for (const item of Array.from(items)) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        hasImage = true
        e.preventDefault()
        handleImageFile(file)
      }
    }
  }

  // 如果没有图片，则让 contenteditable 默认处理文本粘贴
  if (!hasImage) {
    e.preventDefault()
    const text = e.clipboardData?.getData('text/plain') || ''
    document.execCommand('insertText', false, text)
  }
}

/** 拖拽进入 */
const onDragOver = (e: DragEvent) => {
  e.preventDefault()
}

/** 放置图片 */
const onDrop = (e: DragEvent) => {
  e.preventDefault()
  if (props.disabled || sendingImage.value) return

  const files = e.dataTransfer?.files
  if (!files) return

  for (const file of Array.from(files)) {
    if (file.type.startsWith('image/')) {
      handleImageFile(file)
      break
    }
  }
}

/** 发送文本 */
const onSendText = () => {
  if (props.disabled || !inputRef.value) return

  const text = inputRef.value.innerText.trim()
  if (!text) return

  // 仅保留正文中仍存在的 @成员，避免用户手动删除后误提醒
  const validMentions = mentions.value.filter((m) => text.includes(`@${m.name}`))

  emit('send-text', text, validMentions.length ? validMentions : undefined)
  clear()
  focus()
}

/** 输入框键盘事件 */
const onKeydown = (e: KeyboardEvent) => {
  // @成员选择器打开时，方向键切换、回车选中、Esc 关闭
  if (mentionVisible.value) {
    const candidates = mentionCandidates.value

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (candidates.length) {
        mentionActiveIndex.value = (mentionActiveIndex.value + 1) % candidates.length
      }
      return
    }

    if (e.key === 'ArrowUp') {
      e.preventDefault()
      if (candidates.length) {
        mentionActiveIndex.value =
          (mentionActiveIndex.value - 1 + candidates.length) % candidates.length
      }
      return
    }

    if (e.key === 'Escape') {
      e.preventDefault()
      mentionVisible.value = false
      return
    }

    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      const member = candidates[mentionActiveIndex.value]
      if (member) {
        insertMention(member)
      } else {
        mentionVisible.value = false
      }
      return
    }
  }

  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    onSendText()
  }
}

/** 点击外部关闭表情面板 */
const onClickOutside = (e: MouseEvent) => {
  if (emojiRef.value && !emojiRef.value.contains(e.target as Node)) {
    emojiVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

defineExpose({
  focus,
  clear,
  setContent,
  insertEmoji,
})
</script>

<template>
  <div class="rich-input" @dragover="onDragOver" @drop="onDrop">
    <!-- 工具栏 -->
    <div class="rich-input-toolbar">
      <div ref="emojiRef" class="toolbar-item">
        <el-button
          link
          :disabled="disabled"
          title="表情"
          @click.stop="emojiVisible = !emojiVisible"
        >
          <el-icon><ChatRound /></el-icon>
        </el-button>

        <div v-if="emojiVisible" class="emoji-panel">
          <span
            v-for="emoji in emojis"
            :key="emoji"
            class="emoji-item"
            @click="insertEmoji(emoji)"
          >
            {{ emoji }}
          </span>
        </div>
      </div>

      <div v-if="!isSystemUser" class="toolbar-item">
        <el-button
          link
          :disabled="disabled || sendingImage"
          title="图片"
          @click="onUploadClick"
        >
          <el-icon><Picture /></el-icon>
        </el-button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="onFileChange"
        />
      </div>

      <div v-if="!isSystemUser" class="toolbar-item">
        <el-button
          link
          :disabled="disabled || sendingImage"
          title="粘贴图片可直接发送"
        >
          <el-icon><Pointer /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 可编辑区域 -->
    <div class="rich-input-editor-wrap">
      <div
        ref="inputRef"
        class="rich-input-editor"
        :class="{ disabled }"
        :contenteditable="!disabled"
        placeholder="请输入消息，回车发送，Shift+Enter 换行"
        @keydown="onKeydown"
        @input="handleInput"
        @paste="onPaste"
      />

      <!-- @成员选择器 -->
      <div v-if="mentionVisible" class="mention-panel">
        <div v-if="mentionCandidates.length === 0" class="mention-panel__empty">未找到成员</div>
        <div
          v-for="(member, index) in mentionCandidates"
          :key="member.uid"
          class="mention-panel__item"
          :class="{ active: index === mentionActiveIndex }"
          @mousedown.prevent="insertMention(member)"
          @mouseenter="mentionActiveIndex = index"
        >
          <img v-if="member.avatar" class="mention-panel__avatar" :src="member.avatar" alt="avatar" />
          <img v-else class="mention-panel__avatar" src="@/assets/images/logo.jpg" alt="avatar" />
          <span class="mention-panel__name">{{ member.name }}</span>
        </div>
      </div>
    </div>

    <!-- 发送按钮 -->
    <div class="rich-input-footer">
      <span class="hint">Shift + Enter 换行</span>
      <el-button
        type="primary"
        :disabled="(!hasText && !disabled) || disabled"
        :loading="sendingImage"
        @click="onSendText"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.rich-input {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: size(8);
  background: #fff;

  &-toolbar {
    display: flex;
    align-items: center;
    gap: size(8);
    padding: size(8) size(12);
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);

    .toolbar-item {
      position: relative;
    }

    .emoji-panel {
      position: absolute;
      left: 0;
      bottom: calc(100% + 8px);
      width: size(280);
      max-height: size(220);
      overflow-y: auto;
      background: #fff;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: size(8);
      padding: size(8);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      z-index: 100;
      display: flex;
      flex-wrap: wrap;
      gap: size(6);

      .emoji-item {
        font-size: size(20);
        cursor: pointer;
        padding: size(4);
        border-radius: size(4);
        transition: background 0.2s;

        &:hover {
          background: #f0f0f0;
        }
      }
    }
  }

  &-editor-wrap {
    position: relative;
    flex: 1;
    display: flex;
  }

  // @成员选择器
  .mention-panel {
    position: absolute;
    left: size(8);
    bottom: calc(100% + 6px);
    width: size(200);
    max-height: size(220);
    overflow-y: auto;
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: size(8);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    z-index: 100;
    padding: size(4) 0;

    &__empty {
      padding: size(10) size(12);
      font-size: size(13);
      color: #999;
      text-align: center;
    }

    &__item {
      display: flex;
      align-items: center;
      gap: size(8);
      padding: size(6) size(12);
      cursor: pointer;

      &.active,
      &:hover {
        background: #f2f2f2;
      }
    }

    &__avatar {
      width: size(24);
      height: size(24);
      border-radius: 50%;
      object-fit: cover;
      flex-shrink: 0;
    }

    &__name {
      font-size: size(14);
      color: #1f1f1f;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &-editor {
    flex: 1;
    min-height: size(100);
    max-height: size(200);
    overflow-y: auto;
    padding: size(12);
    font-size: size(15);
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
    outline: none;

    &:empty::before {
      content: attr(placeholder);
      color: #999;
      pointer-events: none;
    }

    &.disabled {
      background: #f5f5f5;
      color: #999;
      cursor: not-allowed;
    }

    :deep(.rich-input-preview-img) {
      max-width: 120px;
      max-height: 120px;
      border-radius: 8px;
      vertical-align: middle;
      margin: 4px 0;
    }
  }

  &-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: size(8) size(12);
    border-top: 1px solid rgba(0, 0, 0, 0.06);

    .hint {
      font-size: size(12);
      color: #999;
    }
  }
}
</style>

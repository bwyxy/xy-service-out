<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, toRefs } from 'vue'
import { Picture, Pointer, ChatRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'
import { compressImage, isOversize } from '@/utils/image'

const props = defineProps<{
  disabled?: boolean,
  isSystemUser: boolean,
}>()

const emit = defineEmits<{
  /** 发送文本消息 */
  (e: 'send-text', text: string): void
  /** 发送图片消息 */
  (e: 'send-image', url: string): void
}>()
const { isSystemUser } = toRefs(props)

const inputRef = ref<HTMLDivElement | null>(null)
const sendingImage = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const emojiVisible = ref(false)
const emojiRef = ref<HTMLDivElement | null>(null)

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

  emit('send-text', text)
  clear()
  focus()
}

/** 输入框键盘事件 */
const onKeydown = (e: KeyboardEvent) => {
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
    <div
      ref="inputRef"
      class="rich-input-editor"
      :class="{ disabled }"
      :contenteditable="!disabled"
      placeholder="请输入消息，回车发送，Shift+Enter 换行"
      @keydown="onKeydown"
      @paste="onPaste"
    />

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

<script setup lang="ts">

import {reactive, ref} from "vue";
import {ElMessage} from "element-plus";

const emojiPopoverRefs = ref<HTMLDivElement>();
const emojis = reactive([
  "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
  "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔",
  "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺",
  "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜",
  "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎",
  "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟",
  "🐬", "🐳", "🐋", "🦈", "🐊"
])
const form = reactive({
  nickname: "",
  email: "",
  content: ""
})

defineProps<{
  theme?: string,
  placeholder?: string
}>()

const emoji = defineModel('modelValue', {
  type: String,
  default: '🐶'
})
const emit = defineEmits(['submit'])

const handleEmoji = (value: string) => {
  emoji.value = value;
}

const handleSend = () => {
  if (!form.nickname) {
    ElMessage.error('该怎么称呼你呢! 可以委屈下填下昵称吗?')
    return
  }
  if (form.nickname.length > 6) {
    ElMessage.error("昵称不能超过六位哦!")
    return
  }
  if (!form.content) {
    ElMessage.error("内容不能为空哦!")
    return
  }
  emit('submit', form)
  form.content = ''
  form.nickname = ''
  form.email = ''
}

</script>

<template>
  <div :class="`word-pad${theme ? `_${theme}` : ''} innerContent`">
    <div class="word-pad-top">
      <el-popover
          trigger="click"
          placement="bottom"
          :teleported="false"
          :append-to="emojiPopoverRefs"
      >
        <template #reference>
          <div class="avatar-emoji">{{ emoji }}</div>
        </template>
        <div class="emoji-box">
          <div v-for="(item, index) in emojis" :key="index" class="emoji-item" @click="handleEmoji(item)">
            <span>{{ item }}</span>
          </div>
        </div>
      </el-popover>

      <input v-model="form.nickname" placeholder="昵称" class="word-input"  />

      <input v-model="form.email" placeholder="邮箱" class="word-input" />
    </div>

    <div class="word-pad-main">
      <textarea v-model="form.content" id="word-textarea" :placeholder="placeholder || '可以在这里留下您的痕迹'" />

      <div class="word-pad-option">
        <div class="send-btn" @click="handleSend">
          <span>发送</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.word-pad {
  width: 100%;
  --color: #fff;
  --padding: 24px;
  --textarea-height: 240px;
  --border-color: rgba(255, 255, 255, .2);
  --bg-color: rgba(255, 255, 255, .1);
  --p-bg-color: rgba(0, 0, 0, .8);
  --btn-border-color: rgba(255, 255, 255, .3);
  --btn-bg-color: rgba(255, 255, 255, .2);
}

.word-pad_light {
  width: 100%;
  --color: #1f1f1f;
  --padding: 12px;
  --textarea-height: 140px;
  --border-color: #b9aa97;
  --bg-color: #fff;
  --p-bg-color: #fff;
  --btn-border-color: #6b5a45;
  --btn-bg-color: #6b5a45;
}

.word-pad-top {
  display: flex;
  align-items: center;
  margin-bottom: var(--padding);
}

.word-pad-main {
  width: 100%;
  //min-height: 240px;
  box-sizing: border-box;
  background: var(--bg-color);
  backdrop-filter: blur(10px);
  border: 1.3px solid var(--border-color);
  border-radius: 13px;
}

#word-textarea {
  width: 100%;
  height: var(--textarea-height);
  border-radius: 13px;
  background: none;
  border: none;
  color: var(--color);
  font-size: 16px;
  letter-spacing: 1px;
  box-sizing: border-box;
  padding: var(--padding);
  &:focus {
    border: none;
    outline: none;
  }
}

.word-pad-option {
  display: flex;
  border-top: 1px solid rgba(255, 255, 255, .15);
  justify-content: flex-end;
  padding: var(--padding);
}

.send-btn {
  width: 100px;
  height: 50px;
  background: var(--btn-bg-color);
  border: 1.3px solid var(--btn-border-color);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover{
    color: #1668dc;
    background: rgba(22, 104, 220, 0.2);
    border: 1.3px solid #1668dc !important;
  }
}

.word-input {
  width: 240px;
  height: 50px;
  background: var(--bg-color);
  border: 1.3px solid var(--border-color);
  border-radius: 8px;
  color: var(--color);
  font-size: 18px;
  box-sizing: border-box;
  padding: 0 16px;
  margin-right: 24px;
  &:last-child {
    margin-right: 0;
  }
  &:focus {
    outline: none;
  }
}

:deep(.el-popper) {
  width: 480px !important;
  background: var(--p-bg-color);
  border: 1px solid var(--border-color);
  .el-popper__arrow:before {
    background: var(--p-bg-color);
    border: 1px solid var(--border-color);
  }
}

.avatar-emoji {
  width: 50px;
  height: 50px;
  font-size: 32px;
  background: var(--bg-color);
  backdrop-filter: blur(10px);
  border: 1.3px solid var(--border-color);
  border-radius: 50px;
  cursor: pointer;
  margin-right: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-box {
  width: 100%;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
}

.emoji-item {
  width: 45px;
  height: 45px;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
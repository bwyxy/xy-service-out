<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { Phone, SwitchButton, VideoCamera } from '@element-plus/icons-vue'
import useCallStore from '@/store/call.ts'

/**
 * 来电弹窗（incoming 状态显示）
 *
 * 对端头像 + 昵称 + 媒体类型（群聊显示群名与「邀请你加入…通话」），接听 / 拒绝两个操作。
 * 铃声由 store/call.ts 的 ringtoneEngine 负责。
 * */

const callStore = useCallStore()
const { status, peer, mediaType, statusText } = storeToRefs(callStore)

const visible = computed(() => status.value === 'incoming')
</script>

<template>
  <el-dialog
      :model-value="visible"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      width="360px"
      align-center
      class="incoming-call-dialog"
      append-to-body
  >
    <div class="incoming-body">
      <img v-if="peer?.avatar" class="caller-avatar" :src="peer.avatar" alt="avatar" />
      <div v-else class="caller-avatar caller-avatar--text">{{ (peer?.nickname || '?').slice(0, 1) }}</div>

      <span class="caller-name">{{ peer?.nickname }}</span>
      <span class="caller-hint">{{ statusText }}</span>

      <div class="incoming-actions">
        <div class="action-btn action-btn--reject" title="拒绝" @click="callStore.rejectCall()">
          <el-icon :size="26"><SwitchButton /></el-icon>
        </div>
        <div class="action-btn action-btn--accept" title="接听" @click="callStore.acceptCall()">
          <el-icon :size="26"><VideoCamera v-if="mediaType === 2" /><Phone v-else /></el-icon>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.incoming-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: size(10);
  padding: size(8) 0 size(16);
}

.caller-avatar {
  width: size(84);
  height: size(84);
  border-radius: 50%;
  object-fit: cover;

  &--text {
    display: flex;
    align-items: center;
    justify-content: center;
    background: $theme-color;
    color: #fff;
    font-size: size(34);
  }
}

.caller-name {
  font-size: size(18);
  color: #333;
  font-weight: 500;
  margin-top: size(6);
}

.caller-hint {
  font-size: size(14);
  color: #999;
}

.incoming-actions {
  display: flex;
  align-items: center;
  gap: size(56);
  margin-top: size(24);

  .action-btn {
    width: size(64);
    height: size(64);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;
    transition: transform .12s ease;

    &:active {
      transform: scale(.94);
    }

    &--accept {
      background: #67c23a;
    }

    &--reject {
      background: #f56c6c;
    }
  }
}
</style>

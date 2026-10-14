<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { Microphone, Mute, SwitchButton, VideoCamera, VideoCameraFilled } from '@element-plus/icons-vue'
import useCallStore from '@/store/call.ts'

/**
 * 通话全屏浮层
 *
 * 远端视频全屏 + 本地画面小窗（可点击交换位置），
 * 底部控制栏：静音 / 摄像头 / 挂断；顶部显示状态与网络质量。
 * 纯语音通话显示对端头像大图。
 * */

const callStore = useCallStore()
const { status, peer, mediaType, localStream, remoteStream, audioMuted, videoMuted,
        remoteVideoMuted, duration, networkLevel, degraded, statusText } = storeToRefs(callStore)

const remoteVideoRef = ref<HTMLVideoElement | null>(null)
const localVideoRef = ref<HTMLVideoElement | null>(null)
/** 画面互换（大屏显示本地，调试用；默认大屏远端） */
const swapped = ref(false)

// flush: 'post' 保证在 <video> 渲染后再赋值；immediate 覆盖「挂载前流已就绪」的情况。
// 注意：本地 video 的 v-if 依赖 localStream，若用默认的 pre 刷新，首次拿到流时元素尚未渲染，
// srcObject 会被跳过且不再重试，导致本地画面空白。
watch(remoteStream, (stream) => {
  if (remoteVideoRef.value) {
    remoteVideoRef.value.srcObject = stream || null
  }
}, { immediate: true, flush: 'post' })

watch(localStream, (stream) => {
  if (localVideoRef.value) {
    localVideoRef.value.srcObject = stream || null
  }
}, { immediate: true, flush: 'post' })

// 来电阶段（incoming）由 IncomingCallDialog 负责展示接听/拒绝；
// 浮层 z-index 为 3000，若此时也显示会全屏盖住弹窗，导致看不到接听按钮
const visible = () => status.value !== 'idle' && status.value !== 'incoming'

/** mm:ss */
function fmtDuration(sec: number): string {
  return `${String(Math.floor(sec / 60)).padStart(2, '0')}:${String(sec % 60).padStart(2, '0')}`
}

const networkText = () => {
  switch (networkLevel.value) {
    case 'poor': return '网络差'
    case 'medium': return '网络一般'
    default: return ''
  }
}

function swapLayout(): void {
  if (mediaType.value === 2) {
    swapped.value = !swapped.value
  }
}

function handleHangup(): void {
  callStore.hangupCall()
}

onUnmounted(() => {
  // 组件卸载不影响通话（浮层常驻 layout，仅状态驱动显隐）
})
</script>

<template>
  <div v-if="visible()" class="call-overlay">
    <!-- 远端主画面 -->
    <video
        v-if="mediaType === 2 && !remoteVideoMuted"
        ref="remoteVideoRef"
        class="remote-video"
        :class="{ pip: swapped }"
        autoplay
        playsinline
    />
    <!-- 纯语音 / 对端关视频：头像占位 -->
    <div v-else class="voice-stage">
      <img v-if="peer?.avatar" class="voice-avatar" :src="peer.avatar" alt="avatar" />
      <div v-else class="voice-avatar voice-avatar--text">{{ (peer?.nickname || '?').slice(0, 1) }}</div>
      <span class="voice-name">{{ peer?.nickname }}</span>
    </div>

    <!-- 本地画中画 -->
    <video
        v-if="mediaType === 2 && localStream"
        ref="localVideoRef"
        class="local-video"
        :class="{ pip: swapped }"
        autoplay
        playsinline
        muted
        @click="swapLayout"
    />

    <!-- 顶部状态 -->
    <div class="call-topbar">
      <span class="peer-name">{{ peer?.nickname }}</span>
      <span v-if="status === 'active'" class="call-duration">{{ fmtDuration(duration) }}</span>
      <span v-else class="call-state">{{ statusText }}</span>
      <span v-if="networkText()" class="network-badge" :class="networkLevel">{{ networkText() }}</span>
    </div>

    <!-- 弱网降级提示 + 恢复按钮 -->
    <div v-if="degraded && status === 'active'" class="degrade-tip">
      网络不佳，已关闭视频画面
      <el-button size="small" text type="primary" @click="callStore.restoreVideo()">恢复视频</el-button>
    </div>

    <!-- 底部控制栏 -->
    <div class="call-controls">
      <div class="ctrl-btn" :class="{ off: audioMuted }" :title="audioMuted ? '取消静音' : '静音'" @click="callStore.toggleAudio()">
        <el-icon :size="22"><Microphone v-if="!audioMuted" /><Mute v-else /></el-icon>
      </div>
      <div
          v-if="mediaType === 2"
          class="ctrl-btn"
          :class="{ off: videoMuted }"
          :title="videoMuted ? '开启摄像头' : '关闭摄像头'"
          @click="callStore.toggleVideo()"
      >
        <el-icon :size="22"><VideoCamera v-if="!videoMuted" /><VideoCameraFilled v-else /></el-icon>
      </div>
      <div class="ctrl-btn ctrl-btn--hangup" title="挂断" @click="handleHangup">
        <el-icon :size="24"><SwitchButton /></el-icon>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.call-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: #1c1c1e;
  display: flex;
  flex-direction: column;
}

.remote-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;

  &.pip {
    inset: auto size(16) size(96) auto;
    width: size(160);
    height: size(220);
    border-radius: size(10);
    object-fit: cover;
    box-shadow: 0 4px 16px rgba(0, 0, 0, .5);
    z-index: 2;
  }
}

.local-video {
  position: absolute;
  right: size(16);
  bottom: size(96);
  width: size(160);
  height: size(220);
  border-radius: size(10);
  object-fit: cover;
  box-shadow: 0 4px 16px rgba(0, 0, 0, .5);
  z-index: 2;
  cursor: pointer;

  &.pip {
    inset: 0;
    width: 100%;
    height: 100%;
    border-radius: 0;
    box-shadow: none;
    z-index: 1;
  }
}

.voice-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: size(16);

  .voice-avatar {
    width: size(120);
    height: size(120);
    border-radius: 50%;
    object-fit: cover;

    &--text {
      display: flex;
      align-items: center;
      justify-content: center;
      background: $theme-color;
      color: #fff;
      font-size: size(48);
    }
  }

  .voice-name {
    font-size: size(20);
    color: #fff;
  }
}

.call-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: size(20) size(24) 0;
  display: flex;
  align-items: center;
  gap: size(12);
  z-index: 3;

  .peer-name {
    font-size: size(18);
    color: #fff;
    font-weight: 500;
  }

  .call-duration,
  .call-state {
    font-size: size(14);
    color: rgba(255, 255, 255, .8);
  }

  .network-badge {
    margin-left: auto;
    font-size: size(12);
    padding: size(2) size(10);
    border-radius: size(10);

    &.medium {
      background: rgba(230, 162, 60, .25);
      color: #e6a23c;
    }
    &.poor {
      background: rgba(245, 108, 108, .25);
      color: #f56c6c;
    }
  }
}

.degrade-tip {
  position: absolute;
  top: size(64);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: size(8);
  padding: size(6) size(14);
  border-radius: size(8);
  background: rgba(230, 162, 60, .2);
  color: #e6a23c;
  font-size: size(13);
  z-index: 3;
}

.call-controls {
  position: absolute;
  bottom: size(36);
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: size(32);
  z-index: 3;

  .ctrl-btn {
    width: size(56);
    height: size(56);
    border-radius: 50%;
    background: rgba(255, 255, 255, .15);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color .15s ease;

    &:hover {
      background: rgba(255, 255, 255, .25);
    }

    &.off {
      background: rgba(255, 255, 255, .55);
      color: #1c1c1e;
    }

    &--hangup {
      background: #f56c6c;

      &:hover {
        background: #f78989;
      }
    }
  }
}
</style>

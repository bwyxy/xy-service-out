<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import {
  Microphone, Mute, SwitchButton, VideoCamera, VideoCameraFilled, Plus, CircleClose,
} from '@element-plus/icons-vue'
import useCallStore from '@/store/call.ts'
import useUserStore from '@/store/user.ts'
import GroupCallMemberPicker from '@/views/chat/components/ChatWindow/GroupCallMemberPicker.vue'

/**
 * 通话全屏浮层（LiveKit 多参与者）
 *
 * 布局：1 人在场（等待中）单人占位；1v1 双人沿用「远端全屏 + 本地小窗」；
 * 群聊 3-4 人 2×2、5-6 人 3×2、7-9 人 3×3 宫格。
 * 每格：有视频轨则渲染 <video>，否则头像 + 昵称兜底；含静音角标、说话高亮、受邀中角标。
 * 远端音频必须挂到真实 <audio> 元素（隐藏容器 audioSink）。
 * */

const callStore = useCallStore()
const userStore = useUserStore()
const {
  status, peer, mediaType, participants, presentCount, localParticipant, remoteParticipants,
  audioMuted, videoMuted, duration, connState, audioBlocked, statusText, isGroupCall,
  groupName, maxParticipants, canEndRoom, isRoomAdmin, networkLevel,
} = storeToRefs(callStore)

/** 来电阶段由 IncomingCallDialog 负责展示接听/拒绝；浮层 z-index 更高会盖住弹窗 */
const visible = computed(() => status.value !== 'idle' && status.value !== 'incoming')

/** 已接入的远端成员（不含受邀未接入） */
const joinedRemotes = computed(() => remoteParticipants.value.filter((p) => !p.invited))

type StageMode = 'solo' | 'pip' | 'grid'
const stageMode = computed<StageMode>(() => {
  if (presentCount.value <= 1) {
    return 'solo'
  }
  if (!isGroupCall.value && presentCount.value === 2) {
    return 'pip'
  }
  return 'grid'
})

const gridClass = computed(() => {
  const n = participants.value.length
  if (n <= 1) return 'g1'
  if (n <= 2) return 'g2'
  if (n <= 4) return 'g4'
  if (n <= 6) return 'g6'
  return 'g9'
})

/** 1v1 主画面成员 */
const pipRemote = computed(() => joinedRemotes.value[0] ?? null)
const pipRemoteUid = computed(() => pipRemote.value?.uid ?? '')
const localUid = computed(() => localParticipant.value?.uid ?? '')
const selfUid = computed(() => String(userStore.userInfo?.uid ?? ''))

const title = computed(() => {
  if (isGroupCall.value) {
    return `${groupName.value || '群通话'} · ${presentCount.value}人`
  }
  return peer.value?.nickname || '通话'
})

const reconnecting = computed(() => connState.value === 'reconnecting')

/** 是否有权管理通话（发起人或群主/管理员） */
const canManage = computed(() => isRoomAdmin.value)
/** 是否可以继续邀请（群聊 + 有管理权 + 未满员） */
const canInvite = computed(
  () => isGroupCall.value && canManage.value && participants.value.length < maxParticipants.value,
)

const networkText = computed(() => {
  switch (networkLevel.value) {
    case 'poor': return '网络差'
    case 'good': return '网络一般'
    default: return ''
  }
})

// ---- 媒体元素挂载 ----
const videoRefs = new Map<string, HTMLVideoElement>()
const audioRefs = new Map<string, HTMLAudioElement>()

function bindVideo(uid: string, el: unknown): void {
  if (el) {
    const video = el as HTMLVideoElement
    videoRefs.set(uid, video)
    callStore.attachVideo(uid, video)
  } else {
    videoRefs.delete(uid)
  }
}

function bindAudio(uid: string, el: unknown): void {
  if (el) {
    const audio = el as HTMLAudioElement
    audioRefs.set(uid, audio)
    callStore.attachAudio(uid, audio)
  } else {
    audioRefs.delete(uid)
  }
}

/**
 * 重新挂载所有媒体轨
 *
 * 轨道的到达（TrackSubscribed）会驱动 participants 变化，故在此统一重挂，
 * 避免「先渲染元素、后到达轨道」时画面空白。
 */
async function attachAll(): Promise<void> {
  await nextTick()
  participants.value.forEach((p) => {
    const video = videoRefs.get(p.uid)
    if (video) {
      callStore.attachVideo(p.uid, video)
    }
    const audio = audioRefs.get(p.uid)
    if (audio) {
      callStore.attachAudio(p.uid, audio)
    }
  })
}

watch([participants, mediaType, status], () => {
  void attachAll()
}, { flush: 'post' })

/** 首个用户手势后恢复远端音频（绕过浏览器自动播放限制） */
const gestureHandled = ref(false)
function handleGesture(): void {
  if (gestureHandled.value) {
    return
  }
  gestureHandled.value = true
  void callStore.resumeAudio()
}

// ---- 成员邀请 ----
const showPicker = ref(false)
const inviteExclude = computed<Array<string | number>>(() => [
  selfUid.value,
  ...participants.value.map((p) => p.uid),
])
const inviteMax = computed(() => Math.max(1, maxParticipants.value - participants.value.length))

function openPicker(): void {
  if (!canInvite.value) {
    return
  }
  showPicker.value = true
}

function handleInvite(uids: Array<string | number>): void {
  void callStore.inviteMembers(uids)
}

/** mm:ss */
function fmtDuration(sec: number): string {
  return `${String(Math.floor(sec / 60)).padStart(2, '0')}:${String(sec % 60).padStart(2, '0')}`
}
</script>

<template>
  <div v-if="visible" class="call-overlay" @click="handleGesture">
    <!-- 舞台 -->
    <!-- 等待中：单人占位 -->
    <div v-if="stageMode === 'solo'" class="voice-stage">
      <img v-if="peer?.avatar" class="voice-avatar" :src="peer.avatar" alt="avatar" />
      <div v-else class="voice-avatar voice-avatar--text">{{ (peer?.nickname || '通').slice(0, 1) }}</div>
      <span class="voice-name">{{ title }}</span>
      <span class="voice-hint">{{ statusText || (isGroupCall ? '等待成员加入…' : '') }}</span>
    </div>

    <!-- 1v1：远端全屏 + 本地小窗 -->
    <template v-else-if="stageMode === 'pip'">
      <video
          v-if="mediaType === 2 && pipRemote?.hasVideo"
          :ref="(el) => bindVideo(pipRemoteUid, el)"
          class="remote-video"
          autoplay
          playsinline
      />
      <div v-else class="voice-stage">
        <img v-if="pipRemote?.avatar" class="voice-avatar" :src="pipRemote.avatar" alt="avatar" />
        <div v-else class="voice-avatar voice-avatar--text">{{ (pipRemote?.name || '对方').slice(0, 1) }}</div>
        <span class="voice-name">{{ pipRemote?.name || peer?.nickname }}</span>
      </div>

      <video
          v-if="mediaType === 2 && localParticipant?.hasVideo"
          :ref="(el) => bindVideo(localUid, el)"
          class="local-video"
          autoplay
          playsinline
          muted
      />
    </template>

    <!-- 群聊宫格 -->
    <div v-else class="stage-grid" :class="gridClass">
      <div
          v-for="p in participants"
          :key="p.uid"
          class="grid-tile"
          :class="{ speaking: p.speaking, invited: p.invited }"
      >
        <video
            v-if="mediaType === 2 && p.hasVideo"
            :ref="(el) => bindVideo(p.uid, el)"
            class="tile-video"
            autoplay
            playsinline
            :muted="p.isLocal"
        />
        <div v-else class="tile-avatar-wrap">
          <img v-if="p.avatar" class="tile-avatar" :src="p.avatar" alt="avatar" />
          <div v-else class="tile-avatar tile-avatar--text">{{ (p.name || '?').slice(0, 1) }}</div>
        </div>

        <div class="tile-footer">
          <el-icon v-if="p.audioMuted" class="tile-mute"><Mute /></el-icon>
          <span class="tile-name">{{ p.name }}{{ p.isLocal ? '（我）' : '' }}</span>
        </div>

        <span v-if="p.invited" class="tile-badge">邀请中…</span>
        <span v-if="!p.isLocal && p.quality === 'poor' && !p.invited" class="tile-quality" />
      </div>
    </div>

    <!-- 远端音频出口（必须挂真实 DOM 才能出声） -->
    <div class="audio-sink">
      <audio
          v-for="p in joinedRemotes"
          :key="p.uid"
          :ref="(el) => bindAudio(p.uid, el)"
          autoplay
          playsinline
      />
    </div>

    <!-- 顶部状态 -->
    <div class="call-topbar">
      <span class="peer-name">{{ title }}</span>
      <span v-if="status === 'active'" class="call-duration">{{ fmtDuration(duration) }}</span>
      <span v-else class="call-state">{{ statusText }}</span>
      <span v-if="networkText" class="network-badge" :class="networkLevel">{{ networkText }}</span>
    </div>

    <!-- 重连提示 -->
    <div v-if="reconnecting" class="call-tip">
      网络不稳定，重连中…
    </div>

    <!-- 自动播放被拦截提示 -->
    <div v-else-if="audioBlocked" class="call-tip">
      浏览器已拦截声音播放
      <el-button size="small" text type="primary" @click.stop="callStore.resumeAudio()">点击开启声音</el-button>
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
      <div
          v-if="isGroupCall"
          class="ctrl-btn"
          :class="{ disabled: !canInvite }"
          :title="canInvite ? '邀请成员' : '不可邀请（已满员或无权限）'"
          @click="openPicker"
      >
        <el-icon :size="22"><Plus /></el-icon>
      </div>
      <div
          v-if="canEndRoom"
          class="ctrl-btn ctrl-btn--end"
          title="结束通话"
          @click="callStore.endRoom()"
      >
        <el-icon :size="22"><CircleClose /></el-icon>
      </div>
      <div class="ctrl-btn ctrl-btn--hangup" :title="isGroupCall ? '离开通话' : '挂断'" @click="callStore.leaveRoom()">
        <el-icon :size="24"><SwitchButton /></el-icon>
      </div>
    </div>

    <GroupCallMemberPicker
        v-model="showPicker"
        :group-id="callStore.channelId"
        :max="inviteMax"
        :exclude-uids="inviteExclude"
        @confirm="handleInvite"
    />
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
}

.voice-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: size(14);

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

  .voice-hint {
    font-size: size(14);
    color: rgba(255, 255, 255, .65);
  }
}

.stage-grid {
  position: absolute;
  inset: size(64) size(16) size(112);
  display: grid;
  gap: size(8);

  &.g1 { grid-template-columns: 1fr; grid-template-rows: 1fr; }
  &.g2 { grid-template-columns: repeat(2, 1fr); grid-template-rows: 1fr; }
  &.g4 { grid-template-columns: repeat(2, 1fr); grid-template-rows: repeat(2, 1fr); }
  &.g6 { grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(2, 1fr); }
  &.g9 { grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr); }
}

.grid-tile {
  position: relative;
  overflow: hidden;
  border-radius: size(8);
  background: #2a2a2d;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  transition: border-color .15s ease;

  &.speaking {
    border-color: #67c23a;
  }

  &.invited {
    opacity: .6;
  }

  .tile-video {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .tile-avatar-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tile-avatar {
    width: size(64);
    height: size(64);
    border-radius: 50%;
    object-fit: cover;

    &--text {
      display: flex;
      align-items: center;
      justify-content: center;
      background: $theme-color;
      color: #fff;
      font-size: size(26);
    }
  }

  .tile-footer {
    position: absolute;
    left: size(6);
    bottom: size(6);
    right: size(6);
    display: flex;
    align-items: center;
    gap: size(4);
    padding: size(2) size(6);
    border-radius: size(6);
    background: rgba(0, 0, 0, .45);
    color: #fff;
    font-size: size(12);

    .tile-mute {
      color: #f56c6c;
    }

    .tile-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .tile-badge {
    position: absolute;
    top: size(6);
    left: size(6);
    padding: size(1) size(6);
    border-radius: size(6);
    background: rgba(0, 0, 0, .5);
    color: #e6a23c;
    font-size: size(11);
  }

  .tile-quality {
    position: absolute;
    top: size(6);
    right: size(6);
    width: size(8);
    height: size(8);
    border-radius: 50%;
    background: #f56c6c;
  }
}

.audio-sink {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
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

.call-tip {
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
  gap: size(24);
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

    &.disabled {
      opacity: .4;
      cursor: not-allowed;
    }

    &--end {
      background: #e6a23c;

      &:hover {
        background: #ebb563;
      }
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

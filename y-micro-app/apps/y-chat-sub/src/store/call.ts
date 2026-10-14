import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  rtcCall, rtcAccept, rtcReject, rtcHangup, rtcCancel, getIceServers,
  type IceServer,
} from '@/api/rtc.ts'
import { rtcSignalingService, type RtcSignalData } from '@/services/rtc/signaling.ts'
import { RtcConnection, type NetworkLevel, type NetworkStats } from '@/services/rtc/webrtc.ts'
import { ringtoneEngine } from '@/services/rtc/ringtone.ts'
import { MessageType } from '@/services/im/constants'
import useUserStore from '@/store/user.ts'

/**
 * 通话状态机
 *
 * idle → outgoing（主叫发起，回铃中）
 * idle → incoming（被叫收到邀请，响铃中）
 * outgoing/incoming → connecting（双方都在，WebRTC 建连中）
 * connecting → active（媒体通道建立，计时中）
 * outgoing/incoming/connecting/active → ended（任一方结束/异常）→ 3s 后回 idle
 * */

export type CallStatus =
  | 'idle'
  | 'outgoing'
  | 'incoming'
  | 'connecting'
  | 'active'
  | 'ended'

/** 通话对端信息 */
export interface CallPeer {
  uid: string
  nickname: string
  avatar: string
}

/** 结束原因（用于 ended 状态的提示文案） */
export type CallEndReason =
  | 'completed'
  | 'canceled'
  | 'rejected'
  | 'busy'
  | 'timeout'
  | 'failed'
  | 'remote'
  | null

const END_REASON_TEXT: Record<Exclude<CallEndReason, null>, string> = {
  completed: '通话已结束',
  canceled: '已取消呼叫',
  rejected: '对方已拒绝',
  busy: '对方忙线',
  timeout: '无人接听',
  failed: '连接失败',
  remote: '对方已挂断',
}

/** 呼叫等待超时（秒），与后端 xy.rtc.call-timeout-seconds 对齐 */
const CALL_TIMEOUT_SECONDS = 90

export const useCallStore = defineStore('call', () => {
  // ---- 状态 ----
  const status = ref<CallStatus>('idle')
  const roomId = ref('')
  const peer = ref<CallPeer | null>(null)
  /** 媒体类型 1-语音 2-视频 */
  const mediaType = ref<1 | 2>(1)
  const localStream = ref<MediaStream | null>(null)
  const remoteStream = ref<MediaStream | null>(null)
  const audioMuted = ref(false)
  const videoMuted = ref(false)
  const remoteVideoMuted = ref(false)
  const duration = ref(0)
  const networkLevel = ref<NetworkLevel>('good')
  /** 弱网自动降级（视频转语音） */
  const degraded = ref(false)
  const endReason = ref<CallEndReason>(null)
  const statusText = computed(() => {
    switch (status.value) {
      case 'outgoing':
        return degraded.value ? '网络不佳，已转为语音' : '正在等待对方接受...'
      case 'incoming':
        return mediaType.value === 2 ? '邀请你视频通话' : '邀请你语音通话'
      case 'connecting':
        return '连接中...'
      case 'active':
        return ''
      case 'ended':
        return endReason.value ? END_REASON_TEXT[endReason.value] : '通话已结束'
      default:
        return ''
    }
  })

  // ---- 内部（非响应式） ----
  let connection: RtcConnection | null = null
  let iceServers: RTCIceServer[] = []
  /** 对端 candidate 早于 remoteDescription 到达时的缓存 */
  let pendingCandidates: RTCIceCandidateInit[] = []
  /** 通话计时器 */
  let durationTimer: ReturnType<typeof setInterval> | null = null
  /** 呼叫等待超时计时器 */
  let callTimeoutTimer: ReturnType<typeof setTimeout> | null = null
  /** ended → idle 复位计时器 */
  let resetTimer: ReturnType<typeof setTimeout> | null = null
  /** ended 提示保留时长 */
  let signalListener: ((fromUid: string, type: number, data: RtcSignalData) => void) | null = null

  /** 是否忙（任何进行中的通话状态） */
  const isBusy = computed(() => status.value !== 'idle' && status.value !== 'ended')

  // ---- 信令监听初始化（幂等） ----
  function ensureSignalListener(): void {
    rtcSignalingService.init()
    if (signalListener) {
      return
    }
    signalListener = (fromUid, type, data) => onSignal(fromUid, type, data)
    rtcSignalingService.onSignal(signalListener)
  }

  // ---- 生命周期 ----

  /**
   * 发起通话（主叫）
   * */
  async function startCall(peerInfo: CallPeer, type: 1 | 2): Promise<void> {
    if (isBusy.value) {
      ElMessage.warning('正处于通话中')
      return
    }
    ensureSignalListener()

    try {
      // 拉取 ICE 配置（失败用默认公共 STUN）
      try {
        const res = await getIceServers() as { data: IceServer[] }
        if (res.data?.length) {
          iceServers = res.data.map((s) => ({
            urls: s.urls,
            username: s.username,
            credential: s.credential,
          }))
        }
      } catch {
        iceServers = [{ urls: 'stun:stun.l.google.com:19302' }]
      }

      const res = await rtcCall(peerInfo.uid, type) as { data: { roomId: string; mediaType: number } }
      roomId.value = res.data.roomId
      peer.value = peerInfo
      mediaType.value = type
      status.value = 'outgoing'
      endReason.value = null
      duration.value = 0

      // 回铃音
      ringtoneEngine.start('outgoing')
      // 主叫等待超时兜底（后端定时任务也会处理）
      armCallTimeout()
    } catch (e) {
      console.error('[CallStore] 发起通话失败', e)
    }
  }

  /**
   * 接听（被叫）
   * */
  async function acceptCall(): Promise<void> {
    if (status.value !== 'incoming' || !roomId.value) {
      return
    }
    ringtoneEngine.stop()
    clearCallTimeout()

    try {
      await rtcAccept(roomId.value)
      status.value = 'connecting'

      // 被叫为 offer 方：采集媒体 → 创建连接 → 发送 offer
      // （localStream 由连接回调 onLocalStream 更新）
      connection = createConnection()
      await connection.initLocalMedia(mediaType.value)
      const offer = await connection.createOffer()
      await rtcSignalingService.send(peer.value!.uid, MessageType.RTC_OFFER, {
        roomId: roomId.value,
        sdp: offer.sdp,
      })
    } catch (e) {
      console.error('[CallStore] 接听失败', e)
      ElMessage.error('接听失败，请重试')
      endCallInternal('failed', false)
    }
  }

  /**
   * 拒绝（被叫）
   * */
  async function rejectCall(): Promise<void> {
    if (status.value !== 'incoming' || !roomId.value) {
      return
    }
    ringtoneEngine.stop()
    clearCallTimeout()
    try {
      await rtcReject(roomId.value)
    } catch (e) {
      console.warn('[CallStore] 拒绝接口失败', e)
    }
    endCallInternal('canceled', false)
  }

  /**
   * 挂断（通话中任一方）
   * */
  async function hangupCall(): Promise<void> {
    if (!roomId.value || status.value === 'idle' || status.value === 'ended') {
      return
    }
    const current = status.value
    ringtoneEngine.stop()
    clearCallTimeout()
    try {
      if (current === 'outgoing') {
        // 呼出中挂断 = 取消
        await rtcCancel(roomId.value)
      } else {
        await rtcHangup(roomId.value)
      }
    } catch (e) {
      console.warn('[CallStore] 挂断接口失败', e)
    }
    endCallInternal(current === 'active' ? 'completed' : 'canceled', false)
  }

  /**
   * 通话中开关麦克风
   * */
  function toggleAudio(): void {
    audioMuted.value = !audioMuted.value
    connection?.toggleAudio(audioMuted.value)
  }

  /**
   * 通话中开关摄像头
   * */
  function toggleVideo(): void {
    if (mediaType.value !== 2) {
      return
    }
    videoMuted.value = !videoMuted.value
    connection?.toggleVideo(videoMuted.value)
  }

  /**
   * 弱网恢复后手动重开视频
   * */
  function restoreVideo(): void {
    if (connection?.restoreVideo()) {
      degraded.value = false
    }
  }

  // ---- 信令处理 ----

  async function onSignal(fromUid: string, type: number, data: RtcSignalData): Promise<void> {
    // 忽略多端同步的"自己发出的信令"回显（悟空 IM 会把发送者的消息同步回其所有设备）
    let selfUid = ''
    try {
      selfUid = String(useUserStore().userInfo?.uid ?? '')
    } catch (e) {
      // 非组件上下文取 store 失败时跳过过滤，绝不能因此中断信令处理
      console.warn('[CallStore] 获取当前用户失败，跳过自回显过滤', e)
    }
    if (selfUid && fromUid === selfUid) {
      return
    }

    switch (type) {
      case MessageType.RTC_INVITE: {
        // 来电邀请
        if (isBusy.value) {
          // 忙线：后端 /rtc/call 已拦，双保险
          return
        }
        ensureSignalListener()
        roomId.value = data.roomId || ''
        mediaType.value = (data.mediaType === 2 ? 2 : 1)
        peer.value = {
          uid: String(data.caller?.uid ?? fromUid),
          nickname: data.caller?.nickname || fromUid,
          avatar: data.caller?.avatar || '',
        }
        status.value = 'incoming'
        endReason.value = null
        duration.value = 0
        ringtoneEngine.start('incoming')
        armCallTimeout()
        break
      }

      case MessageType.RTC_ACCEPT: {
        // 被叫已接听（主叫侧）：进入连接阶段，等待对方 offer
        if (status.value !== 'outgoing' || data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        status.value = 'connecting'
        // 主叫预取媒体（并行于等待 offer，localStream 由回调更新）
        try {
          connection = connection || createConnection()
          await connection.initLocalMedia(mediaType.value)
        } catch (e) {
          console.error('[CallStore] 主叫媒体采集失败', e)
          ElMessage.error('无法访问麦克风/摄像头')
          await hangupCall()
        }
        break
      }

      case MessageType.RTC_REJECT: {
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal('rejected', false)
        break
      }

      case MessageType.RTC_CANCEL: {
        // 主叫取消/超时
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal(status.value === 'incoming' ? 'canceled' : 'timeout', false)
        break
      }

      case MessageType.RTC_HANGUP: {
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal('remote', false)
        break
      }

      case MessageType.RTC_BUSY: {
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal('busy', false)
        break
      }

      case MessageType.RTC_OFFER: {
        if (!connection || data.roomId !== roomId.value) {
          return
        }
        try {
          const answer = await connection.handleOffer(data.sdp || '')
          flushPendingCandidates()
          await rtcSignalingService.send(fromUid, MessageType.RTC_ANSWER, {
            roomId: roomId.value,
            sdp: answer.sdp,
          })
        } catch (e) {
          console.error('[CallStore] 处理 offer 失败', e)
        }
        break
      }

      case MessageType.RTC_ANSWER: {
        if (!connection || data.roomId !== roomId.value) {
          return
        }
        try {
          await connection.handleAnswer(data.sdp || '')
          flushPendingCandidates()
        } catch (e) {
          console.error('[CallStore] 处理 answer 失败', e)
        }
        break
      }

      case MessageType.RTC_CANDIDATE: {
        if (!connection || data.roomId !== roomId.value || !data.candidate) {
          return
        }
        if (connection.hasRemoteDescription()) {
          connection.addCandidate(data.candidate).catch((e) => {
            console.warn('[CallStore] 添加 candidate 失败', e)
          })
        } else {
          pendingCandidates.push(data.candidate)
        }
        break
      }
    }
  }

  /** remoteDescription 就绪后回放缓存的 candidate */
  function flushPendingCandidates(): void {
    if (!connection) {
      return
    }
    const candidates = pendingCandidates
    pendingCandidates = []
    candidates.forEach((c) => {
      connection!.addCandidate(c).catch((e) => {
        console.warn('[CallStore] 回放 candidate 失败', e)
      })
    })
  }

  // ---- WebRTC 连接工厂 ----

  function createConnection(): RtcConnection {
    if (!iceServers.length) {
      iceServers = [{ urls: 'stun:stun.l.google.com:19302' }]
    }
    return new RtcConnection(iceServers, {
      onLocalStream: (stream) => {
        localStream.value = stream
      },
      onRemoteStream: (stream) => {
        remoteStream.value = stream
        if (status.value === 'connecting') {
          status.value = 'active'
          startDuration()
          connection?.startStatsMonitor()
        }
      },
      onIceCandidate: (candidate) => {
        if (peer.value && roomId.value) {
          rtcSignalingService
              .send(peer.value.uid, MessageType.RTC_CANDIDATE, {
                roomId: roomId.value,
                candidate,
              })
              .catch((e) => console.warn('[CallStore] 发送 candidate 失败', e))
        }
      },
      onRenegotiationOffer: (offer) => {
        // ICE 重启产生的重协商 offer：经信令通道发给对端
        if (peer.value && roomId.value) {
          rtcSignalingService
              .send(peer.value.uid, MessageType.RTC_OFFER, {
                roomId: roomId.value,
                sdp: offer.sdp,
              })
              .catch((e) => console.warn('[CallStore] 发送重协商 offer 失败', e))
        }
      },
      onConnected: () => {
        if (status.value === 'connecting') {
          status.value = 'active'
          startDuration()
          connection?.startStatsMonitor()
        }
      },
      onFailed: async () => {
        console.error('[CallStore] WebRTC 连接失败（ICE 重启后仍失败）')
        if (status.value === 'active' || status.value === 'connecting') {
          await hangupCall()
        }
      },
      onRemoteVideoMuted: (muted) => {
        remoteVideoMuted.value = muted
      },
      onStats: (stats: NetworkStats) => {
        networkLevel.value = stats.level
        // 弱网自动降级：视频通话关视频转纯语音
        if (stats.level === 'poor' && mediaType.value === 2 && !degraded.value
            && (status.value === 'active' || status.value === 'connecting')) {
          connection?.degradeVideo()
        }
      },
      onVideoDegraded: () => {
        degraded.value = true
        ElMessage.warning('当前网络不佳，已暂时关闭视频画面')
      },
    })
  }

  // ---- 辅助 ----

  function startDuration(): void {
    if (durationTimer) {
      return
    }
    duration.value = 0
    durationTimer = setInterval(() => {
      duration.value++
    }, 1000)
  }

  function armCallTimeout(): void {
    clearCallTimeout()
    callTimeoutTimer = setTimeout(() => {
      if (status.value === 'outgoing' || status.value === 'incoming') {
        if (status.value === 'outgoing') {
          hangupCall()
        } else {
          // 被叫侧超时：等待后端 cancel 信令；本地直接清理避免卡铃
          ringtoneEngine.stop()
          endCallInternal('timeout', false)
        }
      }
    }, CALL_TIMEOUT_SECONDS * 1000)
  }

  function clearCallTimeout(): void {
    if (callTimeoutTimer) {
      clearTimeout(callTimeoutTimer)
      callTimeoutTimer = null
    }
  }

  /**
   * 结束通话并清理（notifyRemote=false 时不重复通知对端）
   * */
  function endCallInternal(reason: Exclude<CallEndReason, null>, _notifyRemote: boolean): void {
    ringtoneEngine.stop()
    clearCallTimeout()
    if (durationTimer) {
      clearInterval(durationTimer)
      durationTimer = null
    }
    connection?.close()
    connection = null
    pendingCandidates = []
    // 本地流引用随连接释放，UI 停止渲染
    localStream.value = null
    remoteStream.value = null
    audioMuted.value = false
    videoMuted.value = false
    remoteVideoMuted.value = false
    degraded.value = false
    networkLevel.value = 'good'
    endReason.value = reason
    status.value = 'ended'

    if (resetTimer) {
      clearTimeout(resetTimer)
    }
    resetTimer = setTimeout(() => {
      // 仅当仍处于 ended（未被新呼叫覆盖）时复位
      if (status.value === 'ended') {
        status.value = 'idle'
        roomId.value = ''
        peer.value = null
        endReason.value = null
        duration.value = 0
      }
    }, 3000)
  }

  // 全局信令监听：store 被使用（layout 已挂载 CallOverlay/IncomingCallDialog）即注册，
  // 保证纯被叫端（从未调用 startCall）也能收到来电邀请弹窗
  ensureSignalListener()

  return {
    // 状态
    status,
    roomId,
    peer,
    mediaType,
    localStream,
    remoteStream,
    audioMuted,
    videoMuted,
    remoteVideoMuted,
    duration,
    networkLevel,
    degraded,
    endReason,
    statusText,
    isBusy,
    // 动作
    startCall,
    acceptCall,
    rejectCall,
    hangupCall,
    toggleAudio,
    toggleVideo,
    restoreVideo,
  }
})

export default useCallStore

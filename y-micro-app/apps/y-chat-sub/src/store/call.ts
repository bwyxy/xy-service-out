import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { DisconnectReason } from 'livekit-client'
import {
  rtcCall, rtcAccept, rtcReject, rtcHangup, rtcCancel, rtcInvite, rtcEnd,
  type CallSession, type RtcMember,
} from '@/api/rtc.ts'
import { rtcSignalingService, type RtcSignalData } from '@/services/rtc/signaling.ts'
import { LivekitSession, type LivekitConnState, type ParticipantState, type ParticipantQuality } from '@/services/rtc/livekit.ts'
import { ringtoneEngine } from '@/services/rtc/ringtone.ts'
import { MessageType, isCallControlType } from '@/services/im/constants'
import useUserStore from '@/store/user.ts'

/**
 * 通话状态机
 *
 * idle → outgoing（主叫发起：1v1 回铃中 / 群聊等待成员加入）
 * idle → incoming（被叫收到邀请，响铃中）
 * outgoing/incoming → connecting（正在接入 LiveKit 房间）
 * connecting → active（房间内在场人数 ≥ 2，计时中）
 * outgoing/incoming/connecting/active → ended（任一方结束/异常）→ 3s 后回 idle
 *
 * 媒体面全部由 LiveKit SFU 承载；呼叫控制（200-205）经悟空 IM 个人频道接收。
 * */

export type CallStatus =
  | 'idle'
  | 'outgoing'
  | 'incoming'
  | 'connecting'
  | 'active'
  | 'ended'

/** 通话对端信息（1v1 展示用；群聊用群名） */
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
  | 'hostEnded'
  | 'noOneJoined'
  | null

/** 发起通话目标 */
export type CallTarget =
  | { mode: 'p2p'; peer: CallPeer }
  | { mode: 'group'; channelId: string; groupName: string; inviteeUids: Array<string | number> }

const END_REASON_TEXT: Record<Exclude<CallEndReason, null>, string> = {
  completed: '通话已结束',
  canceled: '已取消呼叫',
  rejected: '对方已拒绝',
  busy: '对方忙线',
  timeout: '无人接听',
  failed: '连接失败',
  remote: '对方已挂断',
  hostEnded: '通话已被结束',
  noOneJoined: '无人加入通话',
}

/** 呼叫等待超时（秒），与后端 xy.rtc.call-timeout-seconds 对齐 */
const CALL_TIMEOUT_SECONDS = 90
/** ended 提示保留时长 */
const ENDED_RESET_MS = 3000
/** 计时触发下限：房间内在场人数 */
const ACTIVE_MIN_PRESENT = 2

/** 接口业务错误（请求层已提示，调用方无需重复弹窗） */
class RtcBusinessError extends Error {}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

/** 解包后端统一响应体；业务失败抛出 RtcBusinessError */
function unwrap<T>(res: unknown): T {
  const result = res as ApiResult<T>
  if (!result || result.code !== 200) {
    throw new RtcBusinessError(result?.msg || '请求失败')
  }
  return result.data
}

export const useCallStore = defineStore('call', () => {
  // ---- 状态 ----
  const status = ref<CallStatus>('idle')
  const roomId = ref('')
  /** 频道类型 1-单聊 2-群聊 */
  const channelType = ref<1 | 2>(1)
  /** 单聊：对端 uid；群聊：g_{groupId} */
  const channelId = ref('')
  const groupName = ref('')
  /** 主持人 uid（群聊=发起人） */
  const hostUid = ref('')
  const maxParticipants = ref(2)
  const peer = ref<CallPeer | null>(null)
  /** 媒体类型 1-语音 2-视频 */
  const mediaType = ref<1 | 2>(1)
  /** 媒体面参与者（含受邀未接入者，invited=true） */
  const participants = ref<ParticipantState[]>([])
  /** 后端视角的房间成员（用于补齐受邀未接入者的昵称/头像） */
  const roomMembers = ref<RtcMember[]>([])
  const audioMuted = ref(false)
  const videoMuted = ref(false)
  /** 浏览器自动播放策略阻断远端音频 */
  const audioBlocked = ref(false)
  const connState = ref<LivekitConnState>('connecting')
  const duration = ref(0)
  const endReason = ref<CallEndReason>(null)
  /** 观看者是否为该群管理员/群主（用于「结束通话」按钮显隐） */
  const viewerGroupAdmin = ref(false)

  const isGroupCall = computed(() => channelType.value === 2)
  /** 是否忙（任何进行中的通话状态） */
  const isBusy = computed(() => status.value !== 'idle' && status.value !== 'ended')
  /** 房间内在场人数（不含受邀未接入） */
  const presentCount = computed(() => participants.value.filter((p) => !p.invited).length)
  /** 本地成员 */
  const localParticipant = computed(() => participants.value.find((p) => p.isLocal) ?? null)
  /** 远端成员（含受邀未接入） */
  const remoteParticipants = computed(() => participants.value.filter((p) => !p.isLocal))
  /** 是否为主持人 */
  const isHost = computed(() => {
    const selfUid = String(useUserStore().userInfo?.uid ?? '')
    return !!selfUid && !!hostUid.value && selfUid === hostUid.value
  })
  /** 是否有管理权（主持人或群主/管理员） */
  const isRoomAdmin = computed(() => isHost.value || viewerGroupAdmin.value)
  /** 是否有权结束整场通话（群聊：主持人或群管理员） */
  const canEndRoom = computed(() => isGroupCall.value && isRoomAdmin.value)
  /** 本地网络质量 */
  const networkLevel = computed<ParticipantQuality>(() => localParticipant.value?.quality ?? 'unknown')

  const statusText = computed(() => {
    switch (status.value) {
      case 'outgoing':
        if (isGroupCall.value) {
          return presentCount.value < ACTIVE_MIN_PRESENT ? '等待成员加入…' : ''
        }
        return '正在等待对方接受...'
      case 'incoming':
        return isGroupCall.value
          ? `${groupName.value || peer.value?.nickname || ''} 邀请你加入${mediaType.value === 2 ? '视频' : '语音'}通话`
          : `邀请你${mediaType.value === 2 ? '视频' : '语音'}通话`
      case 'connecting':
        return connState.value === 'reconnecting' ? '网络不稳定，重连中…' : '连接中...'
      case 'active':
        return ''
      case 'ended':
        return endReason.value ? END_REASON_TEXT[endReason.value] : '通话已结束'
      default:
        return ''
    }
  })

  // ---- 内部（非响应式） ----
  let session: LivekitSession | null = null
  /** 是否已进入结束流程（防止重复清理 / 误判远端断开） */
  let ending = false
  let durationTimer: ReturnType<typeof setInterval> | null = null
  let callTimeoutTimer: ReturnType<typeof setTimeout> | null = null
  let resetTimer: ReturnType<typeof setTimeout> | null = null
  let signalListener: ((fromUid: string, type: number, data: RtcSignalData) => void) | null = null
  /** 本次会话的 LiveKit 接入信息（仅当前会话有效，不入响应式状态） */
  let pendingUrl = ''
  let pendingToken = ''

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
  async function startCall(target: CallTarget, type: 1 | 2): Promise<void> {
    if (isBusy.value) {
      ElMessage.warning('正处于通话中')
      return
    }
    ensureSignalListener()

    const group = target.mode === 'group'
    channelType.value = group ? 2 : 1
    channelId.value = group ? target.channelId : String(target.peer.uid)
    groupName.value = group ? target.groupName : ''
    peer.value = group ? null : target.peer
    mediaType.value = type
    endReason.value = null
    duration.value = 0
    roomMembers.value = []
    participants.value = []

    try {
      const payload = group
        ? { channelId: target.channelId, channelType: 2, inviteeUids: target.inviteeUids, mediaType: type }
        : { calleeUid: target.peer.uid, channelType: 1, mediaType: type }
      const data = unwrap<CallSession>(await rtcCall(payload))
      applySession(data)
      status.value = 'outgoing'
      if (!group) {
        // 群聊无回铃音，仅 1v1 播放
        ringtoneEngine.start('outgoing')
      }
      armCallTimeout()
      await joinRoom()
    } catch (e) {
      if (!(e instanceof RtcBusinessError)) {
        ElMessage.error('发起通话失败，请稍后重试')
      }
      resetToIdle()
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
    status.value = 'connecting'
    try {
      const data = unwrap<CallSession>(await rtcAccept(roomId.value))
      applySession(data)
      await joinRoom()
    } catch (e) {
      if (!(e instanceof RtcBusinessError)) {
        ElMessage.error('接听失败，请重试')
      }
      endCallInternal('failed')
    }
  }

  /**
   * 拒绝（被叫）
   * */
  async function rejectCall(): Promise<void> {
    if (status.value !== 'incoming' || !roomId.value) {
      return
    }
    const target = roomId.value
    endCallInternal('canceled')
    try {
      await rtcReject(target)
    } catch (e) {
      console.warn('[CallStore] 拒绝接口失败', e)
    }
  }

  /**
   * 离开通话：群聊仅本人离开，1v1 结束整房
   * */
  async function leaveRoom(): Promise<void> {
    if (!roomId.value || status.value === 'idle' || status.value === 'ended') {
      return
    }
    const target = roomId.value
    const current = status.value
    endCallInternal(current === 'active' ? 'completed' : 'canceled')
    try {
      if (current === 'outgoing') {
        await rtcCancel(target)
      } else {
        await rtcHangup(target)
      }
    } catch (e) {
      console.warn('[CallStore] 离开通话接口失败', e)
    }
  }

  /**
   * 异常收尾：媒体面建连失败或异常断开时补一次后端挂断
   * 不上报的话房间会停留在 CALLING/ACTIVE，阻塞双方后续通话，只能等后端超时兜底
   * */
  function notifyServerHangup(): void {
    const target = roomId.value
    if (!target) {
      return
    }
    rtcHangup(target).catch((e: unknown) => {
      console.warn('[CallStore] 异常挂断上报失败', e)
    })
  }

  /**
   * 结束整场通话（群聊主持人 / 群主、管理员）
   * */
  async function endRoom(): Promise<void> {
    if (!isGroupCall.value || !roomId.value) {
      return
    }
    const target = roomId.value
    endCallInternal('completed')
    try {
      await rtcEnd(target)
    } catch (e) {
      console.warn('[CallStore] 结束通话接口失败', e)
    }
  }

  /**
   * 群通话中追加邀请成员
   * */
  async function inviteMembers(uids: Array<string | number>): Promise<void> {
    if (!isGroupCall.value || !roomId.value || !uids.length) {
      return
    }
    try {
      const data = unwrap<{ roomId: string; maxParticipants: number; participants: RtcMember[] }>(
        await rtcInvite(roomId.value, uids),
      )
      roomMembers.value = data.participants || []
      refreshParticipants(session?.snapshot() ?? [])
      ElMessage.success('邀请已发送')
    } catch (e) {
      if (!(e instanceof RtcBusinessError)) {
        ElMessage.error('邀请失败，请稍后重试')
      }
    }
  }

  /**
   * 通话中开关麦克风
   * */
  async function toggleAudio(): Promise<void> {
    const next = !audioMuted.value
    audioMuted.value = next
    await session?.setMicEnabled(!next)
  }

  /**
   * 通话中开关摄像头
   * */
  async function toggleVideo(): Promise<void> {
    if (mediaType.value !== 2) {
      return
    }
    const next = !videoMuted.value
    videoMuted.value = next
    await session?.setCamEnabled(!next)
  }

  /**
   * 用户手势后恢复远端音频播放
   * */
  async function resumeAudio(): Promise<void> {
    await session?.resumeAudio()
    audioBlocked.value = false
  }

  /** 把某成员视频轨挂到目标 <video> */
  function attachVideo(uid: string, el: HTMLVideoElement | null): void {
    session?.attachVideo(uid, el)
  }

  /** 把某成员音频轨挂到目标 <audio>（远端音频必须挂真实 DOM） */
  function attachAudio(uid: string, el: HTMLAudioElement | null): void {
    session?.attachAudio(uid, el)
  }

  /** 更新观看者的群管理员身份（驱动「结束通话」按钮显隐） */
  function setViewerGroupAdmin(value: boolean): void {
    viewerGroupAdmin.value = value
  }

  // ---- 房间接入 ----

  /** 记录后端返回的房间会话信息 */
  function applySession(data: CallSession): void {
    roomId.value = data.roomId
    mediaType.value = data.mediaType === 2 ? 2 : 1
    channelType.value = data.channelType === 2 ? 2 : 1
    channelId.value = data.channelId || channelId.value
    maxParticipants.value = data.maxParticipants || (data.channelType === 2 ? 9 : 2)
    roomMembers.value = data.participants || []
    pendingUrl = data.livekitUrl
    pendingToken = data.token
    if (data.channelType === 2 && !hostUid.value) {
      hostUid.value = String(useUserStore().userInfo?.uid ?? '')
    }
  }

  /** 接入 LiveKit 房间并发布本地媒体 */
  async function joinRoom(): Promise<void> {
    ending = false
    connState.value = 'connecting'
    session = new LivekitSession(pendingUrl, pendingToken, {
      onParticipantsChanged: (list) => refreshParticipants(list),
      onConnectionState: (state) => {
        connState.value = state
      },
      onDisconnected: (reason) => handleDisconnected(reason),
      onAudioBlocked: () => {
        audioBlocked.value = true
      },
      onError: (err) => {
        console.error('[CallStore] LiveKit 错误', err)
      },
    })
    try {
      await session.connect({ audio: true, video: mediaType.value === 2 })
    } catch (e) {
      console.error('[CallStore] 加入通话失败', e)
      ElMessage.error('无法加入通话，请检查麦克风/摄像头权限')
      notifyServerHangup()
      endCallInternal('failed')
    }
  }

  // ---- 参与者状态聚合 ----

  /**
   * 合并媒体面在线成员与后端受邀成员
   * */
  function refreshParticipants(liveList: ParticipantState[]): void {
    const present = new Set(liveList.map((p) => p.uid))
    const merged: ParticipantState[] = [...liveList]

    // 仅群聊展示「受邀未接入」占位（1v1 用对端头像占位表达等待）
    if (isGroupCall.value) {
      roomMembers.value.forEach((m) => {
        const uid = String(m.uid)
        if (present.has(uid) || m.status !== 0) {
          return
        }
        merged.push({
          uid,
          name: m.groupNickname || m.nickname || uid,
          avatar: m.avatar || '',
          isLocal: false,
          audioMuted: true,
          videoMuted: true,
          hasVideo: false,
          speaking: false,
          quality: 'unknown',
          invited: true,
        })
      })
    }
    participants.value = merged
    syncSelfMuteState(liveList)

    // 在场人数达标即进入通话中（计时启动）
    if (liveList.length >= ACTIVE_MIN_PRESENT && (status.value === 'outgoing' || status.value === 'connecting')) {
      ringtoneEngine.stop()
      clearCallTimeout()
      status.value = 'active'
      startDuration()
    }
    // 通话中掉到 1 人：1v1 视为对端已离开（远端 204 信令会兜底清理）
    if (liveList.length < ACTIVE_MIN_PRESENT && status.value === 'active' && !isGroupCall.value) {
      stopDuration()
    }
  }

  /** 用媒体面真实状态回写本地静音开关（避免开关与实际发布状态不一致） */
  function syncSelfMuteState(liveList: ParticipantState[]): void {
    const self = liveList.find((p) => p.isLocal)
    if (!self) {
      return
    }
    audioMuted.value = self.audioMuted
    if (mediaType.value === 2) {
      videoMuted.value = self.videoMuted
    }
  }

  /** LiveKit 侧断开连接 */
  function handleDisconnected(reason: DisconnectReason): void {
    if (ending) {
      return
    }
    if (reason === DisconnectReason.DUPLICATE_IDENTITY) {
      ElMessage.warning('该账号已在其他设备加入通话')
      endCallInternal('failed')
      return
    }
    if (reason === DisconnectReason.ROOM_DELETED || reason === DisconnectReason.PARTICIPANT_REMOVED) {
      endCallInternal(isGroupCall.value ? 'hostEnded' : 'remote')
      return
    }
    // 断网、媒体建连失败、连接超时等异常断开：后端房间不会自行结束，必须补上报
    notifyServerHangup()
    endCallInternal('failed')
  }

  // ---- 信令处理 ----

  function onSignal(fromUid: string, type: number, data: RtcSignalData): void {
    // 只处理呼叫控制信令；媒体面遗留信令（206-208）一律忽略
    if (!isCallControlType(type)) {
      return
    }
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
        // 来电邀请（1v1 或被拉入群通话）
        if (isBusy.value) {
          return
        }
        ensureSignalListener()
        const invitedGroup = data.channelType === 2
        roomId.value = data.roomId || ''
        mediaType.value = data.mediaType === 2 ? 2 : 1
        channelType.value = invitedGroup ? 2 : 1
        channelId.value = data.channelId || fromUid
        groupName.value = data.groupName || ''
        hostUid.value = String(data.hostUid ?? fromUid)
        maxParticipants.value = data.maxParticipants || (invitedGroup ? 9 : 2)
        peer.value = {
          uid: String(data.caller?.uid ?? fromUid),
          nickname: invitedGroup
            ? (data.groupName || data.caller?.nickname || fromUid)
            : (data.caller?.nickname || fromUid),
          avatar: data.caller?.avatar || '',
        }
        roomMembers.value = []
        participants.value = []
        endReason.value = null
        duration.value = 0
        status.value = 'incoming'
        ringtoneEngine.start('incoming')
        armCallTimeout()
        break
      }

      case MessageType.RTC_ACCEPT: {
        // 1v1：被叫已接听，停止回铃（计时改由「在场人数≥2」驱动）
        if (status.value !== 'outgoing' || data.roomId !== roomId.value) {
          return
        }
        if (!isGroupCall.value) {
          ringtoneEngine.stop()
          clearCallTimeout()
        }
        break
      }

      case MessageType.RTC_REJECT: {
        if (data.roomId !== roomId.value) {
          return
        }
        if (!isGroupCall.value) {
          ringtoneEngine.stop()
          clearCallTimeout()
          endCallInternal('rejected')
          return
        }
        // 群聊：某成员拒绝邀请，仅更新其状态
        markMemberStatus(fromUid, 3)
        ElMessage.info(`${memberName(fromUid)} 已拒绝邀请`)
        break
      }

      case MessageType.RTC_CANCEL: {
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal('canceled')
        break
      }

      case MessageType.RTC_HANGUP: {
        if (data.roomId !== roomId.value) {
          return
        }
        if (!isGroupCall.value) {
          ringtoneEngine.stop()
          clearCallTimeout()
          endCallInternal('remote')
          return
        }
        // 群聊：主持人离开/结束整场 -> 本地一并结束；否则仅该成员离开
        if (fromUid === hostUid.value) {
          endCallInternal('hostEnded')
          return
        }
        const name = memberName(fromUid)
        markMemberStatus(fromUid, 2)
        participants.value = participants.value.filter((p) => !(p.uid === fromUid && p.invited))
        ElMessage.info(`${name} 已离开通话`)
        break
      }

      case MessageType.RTC_BUSY: {
        if (data.roomId !== roomId.value) {
          return
        }
        ringtoneEngine.stop()
        clearCallTimeout()
        endCallInternal('busy')
        break
      }
    }
  }

  /** 覆盖某个后端成员的状态（触发受邀占位消失） */
  function markMemberStatus(uid: string, memberStatus: number): void {
    roomMembers.value = roomMembers.value.map((m) =>
      String(m.uid) === uid ? { ...m, status: memberStatus } : m,
    )
  }

  function memberName(uid: string): string {
    const member = roomMembers.value.find((m) => String(m.uid) === uid)
    const live = participants.value.find((p) => p.uid === uid)
    return live?.name || member?.groupNickname || member?.nickname || uid
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

  function stopDuration(): void {
    if (durationTimer) {
      clearInterval(durationTimer)
      durationTimer = null
    }
  }

  function armCallTimeout(): void {
    clearCallTimeout()
    callTimeoutTimer = setTimeout(() => {
      if (status.value === 'outgoing') {
        // 呼叫中无人接听：主叫取消（群聊由后端超时任务兜底）
        void leaveRoom()
      } else if (status.value === 'incoming') {
        ringtoneEngine.stop()
        endCallInternal('timeout')
      }
    }, CALL_TIMEOUT_SECONDS * 1000)
  }

  function clearCallTimeout(): void {
    if (callTimeoutTimer) {
      clearTimeout(callTimeoutTimer)
      callTimeoutTimer = null
    }
  }

  function scheduleReset(): void {
    if (resetTimer) {
      clearTimeout(resetTimer)
    }
    resetTimer = setTimeout(() => {
      if (status.value === 'ended') {
        resetToIdle()
      }
    }, ENDED_RESET_MS)
  }

  /** 复位到 idle（保留结束原因供 UI 展示，故仅在 3s 后调用） */
  function resetToIdle(): void {
    status.value = 'idle'
    roomId.value = ''
    channelId.value = ''
    groupName.value = ''
    hostUid.value = ''
    maxParticipants.value = 2
    peer.value = null
    roomMembers.value = []
    participants.value = []
    audioMuted.value = false
    videoMuted.value = false
    audioBlocked.value = false
    connState.value = 'connecting'
    endReason.value = null
    duration.value = 0
    viewerGroupAdmin.value = false
  }

  /**
   * 结束通话并清理
   * */
  function endCallInternal(reason: Exclude<CallEndReason, null>): void {
    if (ending) {
      return
    }
    ending = true
    ringtoneEngine.stop()
    clearCallTimeout()
    stopDuration()
    const current = session
    session = null
    current?.disconnect()
    participants.value = []
    audioMuted.value = false
    videoMuted.value = false
    audioBlocked.value = false
    endReason.value = reason
    status.value = 'ended'
    scheduleReset()
  }

  // 全局信令监听：store 被使用（layout 已挂载 CallOverlay/IncomingCallDialog）即注册，
  // 保证纯被叫端（从未调用 startCall）也能收到来电邀请弹窗
  ensureSignalListener()

  return {
    // 状态
    status,
    roomId,
    channelType,
    channelId,
    groupName,
    hostUid,
    maxParticipants,
    peer,
    mediaType,
    participants,
    roomMembers,
    audioMuted,
    videoMuted,
    audioBlocked,
    connState,
    duration,
    endReason,
    // 计算属性
    isGroupCall,
    isBusy,
    presentCount,
    localParticipant,
    remoteParticipants,
    isHost,
    isRoomAdmin,
    canEndRoom,
    viewerGroupAdmin,
    networkLevel,
    statusText,
    // 动作
    startCall,
    acceptCall,
    rejectCall,
    leaveRoom,
    endRoom,
    inviteMembers,
    toggleAudio,
    toggleVideo,
    resumeAudio,
    attachVideo,
    attachAudio,
    setViewerGroupAdmin,
  }
})

export default useCallStore

/**
 * WebRTC 连接封装
 *
 * 负责：媒体采集、PeerConnection 生命周期、offer/answer/candidate 交换、
 * 网络质量统计（getStats）、弱网降级与恢复、ICE 重启。
 *
 * 信令收发由 store/call.ts 通过 services/rtc/signaling.ts 驱动，
 * 本模块不直接感知信令通道。
 * */

export type NetworkLevel = 'good' | 'medium' | 'poor'

/** 网络统计数据 */
export interface NetworkStats {
  /** 往返时延（毫秒） */
  rtt: number
  /** 丢包率（0-1） */
  lossRate: number
  /** 上行码率（kbps） */
  bitrate: number
  level: NetworkLevel
}

export interface RtcConnectionCallbacks {
  /** 本地媒体流就绪 */
  onLocalStream: (stream: MediaStream) => void
  /** 远端媒体流就绪/更新 */
  onRemoteStream: (stream: MediaStream) => void
  /** 本地 ICE candidate 产出（需发送给对端） */
  onIceCandidate: (candidate: RTCIceCandidateInit) => void
  /** 连接建立成功 */
  onConnected: () => void
  /** 连接断开/失败（底层重试由本模块处理，最终失败才回调） */
  onFailed: () => void
  /** ICE 重启产生的重协商 offer（需经信令发送给对端） */
  onRenegotiationOffer: (offer: RTCSessionDescriptionInit) => void
  /** 远端视频暂停（对端弱网降级 replaceTrack(null)） */
  onRemoteVideoMuted: (muted: boolean) => void
  /** 网络质量统计（每 5s 回调一次） */
  onStats: (stats: NetworkStats) => void
  /** 弱网自动降级（仅视频通话） */
  onVideoDegraded: () => void
}

/** 弱网判定阈值 */
const POOR_LOSS = 0.15
const POOR_RTT = 500
const MEDIUM_LOSS = 0.05
const MEDIUM_RTT = 200

/** 连接中断（disconnected）宽限期：等待自愈，超时仍未恢复视为连接丢失 */
const DISCONNECT_GRACE_MS = 12000
/** ICE 重启后等待重连的宽限期 */
const RESTART_GRACE_MS = 8000

export class RtcConnection {
  private pc: RTCPeerConnection
  private localStream: MediaStream | null = null
  private callbacks: RtcConnectionCallbacks
  /** ICE 重启是否已尝试过一次 */
  private restarted = false
  /** 是否为 offer 发起方（负责重协商） */
  private isOfferer = false
  private statsTimer: ReturnType<typeof setInterval> | null = null
  private lastBytesSent = 0
  private lastStatsTime = 0
  /** 弱网降级标记 */
  private degraded = false
  private savedVideoTrack: MediaStreamTrack | null = null
  private remoteVideoMuteBound = false
  /** disconnected 宽限定时器 */
  private disconnectTimer: ReturnType<typeof setTimeout> | null = null
  /** ICE 重启后等待重连的定时器 */
  private restartTimer: ReturnType<typeof setTimeout> | null = null
  /** 是否已回调过 onFailed，避免重复通知上层 */
  private failedNotified = false

  constructor(iceServers: RTCIceServer[], callbacks: RtcConnectionCallbacks) {
    this.callbacks = callbacks
    this.pc = new RTCPeerConnection({
      iceServers,
    })

    this.pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.callbacks.onIceCandidate(event.candidate.toJSON())
      }
    }

    this.pc.ontrack = (event) => {
      const [stream] = event.streams
      if (stream) {
        this.callbacks.onRemoteStream(stream)
      }
      // 监听远端视频轨道 mute/unmute（对端 replaceTrack(null) 降级时触发）
      const videoTrack = event.track
      if (videoTrack.kind === 'video' && !this.remoteVideoMuteBound) {
        this.remoteVideoMuteBound = true
        videoTrack.onmute = () => this.callbacks.onRemoteVideoMuted(true)
        videoTrack.onunmute = () => this.callbacks.onRemoteVideoMuted(false)
      }
    }

    this.pc.onconnectionstatechange = () => {
      const state = this.pc.connectionState
      if (state === 'connected') {
        this.onLinkUp()
      } else if (state === 'disconnected') {
        // 对端可能异常退出（关窗口/进程被杀）或短暂断网，先给宽限期等待自愈
        this.armDisconnectTimeout()
      } else if (state === 'failed') {
        this.handleConnectionLoss()
      }
    }

    // iceConnectionState 兼容性更好（部分 WebView 不派发 connectionState）
    this.pc.oniceconnectionstatechange = () => {
      const state = this.pc.iceConnectionState
      if (state === 'connected' || state === 'completed') {
        this.onLinkUp()
      } else if (state === 'disconnected') {
        this.armDisconnectTimeout()
      } else if (state === 'failed') {
        this.handleConnectionLoss()
      }
    }
  }

  /**
   * 采集本地媒体（音频必选，视频按媒体类型）
   * */
  async initLocalMedia(mediaType: number): Promise<MediaStream> {
    if (this.localStream) {
      return this.localStream
    }
    const constraints: MediaStreamConstraints = {
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
      video: mediaType === 2
        ? { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: 'user' }
        : false,
    }
    this.localStream = await navigator.mediaDevices.getUserMedia(constraints)
    this.localStream.getTracks().forEach((track) => {
      this.pc.addTrack(track, this.localStream!)
    })
    this.callbacks.onLocalStream(this.localStream)
    return this.localStream
  }

  /**
   * 创建 offer（被叫接听后作为 offer 方发起）
   * */
  async createOffer(): Promise<RTCSessionDescriptionInit> {
    this.isOfferer = true
    const offer = await this.pc.createOffer()
    await this.pc.setLocalDescription(offer)
    return offer
  }

  /**
   * 处理远端 offer：设置 remote description 并生成 answer
   * */
  async handleOffer(offerSdp: string): Promise<RTCSessionDescriptionInit> {
    await this.pc.setRemoteDescription({ type: 'offer', sdp: offerSdp })
    const answer = await this.pc.createAnswer()
    await this.pc.setLocalDescription(answer)
    return answer
  }

  /**
   * 处理远端 answer
   * */
  async handleAnswer(answerSdp: string): Promise<void> {
    await this.pc.setRemoteDescription({ type: 'answer', sdp: answerSdp })
  }

  /**
   * 添加远端 ICE candidate
   * */
  async addCandidate(candidate: RTCIceCandidateInit): Promise<void> {
    await this.pc.addIceCandidate(candidate)
  }

  /** 远端 SDP 是否已设置（candidate 需在其之后才能添加） */
  hasRemoteDescription(): boolean {
    return !!this.pc.remoteDescription
  }

  /**
   * ICE 重启（仅 offer 方）：重新生成 offer，由上层经信令发送
   * */
  async restartIce(): Promise<RTCSessionDescriptionInit> {
    this.pc.restartIce()
    const offer = await this.pc.createOffer()
    await this.pc.setLocalDescription(offer)
    return offer
  }

  /** 是否已完成重协商（重试仍失败判断用） */
  hasRestarted(): boolean {
    return this.restarted
  }

  /**
   * 静音/取消静音本地麦克风
   * */
  toggleAudio(muted: boolean): void {
    this.localStream?.getAudioTracks().forEach((track) => {
      track.enabled = !muted
    })
  }

  /**
   * 开关本地摄像头
   * */
  toggleVideo(muted: boolean): void {
    this.localStream?.getVideoTracks().forEach((track) => {
      track.enabled = !muted
    })
  }

  /**
   * 弱网降级：暂停发送视频轨道（replaceTrack(null)，停止占用带宽）
   * */
  degradeVideo(): void {
    if (this.degraded) {
      return
    }
    const sender = this.pc.getSenders().find((s) => s.track?.kind === 'video')
    if (sender && sender.track) {
      this.savedVideoTrack = sender.track
      // replaceTrack(null) 后远端会触发 track.onmute
      sender.replaceTrack(null)
      this.degraded = true
      this.callbacks.onVideoDegraded()
    }
  }

  /**
   * 恢复视频（弱网恢复后手动/自动恢复）
   * */
  restoreVideo(): boolean {
    if (!this.degraded || !this.savedVideoTrack) {
      return false
    }
    const sender = this.pc.getSenders().find((s) => s.track === null || s.track?.kind === 'video')
    if (sender) {
      sender.replaceTrack(this.savedVideoTrack)
      this.degraded = false
      return true
    }
    return false
  }

  isDegraded(): boolean {
    return this.degraded
  }

  /**
   * 启动网络质量监测（每 5s 采样一次）
   * */
  startStatsMonitor(): void {
    if (this.statsTimer) {
      return
    }
    this.lastStatsTime = Date.now()
    this.statsTimer = setInterval(async () => {
      try {
        const stats = await this.pc.getStats()
        let rtt = 0
        let lossRate = 0
        let bytesSent = 0

        stats.forEach((report: RTCStats) => {
          const r = report as unknown as Record<string, number | string>
          // 远端入站报告携带对端观测的 RTT 与丢包率
          if (report.type === 'remote-inbound-rtp') {
            if (typeof r.roundTripTime === 'number') {
              rtt = r.roundTripTime * 1000
            }
            if (typeof r.fractionLost === 'number') {
              lossRate = r.fractionLost
            }
          }
          if (report.type === 'outbound-rtp' && typeof r.bytesSent === 'number') {
            bytesSent = r.bytesSent
          }
        })

        const now = Date.now()
        const bitrate = this.lastStatsTime
          ? Math.round(((bytesSent - this.lastBytesSent) * 8) / (now - this.lastStatsTime))
          : 0
        this.lastBytesSent = bytesSent
        this.lastStatsTime = now

        const level: NetworkLevel = (lossRate > POOR_LOSS || rtt > POOR_RTT)
          ? 'poor'
          : (lossRate > MEDIUM_LOSS || rtt > MEDIUM_RTT)
            ? 'medium'
            : 'good'

        this.callbacks.onStats({ rtt: Math.round(rtt), lossRate, bitrate, level })
      } catch (e) {
        console.warn('[RtcConnection] getStats 失败', e)
      }
    }, 5000)
  }

  stopStatsMonitor(): void {
    if (this.statsTimer) {
      clearInterval(this.statsTimer)
      this.statsTimer = null
    }
  }

  /**
   * 连接恢复可用
   * */
  private onLinkUp(): void {
    this.clearDisconnectTimeout()
    if (this.restartTimer) {
      clearTimeout(this.restartTimer)
      this.restartTimer = null
    }
    if (!this.failedNotified) {
      this.callbacks.onConnected()
    }
  }

  /**
   * disconnected 后启动宽限计时，超时仍未恢复视为连接丢失
   * */
  private armDisconnectTimeout(): void {
    if (this.disconnectTimer || this.failedNotified) {
      return
    }
    this.disconnectTimer = setTimeout(() => {
      this.disconnectTimer = null
      const connState = this.pc.connectionState
      const iceState = this.pc.iceConnectionState
      if (connState !== 'connected' && iceState !== 'connected' && iceState !== 'completed') {
        this.notifyFailed()
      }
    }, DISCONNECT_GRACE_MS)
  }

  private clearDisconnectTimeout(): void {
    if (this.disconnectTimer) {
      clearTimeout(this.disconnectTimer)
      this.disconnectTimer = null
    }
  }

  /**
   * 连接丢失（对端异常退出/网络中断）
   *
   * offer 方先尝试一次 ICE 重启（重协商 offer 由上层经信令发回对端），
   * 超时仍未重连则结束；非 offer 方无法主动重协商，直接结束。
   * */
  private handleConnectionLoss(): void {
    if (this.failedNotified) {
      return
    }
    this.clearDisconnectTimeout()
    if (!this.restarted && this.isOfferer) {
      this.restarted = true
      this.restartIce()
          .then((offer) => {
            this.callbacks.onRenegotiationOffer(offer)
            this.restartTimer = setTimeout(() => {
              this.restartTimer = null
              this.notifyFailed()
            }, RESTART_GRACE_MS)
          })
          .catch(() => this.notifyFailed())
      return
    }
    this.notifyFailed()
  }

  private notifyFailed(): void {
    if (this.failedNotified) {
      return
    }
    this.failedNotified = true
    this.clearDisconnectTimeout()
    this.callbacks.onFailed()
  }

  /**
   * 释放全部资源（挂断/结束时调用）
   * */
  close(): void {
    this.stopStatsMonitor()
    this.clearDisconnectTimeout()
    if (this.restartTimer) {
      clearTimeout(this.restartTimer)
      this.restartTimer = null
    }
    this.pc.onicecandidate = null
    this.pc.ontrack = null
    this.pc.onconnectionstatechange = null
    this.pc.oniceconnectionstatechange = null
    this.localStream?.getTracks().forEach((track) => track.stop())
    this.localStream = null
    this.savedVideoTrack = null
    try {
      this.pc.close()
    } catch {
      // ignore
    }
  }
}

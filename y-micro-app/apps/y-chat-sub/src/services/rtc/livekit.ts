import {
  Room,
  RoomEvent,
  Track,
  ConnectionState,
  ConnectionQuality,
  DisconnectReason,
  VideoPresets,
  type Participant,
  type RemoteParticipant,
  type RemoteTrack,
  type RemoteTrackPublication,
  type LocalTrackPublication,
} from 'livekit-client'

/**
 * LiveKit 媒体会话封装
 *
 * 负责：入房/离房、轨道收发与挂载、静音/摄像头开关、连接状态与参与者状态聚合。
 * 呼叫控制（200-205 信令）与房间状态机由后端 + store/call.ts 负责，本类只关心媒体面。
 *
 * 静音状态、说话检测、连接质量均取自 LiveKit 服务端同步，无需自研状态信令。
 * */

/** 连接状态（对 UI 暴露的收敛状态） */
export type LivekitConnState = 'connecting' | 'connected' | 'reconnecting' | 'disconnected'

/** 连接质量 */
export type ParticipantQuality = 'excellent' | 'good' | 'poor' | 'unknown' | 'lost'

/** 参与者（媒体面）状态 */
export interface ParticipantState {
  /** identity = String(uid) */
  uid: string
  name: string
  avatar: string
  isLocal: boolean
  /** 麦克风关闭（含未发布音轨） */
  audioMuted: boolean
  /** 摄像头关闭（含未发布视频轨） */
  videoMuted: boolean
  /** 有可渲染的视频轨 */
  hasVideo: boolean
  /** 正在说话 */
  speaking: boolean
  quality: ParticipantQuality
  /** 仅群聊：已邀请但未接入（由 store 用后端参与者列表补齐，此处恒为 false） */
  invited: boolean
}

export interface LivekitCallbacks {
  onParticipantsChanged(list: ParticipantState[]): void
  onConnectionState(s: LivekitConnState): void
  onDisconnected(reason: DisconnectReason): void
  onAudioBlocked(): void
  onError(err: unknown): void
}

interface TrackBucket {
  audio?: RemoteTrack
  video?: RemoteTrack
}

export class LivekitSession {
  private readonly url: string
  private readonly token: string
  private readonly cb: LivekitCallbacks
  private room: Room | null = null
  /** uid → 已发布/已订阅的轨道（含本地） */
  private readonly buckets = new Map<string, TrackBucket>()
  private disposed = false

  constructor(url: string, token: string, callbacks: LivekitCallbacks) {
    this.url = url
    this.token = token
    this.cb = callbacks
  }

  /** 入房并发布本地媒体（opts 为初始发布意图） */
  async connect(opts: { audio: boolean; video: boolean }): Promise<void> {
    if (this.room || this.disposed) {
      return
    }
    const room = new Room({
      adaptiveStream: true,
      dynacast: true,
      publishDefaults: { simulcast: true },
      videoCaptureDefaults: { resolution: VideoPresets.h720.resolution },
    })
    this.room = room
    this.bind(room)

    this.cb.onConnectionState('connecting')
    try {
      await room.connect(this.url, this.token)
      if (opts.audio) {
        await room.localParticipant.setMicrophoneEnabled(true)
      }
      if (opts.video) {
        await room.localParticipant.setCameraEnabled(true)
      }
      this.cb.onConnectionState('connected')
      this.emit()
    } catch (err) {
      this.cb.onError(err)
      throw err
    }
  }

  /** 离房（stopTracks=true 会同时停止本地采集） */
  async disconnect(): Promise<void> {
    this.disposed = true
    const room = this.room
    this.room = null
    this.buckets.clear()
    if (!room) {
      return
    }
    try {
      await room.disconnect(true)
    } catch (err) {
      console.warn('[LivekitSession] 断开连接失败', err)
    }
  }

  async setMicEnabled(enabled: boolean): Promise<void> {
    if (!this.room) {
      return
    }
    try {
      await this.room.localParticipant.setMicrophoneEnabled(enabled)
      this.emit()
    } catch (err) {
      this.cb.onError(err)
      // 采集失败（无设备/无权限）时回滚 UI 开关状态
      this.emit()
    }
  }

  async setCamEnabled(enabled: boolean): Promise<void> {
    if (!this.room) {
      return
    }
    try {
      await this.room.localParticipant.setCameraEnabled(enabled)
    } catch (err) {
      this.cb.onError(err)
    }
    this.emit()
  }

  /** 把某成员的视频轨挂到目标 <video>（el 为 null 时不处理） */
  attachVideo(uid: string, el: HTMLVideoElement | null): void {
    const track = this.buckets.get(uid)?.video
    if (!el || !track) {
      return
    }
    track.attach(el)
  }

  /** 把某成员的音频轨挂到目标 <audio>（远端音频必须挂真实 DOM 才能出声） */
  attachAudio(uid: string, el: HTMLAudioElement | null): void {
    const track = this.buckets.get(uid)?.audio
    if (!el || !track) {
      return
    }
    track.attach(el)
  }

  /** 应对浏览器自动播放策略：用户手势后恢复音频播放 */
  async resumeAudio(): Promise<void> {
    if (!this.room) {
      return
    }
    try {
      await this.room.startAudio()
    } catch (err) {
      this.cb.onError(err)
    }
  }

  /** 当前参与者状态快照（本地优先） */
  snapshot(): ParticipantState[] {
    const room = this.room
    if (!room) {
      return []
    }
    const list: ParticipantState[] = [this.toState(room.localParticipant, true)]
    room.remoteParticipants.forEach((p) => list.push(this.toState(p, false)))
    return list
  }

  // ==================== 内部 ====================

  private bind(room: Room): void {
    room
      .on(RoomEvent.ParticipantConnected, () => this.emit())
      .on(RoomEvent.ParticipantDisconnected, (p: RemoteParticipant) => {
        this.buckets.delete(p.identity)
        this.emit()
      })
      .on(RoomEvent.TrackSubscribed, (track: RemoteTrack, _pub: RemoteTrackPublication, p: RemoteParticipant) => {
        const bucket = this.bucket(p.identity)
        if (track.kind === Track.Kind.Audio) {
          bucket.audio = track
        } else if (track.kind === Track.Kind.Video) {
          bucket.video = track
        }
        this.emit()
      })
      .on(RoomEvent.TrackUnsubscribed, (track: RemoteTrack, _pub: RemoteTrackPublication, p: RemoteParticipant) => {
        const bucket = this.buckets.get(p.identity)
        if (!bucket) {
          return
        }
        if (track.kind === Track.Kind.Audio && bucket.audio === track) {
          delete bucket.audio
        }
        if (track.kind === Track.Kind.Video && bucket.video === track) {
          delete bucket.video
        }
        this.emit()
      })
      .on(RoomEvent.LocalTrackPublished, (pub: LocalTrackPublication) => {
        const identity = room.localParticipant.identity
        const bucket = this.bucket(identity)
        if (pub.kind === Track.Kind.Audio && pub.track) {
          bucket.audio = pub.track as unknown as RemoteTrack
        } else if (pub.kind === Track.Kind.Video && pub.track) {
          bucket.video = pub.track as unknown as RemoteTrack
        }
        this.emit()
      })
      .on(RoomEvent.LocalTrackUnpublished, (pub: LocalTrackPublication) => {
        const bucket = this.buckets.get(room.localParticipant.identity)
        if (!bucket) {
          return
        }
        if (pub.kind === Track.Kind.Audio) {
          delete bucket.audio
        } else if (pub.kind === Track.Kind.Video) {
          delete bucket.video
        }
        this.emit()
      })
      .on(RoomEvent.TrackMuted, () => this.emit())
      .on(RoomEvent.TrackUnmuted, () => this.emit())
      .on(RoomEvent.ActiveSpeakersChanged, () => this.emit())
      .on(RoomEvent.ConnectionQualityChanged, () => this.emit())
      .on(RoomEvent.ConnectionStateChanged, (state: ConnectionState) => {
        this.cb.onConnectionState(mapState(state))
      })
      .on(RoomEvent.Reconnecting, () => this.cb.onConnectionState('reconnecting'))
      .on(RoomEvent.Reconnected, () => this.cb.onConnectionState('connected'))
      .on(RoomEvent.AudioPlaybackStatusChanged, () => {
        if (!room.canPlaybackAudio) {
          this.cb.onAudioBlocked()
        }
      })
      .on(RoomEvent.Disconnected, (reason?: DisconnectReason) => {
        this.cb.onConnectionState('disconnected')
        this.cb.onDisconnected(reason ?? DisconnectReason.UNKNOWN_REASON)
      })
  }

  private bucket(uid: string): TrackBucket {
    let bucket = this.buckets.get(uid)
    if (!bucket) {
      bucket = {}
      this.buckets.set(uid, bucket)
    }
    return bucket
  }

  private emit(): void {
    if (this.disposed) {
      return
    }
    this.cb.onParticipantsChanged(this.snapshot())
  }

  private toState(p: Participant, isLocal: boolean): ParticipantState {
    const meta = parseMetadata(p.metadata)
    const bucket = this.buckets.get(p.identity)
    return {
      uid: p.identity,
      name: p.name || meta.nickname || p.identity,
      avatar: meta.avatar,
      isLocal,
      audioMuted: !bucket?.audio || bucket.audio.isMuted,
      videoMuted: !bucket?.video || bucket.video.isMuted,
      hasVideo: !!bucket?.video && !bucket.video.isMuted,
      speaking: p.isSpeaking,
      quality: (p.connectionQuality ?? ConnectionQuality.Unknown) as ParticipantQuality,
      invited: false,
    }
  }
}

function parseMetadata(raw?: string): { nickname: string; avatar: string } {
  if (!raw) {
    return { nickname: '', avatar: '' }
  }
  try {
    const parsed = JSON.parse(raw) as { nickname?: string; avatar?: string }
    return { nickname: parsed.nickname || '', avatar: parsed.avatar || '' }
  } catch {
    return { nickname: '', avatar: '' }
  }
}

function mapState(state: ConnectionState): LivekitConnState {
  switch (state) {
    case ConnectionState.Connected:
      return 'connected'
    case ConnectionState.Reconnecting:
    case ConnectionState.SignalReconnecting:
      return 'reconnecting'
    case ConnectionState.Connecting:
      return 'connecting'
    default:
      return 'disconnected'
  }
}

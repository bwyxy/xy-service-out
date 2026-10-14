/**
 * 通话铃声引擎（WebAudio 合成，无外部音频资源依赖）
 *
 * - 来电铃声：双音制式铃音（440Hz + 480Hz，响 2s 停 4s 循环）
 * - 回铃音：单音提示（450Hz，响 1s 停 3s 循环）
 *
 * 注意：部分浏览器/WKWebView 在用户交互前会限制 AudioContext，
 * 铃声可能静音直到首次交互，属预期降级（弹窗本身仍有视觉提示）。
 * */

type RingKind = 'incoming' | 'outgoing'

class RingtoneEngine {
  private ctx: AudioContext | null = null
  private timer: ReturnType<typeof setInterval> | null = null

  private ensureContext(): AudioContext | null {
    try {
      if (!this.ctx) {
        this.ctx = new AudioContext()
      }
      if (this.ctx.state === 'suspended') {
        // 尽力恢复（无用户手势时可能失败）
        this.ctx.resume().catch(() => undefined)
      }
      return this.ctx
    } catch {
      return null
    }
  }

  /**
   * 播放一段音调
   * */
  private tone(freqs: number[], duration: number): void {
    const ctx = this.ensureContext()
    if (!ctx || ctx.state !== 'running') {
      return
    }
    const gain = ctx.createGain()
    gain.gain.value = 0.12
    gain.connect(ctx.destination)

    const oscs = freqs.map((freq) => {
      const osc = ctx.createOscillator()
      osc.type = 'sine'
      osc.frequency.value = freq
      osc.connect(gain)
      osc.start()
      return osc
    })

    setTimeout(() => {
      oscs.forEach((osc) => {
        try {
          osc.stop()
        } catch {
          // ignore
        }
      })
      gain.disconnect()
    }, duration)
  }

  /**
   * 开始循环播放
   * */
  start(kind: RingKind): void {
    this.stop()
    if (kind === 'incoming') {
      // 双音铃：响 2s 停 4s
      const ring = () => this.tone([440, 480], 2000)
      ring()
      this.timer = setInterval(ring, 6000)
    } else {
      // 回铃：响 1s 停 3s
      const ring = () => this.tone([450], 1000)
      ring()
      this.timer = setInterval(ring, 4000)
    }
  }

  /**
   * 停止播放（音调自带定时停止，只需停止循环）
   * */
  stop(): void {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  /** 当前是否在播放 */
  playing(): boolean {
    return this.timer !== null
  }
}

export const ringtoneEngine = new RingtoneEngine()

import { Application, Assets } from 'pixi.js'
import { Spine } from '@esotericsoftware/spine-pixi-v8'

/** 资源别名固定，替换真实模型时只要同名覆盖 public/spine 下的文件即可，代码不用动 */
const SKELETON_ALIAS = 'pet-skeleton'
const ATLAS_ALIAS = 'pet-atlas'

export interface SpinePet {
  /**
   * 播放指定动画。
   * loop 为 false 时播完自动回到待机动画，避免一次性动作卡在最后一帧。
   */
  play(name: string, loop: boolean): void
  /** 模型里可用的动画名，调试面板和后续状态机都要用 */
  animationNames: string[]
  destroy(): void
}

export interface CreateSpinePetOptions {
  /** 承载 canvas 的容器，尺寸按它的 clientWidth/Height 决定 */
  host: HTMLElement
  skeletonUrl: string
  atlasUrl: string
  /** 形象占画布的比例，留白避免顶到窗口边缘 */
  fit?: number
  /** 初始循环动画，默认 idle */
  idleAnimation?: string
}

export async function createSpinePet(options: CreateSpinePetOptions): Promise<SpinePet> {
  const { host, skeletonUrl, atlasUrl, fit = 0.78, idleAnimation = 'idle' } = options

  const width = host.clientWidth || 320
  const height = host.clientHeight || 320

  const app = new Application()
  await app.init({
    width,
    height,
    // 透明窗口：canvas 自身也不能有底色
    backgroundAlpha: 0,
    antialias: true,
    resolution: window.devicePixelRatio,
    autoDensity: true,
  })

  Assets.add({ alias: SKELETON_ALIAS, src: skeletonUrl })
  Assets.add({ alias: ATLAS_ALIAS, src: atlasUrl })
  await Assets.load([SKELETON_ALIAS, ATLAS_ALIAS])

  // 让骨骼更新跟着应用自身的 ticker 走，窗口不可见时随 rAF 一起被节流
  const spine = Spine.from({
    skeleton: SKELETON_ALIAS,
    atlas: ATLAS_ALIAS,
    autoUpdate: true,
    ticker: app.ticker,
  })

  // 先按中立姿势取一次包围盒，等比缩放并居中。
  // spine-pixi 会把 Skeleton.yDown 置为 true，顶点已经是 y 轴向下，不需要再翻转
  spine.update(0)
  const bounds = spine.bounds
  const usable = Number.isFinite(bounds.width) && bounds.width > 0 && bounds.height > 0
  const scale = usable
    ? Math.min(width / bounds.width, height / bounds.height) * fit
    : 1
  spine.scale.set(scale)
  spine.position.set(
    width / 2 - (bounds.x + bounds.width / 2) * scale,
    height / 2 - (bounds.y + bounds.height / 2) * scale,
  )
  app.stage.addChild(spine)

  const animationNames = spine.skeleton.data.animations.map((animation) => animation.name)
  const startAnimation = animationNames.includes(idleAnimation) ? idleAnimation : animationNames[0]
  if (startAnimation) {
    spine.state.setAnimation(0, startAnimation, true)
  }

  host.appendChild(app.canvas)

  return {
    animationNames,
    play(name, loop) {
      if (!animationNames.includes(name)) return
      spine.state.setAnimation(0, name, loop)
      // 一次性动画按队列接回待机，避免自己监听 complete 事件
      if (!loop && animationNames.includes(idleAnimation)) {
        spine.state.addAnimation(0, idleAnimation, true, 0)
      }
    },
    destroy() {
      app.destroy(true, { children: true })
    },
  }
}

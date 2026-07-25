import type { DragPosition, DragTouchPosition, DragCallbacks } from './types'

interface MapState {
  left: number
  top: number
  zoom: number
  width: number
  height: number
}

interface DragContainerOptions extends DragCallbacks {
  /** 被拖动的元素 */
  element: HTMLElement
  /** 拖动手柄选择器或元素，不传则整个元素可拖动 */
  handle?: string | HTMLElement
  /** 是否限制在父元素内 */
  constrain?: boolean
  /** 是否启用触摸支持 */
  enableTouch?: boolean
  /** 是否阻止默认事件 */
  preventDefault?: boolean
  /** 是否开启滚轮缩放 */
  isWheel?: boolean
  /** 最大缩放 */
  maxZoom?: number
  /** 最小缩放 */
  minZoom?: number
}

export class DragContainer {
  private _element: HTMLElement
  private _handleSelector: string | null = null
  private _handleElement: HTMLElement | null = null
  private _constrain: boolean
  private _enableTouch: boolean
  private _preventDefault: boolean
  private _isWheel: boolean
  private _maxZoom: number
  private _minZoom: number
  private _callbacks: DragCallbacks
  private _enabled = false

  private _startX = 0
  private _startY = 0
  private _prevX = 0
  private _prevY = 0
  private _initialX = 0
  private _initialY = 0

  private _map: MapState = {
    left: 0,
    top: 0,
    zoom: 1,
    width: 1280,
    height: 650,
  }

  private _position: DragTouchPosition = {
    x: 0,
    y: 0,
    isDragging: false,
    deltaX: 0,
    deltaY: 0,
    velocityX: 0,
    velocityY: 0,
  }

  private _boundMouseMove: (e: MouseEvent) => void
  private _boundMouseUp: (e: MouseEvent) => void
  private _boundMouseLeave: (e: MouseEvent) => void
  private _boundWheel: (e: WheelEvent) => void
  private _boundTouchMove: (e: TouchEvent) => void
  private _boundTouchEnd: (e: TouchEvent) => void
  private _boundTouchCancel: (e: TouchEvent) => void
  private _boundMouseDown: (e: MouseEvent) => void
  private _boundTouchStart: (e: TouchEvent) => void

  constructor(options: DragContainerOptions) {
    this._element = options.element
    this._constrain = options.constrain ?? false
    this._enableTouch = options.enableTouch ?? true
    this._preventDefault = options.preventDefault ?? true
    this._isWheel = options.isWheel ?? false
    this._maxZoom = options.maxZoom ?? 10
    this._minZoom = options.minZoom ?? 1
    this._callbacks = {
      onStart: options.onStart,
      onMove: options.onMove,
      onEnd: options.onEnd,
      onCancel: options.onCancel,
      onZoom: options.onZoom,
    }

    if (options.handle) {
      if (typeof options.handle === 'string') {
        this._handleSelector = options.handle
      } else {
        this._handleElement = options.handle
      }
    }

    this._boundMouseDown = this._onMouseDown.bind(this)
    this._boundMouseMove = this._onMouseMove.bind(this)
    this._boundMouseUp = this._onMouseUp.bind(this)
    this._boundMouseLeave = this._onMouseCancel.bind(this)
    this._boundWheel = this._onWheel.bind(this)
    this._boundTouchStart = this._onTouchStart.bind(this)
    this._boundTouchMove = this._onTouchMove.bind(this)
    this._boundTouchEnd = this._onTouchEnd.bind(this)
    this._boundTouchCancel = this._onTouchCancel.bind(this)

    this.enable()
  }

  /** 当前状态 */
  get position(): Readonly<DragTouchPosition> {
    return this._position
  }

  get zoom(): number {
    return this._map.zoom
  }

  get isDragging(): boolean {
    return this._position.isDragging
  }

  get isEnabled(): boolean {
    return this._enabled
  }

  /** 启用 */
  enable(): void {
    if (this._enabled) return
    this._enabled = true
    this._element.addEventListener('mousedown', this._boundMouseDown)

    if (this._isWheel) {
      this._element.addEventListener('wheel', this._boundWheel, { passive: false })
    }

    if (this._enableTouch) {
      this._element.addEventListener('touchstart', this._boundTouchStart, { passive: false })
    }
  }

  /** 禁用 */
  disable(): void {
    if (!this._enabled) return
    this._enabled = false
    this._element.removeEventListener('mousedown', this._boundMouseDown)

    if (this._isWheel) {
      this._element.removeEventListener('wheel', this._boundWheel)
    }

    if (this._enableTouch) {
      this._element.removeEventListener('touchstart', this._boundTouchStart)
    }

    this._cleanupMouseEvents()
    this._cleanupTouchEvents()
  }

  /** 销毁 */
  destroy(): void {
    this.disable()
  }

  /** 设置容器尺寸 */
  setContainerSize(width: number, height: number): void {
    this._map.width = width
    this._map.height = height
  }

  /** 重置位置 */
  resetPosition(): void {
    this._position.x = 0
    this._position.y = 0
    this._position.deltaX = 0
    this._position.deltaY = 0
    this._position.velocityX = 0
    this._position.velocityY = 0

    this._element.style.transition = 'transform 0.3s'
    this._element.style.left = '0px'
    this._element.style.top = '0px'
    this._map.left = 0
    this._map.top = 0
  }

  /** 设置位置和缩放 */
  setPosition(x: number, y: number, zoom?: number, animated = true): void {
    this._position.x = x
    this._position.y = y
    this._map.left = x
    this._map.top = y
    if (zoom != null) {
      this._map.zoom = zoom
    }

    this._element.style.transition = animated ? 'transform 0.3s' : 'none'
    this._element.style.left = `${x}px`
    this._element.style.top = `${y}px`
  }

  /** 获取当前位置 */
  getPosition(): DragTouchPosition & { zoom: number } {
    return { ...this._position, zoom: this._map.zoom }
  }

  // ---- 内部事件处理 ----

  private _getClientPosition(event: MouseEvent | TouchEvent): { clientX: number; clientY: number } {
    if ('touches' in event) {
      const touch = event.touches[0] || event.changedTouches[0]
      return { clientX: touch.clientX, clientY: touch.clientY }
    }
    return { clientX: event.clientX, clientY: event.clientY }
  }

  private _isHandle(target: HTMLElement): boolean {
    if (!this._handleSelector && !this._handleElement) return true
    if (this._handleElement) return target === this._handleElement || this._handleElement.contains(target)
    return !!target.closest(this._handleSelector!)
  }

  private _handleStart(event: MouseEvent | TouchEvent): boolean {
    const target = event.target as HTMLElement
    if (!this._isHandle(target)) return false

    if (this._preventDefault) {
      event.preventDefault()
    }

    this._position.isDragging = true

    const { clientX, clientY } = this._getClientPosition(event)
    this._startX = clientX
    this._startY = clientY
    this._prevX = clientX
    this._prevY = clientY
    console.log('鼠标按下, 拖拽开始', this._startX, this._startY)

    this._position.x = this._map.left
    this._position.y = this._map.top
    this._position.velocityX = 0
    this._position.velocityY = 0

    this._callbacks.onStart?.(event, this._element)
    return true
  }

  private _handleEnd(event: MouseEvent | TouchEvent): void {
    if (!this._position.isDragging) return

    if (this._preventDefault) {
      event.preventDefault()
    }

    this._position.isDragging = false
    this._applyInertia()
    this._callbacks.onEnd?.(event, this._element)
  }

  private _handleCancel(event: MouseEvent | TouchEvent): void {
    this._position.isDragging = false
    this._element.style.transition = 'transform 0.2s'
    this._element.style.left = `${this._position.x}px`
    this._element.style.top = `${this._position.y}px`
    this._callbacks.onCancel?.(event)
  }

  private _applyInertia(): void {
    const minVelocity = 0.1
    const vx = this._position.velocityX
    const vy = this._position.velocityY

    if (Math.abs(vx) <= minVelocity && Math.abs(vy) <= minVelocity) return

    let inertiaLeft = this._map.left + vx * 50
    let inertiaTop = this._map.top + vy * 50

    if (this._constrain) {
      const elRect = this._element.getBoundingClientRect()
      const parent = this._element.parentElement
      if (parent) {
        const parentRect = parent.getBoundingClientRect()
        inertiaLeft = Math.max(0, Math.min(inertiaLeft, parentRect.width - elRect.width))
        inertiaTop = Math.max(0, Math.min(inertiaTop, parentRect.height - elRect.height))
      }
    }

    this._element.style.transition = 'transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94)'
    this._element.style.left = `${inertiaLeft}px`
    this._element.style.top = `${inertiaTop}px`
    this._map.left = inertiaLeft
    this._map.top = inertiaTop

    setTimeout(() => {
      this._position.x = inertiaLeft
      this._position.y = inertiaTop
    }, 300)
  }

  private _applyConstrain(): void {
    if (!this._constrain) return
    const parent = this._element.parentElement
    if (!parent) return
    const elRect = this._element.getBoundingClientRect()
    const parentRect = parent.getBoundingClientRect()
    const maxX = parentRect.width - elRect.width
    const maxY = parentRect.height - elRect.height
    this._map.left = Math.max(0, Math.min(this._map.left, maxX))
    this._map.top = Math.max(0, Math.min(this._map.top, maxY))
  }

  // ---- 鼠标事件 ----

  private _onMouseDown(e: MouseEvent): void {
    if (e.button !== 0) return
    e.stopPropagation()

    if (this._handleStart(e)) {
      window.addEventListener('mousemove', this._boundMouseMove)
      window.addEventListener('mouseup', this._boundMouseUp)
      window.addEventListener('mouseleave', this._boundMouseLeave)
    }
  }

  private _onMouseMove(e: MouseEvent): void {
    if (!this._position.isDragging) return

    this._position.deltaX = e.clientX - this._prevX
    this._position.deltaY = e.clientY - this._prevY
    this._position.velocityX = e.movementX || this._position.deltaX
    this._position.velocityY = e.movementY || this._position.deltaY

    this._map.left += e.clientX - this._startX
    this._map.top += e.clientY - this._startY
    this._startX = e.clientX
    this._startY = e.clientY
    this._prevX = e.clientX
    this._prevY = e.clientY

    this._applyConstrain()

    this._element.style.left = `${this._map.left}px`
    this._element.style.top = `${this._map.top}px`

    this._position.x = this._map.left
    this._position.y = this._map.top

    this._callbacks.onMove?.(e, this._element, { x: this._map.left, y: this._map.top })
  }

  private _onMouseUp(e: MouseEvent): void {
    this._handleEnd(e)
    this._cleanupMouseEvents()
  }

  private _onMouseCancel(e: MouseEvent): void {
    this._handleCancel(e)
    this._cleanupMouseEvents()
  }

  private _cleanupMouseEvents(): void {
    window.removeEventListener('mousemove', this._boundMouseMove)
    window.removeEventListener('mouseup', this._boundMouseUp)
    window.removeEventListener('mouseleave', this._boundMouseLeave)
  }

  // ---- 滚轮缩放 ----

  private _onWheel(e: WheelEvent): void {
    e.stopPropagation()
    e.preventDefault()

    const startX = e.offsetX + this._map.left
    const startY = e.offsetY + this._map.top

    if (e.deltaY > 0) {
      if (this._map.zoom > this._minZoom) {
        const s1 = this._map.zoom
        const s2 = this._map.zoom * 0.9
        this._map.zoom = s2

        if (this._map.left >= 0) {
          this._map.left = 0
        } else if (this._map.left <= (1 - s2) * this._map.width) {
          this._map.left = (1 - s2) * this._map.width
        } else {
          this._map.left = this._map.left + (s1 - s2) * startX
        }

        if (this._map.top >= 0) {
          this._map.top = 0
        } else if (this._map.top <= (1 - s2) * this._map.height) {
          this._map.top = (1 - s2) * this._map.height
        } else {
          this._map.top = this._map.top + (s1 - s2) * startY
        }
      } else {
        this._map.left = 0
        this._map.top = 0
      }
    } else {
      if (this._map.zoom < this._maxZoom) {
        const s1 = this._map.zoom
        const s2 = this._map.zoom * 1.1
        this._map.zoom = s2
        this._map.left = this._map.left - (s2 - s1) * startX
        this._map.top = this._map.top - (s2 - s1) * startY
      }
    }

    this._element.style.left = `${this._map.left}px`
    this._element.style.top = `${this._map.top}px`
    this._position.x = this._map.left
    this._position.y = this._map.top

    this._callbacks.onMove?.(e, this._element, { x: this._map.left, y: this._map.top })
    this._callbacks.onZoom?.(e, this._map.zoom)
  }

  // ---- 触摸事件 ----

  private _onTouchStart(e: TouchEvent): void {
    e.stopPropagation()

    if (this._handleStart(e)) {
      window.addEventListener('touchmove', this._boundTouchMove, { passive: false })
      window.addEventListener('touchend', this._boundTouchEnd)
      window.addEventListener('touchcancel', this._boundTouchCancel)
    }
  }

  private _onTouchMove(e: TouchEvent): void {
    if (!this._position.isDragging) return

    const touch = e.targetTouches[0]
    this._position.deltaX = touch.clientX - this._prevX
    this._position.deltaY = touch.clientY - this._prevY
    this._position.velocityX = this._position.deltaX
    this._position.velocityY = this._position.deltaY

    this._map.left += touch.clientX - this._startX
    this._map.top += touch.clientY - this._startY
    this._startX = touch.clientX
    this._startY = touch.clientY
    this._prevX = touch.clientX
    this._prevY = touch.clientY

    this._applyConstrain()

    this._element.style.left = `${this._map.left}px`
    this._element.style.top = `${this._map.top}px`

    this._position.x = this._map.left
    this._position.y = this._map.top

    this._callbacks.onMove?.(e, this._element, { x: this._map.left, y: this._map.top })
  }

  private _onTouchEnd(e: TouchEvent): void {
    this._handleEnd(e)
    this._cleanupTouchEvents()
  }

  private _onTouchCancel(e: TouchEvent): void {
    this._handleCancel(e)
    this._cleanupTouchEvents()
  }

  private _cleanupTouchEvents(): void {
    window.removeEventListener('touchmove', this._boundTouchMove)
    window.removeEventListener('touchend', this._boundTouchEnd)
    window.removeEventListener('touchcancel', this._boundTouchCancel)
  }
}

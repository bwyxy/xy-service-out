import type { SizeInfo, ResizeCallback } from './types'

export class SizeObserver {
  private _observer: ResizeObserver | null = null
  private _callbacks = new WeakMap<Element, ResizeCallback>()
  private _elements = new Set<Element>()

  constructor() {
    this._observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const callback = this._callbacks.get(entry.target)
        if (callback) {
          const { width, height } = entry.contentRect
          callback({ width, height }, entry)
        }
      }
    })
  }

  /** 监听元素尺寸变化 */
  observe(element: Element, callback: ResizeCallback): void {
    if (!this._observer) return
    this._callbacks.set(element, callback)
    this._elements.add(element)
    this._observer.observe(element)

    const rect = element.getBoundingClientRect()
    callback({ width: rect.width, height: rect.height }, null as any)
  }

  /** 停止监听某个元素 */
  unobserve(element: Element): void {
    if (!this._observer) return
    this._observer.unobserve(element)
    this._callbacks.delete(element)
    this._elements.delete(element)
  }

  /** 停止所有监听 */
  disconnect(): void {
    if (this._observer) {
      this._observer.disconnect()
      this._observer = null
    }
    this._callbacks = new WeakMap()
    this._elements.clear()
  }

  /** 当前监听的元素数量 */
  get count(): number {
    return this._elements.size
  }
}
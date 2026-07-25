export interface SizeInfo {
  width: number
  height: number
}

export type ResizeCallback = (size: SizeInfo, entry: ResizeObserverEntry) => void
export interface DragPosition {
  x: number
  y: number
}

export interface DragTouchPosition extends DragPosition {
  isDragging: boolean
  deltaX: number
  deltaY: number
  velocityX: number
  velocityY: number
}

export interface DragCallbacks {
  onStart?: (event: MouseEvent | TouchEvent, element: HTMLElement) => void
  onMove?: (event: MouseEvent | TouchEvent, element: HTMLElement, position: DragPosition) => void
  onEnd?: (event: MouseEvent | TouchEvent, element: HTMLElement) => void
  onCancel?: (event: MouseEvent | TouchEvent) => void
  onZoom?: (event: WheelEvent, zoom: number) => void
}
import { nextTick, onUnmounted, onMounted, ref, type Ref } from 'vue'

/**
 * 右键菜单通用逻辑
 *
 * 统一处理菜单定位（视口边缘收敛）与关闭时机（点击空白、滚动、窗口尺寸变化、Esc）。
 * 调用方只需负责菜单内容与各自的业务动作。
 */
export function useContextMenu<T>() {
  const visible = ref(false)
  const target = ref<T | null>(null) as Ref<T | null>
  /** 菜单根元素，用于测量尺寸做边缘收敛 */
  const menuRef = ref<HTMLElement | null>(null)
  const style = ref<{ left: string; top: string }>({ left: '0px', top: '0px' })

  /** 关闭菜单并清空当前操作对象 */
  const close = () => {
    visible.value = false
    target.value = null
  }

  /**
   * 在鼠标位置打开菜单
   *
   * @param event 触发菜单的鼠标事件
   * @param payload 当前操作对象（消息、会话等）
   */
  const open = async (event: MouseEvent, payload: T) => {
    visible.value = true
    target.value = payload
    await nextTick()

    // 视口边缘收敛，避免菜单溢出屏幕
    const menuWidth = menuRef.value?.offsetWidth ?? 0
    const menuHeight = menuRef.value?.offsetHeight ?? 0

    style.value = {
      left: `${Math.min(event.clientX, window.innerWidth - menuWidth - 8)}px`,
      top: `${Math.min(event.clientY, window.innerHeight - menuHeight - 8)}px`,
    }
  }

  /** Esc 关闭菜单 */
  const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      close()
    }
  }

  onMounted(() => {
    document.addEventListener('click', close)
    document.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    window.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('click', close)
    document.removeEventListener('scroll', close, true)
    window.removeEventListener('resize', close)
    window.removeEventListener('keydown', handleKeydown)
  })

  return { visible, target, menuRef, style, open, close }
}

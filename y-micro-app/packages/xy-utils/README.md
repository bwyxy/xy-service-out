# @hzy/utils

## 全局工具包

#### 监听元素尺寸变化(使用示例)
````
import { SizeObserver } from '@hzy/utils'

const observer = new SizeObserver()

// 监听元素尺寸变化，首次主动回调当前尺寸
observer.observe(document.getElementById('container'), (size) => {
  console.log(size.width, size.height)
})

// 停止监听某个元素
observer.unobserve(element)

// 停止所有监听并销毁
observer.disconnect()

// 当前监听的元素数量
observer.count
````
#### 容器拖动(使用示例)
````
import { DragContainer } from '@hzy/utils'

const drag = new DragContainer({
  element: document.getElementById('box'),           // 被拖动的元素
  handle: document.getElementById('box-header'),     // 拖动手柄（可选，不传则整个元素可拖动）
  boundary: 'parent',                                // 边界: 'parent' | 'viewport' | 'none'
  onDragStart: (pos) => console.log('开始', pos),
  onDrag: (pos) => console.log('拖动中', pos.x, pos.y),
  onDragEnd: (pos) => console.log('结束', pos),
})

// 禁用拖动
drag.disable()

// 重新启用拖动
drag.enable()

// 销毁
drag.destroy()

// 状态
drag.isDragging   // 是否正在拖动
drag.isEnabled    // 是否已启用
````

#### 萤石云视频播放api(使用示例)
````
const player = new EzvizPlayer()

// --- 直播播放 ---
await player.playLive({
id: 'video-container',
accessToken: 'xxx',
deviceSerial: 'xxx',
width: 640,
height: 480,
template: 'security'
})

// --- 录像回放 ---
await player.playRecord({
id: 'video-container',
accessToken: 'xxx',
deviceSerial: 'xxx',
cameraNo: 1,
begin: startTimestamp,
end: endTimestamp,
width: 640,
height: 480,
})

// --- 播放控制 ---
await player.stop()
await player.destroy()
await player.seekPlayback({ deviceSerial, cameraNo, begin, end })
await player.switchLive(deviceSerial, cameraNo)

// --- 声音控制 ---
player.openSound()
player.closeSound()
player.setVolumeGain(5)

// --- 画面控制 ---
player.reSize(800, 600)
player.capturePicture('screenshot')
player.zoomAdd(2)
player.setDefinition('hd')

// --- 录制 ---
player.startSave('record.mp4')
player.stopSave()

// --- 云台 ---
player.openPtz()
player.closePtz()

// --- 全屏 ---
player.browserFullscreen()
player.exitBrowserFullscreen()

// --- 对讲 ---
player.startTalk()
player.stopTalk()

// --- 事件监听 ---
player.on('playing', () => console.log('播放中'))
player.on('stopped', () => console.log('已停止'))
player.on('error', () => console.log('出错了'))
player.on('destroyed', () => console.log('已销毁'))
player.off('playing')

// --- 状态 ---
player.isPlaying   // boolean
player.isLoading   // boolean
player.isMuted     // boolean
player.isDestroyed // boolean
````

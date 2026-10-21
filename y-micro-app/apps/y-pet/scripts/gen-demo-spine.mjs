/**
 * 生成演示用的 Spine 模型：public/spine/pet.png、pet.atlas、pet.json
 *
 * 为什么需要它：Spine 资产必须由 Spine 编辑器导出，仓库里没有模型就跑不起 idle 循环。
 * 这个脚本用纯 Node 手写 PNG + atlas + skeleton JSON，产出一个最小可用的占位模型，
 * 让渲染链路能先跑通。拿到真实模型后，直接用编辑器导出的三个同名文件覆盖即可。
 *
 * 用法：pnpm --filter y-pet gen:spine
 */
import { deflateSync } from 'node:zlib'
import { mkdirSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const OUT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../public/spine')

/* ---------------------------------- 画布 ---------------------------------- */
// 用 0..1 的浮点直通色（straight alpha）做 source-over 叠加，最后统一预乘

function createLayer(w, h) {
  return { w, h, data: new Float32Array(w * h * 4) }
}

function blendPixel(layer, x, y, r, g, b, a) {
  if (x < 0 || y < 0 || x >= layer.w || y >= layer.h || a <= 0) return
  const i = (y * layer.w + x) * 4
  const d = layer.data
  const dA = d[i + 3]
  const outA = a + dA * (1 - a)
  if (outA <= 0) return
  d[i] = (r * a + d[i] * dA * (1 - a)) / outA
  d[i + 1] = (g * a + d[i + 1] * dA * (1 - a)) / outA
  d[i + 2] = (b * a + d[i + 2] * dA * (1 - a)) / outA
  d[i + 3] = outA
}

/** 用有符号距离场填充：距离 0 处取 0.5 覆盖率，得到一个像素宽的抗锯齿边 */
function fill(layer, sdf, color, alpha = 1) {
  for (let y = 0; y < layer.h; y++) {
    for (let x = 0; x < layer.w; x++) {
      const px = x + 0.5
      const py = y + 0.5
      const coverage = Math.min(1, Math.max(0, 0.5 - sdf(px, py)))
      if (coverage <= 0) continue
      const rgb = typeof color === 'function' ? color(px, py) : color
      blendPixel(layer, x, y, rgb[0], rgb[1], rgb[2], coverage * alpha)
    }
  }
}

function composite(dst, src, ox, oy) {
  for (let y = 0; y < src.h; y++) {
    for (let x = 0; x < src.w; x++) {
      const i = (y * src.w + x) * 4
      if (src.data[i + 3] <= 0) continue
      blendPixel(dst, x + ox, y + oy, src.data[i], src.data[i + 1], src.data[i + 2], src.data[i + 3])
    }
  }
}

/* ---------------------------------- SDF ---------------------------------- */

const sdEllipse = (cx, cy, rx, ry) => (x, y) =>
  (Math.hypot((x - cx) / rx, (y - cy) / ry) - 1) * Math.min(rx, ry)

const sdCircle = (cx, cy, r) => (x, y) => Math.hypot(x - cx, y - cy) - r

/** 交集：两个圆相减得到一个下弦月，即微笑的嘴 */
const sdSubtract = (a, b) => (x, y) => Math.max(a(x, y), -b(x, y))

function sdPolygon(points) {
  return (px, py) => {
    let d = (px - points[0][0]) ** 2 + (py - points[0][1]) ** 2
    let sign = 1
    for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
      const [xi, yi] = points[i]
      const [xj, yj] = points[j]
      const ex = xj - xi
      const ey = yj - yi
      const wx = px - xi
      const wy = py - yi
      const t = Math.min(Math.max((wx * ex + wy * ey) / (ex * ex + ey * ey), 0), 1)
      const dx = wx - ex * t
      const dy = wy - ey * t
      d = Math.min(d, dx * dx + dy * dy)
      const c1 = py >= yi
      const c2 = py < yj
      const c3 = ex * wy > ey * wx
      if ((c1 && c2 && c3) || (!c1 && !c2 && !c3)) sign = -sign
    }
    return sign * Math.sqrt(d)
  }
}

/* ---------------------------------- 配色 ---------------------------------- */

const rgb = (hex) => [((hex >> 16) & 255) / 255, ((hex >> 8) & 255) / 255, (hex & 255) / 255]
const mix = (a, b, t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t]
const clamp01 = (v) => Math.min(1, Math.max(0, v))

const GREEN_LIGHT = rgb(0x6fe29a)
const GREEN_DARK = rgb(0x049346)
const INK = rgb(0x123027)
const PINK = rgb(0xff9db1)
const WHITE = [1, 1, 1]

/* ---------------------------------- 缓动 ---------------------------------- */
// Spine 的 curve 控制点是「绝对时间 + 绝对数值」，不是 0..1 的归一化值。
// 运行时 SkeletonJson.readCurve 只对 cy 乘以 timeline 的 scale，不做任何归一化，
// 所以这里必须把归一化控制点映射到当前关键帧的实际区间，否则数值区间一大就会严重过冲。
const EASE_IO = [0.4, 0, 0.6, 1] // 易入易出

function curveOf(fromTime, fromValue, toTime, toValue) {
  const [nx1, ny1, nx2, ny2] = EASE_IO
  return [
    fromTime + nx1 * (toTime - fromTime),
    fromValue + ny1 * (toValue - fromValue),
    fromTime + nx2 * (toTime - fromTime),
    fromValue + ny2 * (toValue - fromValue),
  ]
}

/**
 * 把关键帧表转成 Spine 的 JSON 时间轴格式，并自动补上绝对值缓动曲线。
 * @param frames 形如 [{ time, value }] 或 [{ time, x, y }]
 * @param channels 参与插值的字段名，单通道 1 个（曲线 4 个数）、双通道 2 个（曲线 8 个数）
 * @param eased 传 false 得到线性插值（耳朵抖动这类需要「干脆」的动画用得上）
 */
function timeline(frames, channels, eased = true) {
  return frames.map((frame, index) => {
    const next = frames[index + 1]
    const key = { time: frame.time }
    for (const name of channels) key[name] = frame[name]
    if (next && eased) {
      key.curve = channels.flatMap((name) =>
        curveOf(frame.time, frame[name], next.time, next[name]),
      )
    }
    return key
  })
}

/* -------------------------------- 图集区域 -------------------------------- */
// 区域矩形与绘制逻辑共用同一份尺寸定义，避免图集切图和画的内容对不上

const REGIONS = [
  { name: 'body', x: 0, y: 0, w: 120, h: 104 },
  { name: 'head', x: 122, y: 0, w: 96, h: 88 },
  { name: 'ear', x: 220, y: 0, w: 34, h: 34 },
  { name: 'eye', x: 0, y: 106, w: 18, h: 22 },
  { name: 'mouth', x: 22, y: 110, w: 26, h: 14 },
]

const ATLAS_W = 256
const ATLAS_H = 128

function drawBody() {
  const layer = createLayer(120, 104)
  fill(layer, sdEllipse(60, 52, 59, 51), (_x, y) => mix(GREEN_LIGHT, GREEN_DARK, clamp01((y - 4) / 96)))
  // 肚皮
  fill(layer, sdEllipse(60, 74, 29, 16), WHITE, 0.3)
  return layer
}

function drawHead() {
  const layer = createLayer(96, 88)
  fill(layer, sdEllipse(48, 44, 47, 43), (_x, y) => mix(GREEN_LIGHT, GREEN_DARK, clamp01((y - 2) / 84)))
  // 腮红
  fill(layer, sdEllipse(15, 52, 9, 5), PINK, 0.5)
  fill(layer, sdEllipse(81, 52, 9, 5), PINK, 0.5)
  return layer
}

function drawEar() {
  const layer = createLayer(34, 34)
  fill(layer, sdPolygon([[17, 3], [32, 32], [2, 32]]), (_x, y) =>
    mix(GREEN_LIGHT, rgb(0x079c49), clamp01((y - 3) / 29)),
  )
  return layer
}

function drawEye() {
  const layer = createLayer(18, 22)
  fill(layer, sdEllipse(9, 11, 8.4, 10.4), WHITE)
  fill(layer, sdEllipse(9, 12.5, 4.4, 4.6), INK)
  fill(layer, sdEllipse(7, 10, 1.5, 1.6), WHITE, 0.9)
  return layer
}

function drawMouth() {
  const layer = createLayer(26, 14)
  fill(layer, sdSubtract(sdCircle(13, 2, 8), sdCircle(13, -4, 8)), INK)
  return layer
}

const PAINTERS = { body: drawBody, head: drawHead, ear: drawEar, eye: drawEye, mouth: drawMouth }

/* ---------------------------------- PNG ---------------------------------- */

const CRC_TABLE = (() => {
  const table = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    table[n] = c >>> 0
  }
  return table
})()

function crc32(buf) {
  let c = 0xffffffff
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const length = Buffer.alloc(4)
  length.writeUInt32BE(data.length)
  const body = Buffer.concat([Buffer.from(type, 'latin1'), data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(body))
  return Buffer.concat([length, body, crc])
}

/** 输出预乘 alpha 的 RGBA8 PNG（与 atlas 里的 pma: true 对应） */
function encodePng(width, height, layer) {
  const stride = width * 4 + 1
  const raw = Buffer.alloc(stride * height)
  for (let y = 0; y < height; y++) {
    const rowStart = y * stride + 1
    for (let x = 0; x < width; x++) {
      const i = (y * width + x) * 4
      const a = layer.data[i + 3]
      raw[rowStart + x * 4] = Math.round(layer.data[i] * a * 255)
      raw[rowStart + x * 4 + 1] = Math.round(layer.data[i + 1] * a * 255)
      raw[rowStart + x * 4 + 2] = Math.round(layer.data[i + 2] * a * 255)
      raw[rowStart + x * 4 + 3] = Math.round(a * 255)
    }
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(width, 0)
  ihdr.writeUInt32BE(height, 4)
  ihdr[8] = 8 // bit depth
  ihdr[9] = 6 // RGBA
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

/* --------------------------------- 资源文件 -------------------------------- */

function buildAtlas() {
  const lines = [
    'pet.png',
    `size: ${ATLAS_W},${ATLAS_H}`,
    'format: RGBA8888',
    'filter: Linear,Linear',
    'repeat: none',
    'pma: true',
  ]
  for (const region of REGIONS) {
    lines.push(
      region.name,
      '  rotate: false',
      `  xy: ${region.x}, ${region.y}`,
      `  size: ${region.w}, ${region.h}`,
      `  orig: ${region.w}, ${region.h}`,
      '  offset: 0, 0',
      '  index: -1',
    )
  }
  return `${lines.join('\n')}\n`
}

/** 骨骼与附件的尺寸/位置和图集区域一一对应，改结构时两处要一起改 */
function buildSkeleton() {
  const regions = Object.fromEntries(REGIONS.map((r) => [r.name, r]))
  const region = (name) => ({ width: regions[name].w, height: regions[name].h })

  return {
    // 版本号必须与运行时的 major.minor 对齐（当前运行时为 4.3.x）
    skeleton: { spine: '4.3.00', x: 0, y: 0, width: 200, height: 200, fps: 30 },
    // Spine 坐标系 y 轴向上
    bones: [
      { name: 'root' },
      { name: 'body', parent: 'root', y: -34 },
      { name: 'head', parent: 'body', y: 40 },
      { name: 'earL', parent: 'head', x: -24, y: 32, rotation: 16 },
      { name: 'earR', parent: 'head', x: 24, y: 32, rotation: -16 },
      { name: 'eyeL', parent: 'head', x: -15, y: 4 },
      { name: 'eyeR', parent: 'head', x: 15, y: 4 },
      { name: 'mouth', parent: 'head', y: -18 },
    ],
    // 绘制顺序：耳朵在头之前，头会盖住耳朵根部
    slots: [
      { name: 'body', bone: 'body', attachment: 'body' },
      { name: 'earL', bone: 'earL', attachment: 'ear' },
      { name: 'earR', bone: 'earR', attachment: 'ear' },
      { name: 'head', bone: 'head', attachment: 'head' },
      { name: 'eyeL', bone: 'eyeL', attachment: 'eye' },
      { name: 'eyeR', bone: 'eyeR', attachment: 'eye' },
      { name: 'mouth', bone: 'mouth', attachment: 'mouth' },
    ],
    skins: [
      {
        name: 'default',
        attachments: {
          body: { body: region('body') },
          earL: { ear: region('ear') },
          earR: { ear: region('ear') },
          head: { head: region('head') },
          eyeL: { eye: region('eye') },
          eyeR: { eye: region('eye') },
          mouth: { mouth: region('mouth') },
        },
      },
    ],
    animations: {
      // 待机循环：呼吸 + 上下浮动 + 转头 + 错开的耳朵抖动 + 眨眼
      idle: {
        bones: {
          root: {
            translate: timeline(
              [
                { time: 0, x: 0, y: 0 },
                { time: 1.2, x: 0, y: 3 },
                { time: 2.4, x: 0, y: 0 },
              ],
              ['x', 'y'],
            ),
          },
          body: {
            scale: timeline(
              [
                { time: 0, x: 1, y: 1 },
                { time: 1.2, x: 1.03, y: 1.045 },
                { time: 2.4, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
          head: {
            rotate: timeline(
              [
                { time: 0, value: 0 },
                { time: 0.6, value: 1.4 },
                { time: 1.8, value: -1.4 },
                { time: 2.4, value: 0 },
              ],
              ['value'],
            ),
          },
          // 耳朵抖动要「干脆」，用线性插值，缓动反而会软掉
          earL: {
            rotate: timeline(
              [
                { time: 0, value: 0 },
                { time: 1.5, value: 0 },
                { time: 1.65, value: 14 },
                { time: 1.8, value: 0 },
              ],
              ['value'],
              false,
            ),
          },
          earR: {
            rotate: timeline(
              [
                { time: 0, value: 0 },
                { time: 1.9, value: 0 },
                { time: 2.05, value: -14 },
                { time: 2.2, value: 0 },
              ],
              ['value'],
              false,
            ),
          },
          eyeL: {
            scale: timeline(
              [
                { time: 0, x: 1, y: 1 },
                { time: 2.2, x: 1, y: 1 },
                { time: 2.26, x: 1, y: 0.06 },
                { time: 2.32, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
          eyeR: {
            scale: timeline(
              [
                { time: 0, x: 1, y: 1 },
                { time: 2.2, x: 1, y: 1 },
                { time: 2.26, x: 1, y: 0.06 },
                { time: 2.32, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
        },
      },
      // 点击反馈：压扁再弹回，一次性播放，结束帧回到中立姿势以便无缝切回 idle
      tap: {
        bones: {
          body: {
            scale: timeline(
              [
                { time: 0, x: 1.14, y: 0.86 },
                { time: 0.16, x: 0.94, y: 1.1 },
                { time: 0.3, x: 1.02, y: 0.98 },
                { time: 0.45, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
          head: {
            translate: timeline(
              [
                { time: 0, x: 0, y: -4 },
                { time: 0.16, x: 0, y: 2 },
                { time: 0.45, x: 0, y: 0 },
              ],
              ['x', 'y'],
            ),
          },
          earL: {
            rotate: timeline(
              [
                { time: 0, value: -18 },
                { time: 0.16, value: 10 },
                { time: 0.45, value: 0 },
              ],
              ['value'],
            ),
          },
          earR: {
            rotate: timeline(
              [
                { time: 0, value: 18 },
                { time: 0.16, value: -10 },
                { time: 0.45, value: 0 },
              ],
              ['value'],
            ),
          },
          eyeL: {
            scale: timeline(
              [
                { time: 0, x: 1, y: 0.5 },
                { time: 0.16, x: 1, y: 1 },
                { time: 0.45, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
          eyeR: {
            scale: timeline(
              [
                { time: 0, x: 1, y: 0.5 },
                { time: 0.16, x: 1, y: 1 },
                { time: 0.45, x: 1, y: 1 },
              ],
              ['x', 'y'],
            ),
          },
        },
      },
    },
  }
}

/* ---------------------------------- 主流程 --------------------------------- */

const atlas = createLayer(ATLAS_W, ATLAS_H)
for (const region of REGIONS) {
  composite(atlas, PAINTERS[region.name](), region.x, region.y)
}

mkdirSync(OUT_DIR, { recursive: true })
writeFileSync(resolve(OUT_DIR, 'pet.png'), encodePng(ATLAS_W, ATLAS_H, atlas))
writeFileSync(resolve(OUT_DIR, 'pet.atlas'), buildAtlas())
writeFileSync(resolve(OUT_DIR, 'pet.json'), `${JSON.stringify(buildSkeleton(), null, 2)}\n`)

console.log(`已生成演示 Spine 模型 -> ${OUT_DIR}`)

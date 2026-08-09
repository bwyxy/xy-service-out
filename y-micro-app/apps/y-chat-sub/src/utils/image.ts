/**
 * 图片压缩选项
 */
export interface CompressImageOptions {
  /** 最大宽度，超过则等比缩放 */
  maxWidth?: number
  /** 最大高度，超过则等比缩放 */
  maxHeight?: number
  /** 输出图片质量 0~1 */
  quality?: number
  /** 输出格式 */
  type?: 'image/jpeg' | 'image/png' | 'image/webp'
}

const DEFAULT_MAX_SIZE_BYTES = 2 * 1024 * 1024

/**
 * 读取文件为 DataURL
 */
export function readFileAsDataURL(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

/**
 * 使用 canvas 压缩图片
 *
 * 逻辑：
 * 1. 若原图尺寸超过 maxWidth/maxHeight，则等比缩放。
 * 2. 默认输出 JPEG 以降低体积；原图为 PNG 且含透明通道时仍输出 PNG。
 * 3. 通过调节 quality 逼近目标大小（最大 2MB）。
 */
export async function compressImage(
  file: File,
  options: CompressImageOptions = {},
): Promise<File> {
  const {
    maxWidth = 1280,
    maxHeight = 1280,
    quality = 0.8,
    type = file.type === 'image/png' ? 'image/png' : 'image/jpeg',
  } = options

  // 非图片直接返回
  if (!file.type.startsWith('image/')) {
    return file
  }

  const dataUrl = await readFileAsDataURL(file)
  const img = await loadImage(dataUrl)

  let { width, height } = img

  // 等比缩放
  if (width > maxWidth || height > maxHeight) {
    const scale = Math.min(maxWidth / width, maxHeight / height)
    width = Math.floor(width * scale)
    height = Math.floor(height * scale)
  }

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return file
  }

  ctx.drawImage(img, 0, 0, width, height)

  const blob = await canvasToBlob(canvas, type, quality)
  const compressedFile = new File([blob], generateFileName(file.name, type), {
    type,
    lastModified: Date.now(),
  })

  // 若仍超过 2MB，递归降低质量再压缩一次
  if (compressedFile.size > DEFAULT_MAX_SIZE_BYTES && type === 'image/jpeg') {
    const nextQuality = Math.max(0.3, quality - 0.15)
    return compressImage(file, { maxWidth, maxHeight, quality: nextQuality, type })
  }

  return compressedFile
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

function canvasToBlob(
  canvas: HTMLCanvasElement,
  type: string,
  quality: number,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('canvas to blob failed'))
        }
      },
      type,
      quality,
    )
  })
}

function generateFileName(originalName: string, type: string): string {
  const ext = type === 'image/png' ? 'png' : 'jpg'
  const base = originalName.replace(/\.[^/.]+$/, '') || 'image'
  return `${base}_compressed_${Date.now()}.${ext}`
}

/**
 * 检查文件大小是否超过限制
 */
export function isOversize(file: File, maxBytes = DEFAULT_MAX_SIZE_BYTES): boolean {
  return file.size > maxBytes
}

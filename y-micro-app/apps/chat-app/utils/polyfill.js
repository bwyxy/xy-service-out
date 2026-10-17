/**
 * 浏览器 API polyfill
 *
 * uni-app App 端（JSCore / V8 逻辑层）不保证提供 Web 标准 API，而悟空 IM SDK
 * （libs/easyjssdk.js）与项目内的 payload 解码依赖以下全局对象：
 *   - btoa / atob                 Base64 编解码
 *   - TextEncoder / TextDecoder   UTF-8 编解码
 *
 * 缺失时 SDK 会抛出 "No base64 encoding method available"，导致 IM 无法收发消息。
 * 这里仅在缺失时注入纯 JS 实现；H5 端已有原生实现，不受影响。
 */

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

/** 解析全局对象（App 端可能是 global，H5 端是 window） */
const globalObject =
  typeof globalThis !== 'undefined' ? globalThis : typeof global !== 'undefined' ? global : {}

function bytesToBase64(bytes) {
  let result = ''
  for (let i = 0; i < bytes.length; i += 3) {
    const b1 = bytes[i]
    const b2 = bytes[i + 1]
    const b3 = bytes[i + 2]

    result += BASE64_CHARS[b1 >> 2]
    result += BASE64_CHARS[((b1 & 0x03) << 4) | ((b2 || 0) >> 4)]
    result += b2 === undefined ? '=' : BASE64_CHARS[((b2 & 0x0f) << 2) | ((b3 || 0) >> 6)]
    result += b3 === undefined ? '=' : BASE64_CHARS[b3 & 0x3f]
  }
  return result
}

function base64ToBytes(base64) {
  const clean = String(base64).replace(/[^A-Za-z0-9+/]/g, '')
  const bytes = []

  for (let i = 0; i < clean.length; i += 4) {
    const c1 = BASE64_CHARS.indexOf(clean[i])
    const c2 = BASE64_CHARS.indexOf(clean[i + 1])
    const c3 = BASE64_CHARS.indexOf(clean[i + 2])
    const c4 = BASE64_CHARS.indexOf(clean[i + 3])

    if (c1 === -1 || c2 === -1) {
      break
    }

    bytes.push((c1 << 2) | (c2 >> 4))
    if (c3 !== -1) {
      bytes.push(((c2 & 0x0f) << 4) | (c3 >> 2))
    }
    if (c4 !== -1) {
      bytes.push(((c3 & 0x03) << 6) | c4)
    }
  }

  return new Uint8Array(bytes)
}

/** 字符串 → UTF-8 字节 */
function utf8Encode(str) {
  const bytes = []

  for (let i = 0; i < str.length; i++) {
    let code = str.charCodeAt(i)

    // 代理对（emoji 等）合并为一个码点
    if (code >= 0xd800 && code <= 0xdbff && i + 1 < str.length) {
      const next = str.charCodeAt(i + 1)
      if (next >= 0xdc00 && next <= 0xdfff) {
        code = ((code - 0xd800) << 10) + (next - 0xdc00) + 0x10000
        i++
      }
    }

    if (code < 0x80) {
      bytes.push(code)
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f))
    } else if (code < 0x10000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f))
    } else {
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f),
      )
    }
  }

  return new Uint8Array(bytes)
}

/** UTF-8 字节 → 字符串 */
function utf8Decode(bytes) {
  let result = ''
  let i = 0

  while (i < bytes.length) {
    const b1 = bytes[i++]
    let code

    if (b1 < 0x80) {
      code = b1
    } else if (b1 < 0xe0) {
      code = ((b1 & 0x1f) << 6) | (bytes[i++] & 0x3f)
    } else if (b1 < 0xf0) {
      code = ((b1 & 0x0f) << 12) | ((bytes[i++] & 0x3f) << 6) | (bytes[i++] & 0x3f)
    } else {
      code =
        ((b1 & 0x07) << 18) |
        ((bytes[i++] & 0x3f) << 12) |
        ((bytes[i++] & 0x3f) << 6) |
        (bytes[i++] & 0x3f)
    }

    if (code > 0xffff) {
      code -= 0x10000
      result += String.fromCharCode(0xd800 + (code >> 10), 0xdc00 + (code & 0x3ff))
    } else {
      result += String.fromCharCode(code)
    }
  }

  return result
}

class TextEncoderPolyfill {
  encode(input) {
    return utf8Encode(String(input))
  }
}

class TextDecoderPolyfill {
  constructor(encoding) {
    this.encoding = encoding || 'utf-8'
  }

  decode(input) {
    if (input === undefined || input === null) {
      return ''
    }
    if (input instanceof Uint8Array) {
      return utf8Decode(input)
    }
    if (input instanceof ArrayBuffer) {
      return utf8Decode(new Uint8Array(input))
    }
    if (ArrayBuffer.isView(input)) {
      return utf8Decode(new Uint8Array(input.buffer, input.byteOffset, input.byteLength))
    }
    return String(input)
  }
}

if (typeof globalObject.btoa !== 'function') {
  globalObject.btoa = function btoa(binary) {
    const str = String(binary)
    const bytes = new Uint8Array(str.length)

    for (let i = 0; i < str.length; i++) {
      bytes[i] = str.charCodeAt(i) & 0xff
    }

    return bytesToBase64(bytes)
  }
}

if (typeof globalObject.atob !== 'function') {
  globalObject.atob = function atob(base64) {
    const bytes = base64ToBytes(base64)
    let binary = ''

    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i])
    }

    return binary
  }
}

if (typeof globalObject.TextEncoder === 'undefined') {
  globalObject.TextEncoder = TextEncoderPolyfill
}

if (typeof globalObject.TextDecoder === 'undefined') {
  globalObject.TextDecoder = TextDecoderPolyfill
}
